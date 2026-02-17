package com.example.sip_sip_mobile_app

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Setup bottom navigation
        val bottomNavView = findViewById<View>(R.id.layout_bottom_nav)
        val bottomNavManager = BottomNavManager(this, bottomNavView)
        bottomNavManager.setupBottomNavigation()

        loadUserProfile()
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return

        val tvUsername = findViewById<TextView>(R.id.tvUsername)
        val imgAvatar = findViewById<ImageView>(R.id.imgAvatar)

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val name = document.getString("name") ?: ""
                    val avatarUrl = document.getString("avatarUrl")

                    tvUsername.text = name

                    if (!avatarUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(avatarUrl)
                            .into(imgAvatar)
                    }
                }
            }
    }
}