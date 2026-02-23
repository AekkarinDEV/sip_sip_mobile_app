package com.example.sip_sip_mobile_app

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class UserSetup : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_setup)

        // edge to edge
        val mainView: View? = findViewById(R.id.main)
        mainView?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // เช็ค login
        val user = auth.currentUser ?: run {
            finish()
            return
        }
        val uid = user.uid

        // bind views
        val etGender = findViewById<AutoCompleteTextView>(R.id.etGender)
        val etActivity = findViewById<AutoCompleteTextView>(R.id.etActivity)
        val etWeight = findViewById<EditText>(R.id.etWeight)
        val etStartTime = findViewById<EditText>(R.id.etStartTime)
        val etEndTime = findViewById<EditText>(R.id.etEndTime)
        val switchNotify = findViewById<SwitchMaterial>(R.id.switchNotify)
        val btnConfirm = findViewById<MaterialButton>(R.id.btnConfirm)
        val btnBack = findViewById<TextView>(R.id.btnBack)

        // dropdowns
        etGender.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("ชาย", "หญิง", "อื่น ๆ"))
        )
        etActivity.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("ไม่ออกกำลังกาย", "เล็กน้อย", "ปานกลาง", "หนัก"))
        )

        // time picker
        etStartTime.setOnClickListener { showTimePicker { etStartTime.setText(it) } }
        etEndTime.setOnClickListener { showTimePicker { etEndTime.setText(it) } }

        btnBack.setOnClickListener { finish() }

        btnConfirm.setOnClickListener {
            val genderStr = etGender.text.toString()
            val weightStr = etWeight.text.toString()
            val activityStr = etActivity.text.toString()
            val startTime = etStartTime.text.toString()
            val endTime = etEndTime.text.toString()
            val notify = switchNotify.isChecked

            if (genderStr.isEmpty() || weightStr.isEmpty() || activityStr.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
                Toast.makeText(this, "กรุณากรอกข้อมูลให้ครบ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val weightKg = weightStr.toDouble()
            val gender = mapGender(genderStr)
            val activityLevel = mapActivityLevel(activityStr)
            val goalMl = calculateDailyWater(weightKg, gender, activityLevel)

            val profileData = hashMapOf(
                "gender" to genderStr,
                "weight" to weightKg,
                "activity" to activityStr,
                "wakeTime" to startTime,
                "sleepTime" to endTime,
                "notify" to notify,
                "setupCompleted" to false
            )

            db.collection("users").document(uid).update(profileData as Map<String, Any>)
                .addOnSuccessListener {
                    // Create initial consumption for today
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val consumptionData = hashMapOf(
                        "log_id" to "${uid}_$today",
                        "user_id" to uid,
                        "date_string" to today,
                        "total_intake_ml" to 0,
                        "goal_ml" to goalMl,
                        "entries" to emptyList<Map<String, Any>>(),
                    )
                    db.collection("consumptions").document("${uid}_$today").set(consumptionData)

                    startActivity(Intent(this, UserSetupProfile::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showTimePicker(onPicked: (String) -> Unit) {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, h, m -> onPicked(String.format("%02d:%02d", h, m)) },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }
}