package com.vidyavahini.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.vidyavahini.app.R
import com.vidyavahini.app.databinding.FragmentSignInBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSignInBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.btnSignIn.setOnClickListener {

            val email =
                binding.etEmail.text.toString().trim()

            val password =
                binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {

                Toast.makeText(
                    requireContext(),
                    "Enter email and password",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // REAL FIREBASE LOGIN
            auth.signInWithEmailAndPassword(email, password)

                .addOnSuccessListener {

                    Toast.makeText(
                        requireContext(),
                        "Sign In Successful",
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().navigate(
                        R.id.homeFragment
                    )
                }

                .addOnFailureListener { e ->

                    Toast.makeText(
                        requireContext(),
                        "Login Failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}