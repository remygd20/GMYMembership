package com.remy.gmymembership.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.remy.gmymembership.R
import com.remy.gmymembership.model.Member
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MemberAdapter(
    private var membersList: List<Member>,
    private val onItemClick: (Member) -> Unit
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvMemberName)
        val chipStatus: Chip = itemView.findViewById(R.id.chipStatus)
        val tvDays: TextView = itemView.findViewById(R.id.tvRemainingDays)
        val btnEdit: ImageView = itemView.findViewById(R.id.imgEditIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = membersList[position]
        val context = holder.itemView.context

        holder.tvName.text = "${member.firstName} ${member.lastName}".trim()

        if (member.registrationDate != null) {
            val calVencimiento = Calendar.getInstance()
            calVencimiento.time = member.registrationDate!!.toDate()
            calVencimiento.add(Calendar.DAY_OF_YEAR, member.planDurationDays)

            calVencimiento.set(Calendar.HOUR_OF_DAY, 0)
            calVencimiento.set(Calendar.MINUTE, 0)
            calVencimiento.set(Calendar.SECOND, 0)
            calVencimiento.set(Calendar.MILLISECOND, 0)

            val calHoy = Calendar.getInstance()
            calHoy.set(Calendar.HOUR_OF_DAY, 0)
            calHoy.set(Calendar.MINUTE, 0)
            calHoy.set(Calendar.SECOND, 0)
            calHoy.set(Calendar.MILLISECOND, 0)

            val diferenciaMillies = calVencimiento.timeInMillis - calHoy.timeInMillis
            val diasRestantes = TimeUnit.DAYS.convert(diferenciaMillies, TimeUnit.MILLISECONDS)

            holder.tvDays.text = "$diasRestantes días restantes"

            when {
                diasRestantes > 5 -> {
                    holder.chipStatus.text = "ACTIVO"
                    holder.chipStatus.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_active))
                }
                diasRestantes in 0..5 -> {
                    holder.chipStatus.text = "POR VENCER"
                    holder.chipStatus.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_warning))
                }
                else -> {
                    holder.chipStatus.text = "VENCIDO"
                    holder.chipStatus.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_expired))
                }
            }
        } else {
            holder.tvDays.text = "Sin fecha de registro"
            holder.chipStatus.text = "INDEFINIDO"
            holder.chipStatus.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.darker_gray))
        }

        holder.itemView.setOnClickListener { onItemClick(member) }
    }

    override fun getItemCount(): Int = membersList.size

    fun updateData(newMembers: List<Member>) {
        this.membersList = newMembers
        notifyDataSetChanged()
    }
}