package com.example.sip_sip_mobile_app

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class UserSetup : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tilGender: TextInputLayout
    private lateinit var tilWeight: TextInputLayout
    private lateinit var tilActivity: TextInputLayout
    private lateinit var tilStartTime: TextInputLayout
    private lateinit var tilEndTime: TextInputLayout

    private lateinit var etGender: AutoCompleteTextView
    private lateinit var etActivity: AutoCompleteTextView
    private lateinit var etWeight: TextInputEditText
    private lateinit var etStartTime: TextInputEditText
    private lateinit var etEndTime: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_setup)

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

        val user = auth.currentUser ?: run {
            finish()
            return
        }
        val uid = user.uid

        // Bind Views
        tilGender = findViewById(R.id.tilGender)
        tilWeight = findViewById(R.id.tilWeight)
        tilActivity = findViewById(R.id.tilActivity)
        tilStartTime = findViewById(R.id.tilStartTime)
        tilEndTime = findViewById(R.id.tilEndTime)

        etGender = findViewById(R.id.etGender)
        etActivity = findViewById(R.id.etActivity)
        etWeight = findViewById(R.id.etWeight)
        etStartTime = findViewById(R.id.etStartTime)
        etEndTime = findViewById(R.id.etEndTime)

        val switchNotify = findViewById<SwitchMaterial>(R.id.switchNotify)
        val btnConfirm = findViewById<MaterialButton>(R.id.btnConfirm)
        val btnBack = findViewById<TextView>(R.id.btnBack)

        // Dropdowns
        val genders = listOf("ชาย", "หญิง", "อื่น ๆ")
        etGender.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, genders))
        
        val activities = listOf("ไม่ออกกำลังกาย", "เล็กน้อย", "ปานกลาง", "หนัก")
        etActivity.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, activities))

        // Time pickers
        etStartTime.setOnClickListener { showTimePicker { etStartTime.setText(it); tilStartTime.error = null } }
        etEndTime.setOnClickListener { showTimePicker { etEndTime.setText(it); tilEndTime.error = null } }

        // Clear errors on text change
        etWeight.addTextChangedListener { tilWeight.error = null }
        etGender.addTextChangedListener { tilGender.error = null }
        etActivity.addTextChangedListener { tilActivity.error = null }

        btnBack.setOnClickListener { finish() }

        btnConfirm.setOnClickListener {
            hideKeyboard()
            if (validateInputs()) {
                saveProfile(uid, switchNotify.isChecked)
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val gender = etGender.text.toString()
        val weightStr = etWeight.text.toString()
        val activity = etActivity.text.toString()
        val startTime = etStartTime.text.toString()
        val endTime = etEndTime.text.toString()

        if (gender.isEmpty()) {
            tilGender.error = "กรุณาเลือกเพศ"
            isValid = false
        }

        if (weightStr.isEmpty()) {
            tilWeight.error = "กรุณากรอกน้ำหนัก"
            isValid = false
        } else {
            val weight = weightStr.toDoubleOrNull()
            if (weight == null || weight < 30 || weight > 300) {
                tilWeight.error = "กรุณากรอกน้ำหนักที่ถูกต้อง (30-300 กก.)"
                isValid = false
            }
        }

        if (activity.isEmpty()) {
            tilActivity.error = "กรุณาเลือกกิจกรรม"
            isValid = false
        }

        if (startTime.isEmpty()) {
            tilStartTime.error = "กรุณาเลือกเวลา"
            isValid = false
        }

        if (endTime.isEmpty()) {
            tilEndTime.error = "กรุณาเลือกเวลา"
            isValid = false
        } else if (startTime == endTime) {
            tilEndTime.error = "เวลาเข้านอนต้องไม่ซ้ำกับเวลาตื่น"
            isValid = false
        }

        return isValid
    }

    private fun saveProfile(uid: String, notify: Boolean) {
        val genderStr = etGender.text.toString()
        val weightKg = etWeight.text.toString().toDouble()
        val activityStr = etActivity.text.toString()
        val startTime = etStartTime.text.toString()
        val endTime = etEndTime.text.toString()

        val goalMl = calculateDailyWater(weightKg, mapGender(genderStr), mapActivityLevel(activityStr))
        // ปัดเลขให้กลมเป็นหลักสิบเพื่อความสวยงาม
        val roundedGoal = (Math.round(goalMl / 10.0) * 10).toInt()

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
                setupInitialConsumption(uid, roundedGoal)
            }
            .addOnFailureListener {
                Toast.makeText(this, "เกิดข้อผิดพลาด: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupInitialConsumption(uid: String, goalMl: Int) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val docRef = db.collection("consumptions").document("${uid}_$today")
        
        docRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val consumptionData = hashMapOf(
                    "log_id" to "${uid}_$today",
                    "user_id" to uid,
                    "date" to today,
                    "total_intake_ml" to 0,
                    "goal_ml" to goalMl,
                    "entries" to emptyList<Map<String, Any>>(),
                )
                docRef.set(consumptionData).addOnSuccessListener { navigateNext() }
            } else {
                docRef.update("goal_ml", goalMl).addOnSuccessListener { navigateNext() }
            }
        }
    }

    private fun navigateNext() {
        startActivity(Intent(this, UserSetupProfile::class.java))
        finish()
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

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun mapGender(genderStr: String): MainActivity.Gender = when(genderStr) {
        "ชาย" -> MainActivity.Gender.MALE
        else -> MainActivity.Gender.FEMALE
    }

    private fun mapActivityLevel(activityStr: String): MainActivity.ActivityLevel = when(activityStr) {
        "ไม่ออกกำลังกาย" -> MainActivity.ActivityLevel.SEDENTARY
        "เล็กน้อย" -> MainActivity.ActivityLevel.LIGHT
        "ปานกลาง" -> MainActivity.ActivityLevel.MODERATE
        "หนัก" -> MainActivity.ActivityLevel.ACTIVE
        else -> MainActivity.ActivityLevel.SEDENTARY
    }

    private fun calculateDailyWater(weight: Double, gender: MainActivity.Gender, activityLevel: MainActivity.ActivityLevel): Int {
        val baseIntake = weight * 30
        val activityBonus = when (activityLevel) {
            MainActivity.ActivityLevel.SEDENTARY -> 0
            MainActivity.ActivityLevel.LIGHT -> 250
            MainActivity.ActivityLevel.MODERATE -> 500
            MainActivity.ActivityLevel.ACTIVE -> 750
        }
        val genderBonus = if (gender == MainActivity.Gender.MALE) 250 else 0
        return (baseIntake + activityBonus + genderBonus).toInt()
    }
}
