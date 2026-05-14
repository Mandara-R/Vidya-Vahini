package com.vidyavahini.app.ui.home

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.vidyavahini.app.R
import com.vidyavahini.app.databinding.FragmentHomeBinding
import com.vidyavahini.app.viewmodel.TrackingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

/**
 * HomeFragment — the primary dashboard.
 * Shows: route name, last ping info, ETA, PING button, breakdown button, nav actions.
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TrackingViewModel by viewModels()

    private var routeId = ""
    private var stopId = ""
    private var stopOrder = 1

    private var isPingCooldown = false
    private var cooldownTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)

        // Fade animation
        val fadeIn =
            AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)

        binding.root.startAnimation(fadeIn)

        loadPreferences()

        setupUI()

        observeViewModel()

        startTracking()
    }

    /**
     * Loads locally saved student preferences.
     * If nothing exists yet, default demo values are used.
     */
    private fun loadPreferences() {

        val prefs =
            requireContext().getSharedPreferences(
                "vidya",
                Context.MODE_PRIVATE
            )

        // DEFAULT ROUTE
        routeId =
            prefs.getString("routeId", "route_500d")
                ?: "route_500d"

        // DEFAULT STOP
        stopId =
            prefs.getString("stopId", "stop_01")
                ?: "stop_01"

        stopOrder =
            prefs.getInt("stopOrder", 1)

        val name =
            prefs.getString("name", "Student")
                ?: "Student"

        binding.tvGreeting.text =
            "Hello, $name 👋"

        binding.tvRouteName.text =
            "Loading route..."

        // DEBUG LOGS
        android.util.Log.d(
            "Firebase",
            "Route ID = $routeId"
        )

        android.util.Log.d(
            "Firebase",
            "Stop ID = $stopId"
        )
    }

    private fun setupUI() {

        // ── PING BUTTON ──────────────────────────────

        binding.btnPing.setOnClickListener {

            if (isPingCooldown) return@setOnClickListener

            triggerPing()
        }

        // ── BREAKDOWN BUTTON ─────────────────────────

        binding.btnBreakdown.setOnClickListener {

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("⚠️ Report Bus Breakdown?")
                .setMessage(
                    "This will alert all students on your route."
                )

                .setPositiveButton("Yes, Report") { _, _ ->

                    viewModel.reportBreakdown(
                        "Bus has broken down — please find alternatives!"
                    )

                    Snackbar.make(
                        binding.root,
                        "Breakdown reported successfully",
                        Snackbar.LENGTH_LONG
                    ).show()
                }

                .setNegativeButton("Cancel", null)

                .show()
        }

        // ── MAP BUTTON ───────────────────────────────

        binding.btnMap.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Tracking screen coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }

        // ── SAFE REACH BUTTON ────────────────────────

        binding.btnSafeReach.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Safe Reach feature coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }

        // ── LOGOUT BUTTON ────────────────────────────

        binding.btnLogout.setOnClickListener {

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")

                .setPositiveButton("Sign Out") { _, _ ->

                    FirebaseAuth.getInstance().signOut()

                    requireContext()
                        .getSharedPreferences(
                            "vidya",
                            Context.MODE_PRIVATE
                        )
                        .edit()
                        .clear()
                        .apply()

                    Toast.makeText(
                        requireContext(),
                        "Logged Out",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                .setNegativeButton("Cancel", null)

                .show()
        }

        // ── DEMO SIMULATION ──────────────────────────

        binding.tvRouteName.setOnLongClickListener {

            val route = viewModel.currentRoute.value

            if (route == null) {

                Toast.makeText(
                    requireContext(),
                    "Route not loaded yet",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnLongClickListener true
            }

            Toast.makeText(
                requireContext(),
                "Starting Live Demo Simulation...",
                Toast.LENGTH_LONG
            ).show()

            val sortedStops =
                route.stops.entries.sortedBy { it.value.order }

            viewLifecycleOwner.lifecycleScope.launch {

                for ((sid, stop) in sortedStops) {

                    viewModel.pingBus(sid)

                    Snackbar.make(
                        binding.root,
                        "Demo: Bus reached ${stop.name}",
                        Snackbar.LENGTH_SHORT
                    ).show()

                    delay(5000)
                }

                Snackbar.make(
                    binding.root,
                    "Demo Completed",
                    Snackbar.LENGTH_LONG
                ).show()
            }

            true
        }
    }

    /**
     * Sends bus ping to Firebase.
     */
    private fun triggerPing() {

        isPingCooldown = true

        // SEND PING TO FIREBASE
        viewModel.pingBus(stopId)

        // HAPTIC FEEDBACK
        binding.btnPing.performHapticFeedback(
            HapticFeedbackConstants.LONG_PRESS
        )

        try {

            val vibrator =
                requireContext()
                    .getSystemService(Context.VIBRATOR_SERVICE)
                        as Vibrator

            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    100,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )

        } catch (_: Exception) {
        }

        // BUTTON ANIMATION
        val pulseAnim =
            AnimationUtils.loadAnimation(
                requireContext(),
                R.anim.ping_pulse
            )

        binding.btnPing.startAnimation(pulseAnim)

        binding.btnPing.isEnabled = false

        Snackbar.make(
            binding.root,
            "Bus ping sent successfully",
            Snackbar.LENGTH_SHORT
        ).show()

        // TIMER
        cooldownTimer?.cancel()

        cooldownTimer =
            object : CountDownTimer(120000L, 1000L) {

                override fun onTick(millisUntilFinished: Long) {

                    val seconds =
                        (millisUntilFinished / 1000).toInt()

                    val min = seconds / 60

                    val sec = seconds % 60

                    _binding?.btnPing?.text =
                        "⏳ Wait ${min}:${String.format("%02d", sec)}"
                }

                override fun onFinish() {

                    isPingCooldown = false

                    _binding?.btnPing?.text =
                        "🚌 PING BUS"

                    _binding?.btnPing?.isEnabled = true
                }
            }.start()
    }

    private fun observeViewModel() {

        viewModel.etaText.observe(viewLifecycleOwner) {

            binding.tvEta.text = it
        }

        viewModel.etaStatus.observe(viewLifecycleOwner) { status ->

            binding.chipStatus.text = status

            val color = when (status) {

                "ARRIVING" ->
                    R.color.status_arriving

                "SOON" ->
                    R.color.status_soon

                "PASSED" ->
                    R.color.status_passed

                else ->
                    R.color.status_on_time
            }

            binding.chipStatus.setChipBackgroundColorResource(color)
        }

        viewModel.latestPing.observe(viewLifecycleOwner) { ping ->

            val stopName =
                viewModel.currentRoute.value
                    ?.stops
                    ?.get(ping.stopId)
                    ?.name
                    ?: ping.stopId

            val timeStr =
                SimpleDateFormat(
                    "hh:mm a",
                    Locale.getDefault()
                ).format(Date(ping.timestamp))

            val ago =
                getTimeAgo(ping.timestamp)

            binding.tvLastPing.text =
                "Last seen at: $stopName"

            binding.tvLastPingTime.text =
                "$timeStr • $ago"

            binding.pingCard.visibility =
                View.VISIBLE
        }

        viewModel.breakdown.observe(viewLifecycleOwner) { b ->

            if (b?.active == true) {

                binding.breakdownCard.visibility =
                    View.VISIBLE

                binding.tvBreakdownMsg.text =
                    b.message

            } else {

                binding.breakdownCard.visibility =
                    View.GONE
            }
        }

        viewModel.currentRoute.observe(viewLifecycleOwner) {

            binding.tvRouteName.text = it.name
        }
    }

    private fun getTimeAgo(timestamp: Long): String {

        val diff =
            System.currentTimeMillis() - timestamp

        val seconds = diff / 1000

        val minutes = seconds / 60

        val hours = minutes / 60

        return when {

            seconds < 60 ->
                "just now"

            minutes < 60 ->
                "${minutes}m ago"

            hours < 24 ->
                "${hours}h ago"

            else ->
                "${hours / 24}d ago"
        }
    }

    private fun startTracking() {

        if (routeId.isEmpty()) return

        viewModel.loadRoute(routeId)

        viewModel.startListening(
            routeId,
            stopOrder
        )
    }

    override fun onDestroyView() {

        super.onDestroyView()

        cooldownTimer?.cancel()

        _binding = null
    }
}