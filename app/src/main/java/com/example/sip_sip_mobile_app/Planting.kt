package com.example.sip_sip_mobile_app

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class Planting : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var lottieTree: LottieAnimationView
    private lateinit var tvCount: TextView
    private lateinit var tvWaterAmount: TextView
    private lateinit var tvStreakCount: TextView
    private lateinit var imgFire: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planting)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        lottieTree = findViewById(R.id.lottieTree)
        tvCount = findViewById(R.id.tvCount)
        tvWaterAmount = findViewById(R.id.tvWaterAmount)
        tvStreakCount = findViewById(R.id.tvStreakCount)
        imgFire = findViewById(R.id.imgFire)

        val cardWaterAction = findViewById<View>(R.id.cardWaterAction)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        listenToPlantData()
        cardWaterAction.setOnClickListener { handleWatering() }

        val bottomNavView = findViewById<View>(R.id.layout_bottom_nav)
        BottomNavManager(this, bottomNavView).setupBottomNavigation()
    }

    private fun listenToPlantData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("plants")
            .whereEqualTo("user_id", uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener
                if (!snapshots.isEmpty) {
                    val doc = snapshots.documents[0]
                    val currentWaterLevel = doc.getLong("current_water_level")?.toInt() ?: 0
                    val wateringCans = doc.getLong("watering_cans_count")?.toInt() ?: 0
                    val growthStage = doc.getLong("growth_stage")?.toInt() ?: 1
                    val lastUpdated = doc.getTimestamp("last_updated")?.toDate() ?: Date()

                    fetchUserStreak(uid)
                    tvCount.text = currentWaterLevel.toString()
                    tvWaterAmount.text = wateringCans.toString()
                    checkTreeHealth(currentWaterLevel, lastUpdated, growthStage)
                } else {
                    createNewPlantEntry(uid)
                }
            }
    }

    private fun fetchUserStreak(uid: String) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val streak = doc.getLong("streak_count")?.toInt() ?: 0
            tvStreakCount.text = streak.toString()
            updateFireIconColor(streak)
        }
    }

    private fun createNewPlantEntry(uid: String) {
        val newPlant = hashMapOf(
            "user_id" to uid,
            "growth_stage" to 1,
            "watering_cans_count" to 0,
            "current_water_level" to 0,
            "last_updated" to com.google.firebase.Timestamp.now()
        )
        db.collection("plants").add(newPlant)
    }

    private fun updateFireIconColor(streak: Int) {
        val colorString = when {
            streak == 0 -> "#9E9E9E"
            streak <= 3 -> "#FFEB3B"
            streak <= 7 -> "#FF9800"
            else -> "#F44336"
        }
        imgFire.setColorFilter(Color.parseColor(colorString), PorterDuff.Mode.SRC_ATOP)
        imgFire.animate().scaleX(if (streak > 7) 1.25f else 1.0f).scaleY(if (streak > 7) 1.25f else 1.0f).setDuration(300).start()
        tvStreakCount.setTextColor(Color.parseColor(colorString))
    }

    private fun checkTreeHealth(currentWater: Int, lastUpdated: Date, stage: Int) {
        val diffInHours = (Date().time - lastUpdated.time) / (1000 * 60 * 60)
        lottieTree.setAnimation(if (stage < 2) "sprout.json" else "tree.json")
        when {
            diffInHours >= 48 -> {
                applyTreeFilter("#8B4513")
                lottieTree.pauseAnimation()
            }
            diffInHours >= 24 -> {
                applyTreeFilter("#D4D4A1")
                lottieTree.speed = 0.5f
                lottieTree.playAnimation()
            }
            else -> {
                applyTreeFilter(null)
                lottieTree.speed = 1.0f
                lottieTree.playAnimation()
            }
        }
    }

    private fun handleWatering() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("plants").whereEqualTo("user_id", uid).get().addOnSuccessListener { snapshots ->
            if (snapshots.isEmpty) return@addOnSuccessListener
            val doc = snapshots.documents[0]
            val docId = doc.id
            val currentBuckets = doc.getLong("watering_cans_count")?.toInt() ?: 0
            val currentLevel = doc.getLong("current_water_level")?.toInt() ?: 0

            if (currentBuckets > 0) {
                val updates = hashMapOf<String, Any>(
                    "current_water_level" to FieldValue.increment(1),
                    "watering_cans_count" to FieldValue.increment(-1),
                    "last_updated" to FieldValue.serverTimestamp()
                )
                if (currentLevel + 1 >= 50) updates["growth_stage"] = FieldValue.increment(1)
                db.collection("plants").document(docId).update(updates).addOnSuccessListener {
                    val pDialog = SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    pDialog.titleText = "รดน้ำเรียบร้อย!"
                    pDialog.contentText = "ต้นไม้สดชื่นขึ้นแล้ว"
                    pDialog.confirmText = "ตกลง"
                    pDialog.show()
                    pDialog.getButton(SweetAlertDialog.BUTTON_CONFIRM).setBackgroundResource(R.drawable.btn_round_green)
                }
            } else {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE).setTitleText("น้ำหมด!").setContentText("ไปดื่มน้ำเพิ่มเพื่อรับฝักบัวนะ").show()
            }
        }
    }

    private fun applyTreeFilter(colorHex: String?) {
        val callback = if (colorHex == null) null
        else LottieValueCallback<android.graphics.ColorFilter>(PorterDuffColorFilter(Color.parseColor(colorHex), PorterDuff.Mode.SRC_ATOP))
        lottieTree.addValueCallback(KeyPath("**"), LottieProperty.COLOR_FILTER, callback)
    }
}