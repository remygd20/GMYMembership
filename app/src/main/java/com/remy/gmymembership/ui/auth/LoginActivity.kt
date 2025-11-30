package com.remy.gmymembership.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.remy.gmymembership.MainActivity
import com.remy.gmymembership.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException // Importación necesaria
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException // Importación necesaria

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Click en Ingresar
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Éxito: Contraseña y correo son correctos
                            Toast.makeText(this, "¡Bienvenido a GMY! Sesión iniciada.", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            handleFirebaseError(task.exception)
                        }
                    }
            } else {
                Toast.makeText(this, "Por favor completa los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // Click en Registro
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun handleFirebaseError(exception: Exception?) {
        when (exception) {
            is FirebaseAuthInvalidUserException -> {
                // Usuario no existe
                Toast.makeText(this, "Error: El correo no está registrado.", Toast.LENGTH_LONG).show()
            }
            is FirebaseAuthInvalidCredentialsException -> {
                // Credenciales inválidas (ej. contraseña incorrecta)
                Toast.makeText(this, "Error: Contraseña incorrecta.", Toast.LENGTH_LONG).show()
            }
            else -> {
                val errorMessage = exception?.message ?: "Error desconocido. Verifica tu conexión a internet."
                Toast.makeText(this, "Error al iniciar sesión: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }
}