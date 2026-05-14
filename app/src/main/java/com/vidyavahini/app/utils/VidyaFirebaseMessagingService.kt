package com.vidyavahini.app.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Handles incoming FCM push notifications and token refreshes.
 * Registered in AndroidManifest.xml as a service with MESSAGING_EVENT intent filter.
 */
class VidyaFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * Called when a push notification arrives.
     * Shows a local notification on the device.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: "Vidya-Vahini"
        val body  = message.notification?.body  ?: "Bus update"

        // Check for breakdown type in data payload
        val isBreakdown = message.data["type"] == "breakdown"
        val routeName   = message.data["routeName"] ?: ""

        if (isBreakdown && routeName.isNotEmpty()) {
            NotificationHelper.showBreakdownAlert(this, routeName, body)
        } else {
            NotificationHelper.showLocalNotification(this, title, body)
        }
    }

    /**
     * Called when FCM generates a new registration token.
     * We persist this token to Firebase so we can send targeted notifications.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance("https://vidya-vahini-20c3d-default-rtdb.asia-southeast1.firebasedatabase.app").reference
            .child("students").child(uid).child("fcmToken").setValue(token)
    }
}
