package com.vidyavahini.app.data.model

/**
 * Records an active breakdown event for a route.
 * When active = true, all students on the route are warned.
 */
data class Breakdown(
    val active: Boolean = false,
    val reportedBy: String = "",
    val timestamp: Long = 0L,
    val message: String = ""
)
