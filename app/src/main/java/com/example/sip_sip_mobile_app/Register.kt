package com.example.sip_sip_mobile_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilPasswordConfirm: TextInputLayout
    
    private lateinit var etUsername: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirm: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        bindViews()

        findViewById<MaterialButton>(R.id.btnRegister).setOnClickListener {
            hideKeyboard()
            if (validateInputs()) {
                registerUser()
            }
        }

        findViewById<TextView>(R.id.btnhaveUser).setOnClickListener {
            finish()
        }
    }

    private fun bindViews() {
        tilUsername = findViewById(R.id.tilUsername)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        tilPasswordConfirm = findViewById(R.id.tilPasswordConfirm)
        
        etUsername = findViewById(R.id.etRegUsername)
        etEmail = findViewById(R.id.etRegEmail)
        etPassword = findViewById(R.id.etRegPassword)
        etConfirm = findViewById(R.id.etRegPasswordConfirm)

        // ล้าง Error เมื่อมีการพิมพ์ใหม่
        etUsername.addTextChangedListener { tilUsername.error = null }
        etEmail.addTextChangedListener { tilEmail.error = null }
        etPassword.addTextChangedListener { tilPassword.error = null }
        etConfirm.addTextChangedListener { tilPasswordConfirm.error = null }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirm = etConfirm.text.toString().trim()

        if (username.isEmpty()) {
            tilUsername.error = "กรุณากรอกชื่อผู้ใช้งาน"
            isValid = false
        }

        if (email.isEmpty()) {
            tilEmail.error = "กรุณากรอกอีเมล"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "รูปแบบอีเมลไม่ถูกต้อง"
            isValid = false
        }

        if (password.isEmpty()) {
            tilPassword.error = "กรุณากรอกรหัสผ่าน"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร"
            isValid = false
        }

        if (confirm.isEmpty()) {
            tilPasswordConfirm.error = "กรุณายืนยันรหัสผ่าน"
            isValid = false
        } else if (password != confirm) {
            tilPasswordConfirm.error = "รหัสผ่านไม่ตรงกัน"
            isValid = false
        }

        return isValid
    }

    private fun registerUser() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        val loadingDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        loadingDialog.titleText = "กำลังสร้างบัญชี..."
        loadingDialog.setCancelable(false)
        loadingDialog.show()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user!!.uid
                val userData = hashMapOf(
                    "username" to username, // แก้จาก "name" เป็น "username" ให้ตรงกับ MainActivity
                    "email" to email,
                    "setupCompleted" to false
                )

                db.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {
                        loadingDialog.dismissWithAnimation()
                        // สมัครสำเร็จ -> ส่งไปหน้า UserSetup ทันที
                        val intent = Intent(this, UserSetup::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener {
                        loadingDialog.dismissWithAnimation()
                        Toast.makeText(this, "บันทึกข้อมูลล้มเหลว: ${'$'}{it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                loadingDialog.dismissWithAnimation()
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("สมัครสมาชิกล้มเหลว")
                    .setContentText(it.message)
                    .show()
            }
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
