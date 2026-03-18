package com.example.sip_sip_mobile_app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class BottomNavManager(private val activity: Activity, private val bottomNavView: View) {

    fun setupBottomNavigation() {
        val navItems = listOf(
            Triple(R.id.btnHome, R.id.indicatorHome, MainActivity::class.java),
            Triple(R.id.btnStat, R.id.indicatorStat, Statistics::class.java),
            Triple(R.id.btnTree, R.id.indicatorTree, Planting::class.java),
            Triple(R.id.btnSetting, R.id.indicatorSetting, Settings::class.java)
        )

        navItems.forEach { (btnId, indicatorId, targetClass) ->
            val btn = bottomNavView.findViewById<LinearLayout>(btnId)
            val indicator = bottomNavView.findViewById<View>(indicatorId)
            val icon = btn.getChildAt(1) as? ImageView
            val text = btn.getChildAt(2) as? TextView

            val isActive = activity::class.java == targetClass

            if (isActive) {
                indicator.visibility = View.VISIBLE
                icon?.alpha = 1.0f
                text?.setTextColor(Color.parseColor("#212121"))
                text?.paint?.isFakeBoldText = true
            } else {
                indicator.visibility = View.GONE
                icon?.alpha = 0.5f
                text?.setTextColor(Color.parseColor("#888888"))
                text?.paint?.isFakeBoldText = false
            }

            btn.setOnClickListener {
                if (!isActive) {
                    val intent = Intent(activity, targetClass)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    activity.startActivity(intent)
                    if (activity !is MainActivity) {
                        // activity.overridePendingTransition(0, 0) // Optional: smooth transition
                    }
                }
            }
        }
    }
}
