package com.example.sip_sip_mobile_app

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.sip_sip_mobile_app.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPassword : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        // ล้าง Error เมื่อมีการพิมพ์ใหม่
        binding.etEmail.addTextChangedListener {
            binding.tilEmail.error = null
        }

        binding.btnReset.setOnClickListener {
            hideKeyboard()
            val email = binding.etEmail.text.toString().trim()

            if (validateEmail(email)) {
                sendPasswordReset(email)
            }
        }

        binding.btnBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateEmail(email: String): Boolean {
        if (email.isEmpty()) {
            binding.tilEmail.error = "กรุณากรอกอีเมล"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "รูปแบบอีเมลไม่ถูกต้อง"
            return false
        }
        return true
    }

    private fun sendPasswordReset(email: String) {
        val loadingDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        loadingDialog.titleText = "กำลังส่งอีเมล..."
        loadingDialog.setCancelable(false)
        loadingDialog.show()

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                loadingDialog.dismissWithAnimation()
                if (task.isSuccessful) {
                    SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("สำเร็จ!")
                        .setContentText("เราได้ส่งลิงก์รีเซ็ตรหัสผ่านไปยังอีเมลของคุณแล้ว")
                        .setConfirmText("ตกลง")
                        .setConfirmClickListener {
                            it.dismissWithAnimation()
                            finish()
                        }
                        .show()
                } else {
                    SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("เกิดข้อผิดพลาด")
                        .setContentText(task.exception?.message ?: "ไม่สามารถส่งอีเมลได้")
                        .show()
                }
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
