package com.example.onetechbs

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.databinding.ItemLeaveRequestBinding
import com.example.onetechbs.db.LeaveResponse
import com.example.onetechbs.db.LeaveStatus
import java.time.format.DateTimeFormatter

class LeaveListAdapter : ListAdapter<LeaveResponse, LeaveListAdapter.LeaveViewHolder>(LeaveDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaveViewHolder {
        val binding = ItemLeaveRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LeaveViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeaveViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LeaveViewHolder(
        private val binding: ItemLeaveRequestBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @RequiresApi(Build.VERSION_CODES.O)
        private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        @SuppressLint("SetTextI18n")
        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(leave: LeaveResponse) {
            binding.apply {
                employeeName.text = leave.name
                leaveType.text = leave.leaveType.toString()
                dateRange.text = "${leave.startDate.format(dateFormatter)} - ${leave.endDate.format(dateFormatter)}"
                
                // Set status text and color
                status.text = leave.status.toString()
                status.setBackgroundResource(
                    when (leave.status) {
                        LeaveStatus.APPROVED -> android.R.color.holo_green_light
                        LeaveStatus.REJECTED -> android.R.color.holo_red_light
                        LeaveStatus.PENDING -> android.R.color.holo_orange_light
                        else -> android.R.color.darker_gray
                    }
                )
            }
        }
    }

    private class LeaveDiffCallback : DiffUtil.ItemCallback<LeaveResponse>() {
        override fun areItemsTheSame(oldItem: LeaveResponse, newItem: LeaveResponse): Boolean {
            return oldItem.id == newItem.id
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: LeaveResponse, newItem: LeaveResponse): Boolean {
            return oldItem == newItem
        }
    }
}