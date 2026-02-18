package com.example.sip_sip_mobile_app

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // กำหนดรายการปุ่มทั้งหมดเพื่อใช้จัดการสีไฮไลต์
        cardButtons = listOf(
            findViewById(R.id.cardCoffee),
            findViewById(R.id.cardTea),
            findViewById(R.id.cardSmall),
            findViewById(R.id.cardRegular),
            findViewById(R.id.cardLarge),
            findViewById(R.id.cardCustom)
        )

        // Setup bottom navigation
        val bottomNavView = findViewById<View>(R.id.layout_bottom_nav)
        val bottomNavManager = BottomNavManager(this, bottomNavView)
        bottomNavManager.setupBottomNavigation()

        loadUserProfile()
        loadTodayIntake()
        setupIntakeButtons()
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

    // ฟังก์ชันจัดการสีไฮไลต์ปุ่ม (เปลี่ยนเป็นสีฟ้า #AED6F1 เมื่อเลือก)
    private fun highlightCard(selectedCard: CardView) {
        cardButtons.forEach { card ->
            if (card == selectedCard) {
                card.setCardBackgroundColor(Color.parseColor("#AED6F1"))
            } else {
                card.setCardBackgroundColor(Color.WHITE)
            }
        }
    }

    // ฟังก์ชันเด้งถามยืนยันด้วย SweetAlert
    private fun confirmIntake(card: CardView, type: String, volume: Int) {
        highlightCard(card)

        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("ยืนยันการบันทึก?")
            .setContentText("คุณดื่ม $type ปริมาณ $volume ml ใช่หรือไม่?")
            .setConfirmText("ตกลง")
            .setCancelText("ยกเลิก")
            .showCancelButton(true)
            .setConfirmClickListener { sDialog ->
                // เมื่อผู้ใช้กด OK
                logIntake(type, volume)

                // เปลี่ยน Alert เป็นความสำเร็จ
                sDialog.setTitleText("สำเร็จ!")
                    .setContentText("บันทึกการดื่มน้ำแล้ว")
                    .setConfirmText("ตกลง")
                    .showCancelButton(false)
                    .setConfirmClickListener(null) // ปิด Dialog เมื่อกดตกลง
                    .changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
            }
            .setCancelClickListener { sDialog ->
                // เมื่อกดยกเลิก
                sDialog.cancel()
                card.setCardBackgroundColor(Color.WHITE) // คืนสีเดิม
            }
            .show()
    }

    private fun showCustomIntakeDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_intake, null)
        val slider = dialogView.findViewById<Slider>(R.id.sliderCustomVolume)
        val tvSliderValue = dialogView.findViewById<TextView>(R.id.tvSliderValue)

        slider.addOnChangeListener { _, value, _ ->
            tvSliderValue.text = "${value.toInt()} ml"
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("บันทึก") { dialog, _ ->
                val volume = slider.value.toInt()
                if (volume > 0) {
                    logIntake("Custom", volume)
                    // แสดงความสำเร็จแบบสั้นหลังบันทึก Custom
                    SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("สำเร็จ!")
                        .setContentText("บันทึก $volume ml เรียบร้อย")
                        .show()
                } else {
                    Toast.makeText(this, "กรุณาระบุปริมาณที่มากกว่า 0", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("ยกเลิก") { dialog, _ ->
                findViewById<CardView>(R.id.cardCustom).setCardBackgroundColor(Color.WHITE)
                dialog.cancel()
            }
            .create()
            .show()
    }

    private fun logIntake(type: String, volume: Int) {
        val user = auth.currentUser ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val logDocId = "${user.uid}_$today"
        val logDocRef = db.collection("consumptions").document(logDocId)

        val entry = hashMapOf(
            "entry_id" to UUID.randomUUID().toString(),
            "type" to type,
            "volume_ml" to volume,
            "timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date()),
            "user_id" to user.uid // เพิ่มเพื่อให้ Query ได้ง่ายขึ้น
        )

        logDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                logDocRef.update(
                    "entries", FieldValue.arrayUnion(entry),
                    "total_intake_ml", FieldValue.increment(volume.toLong())
                )
            } else {
                val data = hashMapOf(
                    "log_id" to logDocRef.id,
                    "user_id" to user.uid,
                    "date_string" to today,
                    "total_intake_ml" to volume,
                    "goal_ml" to 2000,
                    "entries" to listOf(entry)
                )
                logDocRef.set(data)
            }
            loadTodayIntake() // อัพเดต UI หน้าหลัก
        }
    }

    private fun loadTodayIntake() {
        val user = auth.currentUser ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val logDocId = "${user.uid}_$today"
        val logDocRef = db.collection("consumptions").document(logDocId)

        // ใช้ addSnapshotListener เพื่อความเรียลไทม์
        logDocRef.addSnapshotListener { document, e ->
            if (e != null) return@addSnapshotListener

            if (document != null && document.exists()) {
                val totalIntake = document.getLong("total_intake_ml") ?: 0
                val goal = document.getLong("goal_ml") ?: 2000
                findViewById<TextView>(R.id.tvCurrentAmount).text = totalIntake.toString()
                findViewById<TextView>(R.id.tvGoal).text = " / $goal ml"

                val entries = document.get("entries") as? List<HashMap<String, Any>>
                val layoutRecentEntries = findViewById<LinearLayout>(R.id.layoutRecentEntries)
                layoutRecentEntries.removeAllViews()

                entries?.sortedByDescending { it["timestamp"] as String }?.take(5)?.forEach { entry ->
                    val type = entry["type"] as String
                    val volume = entry["volume_ml"] as Long
                    val timestamp = entry["timestamp"] as String

                    val entryView = LayoutInflater.from(this).inflate(R.layout.view_recent_entry, layoutRecentEntries, false)
                    entryView.findViewById<TextView>(R.id.tvEntryType).text = type
                    entryView.findViewById<TextView>(R.id.tvEntryVolume).text = "$volume ml"

                    try {
                        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).parse(timestamp)
                        entryView.findViewById<TextView>(R.id.tvEntryTime).text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date!!)
                    } catch (e: Exception) {
                        entryView.findViewById<TextView>(R.id.tvEntryTime).text = "--:--"
                    }

                    layoutRecentEntries.addView(entryView)
                }
            }
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    findViewById<TextView>(R.id.tvUsername).text = document.getString("name") ?: "User"
                    val avatarUrl = document.getString("avatarUrl")
                    if (!avatarUrl.isNullOrEmpty()) {
                        Glide.with(this).load(avatarUrl).into(findViewById(R.id.imgAvatar))
                    }
                }
            }
    }
}