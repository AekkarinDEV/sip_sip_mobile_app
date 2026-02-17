package com.example.sip_sip_mobile_app

import android.os.Bundle
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
import com.bumptech.glide.Glide
import com.google.android.material.slider.Slider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

data class WaterIntake(val type: String, val volume: Int, val timestamp: String)

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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

        // Setup bottom navigation
        val bottomNavView = findViewById<View>(R.id.layout_bottom_nav)
        val bottomNavManager = BottomNavManager(this, bottomNavView)
        bottomNavManager.setupBottomNavigation()

        loadUserProfile()
        loadTodayIntake()
        setupIntakeButtons()
    }

    private fun setupIntakeButtons() {
        findViewById<CardView>(R.id.cardCoffee).setOnClickListener { logIntake("Coffee Cup", 100) }
        findViewById<CardView>(R.id.cardTea).setOnClickListener { logIntake("Tea Cup", 150) }
        findViewById<CardView>(R.id.cardSmall).setOnClickListener { logIntake("Small Cup", 175) }
        findViewById<CardView>(R.id.cardRegular).setOnClickListener { logIntake("Regular Glass", 250) }
        findViewById<CardView>(R.id.cardLarge).setOnClickListener { logIntake("Large Glass", 350) }
        findViewById<CardView>(R.id.cardCustom).setOnClickListener { showCustomIntakeDialog() }
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
                } else {
                    Toast.makeText(this, "กรุณาระบุปริมาณที่มากกว่า 0", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("ยกเลิก") { dialog, _ ->
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
            "timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
        )

        logDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                logDocRef.update("entries", FieldValue.arrayUnion(entry),
                    "total_intake_ml", FieldValue.increment(volume.toLong()))
            } else {
                val data = hashMapOf(
                    "log_id" to logDocRef.id,
                    "user_id" to user.uid,
                    "date_string" to today,
                    "total_intake_ml" to volume,
                    "goal_ml" to 2000, // Default goal
                    "entries" to listOf(entry)
                )
                logDocRef.set(data)
            }
            loadTodayIntake() // Refresh UI
        }
    }

    private fun loadTodayIntake() {
        val user = auth.currentUser ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val logDocId = "${user.uid}_$today"
        val logDocRef = db.collection("consumptions").document(logDocId)

        logDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val totalIntake = document.getLong("total_intake_ml") ?: 0
                val goal = document.getLong("goal_ml") ?: 2000
                findViewById<TextView>(R.id.tvCurrentAmount).text = totalIntake.toString()
                findViewById<TextView>(R.id.tvGoal).text = " / $goal ml"

                val entries = document.get("entries") as? List<HashMap<String, Any>>
                val layoutRecentEntries = findViewById<LinearLayout>(R.id.layoutRecentEntries)
                layoutRecentEntries.removeAllViews()

                entries?.sortedByDescending { it["timestamp"] as String }?.forEach { entry ->
                    val type = entry["type"] as String
                    val volume = entry["volume_ml"] as Long
                    val timestamp = entry["timestamp"] as String

                    val inflater = LayoutInflater.from(this)
                    val entryView = inflater.inflate(R.layout.view_recent_entry, layoutRecentEntries, false)

                    entryView.findViewById<TextView>(R.id.tvEntryType).text = type
                    entryView.findViewById<TextView>(R.id.tvEntryVolume).text = "$volume ml"
                    entryView.findViewById<TextView>(R.id.tvEntryTime).text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).parse(timestamp))

                    layoutRecentEntries.addView(entryView)
                }
            }
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return

        val tvUsername = findViewById<TextView>(R.id.tvUsername)
        val imgAvatar = findViewById<ImageView>(R.id.imgAvatar)

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val name = document.getString("name") ?: ""
                    val avatarUrl = document.getString("avatarUrl")

                    tvUsername.text = name

                    if (!avatarUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(avatarUrl)
                            .into(imgAvatar)
                    }
                }
            }
    }
}