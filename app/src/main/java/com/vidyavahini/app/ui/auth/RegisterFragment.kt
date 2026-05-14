package com.vidyavahini.app.ui.auth

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.vidyavahini.app.R
import com.vidyavahini.app.data.model.Student
import com.vidyavahini.app.data.repository.FirebaseRepository
import com.vidyavahini.app.databinding.FragmentRegisterBinding

/**
 * RegisterFragment — first-time profile setup.
 * Loads available routes from Firebase, lets the student pick their boarding stop,
 * then saves the profile and navigates to HomeFragment.
 */
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val repository = FirebaseRepository()

    // Local state: maps route display names → route IDs
    private val routeIdMap  = mutableMapOf<String, String>()   // "Pune → Nashik" → "route_pune_nashik"
    private val stopIdMap   = mutableMapOf<String, String>()   // "Stop 1: Shivajinagar" → "stop_01"
    private val stopOrderMap = mutableMapOf<String, Int>()     // "stop_01" → 1

    private var selectedRouteId = ""
    private var selectedStopId  = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadRoutes()
        setupUI()
    }

    private fun loadRoutes() {
        binding.progressBar.visibility = View.VISIBLE
        repository.getAllRoutes { routes ->
            binding.progressBar.visibility = View.GONE
            if (routes.isEmpty()) {
                repository.seedDummyRoutes {
                    loadRoutes() // Retry after seeding
                }
                return@getAllRoutes
            }

            routeIdMap.clear()
            routes.forEach { (id, route) -> routeIdMap[route.name] = id }

            val routeNames = routeIdMap.keys.toList()
            val routeAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                routeNames
            )
            binding.dropdownRoute.setAdapter(routeAdapter)

            // When route is selected, load its stops into the second dropdown
            binding.dropdownRoute.setOnItemClickListener { _, _, position, _ ->
                selectedRouteId = routeIdMap[routeNames[position]] ?: ""
                loadStopsForRoute(selectedRouteId, routes)
            }
        }
    }

    private fun loadStopsForRoute(
        routeId: String,
        routes: Map<String, com.vidyavahini.app.data.model.Route>
    ) {
        val route = routes[routeId] ?: return
        stopIdMap.clear()
        stopOrderMap.clear()

        val sortedStops = route.stops.entries.sortedBy { it.value.order }
        val stopLabels  = sortedStops.map { (id, stop) ->
            "Stop ${stop.order}: ${stop.name}".also {
                stopIdMap[it]    = id
                stopOrderMap[id] = stop.order
            }
        }

        val stopAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            stopLabels
        )
        binding.dropdownStop.setAdapter(stopAdapter)
        binding.dropdownStop.isEnabled = true

        binding.dropdownStop.setOnItemClickListener { _, _, position, _ ->
            selectedStopId = stopIdMap[stopLabels[position]] ?: ""
        }
    }

    private fun setupUI() {
        binding.btnRegister.setOnClickListener {
            val name        = binding.etName.text.toString().trim()
            val parentPhone = binding.etParentPhone.text.toString().trim()

            if (!validateInputs(name, parentPhone)) return@setOnClickListener

            val stopOrder = stopOrderMap[selectedStopId] ?: 1
            val student   = Student(
                name        = name,
                routeId     = selectedRouteId,
                stopId      = selectedStopId,
                parentPhone = if (parentPhone.isEmpty()) "" else "+91$parentPhone"
            )

            // Save to Firebase
            repository.saveStudent(student)

            // Save to SharedPreferences for quick offline access
            requireContext().getSharedPreferences("vidya", Context.MODE_PRIVATE)
                .edit()
                .putString("name",        name)
                .putString("routeId",     selectedRouteId)
                .putString("stopId",      selectedStopId)
                .putString("parentPhone", student.parentPhone)
                .putInt("stopOrder",      stopOrder)
                .apply()

            Toast.makeText(requireContext(), "Welcome, $name! 🎉", Toast.LENGTH_SHORT).show()
            Toast.makeText(requireContext(), "Registration Successful", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInputs(name: String, parentPhone: String): Boolean {
        if (name.isEmpty()) {
            binding.etName.error = "Please enter your name"
            return false
        }
        if (selectedRouteId.isEmpty()) {
            Toast.makeText(requireContext(), "Please select your route", Toast.LENGTH_SHORT).show()
            return false
        }
        if (selectedStopId.isEmpty()) {
            Toast.makeText(requireContext(), "Please select your boarding stop", Toast.LENGTH_SHORT).show()
            return false
        }
        if (parentPhone.isNotEmpty() && parentPhone.length != 10) {
            binding.etParentPhone.error = "Enter valid 10-digit number"
            return false
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
