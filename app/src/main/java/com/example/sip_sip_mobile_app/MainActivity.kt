package com.example.sip_sip_mobile_app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.getkeepsafe.taptargetview.TapTargetView
import com.google.android.material.slider.Slider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var cardButtons: List<CardView>
    private lateinit var waterDropView: WaterDropView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        db = FirebaseFirestore.getInstance()

        waterDropView = findViewById(R.id.waterDropView)

        cardButtons = listOf(
            findViewById(R.id.cardCoffee), findViewById(R.id.cardTea),
            findViewById(R.id.cardSmall), findViewById(R.id.cardRegular),
            findViewById(R.id.cardLarge), findViewById(R.id.cardCustom)
        )

        val bottomNavView = findViewById<View>(R.id.layout_bottom_nav)
        BottomNavManager(this, bottomNavView).setupBottomNavigation()

        loadUserProfile()
        loadTodayIntake()
        setupIntakeButtons()

        // เริ่มต้นการสอนใช้งาน (Tutorial) สำหรับผู้ใช้ใหม่
        showTutorialIfNeeded()
    }

    private fun showTutorialIfNeeded() {
        val sharedPref = getSharedPreferences("SipSipPrefs", Context.MODE_PRIVATE)
        val isTutorialDone = sharedPref.getBoolean("is_main_tutorial_done", false)

        if (!isTutorialDone) {
            val sequence = TapTargetSequence(this)
                .targets(
                    TapTarget.forView(findViewById(R.id.layoutProgressText), "(1/5) เป้าหมายวันนี้", "ดูปริมาณน้ำที่คุณดื่มไปแล้วเทียบกับเป้าหมายรายวันของคุณได้ที่นี่")
                        .outerCircleColor(R.color.blue_light)
                        .targetCircleColor(R.color.white)
                        .titleTextSize(22)
                        .descriptionTextSize(16)
                        .cancelable(false)
                        .tintTarget(true)
                        .transparentTarget(true),

                    TapTarget.forView(findViewById(R.id.waterDropView), "(2/5) สถานะปัจจุบัน", "วงกลมน้ำนี้จะบอกเปอร์เซ็นต์ความก้าวหน้า ยิ่งดื่มเยอะ น้ำยิ่งเต็มวงกลม!")
                        .outerCircleColor(R.color.blue_light)
                        .targetCircleColor(R.color.white)
                        .titleTextSize(22)
                        .descriptionTextSize(16)
                        .cancelable(false)
                        .tintTarget(true)
                        .transparentTarget(true)
                        .targetRadius(80),
                    
                    TapTarget.forView(findViewById(R.id.cardCoffee), "(3/5) บันทึกง่ายๆ", "เลือกขนาดแก้วน้ำที่คุณดื่มเพื่อบันทึกข้อมูลได้ทันที รวดเร็วและสะดวกมาก")
                        .outerCircleColor(R.color.blue_light)
                        .targetCircleColor(R.color.white)
                        .titleTextSize(22)
                        .descriptionTextSize(16)
                        .cancelable(false)
                        .tintTarget(true)
                        .transparentTarget(true),

                    TapTarget.forView(findViewById(R.id.layoutRecentEntries), "(4/5) ประวัติการดื่ม", "ตรวจสอบรายการน้ำที่คุณเพิ่งดื่มไปในวันนี้ได้จากส่วนนี้")
                        .outerCircleColor(R.color.blue_light)
                        .targetCircleColor(R.color.white)
                        .titleTextSize(22)
                        .descriptionTextSize(16)
                        .cancelable(false)
                        .tintTarget(true)
                        .transparentTarget(true)
                        .targetRadius(100),

                    TapTarget.forView(findViewById(R.id.layout_bottom_nav), "(5/5) เมนูเมนูหลัก", "สลับไปดูสถิติ รดน้ำต้นไม้ หรือตั้งค่าโปรไฟล์ส่วนตัวได้ที่แถบเมนูด้านล่าง")
                        .outerCircleColor(R.color.blue_light)
                        .targetCircleColor(R.color.white)
                        .titleTextSize(22)
                        .descriptionTextSize(16)
                        .cancelable(false)
                        .tintTarget(true)
                        .transparentTarget(true)
                        .targetRadius(100)
                )
                .listener(object : TapTargetSequence.Listener {
                    override fun onSequenceFinish() {
                        sharedPref.edit().putBoolean("is_main_tutorial_done", true).apply()
                    }
                    override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {}
                    override fun onSequenceCanceled(lastTarget: TapTarget?) {}
                })
            
            sequence.start()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    private fun setupIntakeButtons() {
        findViewById<CardView>(R.id.cardCoffee).setOnClickListener { confirmIntake(it as CardView, "Coffee Cup", 100) }
        findViewById<CardView>(R.id.cardTea).setOnClickListener { confirmIntake(it as CardView, "Tea Cup", 150) }
        findViewById<CardView>(R.id.cardSmall).setOnClickListener { confirmIntake(it as CardView, "Small Cup", 175) }
        findViewById<CardView>(R.id.cardRegular).setOnClickListener { confirmIntake(it as CardView, "Regular Glass", 250) }
        findViewById<CardView>(R.id.cardLarge).setOnClickListener { confirmIntake(it as CardView, "Large Glass", 350) }
        findViewById<CardView>(R.id.cardCustom).setOnClickListener {
            highlightCard(it as CardView)
            showCustomIntakeDialog()
        }
    }

    private fun highlightCard(selectedCard: CardView?) {
        cardButtons.forEach { it.setCardBackgroundColor(if (it == selectedCard) Color.parseColor("#AED6F1") else Color.WHITE) }
    }

    private fun confirmIntake(card: CardView, type: String, volume: Int) {
        highlightCard(card)
        val pDialog = SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
        pDialog.titleText = "ยืนยันการดื่มน้ำ?"
        pDialog.contentText = "คุณดื่ม $type ($volume ml) ใช่ไหม?"
        pDialog.confirmText = "ใช่"
        pDialog.cancelText = "ไม่ใช่"
        pDialog.showCancelButton(true)

        pDialog.setConfirmClickListener { sDialog ->
            logIntake(type, volume)

            sDialog.setTitleText("สำเร็จ!")
                .setContentText("บันทึกเรียบร้อยแล้ว")
                .setConfirmText("ตกลง")
                .showCancelButton(false)
                .setConfirmClickListener { it.dismissWithAnimation(); highlightCard(null) }
                .changeAlertType(SweetAlertDialog.SUCCESS_TYPE)

            sDialog.getButton(SweetAlertDialog.BUTTON_CONFIRM).setBackgroundResource(R.drawable.btn_round_green)
        }

        pDialog.setCancelClickListener {
            it.cancel()
            highlightCard(null)
        }

        pDialog.show()
        pDialog.getButton(SweetAlertDialog.BUTTON_CONFIRM).setBackgroundResource(R.drawable.btn_round_green)
        pDialog.getButton(SweetAlertDialog.BUTTON_CANCEL).setBackgroundResource(R.drawable.btn_round_red)
    }

    private fun logIntake(type: String, volume: Int) {
        val user = auth.currentUser ?: run {
            Log.e("SipSipError", "User is not logged in!")
            return
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val earnedBuckets = (volume / 200).toLong()

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")

        val entry = hashMapOf(
            "entry_id" to UUID.randomUUID().toString(),
            "type" to type,
            "volume_ml" to volume,
            "timestamp" to sdf.format(Date()),
            "user_id" to user.uid
        )

        db.collection("consumptions").document("${user.uid}_$today")
            .update("entries", FieldValue.arrayUnion(entry), "total_intake_ml", FieldValue.increment(volume.toLong()))
            .addOnFailureListener { e -> Log.e("SipSipError", "Failed to log intake: ${e.message}") }


        val plantRef = db.collection("plants").document(user.uid)
        plantRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                plantRef.update(
                    "watering_cans_count", FieldValue.increment(earnedBuckets),
                    "last_updated", com.google.firebase.Timestamp.now()
                ).addOnFailureListener { e -> Log.e("SipSipError", "Update plant fail: ${e.message}") }
            } else {
                val newPlant = hashMapOf(
                    "user_id" to user.uid,
                    "growth_stage" to 1,
                    "watering_cans_count" to earnedBuckets,
                    "current_water_level" to 0,
                    "last_updated" to com.google.firebase.Timestamp.now()
                )
                plantRef.set(newPlant).addOnFailureListener { e -> Log.e("SipSipError", "Create plant fail: ${e.message}") }
            }
        }.addOnFailureListener { e -> Log.e("SipSipError", "Get plant document fail: ${e.message}") }
    }

    private fun showCustomIntakeDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_intake, null)
        val slider = dialogView.findViewById<Slider>(R.id.sliderCustomVolume)
        val tvSliderValue = dialogView.findViewById<TextView>(R.id.tvSliderValue)
        slider.addOnChangeListener { _, value, _ -> tvSliderValue.text = "${value.toInt()} ml" }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("บันทึก") { _, _ ->
                val volume = slider.value.toInt()
                if (volume > 0) logIntake("Custom", volume)
                highlightCard(null)
            }
            .setNegativeButton("ยกเลิก") { _, _ -> highlightCard(null) }
            .setOnCancelListener { highlightCard(null) }
            .show()
    }

    private fun getIconForType(type: String): Int {
        return when (type) {
            "Coffee Cup" -> R.drawable.coffeecup
            "Tea Cup" -> R.drawable.teacup
            "Small Cup" -> R.drawable.smallcup
            "Regular Glass" -> R.drawable.regularglass
            "Large Glass" -> R.drawable.largeglass
            else -> R.drawable.custom // Default icon
        }
    }

    private fun loadTodayIntake() {
        val user = auth.currentUser ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val layoutRecentEntries = findViewById<LinearLayout>(R.id.layoutRecentEntries)
        val docRef = db.collection("consumptions").document("${user.uid}_$today")

        docRef.addSnapshotListener { document, error ->
            if (error != null) {
                Log.e("SipSipError", "Listen failed.", error)
                return@addSnapshotListener
            }

            if (document != null && document.exists()) {
                val totalIntake = document.getLong("total_intake_ml") ?: 0
                val goal = document.getLong("goal_ml") ?: 2000
                findViewById<TextView>(R.id.tvCurrentAmount).text = totalIntake.toString()
                findViewById<TextView>(R.id.tvGoal).text = " / $goal ml"

                val percentage = ((totalIntake.toFloat() / goal.toFloat()) * 100).toInt()
                waterDropView.setProgress(percentage)

                layoutRecentEntries.removeAllViews()
                val entries = document.get("entries") as? List<Map<String, Any>>
                entries?.reversed()?.forEach { entry ->
                    val type = entry["type"] as? String ?: "Water"
                    val volume = entry["volume_ml"] ?: 0
                    val timestamp = entry["timestamp"] as? String ?: ""

                    val entryView = LayoutInflater.from(this).inflate(R.layout.view_recent_entry, layoutRecentEntries, false)
                    entryView.findViewById<TextView>(R.id.tvEntryType).text = type
                    entryView.findViewById<TextView>(R.id.tvEntryVolume).text = "$volume ml"
                    entryView.findViewById<ImageView>(R.id.imgEntryIcon).setImageResource(getIconForType(type))

                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                        val date = sdf.parse(timestamp)
                        val displaySdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        entryView.findViewById<TextView>(R.id.tvEntryTime).text = date?.let { displaySdf.format(it) } ?: "--:--"
                    } catch (e: Exception) {
                        entryView.findViewById<TextView>(R.id.tvEntryTime).text = "--:--"
                    }
                    layoutRecentEntries.addView(entryView)
                }
            } else {
                db.collection("users").document(user.uid).get().addOnSuccessListener { userDoc ->
                    val goalMl = if (userDoc != null && userDoc.exists()) {
                        val weight = userDoc.getDouble("weight") ?: 70.0
                        val genderStr = userDoc.getString("gender") ?: "ชาย"
                        val activityStr = userDoc.getString("activity") ?: "ไม่ออกกำลังกาย"
                        calculateDailyWater(weight, mapGender(genderStr), mapActivityLevel(activityStr))
                    } else {
                        2000
                    }

                    val consumptionData = hashMapOf(
                        "user_id" to user.uid,
                        "date" to today,
                        "total_intake_ml" to 0,
                        "goal_ml" to goalMl,
                        "entries" to listOf<Map<String, Any>>()
                    )
                    db.collection("consumptions").document("${user.uid}_$today").set(consumptionData)
                }
            }
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                findViewById<TextView>(R.id.tvUsername).text = document.getString("username")
                val avatarUrl = document.getString("avatarUrl")
                if (!avatarUrl.isNullOrEmpty()) {
                    Glide.with(this).load(avatarUrl).into(findViewById<ImageView>(R.id.imgAvatar))
                }
            }
        }
    }

    private fun calculateDailyWater(weight: Double, gender: Int, activityLevel: Int): Int {
        val baseWater = weight * 30
        val genderBonus = if (gender == 1) 500 else 0
        val activityBonus = activityLevel * 400
        return (baseWater + genderBonus + activityBonus).toInt()
    }

    private fun mapGender(gender: String): Int = if (gender == "ชาย") 1 else 0
    private fun mapActivityLevel(activity: String): Int {
        return when (activity) {
            "ออกกำลังกายเบาๆ" -> 1
            "ออกกำลังกายหนัก" -> 2
            else -> 0
        }
    }
}
