package com.remy.gmymembership

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.remy.gmymembership.databinding.ActivityMainBinding
import androidx.appcompat.app.AppCompatDelegate
import com.remy.gmymembership.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
        } else {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE)
        val darkModeEnabled = sharedPrefs.getBoolean(SettingsFragment.DARK_MODE_KEY, true)

        val mode = if (darkModeEnabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // 3. Conectar la BottomNavigationView
        binding.navView.setupWithNavController(navController)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_members,
                R.id.navigation_calendar
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isTopLevelDestination = destination.id in appBarConfiguration.topLevelDestinations

            binding.navView.visibility = if (isTopLevelDestination) View.VISIBLE else View.GONE

            binding.appBarLayout.visibility = if (isTopLevelDestination) View.GONE else View.VISIBLE
        }


        askNotificationPermission()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    AlertDialog.Builder(this)
                        .setTitle("Permiso de Notificaciones")
                        .setMessage("Para recibir recordatorios diarios sobre el estado de las membresías, por favor, permite que la aplicación envíe notificaciones.")
                        .setPositiveButton("Aceptar") { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Ahora no", null)
                        .show()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}