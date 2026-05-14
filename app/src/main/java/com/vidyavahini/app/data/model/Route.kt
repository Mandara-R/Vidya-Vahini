package com.vidyavahini.app.data.model

/**
 * Represents a bus route with a name and map of stops.
 * Map key = stopId (e.g. "stop_01"), value = Stop object.
 */
data class Route(
    val name: String = "",
    val stops: Map<String, Stop> = emptyMap()
)
