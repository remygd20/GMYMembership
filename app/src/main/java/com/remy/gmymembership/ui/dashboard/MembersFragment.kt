package com.remy.gmymembership.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.remy.gmymembership.R
import com.remy.gmymembership.adapter.MemberAdapter
import com.remy.gmymembership.databinding.FragmentMembersBinding
import com.remy.gmymembership.model.Member
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class MembersFragment : Fragment() {

    private var _binding: FragmentMembersBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val fullMembersList = mutableListOf<Member>()
    private var filteredMembersList = mutableListOf<Member>()
    private lateinit var memberAdapter: MemberAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        setupSwipeToDelete()
        fetchMembers()
    }

    private fun setupRecyclerView() {
        memberAdapter = MemberAdapter(filteredMembersList) { member ->
            val bundle = Bundle().apply { putString("memberId", member.id) }
            findNavController().navigate(R.id.navigation_edit_member, bundle)
        }
        binding.rvMembers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = memberAdapter
        }
    }

    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_members_to_addMember)
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterAndSortMembers()
                return true
            }
        })

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterAndSortMembers()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun fetchMembers() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("members")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null || !isAdded) return@addSnapshotListener

                fullMembersList.clear()
                for (document in snapshots) {
                    val member = document.toObject(Member::class.java).apply { id = document.id }
                    fullMembersList.add(member)
                }
                filterAndSortMembers()
            }
    }

    private fun filterAndSortMembers() {
        val query = binding.searchView.query.toString().lowercase().trim()
        val selectedTabPosition = binding.tabLayout.selectedTabPosition

        val tempFilteredList = fullMembersList.filter { member ->
            val fullName = "${member.firstName} ${member.lastName}".lowercase()
            val matchesSearch = fullName.contains(query)

            val status = getMemberStatus(member)
            val matchesTab = when (selectedTabPosition) {
                1 -> status == MemberStatus.ACTIVE
                2 -> status == MemberStatus.EXPIRING_SOON
                3 -> status == MemberStatus.EXPIRED
                else -> true // "Todos"
            }
            matchesSearch && matchesTab
        }

        filteredMembersList.clear()
        filteredMembersList.addAll(tempFilteredList.sortedBy { getRemainingDays(it) })
        memberAdapter.notifyDataSetChanged()

        binding.tvEmptyList.visibility = if (filteredMembersList.isEmpty()) View.VISIBLE else View.GONE
        binding.rvMembers.visibility = if (filteredMembersList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val memberToDelete = filteredMembersList[position]

                MaterialAlertDialogBuilder(requireContext(), R.style.GMYAlertDialogStyle)
                    .setTitle("¿Eliminar socio?")
                    .setMessage("Estás a punto de eliminar a ${memberToDelete.firstName}. Esta acción no se puede deshacer.")
                    .setNegativeButton("Cancelar") { dialog, _ ->
                        memberAdapter.notifyItemChanged(position)
                        dialog.dismiss()
                    }
                    .setPositiveButton("Eliminar") { dialog, _ ->
                        deleteMemberFromFirestore(memberToDelete)
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.rvMembers)
    }

    private fun deleteMemberFromFirestore(member: Member) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("members").document(member.id)
            .delete()
            .addOnSuccessListener { if (isAdded) Toast.makeText(context, "Socio eliminado", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { if (isAdded) Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show() }
    }

    enum class MemberStatus { ACTIVE, EXPIRING_SOON, EXPIRED, UNKNOWN }

    private fun getRemainingDays(member: Member): Int {
        if (member.registrationDate == null) return Int.MAX_VALUE
        val expirationDate = Calendar.getInstance().apply {
            time = member.registrationDate!!.toDate()
            add(Calendar.DAY_OF_YEAR, member.planDurationDays)
            clearTime()
        }
        val today = Calendar.getInstance().apply { clearTime() }
        val diff = expirationDate.timeInMillis - today.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diff).toInt()
    }

    private fun getMemberStatus(member: Member): MemberStatus {
        if (member.registrationDate == null) return MemberStatus.UNKNOWN
        val remainingDays = getRemainingDays(member)
        return when {
            remainingDays > 5 -> MemberStatus.ACTIVE
            remainingDays in 0..5 -> MemberStatus.EXPIRING_SOON
            else -> MemberStatus.EXPIRED
        }
    }

    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}