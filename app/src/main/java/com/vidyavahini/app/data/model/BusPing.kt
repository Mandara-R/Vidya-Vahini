package com.vidyavahini.app.data.model

/**
 * A crowdsourced bus ping — written by one student, read by all students on the route.
 * status: "on_time" | "delayed" | "breakdown"
 */
data class BusPing(
    val stopId: String = "",
    val timestamp: Long = 0L,
    val pinggedBy: String = "",
    val status: String = "on_time"
)
