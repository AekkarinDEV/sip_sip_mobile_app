package com.example.sip_sip_mobile_app

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.LinearLayout

class BottomNavManager(private val activity: Activity, private val bottomNavView: View) {

    fun setupBottomNavigation() {
        val btnHome = bottomNavView.findViewById<LinearLayout>(R.id.btnHome)
        val btnStat = bottomNavView.findViewById<LinearLayout>(R.id.btnStat)
        val btnTree = bottomNavView.findViewById<LinearLayout>(R.id.btnTree)
        val btnSetting = bottomNavView.findViewById<LinearLayout>(R.id.btnSetting)

        btnHome.setOnClickListener {
            if (activity !is MainActivity) {
                val intent = Intent(activity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
            }
        }

        btnStat.setOnClickListener {
            // Statistics page not yet created
        }

        btnTree.setOnClickListener {
            if (activity !is Planting) {
                val intent = Intent(activity, Planting::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
            }
        }

        btnSetting.setOnClickListener {
            if (activity !is Settings) {
                val intent = Intent(activity, Settings::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
            }
        }
    }
}