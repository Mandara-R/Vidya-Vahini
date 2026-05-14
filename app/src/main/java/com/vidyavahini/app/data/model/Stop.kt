package com.vidyavahini.app.data.model

/**
 * Represents a single bus stop on a route.
 * Default values are REQUIRED for Firebase deserialization.
 */
data class Stop(
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val order: Int = 0
)
