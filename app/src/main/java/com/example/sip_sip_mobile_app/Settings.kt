package com.example.sip_sip_mobile_app

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.widget.addTextChangedListener
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class Settings : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var btnLogout: MaterialButton
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    // ===== header =====
    private lateinit var imgAvatar: ShapeableImageView
    private lateinit var tvNameView: TextView
    private lateinit var etNameEdit: EditText
    private lateinit var tvEmail: TextView

    // ===== form =====
    private lateinit var tilGenderView: TextInputLayout
    private lateinit var etGenderView: EditText
    private lateinit var tilGenderEdit: TextInputLayout
    private lateinit var etGenderEdit: AutoCompleteTextView
    
    private lateinit var tilWeightView: TextInputLayout
    private lateinit var etWeightView: EditText
    private lateinit var tilWeightEdit: TextInputLayout
    private lateinit var etWeightEdit: TextInputEditText
    
    private lateinit var tilStartTime: TextInputLayout
    private lateinit var etStartTime: EditText
    private lateinit var tilEndTime: TextInputLayout
    private lateinit var etEndTime: EditText
    private lateinit var switchNotify: SwitchMaterial

    private lateinit var tilActivityView: TextInputLayout
    private lateinit var etActivityView: EditText
    private lateinit var tilActivityEdit: TextInputLayout
    private lateinit var etActivityEdit: AutoCompleteTextView

    // ===== buttons =====
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnBack: TextView

    // ===== state =====
    private var imageUri: Uri? = null

    // cache (cancel)
    private var oldName = ""
    private var oldActivity = ""
    private var oldWeight = ""
    private var oldStart = ""
    private var oldEnd = ""
    private var oldGender = ""
    private var oldNotify = false

    companion object {
        private const val PICK_IMAGE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, 0) 
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        bindViews()
        setupDropdowns()
        setupTimePickers()
        loadProfile()

        setEditable(false)

        imgAvatar.setOnClickListener {
            if (!imgAvatar.isEnabled) return@setOnClickListener
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, PICK_IMAGE)
        }

        btnEditProfile.setOnClickListener {
            cacheOldValues()
            setEditable(true)
        }

        btnCancel.setOnClickListener {
            restoreOldValues()
            setEditable(false)
            clearErrors()
        }

        btnSave.setOnClickListener {
            hideKeyboard()
            if (validateInputs()) {
                confirmSaveProfile()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnLogout.setOnClickListener {
            confirmLogout()
        }

        val bottomNavView = findViewById<View>(R.id.layout_bottom_nav)
        BottomNavManager(this, bottomNavView).setupBottomNavigation()
    }

    private fun bindViews() {
        imgAvatar = findViewById(R.id.imgAvatar)
        tvNameView = findViewById(R.id.tvNameView)
        etNameEdit = findViewById(R.id.etNameEdit)
        tvEmail = findViewById(R.id.tvEmail)
        btnLogout = findViewById(R.id.btnLogout)
        
        tilGenderView = findViewById(R.id.tilGenderView)
        etGenderView = findViewById(R.id.etGenderView)
        tilGenderEdit = findViewById(R.id.tilGenderEdit)
        etGenderEdit = findViewById(R.id.etGenderEdit)
        
        tilWeightView = findViewById(R.id.tilWeightView)
        etWeightView = findViewById(R.id.etWeightView)
        tilWeightEdit = findViewById(R.id.tilWeightEdit)
        etWeightEdit = findViewById(R.id.etWeightEdit)
        
        tilStartTime = findViewById(R.id.tilStartTime)
        etStartTime = findViewById(R.id.etStartTime)
        tilEndTime = findViewById(R.id.tilEndTime)
        etEndTime = findViewById(R.id.etEndTime)
        switchNotify = findViewById(R.id.switchNotify)
        
        tilActivityView = findViewById(R.id.tilActivityView)
        etActivityView = findViewById(R.id.etActivityView)
        tilActivityEdit = findViewById(R.id.tilActivityEdit)
        etActivityEdit = findViewById(R.id.etActivityEdit)
        
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        btnBack = findViewById(R.id.btnBack)

        etWeightEdit.addTextChangedListener { tilWeightEdit.error = null }
        etGenderEdit.addTextChangedListener { tilGenderEdit.error = null }
        etActivityEdit.addTextChangedListener { tilActivityEdit.error = null }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val gender = etGenderEdit.text.toString()
        val weightStr = etWeightEdit.text.toString()
        val activity = etActivityEdit.text.toString()
        val startTime = etStartTime.text.toString()
        val endTime = etEndTime.text.toString()

        if (gender.isEmpty()) {
            tilGenderEdit.error = "กรุณาเลือกเพศ"
            isValid = false
        }

        if (weightStr.isEmpty()) {
            tilWeightEdit.error = "กรุณากรอกน้ำหนัก"
            isValid = false
        } else {
            val weight = weightStr.toDoubleOrNull()
            if (weight == null || weight < 30 || weight > 300) {
                tilWeightEdit.error = "กรุณากรอกน้ำหนักที่ถูกต้อง (30-300 กก.)"
                isValid = false
            }
        }

        if (activity.isEmpty()) {
            tilActivityEdit.error = "กรุณาเลือกกิจกรรม"
            isValid = false
        }

        if (startTime.isEmpty()) Toast.makeText(this, "กรุณาเลือกเวลาตื่น", Toast.LENGTH_SHORT).show()
        if (endTime.isEmpty()) Toast.makeText(this, "กรุณาเลือกเวลานอน", Toast.LENGTH_SHORT).show()

        if (startTime.isNotEmpty() && endTime.isNotEmpty() && startTime == endTime) {
            Toast.makeText(this, "เวลาเข้านอนต้องไม่ซ้ำกับเวลาตื่น", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun clearErrors() {
        tilGenderEdit.error = null
        tilWeightEdit.error = null
        tilActivityEdit.error = null
    }

    private fun confirmSaveProfile() {
        val pDialog = SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
        pDialog.titleText = "ยืนยันการแก้ไข?"
        pDialog.contentText = "คุณต้องการบันทึกข้อมูลใหม่ใช่หรือไม่?"
        pDialog.confirmText = "ใช่"
        pDialog.cancelText = "ไม่ใช่"
        pDialog.showCancelButton(true)

        pDialog.setConfirmClickListener { sDialog ->
            sDialog.dismissWithAnimation()
            saveProfile()
        }

        pDialog.show()

        pDialog.getButton(SweetAlertDialog.BUTTON_CONFIRM).post {
            pDialog.getButton(SweetAlertDialog.BUTTON_CONFIRM).setBackgroundResource(R.drawable.btn_round_green)
            pDialog.getButton(SweetAlertDialog.BUTTON_CANCEL).setBackgroundResource(R.drawable.btn_round_red)
        }
    }

    private fun confirmLogout() {
        val pDialog = SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
        pDialog.titleText = "ออกจากระบบ?"
        pDialog.contentText = "คุณต้องการออกจากระบบใช่หรือไม่?"
        pDialog.confirmText = "ออก"
        pDialog.cancelText = "ยกเลิก"
        pDialog.showCancelButton(true)

        pDialog.setConfirmClickListener { sDialog ->
            sDialog.dismissWithAnimation()
            logout()
        }

        pDialog.show()

        pDialog.getButton(SweetAlertDialog.BUTTON_CONFIRM).post {
            pDialog.getButton(SweetAlertDialog.BUTTON_CONFIRM).setBackgroundResource(R.drawable.btn_round_red)
            pDialog.getButton(SweetAlertDialog.BUTTON_CANCEL).setBackgroundResource(R.drawable.btn_round_green)
        }
    }

    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return

        val genderStr = etGenderEdit.text.toString()
        val weightStr = etWeightEdit.text.toString()
        val activityStr = etActivityEdit.text.toString()

        val weightKg = weightStr.toDouble()
        
        val gender = WaterIntakeCalculator.mapGender(genderStr)
        val activityLevel = WaterIntakeCalculator.mapActivityLevel(activityStr)
        val roundedGoal = WaterIntakeCalculator.calculateDailyWater(weightKg, gender, activityLevel)

        val profileData: HashMap<String, Any> = hashMapOf(
            "username" to etNameEdit.text.toString(),
            "gender" to genderStr,
            "activity" to activityStr,
            "weight" to weightKg,
            "wakeTime" to etStartTime.text.toString(),
            "sleepTime" to etEndTime.text.toString(),
            "notify" to switchNotify.isChecked
        )

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        db.collection("consumptions").document("${uid}_$today").get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                db.collection("consumptions").document("${uid}_$today").update("goal_ml", roundedGoal)
            }
        }

        val loadingDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        loadingDialog.titleText = "กำลังบันทึก..."
        loadingDialog.setCancelable(false)
        loadingDialog.show()

        if (imageUri != null) {
            val ref = storage.reference.child("profile_images").child("$uid.jpg")
            ref.putFile(imageUri!!)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { uri ->
                        profileData["avatarUrl"] = uri.toString()
                        updateFirestore(uid, profileData, loadingDialog)
                    }
                }
                .addOnFailureListener {
                    loadingDialog.dismissWithAnimation()
                    Toast.makeText(this, "อัปโหลดรูปภาพล้มเหลว", Toast.LENGTH_SHORT).show()
                }
        } else {
            updateFirestore(uid, profileData, loadingDialog)
        }
    }

    private fun updateFirestore(uid: String, data: Map<String, Any>, loadingDialog: SweetAlertDialog) {
        db.collection("users").document(uid).update(data)
            .addOnSuccessListener {
                loadingDialog.dismissWithAnimation()
                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("สำเร็จ!")
                    .setContentText("บันทึกข้อมูลเรียบร้อยแล้ว")
                    .setConfirmClickListener { 
                        it.dismissWithAnimation()
                        imageUri = null
                        setEditable(false)
                    }
                    .show()
            }
            .addOnFailureListener {
                loadingDialog.dismissWithAnimation()
                Toast.makeText(this, "บันทึกข้อมูลล้มเหลว: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            imgAvatar.setImageURI(imageUri)
        }
    }

    private fun setEditable(enable: Boolean) {
        val timeBgColor = if (enable) Color.WHITE else Color.parseColor("#F5F5F5")
        val timeTextColor = if (enable) Color.BLACK else Color.parseColor("#777777")

        if (enable) {
            etActivityEdit.setText(etActivityView.text.toString(), false)
            tilActivityView.visibility = View.GONE
            tilActivityEdit.visibility = View.VISIBLE
            
            etGenderEdit.setText(etGenderView.text.toString(), false)
            tilGenderView.visibility = View.GONE
            tilGenderEdit.visibility = View.VISIBLE
            
            etWeightEdit.setText(etWeightView.text.toString())
            tilWeightView.visibility = View.GONE
            tilWeightEdit.visibility = View.VISIBLE
            
            etNameEdit.setText(tvNameView.text.toString())
            tvNameView.visibility = View.GONE
            etNameEdit.visibility = View.VISIBLE
        } else {
            etActivityView.setText(etActivityEdit.text.toString())
            tilActivityView.visibility = View.VISIBLE
            tilActivityEdit.visibility = View.GONE
            
            etGenderView.setText(etGenderEdit.text.toString())
            tilGenderView.visibility = View.VISIBLE
            tilGenderEdit.visibility = View.GONE
            
            etWeightView.setText(etWeightEdit.text.toString())
            tilWeightView.visibility = View.VISIBLE
            tilWeightEdit.visibility = View.GONE
            
            tvNameView.text = etNameEdit.text.toString()
            tvNameView.visibility = View.VISIBLE
            etNameEdit.visibility = View.GONE
        }
        
        imgAvatar.isEnabled = enable
        
        // สำหรับเวลาให้เปิด/ปิดการทำงานและเปลี่ยนสีพื้นหลัง/ข้อความ
        etStartTime.isEnabled = enable
        etEndTime.isEnabled = enable
        etStartTime.setTextColor(timeTextColor)
        etEndTime.setTextColor(timeTextColor)
        
        tilStartTime.boxBackgroundColor = timeBgColor
        tilEndTime.boxBackgroundColor = timeBgColor
        
        tilStartTime.isEnabled = enable
        tilEndTime.isEnabled = enable

        switchNotify.isEnabled = enable
        btnSave.visibility = if (enable) View.VISIBLE else View.GONE
        btnCancel.visibility = if (enable) View.VISIBLE else View.GONE
        btnEditProfile.visibility = if (enable) View.GONE else View.VISIBLE
        btnLogout.visibility = if (enable) View.GONE else View.VISIBLE
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return
        tvEmail.text = user.email ?: ""
        db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
            if (doc != null && doc.exists()) {
                val name = doc.getString("username") ?: doc.getString("name") ?: ""
                tvNameView.text = name
                etNameEdit.setText(name)

                val avatarUrl = doc.getString("avatarUrl")
                if (!avatarUrl.isNullOrEmpty()) {
                    Glide.with(this).load(avatarUrl).into(imgAvatar)
                }

                val gender = doc.getString("gender") ?: ""
                etGenderView.setText(gender)
                etGenderEdit.setText(gender, false)

                val activity = doc.getString("activity") ?: ""
                etActivityView.setText(activity)
                etActivityEdit.setText(activity, false)
                
                val weightValue = doc.get("weight")?.toString() ?: ""
                etWeightView.setText(weightValue)
                etWeightEdit.setText(weightValue)
                
                etStartTime.setText(doc.getString("wakeTime") ?: "")
                etEndTime.setText(doc.getString("sleepTime") ?: "")
                switchNotify.isChecked = doc.getBoolean("notify") ?: false
            }
        }
    }

    private fun cacheOldValues() {
        oldName = tvNameView.text.toString()
        oldActivity = etActivityView.text.toString()
        oldWeight = etWeightView.text.toString()
        oldStart = etStartTime.text.toString()
        oldEnd = etEndTime.text.toString()
        oldGender = etGenderView.text.toString()
        oldNotify = switchNotify.isChecked
    }

    private fun restoreOldValues() {
        tvNameView.text = oldName
        etNameEdit.setText(oldName)
        etActivityView.setText(oldActivity)
        etActivityEdit.setText(oldActivity, false)
        etWeightView.setText(oldWeight)
        etWeightEdit.setText(oldWeight)
        etStartTime.setText(oldStart)
        etEndTime.setText(oldEnd)
        etGenderView.setText(oldGender)
        etGenderEdit.setText(oldGender, false)
        switchNotify.isChecked = oldNotify
    }

    private fun setupDropdowns() {
        val genders = listOf("ชาย", "หญิง", "อื่นๆ")
        etGenderEdit.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, genders))
        val activities = listOf("ไม่ออกกำลังกาย", "เล็กน้อย", "ปานกลาง", "หนัก")
        etActivityEdit.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, activities))
    }

    private fun setupTimePickers() {
        etStartTime.setOnClickListener { if (etStartTime.isEnabled) showTimePicker { etStartTime.setText(it) } }
        etEndTime.setOnClickListener { if (etEndTime.isEnabled) showTimePicker { etEndTime.setText(it) } }
    }

    private fun showTimePicker(onPicked: (String) -> Unit) {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, h, m -> onPicked(String.format("%02d:%02d", h, m)) }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun logout() {
        auth.signOut()
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
