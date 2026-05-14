package com.vidyavahini.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.vidyavahini.app.R
import com.vidyavahini.app.databinding.FragmentLoginBinding
import com.vidyavahini.app.viewmodel.AuthViewModel

/**
 * LoginFragment — entry point for unauthenticated users.
 * Collects a phone number with country code and triggers OTP dispatch.
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by activityViewModels()

    // Prevent duplicate navigation on config change
    private var hasNavigated = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hasNavigated = false

        // Fade-in entrance animation
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        binding.root.startAnimation(fadeIn)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Enable Send OTP button only when phone number is 10 digits
        binding.etPhone.doAfterTextChanged { text ->
            binding.btnSendOtp.isEnabled = (text?.length ?: 0) == 10
        }

        binding.btnSendOtp.setOnClickListener {
            val phone = "+91${binding.etPhone.text.toString().trim()}"
            viewModel.sendOtp(phone, requireActivity())
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSendOtp.isEnabled   = !loading
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            if (hasNavigated) return@observe

            when {
                state == "otp_sent" -> {
                    hasNavigated = true
                    Toast.makeText(requireContext(), "OTP Sent Successfully", Toast.LENGTH_SHORT).show()
                }
                state.startsWith("error") -> {
                    val msg = state.removePrefix("error: ")
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
