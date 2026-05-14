package com.vidyavahini.app.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.vidyavahini.app.R
import com.vidyavahini.app.databinding.FragmentOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnSkip.setOnClickListener {
            navigateToSignIn()
        }
        
        binding.btnNext.setOnClickListener {
            // For now, jump straight to sign in to demonstrate flow
            navigateToSignIn()
        }
    }

    private fun navigateToSignIn() {
        findNavController().navigate(R.id.action_onboarding_to_signIn)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
