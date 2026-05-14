package com.vidyavahini.app.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.ValueEventListener
import com.vidyavahini.app.data.model.Breakdown
import com.vidyavahini.app.data.model.BusPing
import com.vidyavahini.app.data.model.Route
import com.vidyavahini.app.data.repository.FirebaseRepository
import com.vidyavahini.app.utils.ETACalculator

/**
 * ViewModel shared by HomeFragment and TrackingFragment.
 * Manages real-time ping listeners and ETA computation.
 * Automatically cleans up listeners in onCleared() to prevent memory leaks.
 */
class TrackingViewModel : ViewModel() {

    private val repo = FirebaseRepository()

    /** Latest bus ping — updates every time a student pings on this route. */
    val latestPing    = MutableLiveData<BusPing>()

    /** Human-readable ETA string — e.g. "Bus expected in 12 minutes". */
    val etaText       = MutableLiveData<String>("Waiting for first ping...")

    /** ETA status badge — "ON TIME", "SOON", "ARRIVING", "PASSED". */
    val etaStatus     = MutableLiveData<String>("")

    /** Active breakdown data (or null if no breakdown). */
    val breakdown     = MutableLiveData<Breakdown?>()

    /** The full route object (name + stops map). */
    val currentRoute  = MutableLiveData<Route>()

    /** Whether a ping request is currently being saved. */
    val isPinging     = MutableLiveData<Boolean>(false)

    private var pingListener: ValueEventListener? = null
    private var activeRouteId  = ""
    private var studentStopOrd = 1

    // ── Route loading ────────────────────────────────────────────────────────

    /** Loads the route object into LiveData (one-shot read). */
    fun loadRoute(routeId: String) {
        activeRouteId = routeId
        repo.getRoute(routeId) { route ->
            route?.let { currentRoute.postValue(it) }
        }
    }

    // ── Real-time listeners ──────────────────────────────────────────────────

    /**
     * Starts listening for live ping + breakdown updates.
     * @param routeId          The student's route
     * @param studentStopOrder The stop order where this student boards (for ETA calc)
     */
    fun startListening(routeId: String, studentStopOrder: Int) {
        activeRouteId  = routeId
        studentStopOrd = studentStopOrder

        pingListener = repo.listenForPings(routeId) { ping ->
            latestPing.postValue(ping)
            // Compute ETA using the stop order from the route map
            val stops    = currentRoute.value?.stops ?: return@listenForPings
            val busOrder = stops[ping.stopId]?.order ?: 0
            val eta      = ETACalculator.calculateETA(routeId, busOrder, studentStopOrder)
            etaText.postValue(ETACalculator.formatETA(eta))
            etaStatus.postValue(ETACalculator.getStatusBadge(eta))
        }

        repo.listenForBreakdown(routeId) { b -> breakdown.postValue(b) }
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    /** Sends a ping from this student's stop — the core crowdsourced action. */
    fun pingBus(stopId: String) {
        isPinging.value = true
        repo.pingBus(activeRouteId, stopId)
        isPinging.postValue(false)
    }

    /** Reports a breakdown on the active route. */
    fun reportBreakdown(message: String) {
        repo.reportBreakdown(activeRouteId, message)
    }

    /** Clears an active breakdown (admin action). */
    fun clearBreakdown() {
        repo.clearBreakdown(activeRouteId)
    }

    // ── Cleanup ──────────────────────────────────────────────────────────────

    /** Removes Firebase listener when ViewModel is destroyed — prevents memory leaks. */
    override fun onCleared() {
        super.onCleared()
        pingListener?.let { repo.removePingListener(activeRouteId, it) }
    }
}
