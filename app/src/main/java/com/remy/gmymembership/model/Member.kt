package com.remy.gmymembership.model

import com.google.firebase.Timestamp

data class Member(
    var id: String = "",             // ID único
    var firstName: String = "",      // Nombre
    var lastName: String = "",       // Apellido
    var phone: String = "",          // Teléfono
    var planDurationDays: Int = 30,  // Días pagados
    var registrationDate: Timestamp? = null, // Fecha de registro
    var isActive: Boolean = true     // Si está al día
)