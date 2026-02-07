package com.example.sip_sip_mobile_app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class UserSetupProfile : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var imgAvatar: ShapeableImageView
    private var imageUri: Uri? = null

    private val PICK_IMAGE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_setup_profile)

        // edge to edge
        findViewById<View>(R.id.main)?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }

        // firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val user = auth.currentUser ?: run {
            finish()
            return
        }
        val uid = user.uid

        // views
        imgAvatar = findViewById(R.id.imgAvatar)
        val btnPickImage = findViewById<MaterialButton>(R.id.btnPickImage)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        val btnSkip = findViewById<TextView>(R.id.btnSkip)
        val btnBack = findViewById<TextView>(R.id.btnBack)

        // เลือกรูป (กดปุ่ม / กดรูป)
        btnPickImage.setOnClickListener { pickImage() }
        imgAvatar.setOnClickListener { pickImage() }

        // บันทึก
        btnSave.setOnClickListener {

            btnSave.isEnabled = false
            btnSave.text = "กำลังบันทึก..."

            if (imageUri != null) {
                // มีรูป → อัปโหลด
                uploadImage(uid, btnSave)
            } else {
                // ไม่มีรูป → ต้อง set setupCompleted ด้วย
                db.collection("users")
                    .document(uid)
                    .update("setupCompleted", true)
                    .addOnSuccessListener {
                        startActivity(Intent(this, Settings::class.java))
                        finish()
                    }
            }
        }



        btnSkip.setOnClickListener {
            db.collection("users")
                .document(uid)
                .update("setupCompleted", true)
                .addOnSuccessListener {
                    startActivity(Intent(this, Settings::class.java))
                    finish()
                }
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            imgAvatar.setImageURI(imageUri)
        }
    }



    private fun uploadImage(uid: String, btnSave: MaterialButton) {

        val ref = storage.reference
            .child("profile_images")
            .child(uid)
            .child("avatar_${System.currentTimeMillis()}")

        ref.putFile(imageUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    saveImageUrl(uid, uri.toString())
                }
            }
            .addOnFailureListener {
                btnSave.isEnabled = true
                btnSave.text = "บันทึก"
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
    }



    private fun saveImageUrl(uid: String, url: String) {
        db.collection("users")
            .document(uid)
            .update(
                mapOf(
                    "avatarUrl" to url,
                    "setupCompleted" to true
                )
            )
            .addOnSuccessListener {
                startActivity(Intent(this, Settings::class.java))
                finish()
            }

    }

    private fun goNext() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
