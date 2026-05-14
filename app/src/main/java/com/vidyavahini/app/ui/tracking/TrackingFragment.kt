package com.vidyavahini.app.ui.tracking

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.vidyavahini.app.R
import com.vidyavahini.app.data.model.Route
import com.vidyavahini.app.databinding.FragmentTrackingBinding
import com.vidyavahini.app.viewmodel.TrackingViewModel

/**
 * TrackingFragment — the map screen.
 * Shows the full route polyline, stop markers, live bus position, and ETA info.
 * Uses a dark map style to match the app's premium dark theme.
 */
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrackingFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TrackingViewModel by viewModels()

    private var googleMap: GoogleMap? = null
    private var busMarker: Marker? = null
    private var myStopId = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs   = requireContext().getSharedPreferences("vidya", Context.MODE_PRIVATE)
        val routeId = prefs.getString("routeId", "") ?: ""
        myStopId    = prefs.getString("stopId",  "") ?: ""
        val order   = prefs.getInt("stopOrder", 1)

        // Initialize the map
        (childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment)
            .getMapAsync(this)

        viewModel.loadRoute(routeId)
        viewModel.startListening(routeId, order)
        observeViewModel()

        // Ping button on the map screen
        binding.btnPingMap.setOnClickListener {
            viewModel.pingBus(myStopId)
            val pulseAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.ping_pulse)
            binding.btnPingMap.startAnimation(pulseAnim)
            binding.btnPingMap.text = "✅ Pinged!"
            binding.btnPingMap.isEnabled = false
            binding.btnPingMap.postDelayed({
                _binding?.btnPingMap?.text = "🚌 PING"
                _binding?.btnPingMap?.isEnabled = true
            }, 120_000L)
        }
    }

    private fun observeViewModel() {
        viewModel.etaText.observe(viewLifecycleOwner) { binding.tvEta.text = it }

        viewModel.latestPing.observe(viewLifecycleOwner) { ping ->
            val map = googleMap ?: return@observe
            val stop = viewModel.currentRoute.value?.stops?.get(ping.stopId) ?: return@observe

            // Move the bus marker to the latest pinged stop
            busMarker?.remove()
            busMarker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(stop.lat, stop.lng))
                    .title("🚌 Bus is here: ${stop.name}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    .zIndex(2f)
            )
            binding.tvLastPing.text = "Last seen: ${stop.name}"
        }

        viewModel.breakdown.observe(viewLifecycleOwner) { b ->
            binding.cardBreakdown.visibility = if (b?.active == true) View.VISIBLE else View.GONE
            binding.tvBreakdownMsg.text      = b?.message ?: ""
        }

        viewModel.currentRoute.observe(viewLifecycleOwner) { route ->
            googleMap?.let { drawRouteLine(it, route, myStopId) }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Apply dark theme to map
        try {
            val style = MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_dark)
            map.setMapStyle(style)
        } catch (e: Exception) {
            Log.e("TrackingFragment", "Failed to load dark map style: ${e.message}")
        }

        map.uiSettings.isZoomControlsEnabled    = true
        map.uiSettings.isCompassEnabled         = true
        map.uiSettings.isMapToolbarEnabled      = false
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        // Draw route line once map is ready and route data is available
        viewModel.currentRoute.value?.let { drawRouteLine(map, it, myStopId) }
    }

    /**
     * Draws the full route as a blue polyline with circle + pin markers at each stop.
     * The student's boarding stop is highlighted in a different color.
     */
    private fun drawRouteLine(map: GoogleMap, route: Route, studentStopId: String) {
        map.clear()
        busMarker = null

        val sortedStops = route.stops.entries.sortedBy { it.value.order }
        val latLngs     = sortedStops.map { LatLng(it.value.lat, it.value.lng) }

        if (latLngs.size < 2) return

        // ── Blue route polyline ──────────────────────────────────────────────
        map.addPolyline(
            PolylineOptions()
                .addAll(latLngs)
                .color(Color.parseColor("#5E92F3"))
                .width(12f)
                .geodesic(true)
        )

        // ── Stop markers ─────────────────────────────────────────────────────
        sortedStops.forEach { (stopId, stop) ->
            val isMyStop = (stopId == studentStopId)

            // Filled circle at each stop
            map.addCircle(
                CircleOptions()
                    .center(LatLng(stop.lat, stop.lng))
                    .radius(200.0)
                    .fillColor(
                        if (isMyStop) Color.parseColor("#80FF6D00")  // orange fill for my stop
                        else Color.parseColor("#805E92F3")           // light blue for others
                    )
                    .strokeColor(
                        if (isMyStop) Color.parseColor("#FF6D00")
                        else Color.parseColor("#5E92F3")
                    )
                    .strokeWidth(4f)
                    .zIndex(1f)
            )

            // Pin marker
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(stop.lat, stop.lng))
                    .title("Stop ${stop.order}: ${stop.name}${if (isMyStop) " (Your Stop)" else ""}")
                    .icon(
                        BitmapDescriptorFactory.defaultMarker(
                            if (isMyStop) BitmapDescriptorFactory.HUE_GREEN
                            else BitmapDescriptorFactory.HUE_AZURE
                        )
                    )
            )
        }

        // ── Auto-zoom camera to show the entire route ─────────────────────
        if (latLngs.isNotEmpty()) {
            val bounds = LatLngBounds.Builder().apply { latLngs.forEach { include(it) } }.build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        googleMap = null
        _binding = null
    }
}
