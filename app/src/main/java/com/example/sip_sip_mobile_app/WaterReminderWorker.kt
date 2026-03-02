package com.example.sip_sip_mobile_app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class WaterReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid ?: return Result.success()

        try {
            // 1. ดึงข้อมูล User (เวลาตื่น/นอน และสถานะเปิดแจ้งเตือน)
            val userDoc = db.collection("users").document(userId).get().await()
            if (!userDoc.exists()) return Result.success()

            val isNotifyEnabled = userDoc.getBoolean("notify") ?: false
            if (!isNotifyEnabled) return Result.success()

            val wakeTimeStr = userDoc.getString("wakeTime") ?: "07:00"
            val sleepTimeStr = userDoc.getString("sleepTime") ?: "22:00"

            // 2. เช็คเวลาปัจจุบันว่าอยู่ในช่วงตื่นไหม
            if (!isCurrentTimeInWindow(wakeTimeStr, sleepTimeStr)) {
                return Result.success()
            }

            // 3. เช็คว่าดื่มน้ำถึงเป้าหรือยัง
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val consumptionDoc = db.collection("consumptions").document("${userId}_$today").get().await()
            
            if (consumptionDoc.exists()) {
                val totalIntake = consumptionDoc.getLong("total_intake_ml") ?: 0L
                val goal = consumptionDoc.getLong("goal_ml") ?: 2000L

                if (totalIntake < goal) {
                    showNotification()
                }
            } else {
                // ถ้ายังไม่มีข้อมูลของวันนี้เลย (ยังไม่ได้ดื่มแก้วแรก) ก็ควรเตือน
                showNotification()
            }

        } catch (e: Exception) {
            Log.e("WaterReminderWorker", "Error: ${e.message}")
            return Result.retry()
        }

        return Result.success()
    }

    private fun isCurrentTimeInWindow(wakeTime: String, sleepTime: String): Boolean {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMin = now.get(Calendar.MINUTE)
        val currentTimeInMins = currentHour * 60 + currentMin

        val wakeParts = wakeTime.split(":")
        val wakeMins = wakeParts[0].toInt() * 60 + wakeParts[1].toInt()

        val sleepParts = sleepTime.split(":")
        val sleepMins = sleepParts[0].toInt() * 60 + sleepParts[1].toInt()

        return if (sleepMins > wakeMins) {
            // ช่วงเวลาปกติ เช่น 07:00 - 22:00
            currentTimeInMins in wakeMins..sleepMins
        } else {
            // ช่วงเวลาข้ามคืน เช่น 22:00 - 06:00
            currentTimeInMins >= wakeMins || currentTimeInMins <= sleepMins
        }
    }

    private fun showNotification() {
        val channelId = "water_reminder_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "แจ้งเตือนดื่มน้ำ", 
                NotificationManager.IMPORTANCE_HIGH // ปรับให้มีความสำคัญสูงเพื่อให้เด้งบนหน้าจอ
            ).apply {
                description = "ช่องทางแจ้งเตือนสำหรับการดื่มน้ำรายวัน"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.smallcup)
            .setColor(ContextCompat.getColor(applicationContext, R.color.blue_light)) // ใส่สีฟ้าให้สวยเหมือนกัน
            .setContentTitle("ได้เวลาดื่มน้ำแล้ว!")
            .setContentText("อย่าลืมดื่มน้ำให้ครบตามเป้าหมาย เพื่อสุขภาพที่ดีนะคะ")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // เด้งเตือนทันที
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
