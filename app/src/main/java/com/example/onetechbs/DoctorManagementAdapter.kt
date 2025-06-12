package com.example.onetechbs.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.databinding.ItemDoctorManagementBinding
import com.example.onetechbs.db.MedicalVisitResponse


class DoctorManagementAdapter(
    private val onDeleteClicked: (MedicalVisitResponse) -> Unit
) : RecyclerView.Adapter<DoctorManagementAdapter.VisitViewHolder>() {

    private val visits = mutableListOf<MedicalVisitResponse>()

    fun submitList(newList: List<MedicalVisitResponse>) {
        visits.clear()
        visits.addAll(newList)
        notifyDataSetChanged()
    }

    inner class VisitViewHolder(private val binding: ItemDoctorManagementBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(visit: MedicalVisitResponse) {
            binding.doctorName.text = "Doctor: ${visit.doctorName}"
            binding.visitDate.text = "Date: ${visit.visitDate}"
            binding.timeRange.text = "Time: ${visit.startTime} - ${visit.endTime}"
            binding.deleteButton.setOnClickListener {
                onDeleteClicked(visit)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitViewHolder {
        val binding = ItemDoctorManagementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VisitViewHolder(binding)
    }

    override fun getItemCount(): Int = visits.size

    override fun onBindViewHolder(holder: VisitViewHolder, position: Int) {
        holder.bind(visits[position])
    }
}