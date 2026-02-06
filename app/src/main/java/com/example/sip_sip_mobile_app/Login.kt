package com.example.sip_sip_mobile_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 🔹 ถ้า login ค้างอยู่ → เช็คว่า setup เสร็จยัง
        val user = auth.currentUser
        if (user != null) {
            db.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { doc ->
                    val completed = doc.getBoolean("setupCompleted") ?: false

                    if (completed) {
                        startActivity(Intent(this, Settings::class.java))
                    } else {
                        startActivity(Intent(this, UserSetup::class.java))
                    }
                    finish()
                }
        }

        val etEmail = findViewById<EditText>(R.id.etRegUsername)
        val etPassword = findViewById<EditText>(R.id.etRegEmail)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val btnRegister = findViewById<TextView>(R.id.btnReg)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "กรุณากรอกอีเมลและรหัสผ่าน", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    // 🔹 login สำเร็จ → ไปเช็ค setup
                    db.collection("users")
                        .document(auth.currentUser!!.uid)
                        .get()
                        .addOnSuccessListener { doc ->
                            val completed = doc.getBoolean("setupCompleted") ?: false

                            if (completed) {
                                startActivity(Intent(this, Settings::class.java))
                            } else {
                                startActivity(Intent(this, UserSetup::class.java))
                            }
                            finish()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }
    }
}

