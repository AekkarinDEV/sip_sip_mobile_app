package com.example.sip_sip_mobile_app

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class Planting : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var imgTree: ImageView
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

        imgTree = findViewById(R.id.imgTree)
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
                    updateTreeDisplay(growthStage, lastUpdated)
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

    private fun updateTreeDisplay(stage: Int, lastUpdated: Date) {
        val treeResId = when (stage) {
            1 -> R.drawable.tree_level_01
            2 -> R.drawable.tree_level_02
            3 -> R.drawable.tree_level_03
            4 -> R.drawable.tree_level_04
            5 -> R.drawable.tree_level_05
            6 -> R.drawable.tree_level_06
            7 -> R.drawable.tree_level_07
            else -> R.drawable.tree_level_08
        }
        imgTree.setImageResource(treeResId)

        val diffInHours = (Date().time - lastUpdated.time) / (1000 * 60 * 60)
        when {
            diffInHours >= 48 -> {
                imgTree.setColorFilter(Color.parseColor("#8B4513"), PorterDuff.Mode.MULTIPLY)
            }
            diffInHours >= 24 -> {
                imgTree.setColorFilter(Color.parseColor("#D4D4A1"), PorterDuff.Mode.MULTIPLY)
            }
            else -> {
                imgTree.clearColorFilter()
            }
        }
    }

    private fun getThresholdForStage(stage: Int): Int {
        // Total waterings required to reach the START of each stage
        return when (stage) {
            1 -> 0
            2 -> 3        // +3 from stage 1
            3 -> 7        // +4 from stage 2
            4 -> 14       // +7 from stage 3
            5 -> 24       // +10 from stage 4
            6 -> 39       // +15 from stage 5
            7 -> 60       // +21 from stage 6
            8 -> 90       // +30 from stage 7
            else -> 90
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
            val currentStage = doc.getLong("growth_stage")?.toInt() ?: 1

            if (currentBuckets > 0) {
                val nextLevel = currentLevel + 1
                val updates = hashMapOf<String, Any>(
                    "current_water_level" to FieldValue.increment(1),
                    "watering_cans_count" to FieldValue.increment(-1),
                    "last_updated" to FieldValue.serverTimestamp()
                )
                
                // Check if next level meets the threshold for the next stage
                if (currentStage < 8 && nextLevel >= getThresholdForStage(currentStage + 1)) {
                    updates["growth_stage"] = FieldValue.increment(1)
                }
                
                db.collection("plants").document(docId).update(updates).addOnSuccessListener {
                    val pDialog = SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    pDialog.titleText = "รดน้ำเรียบร้อย!"
                    pDialog.contentText = "ต้นไม้สดชื่นขึ้นแล้ว"
                    pDialog.confirmText = "ตกลง"
                    pDialog.show()
                    pDialog.getButton(SweetAlertDialog.BUTTON_CONFIRM).setBackgroundResource(R.drawable.btn_round_green)
                    
                    imgTree.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction {
                        imgTree.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                    }.start()
                }
            } else {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE).setTitleText("น้ำหมด!").setContentText("ไปดื่มน้ำเพิ่มเพื่อรับฝักบัวนะ").show()
            }
        }
    }
}