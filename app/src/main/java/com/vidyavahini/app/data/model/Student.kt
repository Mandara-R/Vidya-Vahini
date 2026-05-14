package com.vidyavahini.app.data.model

/**
 * Student profile stored in Firebase under students/{uid}.
 * parentPhone enables the SMS fallback for Safe-Reach notifications.
 */
data class Student(
    val name: String = "",
    val routeId: String = "",
    val stopId: String = "",
    val parentPhone: String = "",
    val fcmToken: String = ""
)
