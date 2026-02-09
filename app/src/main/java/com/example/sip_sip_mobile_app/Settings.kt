package com.example.sip_sip_mobile_app

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import com.google.firebase.storage.FirebaseStorage


class Settings : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var btnLogout: MaterialButton
    private val storage by lazy { FirebaseStorage.getInstance() }

    // ===== header =====
    private lateinit var imgAvatar: ShapeableImageView
    private lateinit var tvNameView: TextView
    private lateinit var etNameEdit: EditText
    private lateinit var tvEmail: TextView

    // ===== form =====
    private lateinit var etGender: EditText
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
            saveProfile()
        }

        btnBack.setOnClickListener { finish() }
        btnLogout.setOnClickListener {
            logout()
        }

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

    // ================= UI =================

    private fun setEditable(enable: Boolean) {

        if (enable) {
            // ===== activity =====
            etActivityEdit.setText(etActivityView.text.toString(), false)
            etActivityView.visibility = View.GONE
            tilActivityEdit.visibility = View.VISIBLE

            // ===== name =====
            etNameEdit.setText(tvNameView.text.toString())
            tvNameView.visibility = View.GONE
            etNameEdit.visibility = View.VISIBLE
        } else {
            // ===== activity =====
            etActivityView.setText(etActivityEdit.text.toString())
            etActivityView.visibility = View.VISIBLE
            tilActivityEdit.visibility = View.GONE

            // ===== name =====
            tvNameView.text = etNameEdit.text.toString()
            tvNameView.visibility = View.VISIBLE
            etNameEdit.visibility = View.GONE
        }

        imgAvatar.isEnabled = enable
        etWeight.isEnabled = enable
        etStartTime.isEnabled = enable
        etEndTime.isEnabled = enable
        switchNotify.isEnabled = enable

        // ===== buttons =====
        btnSave.visibility = if (enable) View.VISIBLE else View.GONE
        btnCancel.visibility = if (enable) View.VISIBLE else View.GONE
        btnEditProfile.visibility = if (enable) View.GONE else View.VISIBLE

        // ✅ ปุ่มออกจากระบบ (สำคัญ)
        btnLogout.visibility = if (enable) View.GONE else View.VISIBLE
    }


    private fun setupDropdowns() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            listOf("ไม่ออกกำลังกาย", "เล็กน้อย", "ปานกลาง", "หนัก")
        )
        etActivityEdit.setAdapter(adapter)
        etActivityEdit.threshold = 0
        etActivityEdit.setOnClickListener { etActivityEdit.showDropDown() }
    }

    private fun setupTimePickers() {
        etStartTime.setOnClickListener {
            if (etStartTime.isEnabled) showTimePicker { etStartTime.setText(it) }
        }
        etEndTime.setOnClickListener {
            if (etEndTime.isEnabled) showTimePicker { etEndTime.setText(it) }
        }
    }

    // ================= data =================

    private fun cacheOldValues() {
        oldName = tvNameView.text.toString()
        oldActivity = etActivityView.text.toString()
        oldWeight = etWeight.text.toString()
        oldStart = etStartTime.text.toString()
        oldEnd = etEndTime.text.toString()
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
        switchNotify.isChecked = oldNotify
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return
        tvEmail.text = user.email ?: ""

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name")
                    ?: doc.getString("username")
                    ?: ""
                val avatar = doc.getString("avatarUrl")

                tvNameView.text = name
                etNameEdit.setText(name)

                if (!avatar.isNullOrEmpty()) {
                    Glide.with(this).load(avatar).into(imgAvatar)
                }
            }

        db.collection("users").document(user.uid)
            .collection("profile").document("basic")
            .get()
            .addOnSuccessListener { doc ->

                val gender = doc.getString("gender") ?: ""
                val activity = doc.getString("activity") ?: ""

                etGender.setText(gender)
                etActivityView.setText(activity)
                etActivityEdit.setText(activity, false)

                etWeight.setText(doc.getLong("weight")?.toString() ?: "")
                etStartTime.setText(doc.getString("wakeTime") ?: "")
                etEndTime.setText(doc.getString("sleepTime") ?: "")
                switchNotify.isChecked = doc.getBoolean("notify") ?: false
            }

    }
    private fun saveProfileData(uid: String, profileData: HashMap<String, Any>) {
        db.collection("users")
            .document(uid)
            .collection("profile")
            .document("basic")
            .set(profileData)
            .addOnSuccessListener {
                Toast.makeText(this, "บันทึกสำเร็จ", Toast.LENGTH_SHORT).show()
                imageUri = null
                setEditable(false)
            }
            .addOnFailureListener {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
    }


    private fun saveProfile() {
        if (
            etActivityEdit.text.isEmpty() ||
            etWeight.text.isEmpty() ||
            etStartTime.text.isEmpty() ||
            etEndTime.text.isEmpty()
        ) {
            Toast.makeText(this, "กรุณากรอกข้อมูลให้ครบ", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = auth.currentUser?.uid ?: return

        // อัปเดตชื่อ
        db.collection("users").document(uid).update(
            mapOf("name" to etNameEdit.text.toString())
        )

        val profileData: HashMap<String, Any> = hashMapOf(
            "activity" to etActivityEdit.text.toString(),
            "weight" to etWeight.text.toString().toInt(),
            "wakeTime" to etStartTime.text.toString(),
            "sleepTime" to etEndTime.text.toString(),
            "notify" to switchNotify.isChecked
        )


        // ถ้ามีรูปใหม่ → อัปโหลดก่อน
        if (imageUri != null) {
            uploadAvatar(uid, profileData)
        } else {
            saveProfileData(uid, profileData)
        }
    }
    private fun uploadAvatar(uid: String, profileData: HashMap<String, Any>) {
        val ref = storage.reference
            .child("profile_images")
            .child("$uid.jpg")

        ref.putFile(imageUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    db.collection("users")
                        .document(uid)
                        .update("avatarUrl", uri.toString())

                    saveProfileData(uid, profileData)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            imgAvatar.setImageURI(imageUri)
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

    private fun logout() {
        FirebaseAuth.getInstance().signOut()

        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

}
