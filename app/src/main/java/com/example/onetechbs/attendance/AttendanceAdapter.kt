package com.example.onetechbs.attendance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.R

class AttendanceAdapter : ListAdapter<AttendanceRecordResponseDTO, AttendanceAdapter.AttendanceViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_attendance_record, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(record: AttendanceRecordResponseDTO) {
            itemView.findViewById<TextView>(R.id.employeeNameTextView).text = record.employeeName
            itemView.findViewById<TextView>(R.id.departmentTextView).text = record.department
            itemView.findViewById<TextView>(R.id.dateTextView).text = record.date
            itemView.findViewById<TextView>(R.id.statusTextView).text = record.status
            itemView.findViewById<TextView>(R.id.firstPunchTextView).text = record.firstPunch ?: "-"
            itemView.findViewById<TextView>(R.id.lastPunchTextView).text = record.lastPunch ?: "-"
            itemView.findViewById<TextView>(R.id.totalHoursTextView).text = record.totalHours
            itemView.findViewById<TextView>(R.id.issuesTextView).text = record.issues.joinToString(", ")
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AttendanceRecordResponseDTO>() {
        override fun areItemsTheSame(oldItem: AttendanceRecordResponseDTO, newItem: AttendanceRecordResponseDTO): Boolean {
            return oldItem.employeeId == newItem.employeeId && oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: AttendanceRecordResponseDTO, newItem: AttendanceRecordResponseDTO): Boolean {
            return oldItem == newItem
        }
    }
}
