package com.remy.gmymembership.ui.dashboard

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.remy.gmymembership.R
import com.remy.gmymembership.databinding.FragmentAddMemberBinding
import com.remy.gmymembership.model.Member
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AddMemberFragment : Fragment() {

    private var _binding: FragmentAddMemberBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var fechaVencimientoSeleccionada: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMemberBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInputFilters()
        setupDurationChips()
        binding.btnSelectDate.setOnClickListener { mostrarSelectorFecha() }
        calcularNuevaFecha(1, Calendar.MONTH)

        binding.btnSaveMember.setOnClickListener {
            if (validateInputs()) {
                guardarEnFirebase()
            }
        }

        binding.btnCancelAdd.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupInputFilters() {
        val textFilter = InputFilter { source, _, _, _, _, _ ->
            source.filter { char -> char.isLetter() || char.isWhitespace() }
        }
        val numberFilter = InputFilter { source, _, _, _, _, _ ->
            source.filter { char -> char.isDigit() }
        }

        binding.etFirstName.filters = arrayOf(textFilter, InputFilter.LengthFilter(50))
        binding.etLastName.filters = arrayOf(textFilter, InputFilter.LengthFilter(50))
        binding.etPhone.filters = arrayOf(numberFilter, InputFilter.LengthFilter(15))
    }

    private fun validateInputs(): Boolean {
        val nombre = binding.etFirstName.text.toString().trim()
        val apellido = binding.etLastName.text.toString().trim()
        val telefono = binding.etPhone.text.toString().trim()

        var esValido = true

        if (nombre.isEmpty()) {
            binding.etFirstName.error = "El nombre es obligatorio"
            esValido = false
        } else {
            binding.etFirstName.error = null
        }

        if (apellido.isEmpty()) {
            binding.etLastName.error = "El apellido es obligatorio"
            esValido = false
        } else {
            binding.etLastName.error = null
        }

        if (telefono.length < 10) {
            binding.etPhone.error = "El teléfono debe tener al menos 10 dígitos"
            esValido = false
        } else {
            binding.etPhone.error = null
        }

        return esValido
    }

    private fun setupDurationChips() {
        binding.chip1Month.setOnClickListener { calcularNuevaFecha(1, Calendar.MONTH) }
        binding.chip3Months.setOnClickListener { calcularNuevaFecha(3, Calendar.MONTH) }
        binding.chip6Months.setOnClickListener { calcularNuevaFecha(6, Calendar.MONTH) }
        binding.chip1Year.setOnClickListener { calcularNuevaFecha(1, Calendar.YEAR) }
    }

    private fun calcularNuevaFecha(cantidad: Int, unidad: Int) {
        val hoy = Calendar.getInstance()
        hoy.add(unidad, cantidad)
        fechaVencimientoSeleccionada = hoy
        actualizarTextoFecha()
    }

    private fun mostrarSelectorFecha() {
        val hoy = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            requireContext(),
            R.style.CalendarDialogTheme,
            { _, year, month, day ->
                val fechaElegida = Calendar.getInstance()
                fechaElegida.set(year, month, day)
                fechaVencimientoSeleccionada = fechaElegida
                binding.chipGroupDuration.clearCheck()
                actualizarTextoFecha()
            },
            hoy.get(Calendar.YEAR),
            hoy.get(Calendar.MONTH),
            hoy.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.datePicker.minDate = System.currentTimeMillis()
        datePicker.show()
    }

    private fun actualizarTextoFecha() {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.tvSelectedDateText.text = "Vence el: ${formato.format(fechaVencimientoSeleccionada.time)}"
    }

    private fun guardarEnFirebase() {
        val nombre = binding.etFirstName.text.toString().trim()
        val apellido = binding.etLastName.text.toString().trim()
        val telefono = binding.etPhone.text.toString().trim()
        
        val userId = auth.currentUser?.uid ?: return

        val fechaFin = Calendar.getInstance()
        fechaFin.time = fechaVencimientoSeleccionada.time
        fechaFin.set(Calendar.HOUR_OF_DAY, 0)
        fechaFin.set(Calendar.MINUTE, 0)
        fechaFin.set(Calendar.SECOND, 0)
        fechaFin.set(Calendar.MILLISECOND, 0)

        val fechaInicio = Calendar.getInstance()
        fechaInicio.set(Calendar.HOUR_OF_DAY, 0)
        fechaInicio.set(Calendar.MINUTE, 0)
        fechaInicio.set(Calendar.SECOND, 0)
        fechaInicio.set(Calendar.MILLISECOND, 0)

        val diff = fechaFin.timeInMillis - fechaInicio.timeInMillis
        var diasDuracion = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()

        if (diasDuracion < 1) diasDuracion = 1

        val nuevoDocRef = db.collection("users").document(userId).collection("members").document()

        val nuevoMiembro = Member(
            id = nuevoDocRef.id,
            firstName = nombre,
            lastName = apellido,
            phone = telefono,
            planDurationDays = diasDuracion,
            registrationDate = Timestamp(Date()),
            isActive = true
        )

        nuevoDocRef.set(nuevoMiembro)
            .addOnSuccessListener {
                Toast.makeText(context, "¡Miembro registrado!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}