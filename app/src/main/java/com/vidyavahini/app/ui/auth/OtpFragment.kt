package com.vidyavahini.app.ui.auth

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.vidyavahini.app.R
import com.vidyavahini.app.databinding.FragmentOtpBinding
import com.vidyavahini.app.viewmodel.AuthViewModel

/**
 * OtpFragment — receives the 6-digit OTP and verifies it with Firebase.
 * Navigates to RegisterFragment on first login, HomeFragment for returning users.
 */
class OtpFragment : Fragment() {

    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!

    // Share the same ViewModel instance as LoginFragment via the activity scope
    private val viewModel: AuthViewModel by activityViewModels()

    // Prevent duplicate navigation on config change
    private var hasNavigated = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hasNavigated = false
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Enable Verify button only when 6 digits are entered
        binding.etOtp.doAfterTextChanged { text ->
            binding.btnVerify.isEnabled = (text?.length ?: 0) == 6
        }

        binding.btnVerify.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            viewModel.verifyOtp(otp)
        }

        // Auto-focus OTP field
        binding.etOtp.requestFocus()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnVerify.isEnabled    = !loading
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            if (hasNavigated) return@observe

            when {
                state == "verified" -> {
                    hasNavigated = true

                    // Check if user already has a profile (returning user)
                    val prefs = requireContext().getSharedPreferences("vidya", Context.MODE_PRIVATE)
                    val existingName = prefs.getString("name", null)

                    if (!existingName.isNullOrEmpty()) {
                        // Returning user → skip registration, go straight to home
                        Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show()
                    } else {
                        // First-time user → register profile
                        Toast.makeText(requireContext(), "Navigate to Register", Toast.LENGTH_SHORT).show()
                    }
                }
                state.startsWith("error") -> {
                    val msg = state.removePrefix("error: ")
                    binding.etOtp.error = msg
                    Toast.makeText(requireContext(), "Verification failed: $msg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
