package com.example.onetechbs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.databinding.ItemDoctorVisitBinding
import com.example.onetechbs.db.MedicalVisitResponse

class DoctorVisitAdapter :
    ListAdapter<MedicalVisitResponse, DoctorVisitAdapter.DoctorVisitViewHolder>(DoctorVisitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorVisitViewHolder {
        val binding = ItemDoctorVisitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DoctorVisitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DoctorVisitViewHolder, position: Int) {
        val visit = getItem(position)
        holder.bind(visit)
    }

    inner class DoctorVisitViewHolder(private val binding: ItemDoctorVisitBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(visit: MedicalVisitResponse) {
            binding.apply {
                doctorNameTextView.text = visit.doctorName
                visitDateTextView.text = visit.visitDate.toString()
                startTimeTextView.text = visit.startTime.toString()
                endTimeTextView.text = visit.endTime.toString()
            }
        }
    }

    class DoctorVisitDiffCallback : DiffUtil.ItemCallback<MedicalVisitResponse>() {
        override fun areItemsTheSame(
            oldItem: MedicalVisitResponse, newItem: MedicalVisitResponse
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: MedicalVisitResponse, newItem: MedicalVisitResponse
        ): Boolean {
            return oldItem == newItem
        }
    }
}
