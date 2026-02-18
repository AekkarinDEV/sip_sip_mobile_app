package com.example.sip_sip_mobile_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.imgLogo)

        // animation โลโก้
        val anim = AnimationUtils.loadAnimation(this, R.anim.logo_anim)
        logo.startAnimation(anim)

        // หน่วงให้ animation เล่น
        Handler(Looper.getMainLooper()).postDelayed({
            checkUser()
        }, 900)
    }

    private fun checkUser() {
        val user = auth.currentUser

        // ❌ ยังไม่ login
        if (user == null) {
            goLogin()
            return
        }

        // ✅ login แล้ว → เช็ค Firestore
        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->

                // ❌ user หาย (ลบ app data / db พัง)
                if (!doc.exists()) {
                    forceLogout()
                    return@addOnSuccessListener
                }

                val completed = doc.getBoolean("setupCompleted") ?: false

                val intent = if (completed) {
                    Intent(this, MainActivity::class.java)
                } else {
                    Intent(this, UserSetup::class.java)
                }

                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                // ❌ network / token error
                forceLogout()
            }
    }

    private fun forceLogout() {
        auth.signOut()
        goLogin()
    }

    private fun goLogin() {
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
