package com.vidyavahini.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.vidyavahini.app.MainActivity
import com.vidyavahini.app.R

/**
 * Handles all local and remote notification logic.
 * - Local notifications: shown on device via NotificationManager
 * - Remote safe-reach: writes to Firebase → triggers FCM cloud function (to be set up)
 */
object NotificationHelper {

    private const val CHANNEL_BUS_ALERTS    = "vidya_bus_alerts"
    private const val CHANNEL_SAFE_REACH    = "vidya_safe_reach"

    /**
     * Writes a safe-reach event to Firebase.
     * A Cloud Function (or parent's app) listens to this node and sends FCM to the parent.
     */
    fun sendFCMSafeReach(studentName: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance("https://vidya-vahini-20c3d-default-rtdb.asia-southeast1.firebasedatabase.app").reference
            .child("safereach").child(uid).setValue(
                mapOf(
                    "studentName" to studentName,
                    "timestamp"   to System.currentTimeMillis(),
                    "reached"     to true
                )
            )
    }

    /**
     * Shows a local notification on this device.
     * Used for incoming FCM messages when the app is in the foreground.
     */
    fun showLocalNotification(
        context: Context,
        title: String,
        message: String,
        channelId: String = CHANNEL_BUS_ALERTS
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (required for API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_BUS_ALERTS,
                    "Bus Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Real-time bus ping and ETA updates" },
                NotificationChannel(
                    CHANNEL_SAFE_REACH,
                    "Safe Reach",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Safe arrival notifications for parents" }
            )
            channels.forEach { nm.createNotificationChannel(it) }
        }

        // Tap notification → open app
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_bus)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(context.getColor(R.color.primary_orange))
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Shows a breakdown alert notification — high priority, red-tinted.
     */
    fun showBreakdownAlert(context: Context, routeName: String, message: String) {
        showLocalNotification(
            context,
            "⚠️ Bus Breakdown — $routeName",
            message,
            CHANNEL_BUS_ALERTS
        )
    }
}
