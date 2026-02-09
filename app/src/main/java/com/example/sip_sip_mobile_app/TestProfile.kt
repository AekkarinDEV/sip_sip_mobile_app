package com.example.sip_sip_mobile_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.imageview.ShapeableImageView
import com.bumptech.glide.Glide

class TestProfile : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_test_profile)

        // edge to edge
        findViewById<View>(R.id.main)?.let {
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

        // ===== bind views =====
        val tvUsername = findViewById<TextView>(R.id.tvUsername)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val tvUid = findViewById<TextView>(R.id.tvUid)

        val tvGender = findViewById<TextView>(R.id.tvGender)
        val tvWeight = findViewById<TextView>(R.id.tvWeight)
        val tvActivity = findViewById<TextView>(R.id.tvActivity)
        val tvWakeTime = findViewById<TextView>(R.id.tvWakeTime)
        val tvSleepTime = findViewById<TextView>(R.id.tvSleepTime)
        val tvNotify = findViewById<TextView>(R.id.tvNotify)

        val imgAvatar = findViewById<ShapeableImageView>(R.id.imgAvatar)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)

        // ===== โหลดข้อมูล users/{uid} =====
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {

                    tvUsername.text = "Username: ${doc.getString("username") ?: "-"}"
                    tvEmail.text = "Email: ${doc.getString("email") ?: "-"}"
                    tvUid.text = "UID: $uid"

                    val avatarUrl = doc.getString("avatarUrl")
                    if (!avatarUrl.isNullOrEmpty()) {
                        Glide.with(this@TestProfile)
                            .load(avatarUrl)
                            .placeholder(R.drawable.profile)
                            .into(imgAvatar)
                    }
                }
            }


        // ===== โหลดข้อมูล profile/basic =====
        db.collection("users")
            .document(uid)
            .collection("profile")
            .document("basic")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    tvGender.text = "เพศ: ${doc.getString("gender")}"
                    tvWeight.text = "น้ำหนัก: ${doc.getLong("weight")} กก."
                    tvActivity.text = "กิจกรรม: ${doc.getString("activity")}"
                    tvWakeTime.text = "เวลาตื่น: ${doc.getString("wakeTime")}"
                    tvSleepTime.text = "เวลานอน: ${doc.getString("sleepTime")}"
                    tvNotify.text =
                        "แจ้งเตือน: ${if (doc.getBoolean("notify") == true) "เปิด" else "ปิด"}"
                }
            }

        // ===== logout =====
        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, Login::class.java))
            finish()
        }
    }
}
