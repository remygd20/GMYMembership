package com.remy.gmymembership.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.remy.gmymembership.databinding.FragmentSettingsBinding
import com.remy.gmymembership.ui.auth.LoginActivity

// 🌟 IMPORTACIÓN NECESARIA PARA EL TEMA 🌟
import androidx.appcompat.app.AppCompatDelegate

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()

    companion object {
        const val PREFS_NAME = "GMYMembershipPrefs"
        const val NOTIFICATIONS_ENABLED_KEY = "notificationsEnabled"
        const val DARK_MODE_KEY = "darkModeEnabled"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val notificationsEnabled = sharedPreferences.getBoolean(NOTIFICATIONS_ENABLED_KEY, true)
        binding.switchNotifications.isChecked = notificationsEnabled

        val darkModeEnabled = sharedPreferences.getBoolean(DARK_MODE_KEY, true)
        binding.switchDarkMode.isChecked = darkModeEnabled

        binding.tvAppVersion.text = getAppVersion()

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(NOTIFICATIONS_ENABLED_KEY, isChecked).apply()
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(DARK_MODE_KEY, isChecked).apply()

            applyTheme(isChecked)
        }

        binding.tvContactSupport.setOnClickListener { contactSupport() }
        binding.tvRateApp.setOnClickListener { rateApp() }
        binding.tvShareApp.setOnClickListener { shareApp() }

        binding.tvPrivacyPolicy.setOnClickListener { showToast("Política de Privacidad no disponible aún.") }
        binding.tvTermsConditions.setOnClickListener { showToast("Términos y Condiciones no disponibles aún.") }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }
    }

    private fun applyTheme(isDarkMode: Boolean) {
        val mode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES // Modo Oscuro
        } else {
            AppCompatDelegate.MODE_NIGHT_NO  // Modo Claro
        }

        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun contactSupport() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("remygarcia20@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Soporte de GMY Membership App")
        }
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            showToast("No se encontró una aplicación de correo.")
        }
    }

    private fun rateApp() {
        val packageName = requireActivity().packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: android.content.ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    private fun shareApp() {
        val appName = "GMY Membership"
        val packageName = requireActivity().packageName
        val shareText = "¡Te recomiendo esta app para gestionar tu gimnasio!\n\nhttps://play.google.com/store/apps/details?id=$packageName"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, appName)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Compartir App vía"))
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            "?.?.?"
        }
    }

    private fun showToast(message: String) {
        if (isAdded) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}