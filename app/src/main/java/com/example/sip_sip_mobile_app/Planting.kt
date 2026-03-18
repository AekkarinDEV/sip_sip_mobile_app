package com.example.sip_sip_mobile_app

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.*

class Planting : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var imgTree: ImageView
    private lateinit var tvCount: TextView
    private lateinit var tvWaterAmount: TextView
    private lateinit var tvStreakCount: TextView
    private lateinit var imgFire: ImageView
    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
        db.collection("plants").document(uid)
            .addSnapshotListener { document, e ->
                if (e != null || document == null) return@addSnapshotListener
                if (document.exists()) {
                    val currentWaterLevel = (document.get("current_water_level") as? Number)?.toInt() ?: 0
                    val wateringCans = (document.get("watering_cans_count") as? Number)?.toInt() ?: 0
                    val growthStage = (document.get("growth_stage") as? Number)?.toInt() ?: 1
                    
                    val lastUpdatedRaw = document.get("last_updated")
                    val lastUpdated = when (lastUpdatedRaw) {
                        is com.google.firebase.Timestamp -> lastUpdatedRaw.toDate()
                        is Date -> lastUpdatedRaw
                        else -> Date(0)
                    }

                    checkStreakDecay(uid, lastUpdated)
                    
                    tvCount.text = currentWaterLevel.toString()
                    tvWaterAmount.text = wateringCans.toString()
                    updateTreeDisplay(growthStage, lastUpdated)
                } else {
                    createNewPlantEntry(uid)
                }
            }
        listenToUserData(uid)
    }

    private fun listenToUserData(uid: String) {
        userListener?.remove()
        userListener = db.collection("users").document(uid)
            .addSnapshotListener { document, e ->
                if (e != null || document == null || !document.exists()) return@addSnapshotListener
                
                val streakRaw = document.get("streak_count")
                val streak = when (streakRaw) {
                    is Number -> streakRaw.toInt()
                    is String -> streakRaw.toIntOrNull() ?: 0
                    else -> 0
                }
                
                tvStreakCount.text = streak.toString()
                updateFireIconColor(streak)
            }
    }

    private fun checkStreakDecay(uid: String, lastUpdated: Date) {
        if (lastUpdated.time == 0L) return

        if (!isToday(lastUpdated) && !isYesterday(lastUpdated)) {
            db.collection("users").document(uid).set(mapOf("streak_count" to 0), SetOptions.merge())
        }
    }

    private fun createNewPlantEntry(uid: String) {
        val newPlant = hashMapOf(
            "user_id" to uid,
            "growth_stage" to 1,
            "watering_cans_count" to 0,
            "current_water_level" to 0,
            "last_updated" to com.google.firebase.Timestamp(Date(0))
        )
        db.collection("plants").document(uid).set(newPlant)
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

        if (lastUpdated.time == 0L) {
            imgTree.clearColorFilter()
            return
        }

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
        return when (stage) {
            1 -> 0
            2 -> 3
            3 -> 7
            4 -> 14
            5 -> 24
            6 -> 39
            7 -> 60
            8 -> 90
            else -> 90
        }
    }

    private fun handleWatering() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("plants").document(uid).get().addOnSuccessListener { document ->
            if (!document.exists()) return@addOnSuccessListener
            
            val currentBuckets = (document.get("watering_cans_count") as? Number)?.toInt() ?: 0
            val currentLevel = (document.get("current_water_level") as? Number)?.toInt() ?: 0
            val currentStage = (document.get("growth_stage") as? Number)?.toInt() ?: 1
            
            val lastWateredRaw = document.get("last_updated")
            val lastWatered = when (lastWateredRaw) {
                is com.google.firebase.Timestamp -> lastWateredRaw.toDate()
                is Date -> lastWateredRaw
                else -> Date(0)
            }

            if (currentBuckets > 0) {
                val nextLevel = currentLevel + 1
                val updates = hashMapOf<String, Any>(
                    "current_water_level" to FieldValue.increment(1),
                    "watering_cans_count" to FieldValue.increment(-1),
                    "last_updated" to FieldValue.serverTimestamp()
                )
                
                if (currentStage < 8 && nextLevel >= getThresholdForStage(currentStage + 1)) {
                    updates["growth_stage"] = FieldValue.increment(1)
                }
                
                db.collection("plants").document(uid).update(updates).addOnSuccessListener {
                    updateStreakAfterWatering(uid, lastWatered)
                    
                    imgTree.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction {
                        imgTree.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                    }.start()
                }
            } else {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE).setTitleText("น้ำหมด!").setContentText("ไปดื่มน้ำเพิ่มเพื่อรับฝักบัวนะ").show()
            }
        }
    }

    private fun updateStreakAfterWatering(uid: String, lastWatered: Date) {
        val userRef = db.collection("users").document(uid)
        userRef.get().addOnSuccessListener { userDoc ->
            val streakRaw = userDoc.get("streak_count")
            val currentStreak = when (streakRaw) {
                is Number -> streakRaw.toInt()
                is String -> streakRaw.toIntOrNull() ?: 0
                else -> 0
            }
            
            val newStreak = when {
                isToday(lastWatered) -> currentStreak
                isYesterday(lastWatered) -> currentStreak + 1
                else -> 1
            }
            
            if (newStreak != currentStreak || streakRaw == null) {
                userRef.set(mapOf("streak_count" to newStreak), SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("Planting", "Streak updated for user $uid to $newStreak")
                    }
            }
        }
    }

    private fun isToday(date: Date): Boolean {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = date }
        return now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(date: Date): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val target = Calendar.getInstance().apply { time = date }
        return yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }

    override fun onDestroy() {
        super.onDestroy()
        userListener?.remove()
    }
}
