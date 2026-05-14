package com.vidyavahini.app.utils

/**
 * Pure-Kotlin ETA calculator — no Firebase, no Android dependencies.
 * Uses average travel time between stops to compute arrival estimate.
 * Easily unit-testable.
 */
object ETACalculator {

    /**
     * Average minutes between consecutive stops per route.
     * Tune these values based on real-world measurement once the app is live.
     */
    private val avgMinutesPerStop = mapOf(
        "route_pune_nashik" to 12,
        "route_default"     to 10
    )

    /**
     * Calculates ETA in minutes.
     * @param routeId           The route identifier
     * @param busCurrentOrder   The stop order where the bus was last pinged
     * @param studentStopOrder  The student's boarding stop order
     * @return                  Estimated minutes until bus arrives; 0 if bus has passed
     */
    fun calculateETA(routeId: String, busCurrentOrder: Int, studentStopOrder: Int): Int {
        val avg       = avgMinutesPerStop[routeId] ?: avgMinutesPerStop["route_default"]!!
        val stopsAway = studentStopOrder - busCurrentOrder
        return if (stopsAway > 0) stopsAway * avg else 0
    }

    /**
     * Converts raw minutes into a user-friendly string.
     */
    fun formatETA(minutes: Int): String = when {
        minutes <= 0  -> "🚌 Bus may have already passed your stop"
        minutes <= 2  -> "🚨 Bus arriving NOW — head to your stop!"
        minutes <= 15 -> "⚡ Bus expected in $minutes minutes — get ready!"
        minutes <= 60 -> "🕐 Bus expected in $minutes minutes"
        else          -> "🕐 Bus expected in ${minutes / 60}h ${minutes % 60}m"
    }

    /**
     * Returns a short status badge label.
     */
    fun getStatusBadge(minutes: Int): String = when {
        minutes <= 0  -> "PASSED"
        minutes <= 5  -> "ARRIVING"
        minutes <= 15 -> "SOON"
        else          -> "ON TIME"
    }
}
