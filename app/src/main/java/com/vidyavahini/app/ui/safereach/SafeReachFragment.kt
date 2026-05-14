package com.vidyavahini.app.ui.safereach

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.vidyavahini.app.R
import com.vidyavahini.app.data.repository.FirebaseRepository
import com.vidyavahini.app.databinding.FragmentSafeReachBinding
import com.vidyavahini.app.utils.NotificationHelper

/**
 * SafeReachFragment — "I Reached College Safely" screen.
 *
 * When the student taps the button:
 * 1. Writes to Firebase safereach/{uid} → triggers FCM notification to parent's app
 * 2. Sends an SMS via Android SmsManager — free, works for feature-phone parents
 */
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SafeReachFragment : Fragment() {

    private var _binding: FragmentSafeReachBinding? = null
    private val binding get() = _binding!!
    private val repository = FirebaseRepository()

    private var studentName = ""
    private var parentPhone = ""
    private var hasNotified = false

    // SMS permission launcher
    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && parentPhone.isNotEmpty()) {
            sendSmsToParent(parentPhone, studentName)
        }
        markReached()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSafeReachBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fade-in entrance animation
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        binding.root.startAnimation(fadeIn)

        val prefs   = requireContext().getSharedPreferences("vidya", Context.MODE_PRIVATE)
        studentName = prefs.getString("name", "Student") ?: "Student"
        parentPhone = prefs.getString("parentPhone", "") ?: ""

        binding.tvStudentName.text = studentName

        // Show parent phone info if available
        if (parentPhone.isNotEmpty()) {
            binding.tvNotifInfo.text = "📱 FCM push to parent's smartphone\n📟 SMS to ${parentPhone.takeLast(4).padStart(parentPhone.length, '*')}"
        }

        binding.btnReached.setOnClickListener {
            if (hasNotified) return@setOnClickListener
            onReachedTapped()
        }
    }

    private fun onReachedTapped() {
        // Method 1: FCM via Firebase (smartphones)
        NotificationHelper.sendFCMSafeReach(studentName)
        repository.markSafeReach(studentName)

        // Method 2: SMS via SmsManager (feature phones — no internet needed)
        if (parentPhone.isNotEmpty()) {
            val hasSmsPermission = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasSmsPermission) {
                sendSmsToParent(parentPhone, studentName)
                markReached()
            } else {
                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            }
        } else {
            markReached()
        }
    }

    /**
     * Sends SMS using the device SIM — free, works on any parent phone.
     * No API key, no internet, no cost.
     */
    private fun sendSmsToParent(phone: String, name: String) {
        try {
            val msg = "✅ $name has safely reached college. — Vidya-Vahini App"
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requireContext().getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(phone, null, msg, null, null)
        } catch (e: Exception) {
            // SMS failed — FCM was already sent as the primary method
        }
    }

    private fun markReached() {
        hasNotified = true

        // Haptic feedback for satisfying confirmation
        try {
            val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) { }

        // Animate button
        val bounceAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.bounce_scale)
        binding.btnReached.startAnimation(bounceAnim)

        binding.btnReached.text      = "✅ Parents Notified!"
        binding.btnReached.isEnabled = false
        binding.tvStatus.visibility  = View.VISIBLE
        binding.tvStatus.text        = "$studentName has safely reached college.\nYour parents have been notified."
        binding.ivCheckmark.visibility = View.VISIBLE

        // Animate checkmark appearance
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        binding.ivCheckmark.startAnimation(fadeIn)
        binding.tvStatus.startAnimation(fadeIn)

        Snackbar.make(
            binding.root,
            "Safe reach notification sent to parents!",
            Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
