package com.vidyavahini.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.auth.FirebaseAuth
import com.vidyavahini.app.data.repository.FirebaseRepository
import com.vidyavahini.app.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity — single-activity host for the Navigation component.
 * Handles: splash screen, navigation graph, and auth-state routing.
 *
 * Routing logic:
 *  - User logged in + has profile → HomeFragment
 *  - User logged in + no profile  → RegisterFragment
 *  - User not logged in           → LoginFragment
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {

        // Splash screen — must be called BEFORE super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up Navigation Component
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController

        try {

            // Route to the appropriate screen based on auth + profile state
            val graph = navController.navInflater.inflate(R.navigation.nav_graph)

            val currentUser = FirebaseAuth.getInstance().currentUser

            val prefs = getSharedPreferences("vidya", MODE_PRIVATE)

            val hasProfile = !prefs.getString("name", null).isNullOrEmpty()

            val startDest = when {
                currentUser != null && hasProfile ->
                    R.id.homeFragment

                currentUser != null && !hasProfile ->
                    R.id.profileSetupFragment

                else ->
                    R.id.onboardingFragment
            }

            graph.setStartDestination(startDest)

            navController.graph = graph

            // Seed dummy routes into Firebase
            FirebaseRepository().seedDummyRoutes {
                android.util.Log.d(
                    "Firebase",
                    "Dummy routes added successfully"
                )
            }

        } catch (e: Exception) {

            e.printStackTrace()

            // Fallback to onboarding if anything goes wrong
            val fallbackGraph =
                navController.navInflater.inflate(R.navigation.nav_graph)

            fallbackGraph.setStartDestination(R.id.onboardingFragment)

            navController.graph = fallbackGraph
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
                || super.onSupportNavigateUp()
    }
}