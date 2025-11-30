package com.remy.gmymembership.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.remy.gmymembership.R
import com.remy.gmymembership.adapter.MemberAdapter
import com.remy.gmymembership.databinding.FragmentCalendarBinding
import com.remy.gmymembership.model.Member
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var memberAdapter: MemberAdapter
    private val allMembersList = ArrayList<Member>()
    private val filteredList = ArrayList<Member>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var selectedCalendarDate: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvCalendarMembers.layoutManager = LinearLayoutManager(context)
        binding.rvCalendarMembers.setHasFixedSize(false)
        binding.rvCalendarMembers.isNestedScrollingEnabled = false

        memberAdapter = MemberAdapter(filteredList) { member ->
            val bundle = Bundle()
            bundle.putString("memberId", member.id)
            findNavController().navigate(R.id.navigation_edit_member, bundle)
        }
        binding.rvCalendarMembers.adapter = memberAdapter

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedCalendarDate.set(year, month, dayOfMonth)
            filtrarYMostrar()
        }

        cargarTodosLosMiembros()
    }

    private fun cargarTodosLosMiembros() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("members")
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                val currentBinding = _binding ?: return@addSnapshotListener

                if (snapshots != null) {
                    allMembersList.clear()
                    for (doc in snapshots) {
                        val m = doc.toObject(Member::class.java)
                        m.id = doc.id
                        allMembersList.add(m)
                    }
                    filtrarYMostrar(currentBinding)
                }
            }
    }

    private fun filtrarYMostrar(currentBinding: FragmentCalendarBinding? = null) {

        val safeBinding = currentBinding ?: _binding ?: return

        val formato = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES"))

        val fechaTexto = formato.format(selectedCalendarDate.time)
        safeBinding.tvSummaryTitle.text = fechaTexto.replaceFirstChar { it.uppercase() }

        filteredList.clear()

        val calSeleccionado = Calendar.getInstance()
        calSeleccionado.time = selectedCalendarDate.time
        limpiarHora(calSeleccionado)

        for (member in allMembersList) {
            if (member.registrationDate != null) {
                val calVencimiento = Calendar.getInstance()
                calVencimiento.time = member.registrationDate!!.toDate()
                calVencimiento.add(Calendar.DAY_OF_YEAR, member.planDurationDays)
                limpiarHora(calVencimiento)

                if (calSeleccionado.timeInMillis == calVencimiento.timeInMillis) {
                    filteredList.add(member)
                }
            }
        }

        memberAdapter.notifyDataSetChanged()

        if (filteredList.isEmpty()) {
            safeBinding.rvCalendarMembers.visibility = View.GONE
            safeBinding.layoutEmptyState.visibility = View.VISIBLE
            safeBinding.tvSummarySubtitle.text = "0 vencimientos pendientes"
        } else {
            safeBinding.rvCalendarMembers.visibility = View.VISIBLE
            safeBinding.layoutEmptyState.visibility = View.GONE

            val s = if (filteredList.size == 1) "socio vence" else "socios vencen"
            safeBinding.tvSummarySubtitle.text = "${filteredList.size} $s este día"
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