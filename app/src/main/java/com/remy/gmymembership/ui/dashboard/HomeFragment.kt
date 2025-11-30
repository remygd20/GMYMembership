package com.remy.gmymembership.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.remy.gmymembership.R
import com.remy.gmymembership.databinding.FragmentHomeBinding
import com.remy.gmymembership.model.Member
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private data class GmyLocation(val name: String, val latLng: LatLng)

    private val gmyLocations = listOf(
        GmyLocation("GMY Fitness Centro", LatLng(20.5495, -104.0418)),
        GmyLocation("GMY Fitness Providencia", LatLng(20.5552, -104.0355)),
        GmyLocation("GMY Fitness El Santuario", LatLng(20.5431, -104.0459))
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navegación a Ajustes
        binding.settingsIcon.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }

        // Hacer el mapa interactivo
        binding.mapCard.setOnClickListener { openGoogleMaps() }

        // Cargar Estadísticas
        cargarEstadisticas()

        // Cargar Mapa de Google Maps
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_container) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    private fun openGoogleMaps() {
        if (gmyLocations.isEmpty()) return

        val firstLocation = gmyLocations.first()
        val gmmIntentUri = Uri.parse("geo:${firstLocation.latLng.latitude},${firstLocation.latLng.longitude}?q=GMY Fitness")
        
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")

        if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(mapIntent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        if (gmyLocations.isEmpty()) return

        googleMap.apply {
            uiSettings.setAllGesturesEnabled(false)
            val boundsBuilder = LatLngBounds.builder()
            gmyLocations.forEach { location ->
                addMarker(MarkerOptions().position(location.latLng).title(location.name))
                boundsBuilder.include(location.latLng)
            }
            val bounds = boundsBuilder.build()
            val padding = 150
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            moveCamera(cameraUpdate)
        }
    }

    private fun cargarEstadisticas() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("members")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener
                val currentBinding = _binding ?: return@addSnapshotListener

                var activos = 0
                var porVencer = 0
                var vencidos = 0
                var total = 0

                val hoy = Calendar.getInstance()
                limpiarHora(hoy)
                val diasDesdeEpochHoy = TimeUnit.DAYS.convert(hoy.timeInMillis, TimeUnit.MILLISECONDS)

                for (doc in snapshots) {
                    val member = doc.toObject(Member::class.java)
                    member.id = doc.id
                    total++

                    if (member.registrationDate != null) {
                        val fechaVenc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        fechaVenc.time = member.registrationDate!!.toDate()
                        fechaVenc.add(Calendar.DAY_OF_YEAR, member.planDurationDays)
                        limpiarHora(fechaVenc)

                        val diasDesdeEpochVenc = TimeUnit.DAYS.convert(fechaVenc.timeInMillis, TimeUnit.MILLISECONDS)
                        val diasRestantes = diasDesdeEpochVenc - diasDesdeEpochHoy

                        when {
                            diasRestantes > 5 -> activos++
                            diasRestantes in 0..5 -> porVencer++
                            else -> vencidos++
                        }
                    }
                }

                currentBinding.tvActiveCount.text = activos.toString()
                currentBinding.tvExpiringCount.text = porVencer.toString()
                currentBinding.tvExpiredCount.text = vencidos.toString()
                currentBinding.tvTotalCount.text = total.toString()
            }
    }

    private fun limpiarHora(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
