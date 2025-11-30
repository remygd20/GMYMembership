package com.remy.gmymembership.ui.dashboard

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputFilter
import com.remy.gmymembership.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.remy.gmymembership.databinding.FragmentEditMemberBinding
import com.remy.gmymembership.model.Member
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class EditMemberFragment : Fragment() {

    private var _binding: FragmentEditMemberBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var memberId: String? = null

    private var fechaVencimientoActual: Calendar = Calendar.getInstance()
    private var fechaRegistroOriginal: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditMemberBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInputFilters()

        memberId = arguments?.getString("memberId")
        if (memberId != null) {
            cargarDatos(memberId!!)
        } else {
            findNavController().popBackStack()
        }

        binding.btnSelectDateEdit.setOnClickListener { mostrarSelectorFecha() }
        binding.chipAdd1Month.setOnClickListener { agregarTiempo(1, Calendar.MONTH) }
        binding.chipAdd1Year.setOnClickListener { agregarTiempo(1, Calendar.YEAR) }

        binding.btnUpdateMember.setOnClickListener {
            if (validateInputs()) {
                mostrarAlertaConfirmacion()
            }
        }
        binding.btnCancelEdit.setOnClickListener { findNavController().popBackStack() }
    }

    private fun setupInputFilters() {
        val textFilter = InputFilter { source, _, _, _, _, _ ->
            source.filter { char -> char.isLetter() || char.isWhitespace() }
        }
        val numberFilter = InputFilter { source, _, _, _, _, _ ->
            source.filter { char -> char.isDigit() }
        }

        binding.etFirstNameEdit.filters = arrayOf(textFilter, InputFilter.LengthFilter(50))
        binding.etLastNameEdit.filters = arrayOf(textFilter, InputFilter.LengthFilter(50))
        binding.etPhoneEdit.filters = arrayOf(numberFilter, InputFilter.LengthFilter(15))
    }

    private fun validateInputs(): Boolean {
        val nombre = binding.etFirstNameEdit.text.toString().trim()
        val apellido = binding.etLastNameEdit.text.toString().trim()
        val telefono = binding.etPhoneEdit.text.toString().trim()

        var esValido = true

        if (nombre.isEmpty()) {
            binding.etFirstNameEdit.error = "El nombre es obligatorio"
            esValido = false
        } else {
            binding.etFirstNameEdit.error = null
        }

        if (apellido.isEmpty()) {
            binding.etLastNameEdit.error = "El apellido es obligatorio"
            esValido = false
        } else {
            binding.etLastNameEdit.error = null
        }

        if (telefono.length < 10) {
            binding.etPhoneEdit.error = "El teléfono debe tener al menos 10 dígitos"
            esValido = false
        } else {
            binding.etPhoneEdit.error = null
        }

        return esValido
    }

    private fun cargarDatos(id: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("members").document(id)
            .get()
            .addOnSuccessListener { document ->
                if (!isAdded) return@addOnSuccessListener
                if (document.exists()) {
                    val member = document.toObject(Member::class.java)
                    binding.etFirstNameEdit.setText(member?.firstName)
                    binding.etLastNameEdit.setText(member?.lastName)
                    binding.etPhoneEdit.setText(member?.phone)

                    if (member?.registrationDate != null) {
                        fechaRegistroOriginal = member.registrationDate!!.toDate()
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        cal.time = fechaRegistroOriginal
                        cal.add(Calendar.DAY_OF_YEAR, member.planDurationDays)
                        limpiarHora(cal)
                        fechaVencimientoActual.timeInMillis = cal.timeInMillis
                        actualizarTextoFecha()
                    }
                }
            }
    }

    private fun agregarTiempo(cantidad: Int, unidad: Int) {
        fechaVencimientoActual.add(unidad, cantidad)
        actualizarTextoFecha()
        if (isAdded) {
            Toast.makeText(requireContext(), "Se sumó tiempo al vencimiento", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarSelectorFecha() {
        if (!isAdded) return
        val datePicker = DatePickerDialog(
            requireContext(),
            R.style.CalendarDialogTheme,
            { _, year, month, day ->
                fechaVencimientoActual.set(year, month, day)
                actualizarTextoFecha()
            },
            fechaVencimientoActual.get(Calendar.YEAR),
            fechaVencimientoActual.get(Calendar.MONTH),
            fechaVencimientoActual.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.datePicker.minDate = System.currentTimeMillis()
        datePicker.show()
    }

    private fun actualizarTextoFecha() {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.tvSelectedDateTextEdit.text = "Vence el: ${formato.format(fechaVencimientoActual.time)}"
    }

    private fun mostrarAlertaConfirmacion() {
        if (!isAdded) return

        MaterialAlertDialogBuilder(requireContext(), R.style.GMYAlertDialogStyle)
            .setTitle("¿Guardar cambios?")
            .setMessage("Se actualizará la información del miembro. Si modificaste el vencimiento, la fecha de registro se establecerá en la fecha de hoy, recalculando la duración del plan.")
            .setPositiveButton("Sí, Guardar") { dialog, _ ->
                actualizarDatos()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun actualizarDatos() {
        val userId = auth.currentUser?.uid ?: return
        val id = memberId ?: return

        val todayCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        limpiarHora(todayCal)
        val newRegistrationDate = Timestamp(todayCal.time)

        val diff = fechaVencimientoActual.timeInMillis - todayCal.timeInMillis
        var nuevosDias = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()
        if (nuevosDias < 0) nuevosDias = 0

        val updateMap = mapOf(
            "firstName" to binding.etFirstNameEdit.text.toString().trim(),
            "lastName" to binding.etLastNameEdit.text.toString().trim(),
            "phone" to binding.etPhoneEdit.text.toString().trim(),
            "registrationDate" to newRegistrationDate,
            "planDurationDays" to nuevosDias
        )

        db.collection("users").document(userId).collection("members").document(id)
            .update(updateMap)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(requireContext(), "¡Actualizado!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
            .addOnFailureListener {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
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