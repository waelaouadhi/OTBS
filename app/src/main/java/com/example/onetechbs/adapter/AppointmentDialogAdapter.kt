package com.example.onetechbs.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.R
import com.example.onetechbs.db.AppointmentResponseDTO

class AppointmentDialogAdapter(
    private val appointments: List<AppointmentResponseDTO>
) : RecyclerView.Adapter<AppointmentDialogAdapter.AppointmentViewHolder>() {

    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        val tvDateTime: TextView = itemView.findViewById(R.id.tvDateTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_appointment_dialog, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appt = appointments[position]
        holder.tvName.text = appt.employeeFullName ?: "Unknown"
        holder.tvEmail.text = appt.employeeEmail ?: ""
        holder.tvDateTime.text = "Date & Time: ${appt.timeSlot}"
    }

    override fun getItemCount(): Int = appointments.size
}
