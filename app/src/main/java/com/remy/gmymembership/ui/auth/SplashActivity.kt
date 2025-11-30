package com.remy.gmymembership.ui.auth

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.remy.gmymembership.databinding.ActivitySplashBinding
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser

        Handler(Looper.getMainLooper()).postDelayed({
            if (user != null) {

                startActivity(Intent(this, LoginActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish() // Matamos el Splash
        }, 3000)
    }
}