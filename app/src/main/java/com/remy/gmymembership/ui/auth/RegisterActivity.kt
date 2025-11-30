package com.remy.gmymembership.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.remy.gmymembership.MainActivity
import com.remy.gmymembership.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    private val tilEmail by lazy { binding.etEmailRegister.parent.parent as TextInputLayout }
    private val tilPassword by lazy { binding.etPasswordRegister.parent.parent as TextInputLayout }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnRegister.setOnClickListener {
            attemptRegistration()
        }

        binding.tvGoToLogin.setOnClickListener {
            finish()
        }
    }

    private fun attemptRegistration() {
        val email = binding.etEmailRegister.text.toString().trim()
        val password = binding.etPasswordRegister.text.toString()

        if (!validateRegistration(email, password)) {
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "¡Cuenta creada exitosamente!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                } else {
                    handleFirebaseError(task.exception)
                }
            }
    }

    private fun validateRegistration(email: String, password: String): Boolean {
        var isValid = true

        tilEmail.error = null
        tilPassword.error = null

        if (email.isEmpty()) {
            tilEmail.error = "El email es obligatorio."
            binding.etEmailRegister.requestFocus()
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Ingresa un email con formato válido."
            binding.etEmailRegister.requestFocus()
            isValid = false
        } else {
            tilEmail.error = null
        }

        if (password.isEmpty()) {
            tilPassword.error = "La contraseña es obligatoria."
            if (isValid) binding.etPasswordRegister.requestFocus()
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Debe tener al menos 6 caracteres."
            if (isValid) binding.etPasswordRegister.requestFocus()
            isValid = false
            // 2. Mayúscula
        } else if (!password.any { it.isUpperCase() }) {
            tilPassword.error = "La contraseña debe contener al menos una letra mayúscula."
            if (isValid) binding.etPasswordRegister.requestFocus()
            isValid = false
        } else {
            tilPassword.error = null
        }

        return isValid
    }

    private fun handleFirebaseError(exception: Exception?) {
        val errorMessage = exception?.message ?: "Error desconocido. Intenta de nuevo."

        if (errorMessage.contains("email address is already in use")) {
            tilEmail.error = "Este correo ya está registrado. Intenta Iniciar Sesión."
            binding.etEmailRegister.requestFocus()
        } else if (errorMessage.contains("The email address is badly formatted")) {
            tilEmail.error = "El formato del email es incorrecto."
            binding.etEmailRegister.requestFocus()
        } else {
            Toast.makeText(this, "Error de Firebase: $errorMessage", Toast.LENGTH_LONG).show()
        }
    }
}