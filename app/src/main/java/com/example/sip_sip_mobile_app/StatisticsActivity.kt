package com.example.sip_sip_mobile_app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class StatisticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        // Setup bottom navigation
        val bottomNavView = findViewById<View>(R.id.layout_bottom_nav)
        val bottomNavManager = BottomNavManager(this, bottomNavView)
        bottomNavManager.setupBottomNavigation()
    }
}