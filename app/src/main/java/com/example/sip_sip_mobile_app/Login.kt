package com.example.sip_sip_mobile_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
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

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // หากมีการ Login ค้างไว้ ให้ไปเช็คสถานะการ Setup
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserSetup(currentUser.uid)
        }

        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val btnRegister = findViewById<TextView>(R.id.btnReg)

        // ล้าง Error เมื่อมีการพิมพ์ใหม่
        etEmail.addTextChangedListener { tilEmail.error = null }
        etPassword.addTextChangedListener { tilPassword.error = null }

        btnLogin.setOnClickListener {
            hideKeyboard()
            if (validateInputs()) {
                performLogin()
            }
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

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
        }

        return isValid
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        val loadingDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        loadingDialog.titleText = "กำลังเข้าสู่ระบบ..."
        loadingDialog.setCancelable(false)
        loadingDialog.show()

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                loadingDialog.dismissWithAnimation()
                checkUserSetup(auth.currentUser!!.uid)
            }
            .addOnFailureListener {
                loadingDialog.dismissWithAnimation()
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("เข้าสู่ระบบล้มเหลว")
                    .setContentText(it.message ?: "อีเมลหรือรหัสผ่านไม่ถูกต้อง")
                    .show()
            }
    }

    private fun checkUserSetup(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val completed = doc.getBoolean("setupCompleted") ?: false
                    if (completed) {
                        // ถ้า Setup เสร็จแล้ว ไปหน้า Main
                        startActivity(Intent(this, MainActivity::class.java))
                    } else {
                        // ถ้ายังไม่เสร็จ ไปหน้า Setup
                        startActivity(Intent(this, UserSetup::class.java))
                    }
                } else {
                    // กรณีไม่มีข้อมูลใน Firestore ให้ไปหน้า Setup
                    startActivity(Intent(this, UserSetup::class.java))
                }
                finish()
            }
            .addOnFailureListener {
                // หากดึงข้อมูลล้มเหลว ให้ลองไปหน้า Setup ไว้ก่อน
                startActivity(Intent(this, UserSetup::class.java))
                finish()
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
