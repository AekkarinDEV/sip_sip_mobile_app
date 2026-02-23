package com.example.sip_sip_mobile_app

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.switchmaterial.SwitchMaterial
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
    private lateinit var etGender: AutoCompleteTextView
    private lateinit var etWeight: EditText
    private lateinit var etStartTime: EditText
    private lateinit var etEndTime: EditText
    private lateinit var switchNotify: SwitchMaterial

    private lateinit var etActivityView: EditText
    private lateinit var etActivityEdit: AutoCompleteTextView
    private lateinit var tilActivityEdit: View

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
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        bindViews()
        setupDropdowns()
        setupTimePickers()
        loadProfile()

        setEditable(false)

        // --- ส่วนเลือกรูปภาพ (ทำงานเหมือนเดิม) ---
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
        }

        btnSave.setOnClickListener {
            confirmSaveProfile()
        }

        btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            confirmLogout()
        }

        val bottomNavView = findViewById<View>(R.id.layout_bottom_nav)
        val bottomNavManager = BottomNavManager(this, bottomNavView)
        bottomNavManager.setupBottomNavigation()
    }

    private fun bindViews() {
        imgAvatar = findViewById(R.id.imgAvatar)
        tvNameView = findViewById(R.id.tvNameView)
        etNameEdit = findViewById(R.id.etNameEdit)
        tvEmail = findViewById(R.id.tvEmail)
        btnLogout = findViewById(R.id.btnLogout)
        etGender = findViewById(R.id.etGender)
        etWeight = findViewById(R.id.etWeight)
        etStartTime = findViewById(R.id.etStartTime)
        etEndTime = findViewById(R.id.etEndTime)
        switchNotify = findViewById(R.id.switchNotify)
        etActivityView = findViewById(R.id.etActivityView)
        etActivityEdit = findViewById(R.id.etActivityEdit)
        tilActivityEdit = findViewById(R.id.tilActivityEdit)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        btnBack = findViewById(R.id.btnBack)
    }

    // ================= SweetAlert Logic (ปรับปรุงปุ่มเขียว/แดง มนๆ) =================

    private fun confirmSaveProfile() {
        if (etActivityEdit.text.isEmpty() || etWeight.text.isEmpty() ||
            etStartTime.text.isEmpty() || etEndTime.text.isEmpty()) {
            Toast.makeText(this, "กรุณากรอกข้อมูลให้ครบ", Toast.LENGTH_SHORT).show()
            return
        }

        val pDialog = SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
        pDialog.titleText = "ยืนยันการแก้ไข?"
        pDialog.contentText = "คุณต้องการบันทึกข้อมูลใหม่ใช่หรือไม่?"
        pDialog.confirmText = "ใช่"
        pDialog.cancelText = "ไม่ใช่"
        pDialog.showCancelButton(true)

        pDialog.setConfirmClickListener { sDialog ->
            saveProfile() // ฟังก์ชันบันทึกข้อมูล (รวมถึงรูปภาพ)

            sDialog.setTitleText("สำเร็จ!")
                .setContentText("บันทึกข้อมูลเรียบร้อยแล้ว")
                .setConfirmText("ตกลง")
                .showCancelButton(false)
                .setConfirmClickListener(null)
                .changeAlertType(SweetAlertDialog.SUCCESS_TYPE)

            sDialog.getButton(SweetAlertDialog.BUTTON_CONFIRM).post {
                sDialog.getButton(SweetAlertDialog.BUTTON_CONFIRM).setBackgroundResource(R.drawable.btn_round_green)
            }
        }

        pDialog.show()

        pDialog.getButton(SweetAlertDialog.BUTTON_CONFIRM).post {
            val btnConfirm = pDialog.getButton(SweetAlertDialog.BUTTON_CONFIRM)
            val btnCancel = pDialog.getButton(SweetAlertDialog.BUTTON_CANCEL)
            btnConfirm.setBackgroundResource(R.drawable.btn_round_green)
            btnCancel.setBackgroundResource(R.drawable.btn_round_red)
            btnConfirm.setTextColor(Color.WHITE)
            btnCancel.setTextColor(Color.WHITE)
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
            pDialog.getButton(SweetAlertDialog.BUTTON_CONFIRM).setTextColor(Color.WHITE)
            pDialog.getButton(SweetAlertDialog.BUTTON_CANCEL).setTextColor(Color.WHITE)
        }
    }

    // ================= Profile & Image Logic (Refactored) =================

    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return

        val genderStr = etGender.text.toString()
        val weightStr = etWeight.text.toString()
        val activityStr = etActivityEdit.text.toString()

        val weightKg = weightStr.toDouble()
        val gender = mapGender(genderStr)
        val activityLevel = mapActivityLevel(activityStr)
        val goalMl = calculateDailyWater(weightKg, gender, activityLevel)

        val profileData: HashMap<String, Any> = hashMapOf(
            "name" to etNameEdit.text.toString(),
            "gender" to genderStr,
            "activity" to activityStr,
            "weight" to weightKg,
            "wakeTime" to etStartTime.text.toString(),
            "sleepTime" to etEndTime.text.toString(),
            "notify" to switchNotify.isChecked
        )

        // Update consumption goal
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        db.collection("consumptions").document("${uid}_$today").update("goal_ml", goalMl)

        if (imageUri != null) {
            uploadAvatar(uid, profileData)
        } else {
            saveProfileData(uid, profileData)
        }
    }

    private fun uploadAvatar(uid: String, profileData: HashMap<String, Any>) {
        val ref = storage.reference.child("profile_images").child("$uid.jpg")
        ref.putFile(imageUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    profileData["avatarUrl"] = uri.toString()
                    saveProfileData(uid, profileData)
                }
            }
    }

    private fun saveProfileData(uid: String, profileData: Map<String, Any>) {
        db.collection("users").document(uid)
            .update(profileData)
            .addOnSuccessListener {
                imageUri = null
                setEditable(false)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            imgAvatar.setImageURI(imageUri) // แสดงรูปที่เลือกทันที
        }
    }

    private fun setEditable(enable: Boolean) {
        if (enable) {
            etActivityEdit.setText(etActivityView.text.toString(), false)
            etActivityView.visibility = View.GONE
            tilActivityEdit.visibility = View.VISIBLE
            etNameEdit.setText(tvNameView.text.toString())
            tvNameView.visibility = View.GONE
            etNameEdit.visibility = View.VISIBLE
        } else {
            etActivityView.setText(etActivityEdit.text.toString())
            etActivityView.visibility = View.VISIBLE
            tilActivityEdit.visibility = View.GONE
            tvNameView.text = etNameEdit.text.toString()
            tvNameView.visibility = View.VISIBLE
            etNameEdit.visibility = View.GONE
        }
        imgAvatar.isEnabled = enable
        etGender.isEnabled = enable
        etWeight.isEnabled = enable
        etStartTime.isEnabled = enable
        etEndTime.isEnabled = enable
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
                val name = doc.getString("name") ?: doc.getString("username") ?: ""
                tvNameView.text = name
                etNameEdit.setText(name)
                
                val avatarUrl = doc.getString("avatarUrl")
                if (!avatarUrl.isNullOrEmpty()) {
                    Glide.with(this).load(avatarUrl).into(imgAvatar)
                }

                val gender = doc.getString("gender") ?: ""
                etGender.setText(gender, false)

                val activity = doc.getString("activity") ?: ""
                etActivityView.setText(activity)
                etActivityEdit.setText(activity, false)
                etWeight.setText(doc.getLong("weight")?.toString() ?: "")
                etStartTime.setText(doc.getString("wakeTime") ?: "")
                etEndTime.setText(doc.getString("sleepTime") ?: "")
                switchNotify.isChecked = doc.getBoolean("notify") ?: false
            }
        }
    }

    private fun cacheOldValues() {
        oldName = tvNameView.text.toString()
        oldActivity = etActivityView.text.toString()
        oldWeight = etWeight.text.toString()
        oldStart = etStartTime.text.toString()
        oldEnd = etEndTime.text.toString()
        oldGender = etGender.text.toString()
        oldNotify = switchNotify.isChecked
    }

    private fun restoreOldValues() {
        tvNameView.text = oldName
        etNameEdit.setText(oldName)
        etActivityView.setText(oldActivity)
        etActivityEdit.setText(oldActivity, false)
        etWeight.setText(oldWeight)
        etStartTime.setText(oldStart)
        etEndTime.setText(oldEnd)
        etGender.setText(oldGender)
        switchNotify.isChecked = oldNotify
    }

    private fun setupDropdowns() {
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("ชาย", "หญิง", "อื่นๆ"))
        etGender.setAdapter(genderAdapter)
        etGender.setOnClickListener { etGender.showDropDown() }

        val activityAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("ไม่ออกกำลังกาย", "เล็กน้อย", "ปานกลาง", "หนัก"))
        etActivityEdit.setAdapter(activityAdapter)
        etActivityEdit.setOnClickListener { etActivityEdit.showDropDown() }
    }

    private fun setupTimePickers() {
        etStartTime.setOnClickListener { if (etStartTime.isEnabled) showTimePicker { etStartTime.setText(it) } }
        etEndTime.setOnClickListener { if (etEndTime.isEnabled) showTimePicker { etEndTime.setText(it) } }
    }

    private fun showTimePicker(onPicked: (String) -> Unit) {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, h, m -> onPicked(String.format("%02d:%02d", h, m)) }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun logout() {
        auth.signOut()
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}