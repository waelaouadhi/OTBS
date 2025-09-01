package com.example.onetechbs

import android.annotation.SuppressLint
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.databinding.ItemLeaveRequestBinding
import com.example.onetechbs.db.EStatus
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
                val rangeText = "${leave.startDate.format(dateFormatter)} - ${leave.endDate.format(dateFormatter)}"
                dateRange.text = rangeText

                btnApprove.visibility = View.GONE
                btnReject.visibility = View.GONE

                // Status as Chip with semantic colors
                status.text = leave.status.toString()
                val ctx = root.context
                val bgColorRes = when (leave.status) {
                    EStatus.APPROVED -> android.R.color.holo_green_light
                    EStatus.REJECTED -> android.R.color.holo_red_light
                    EStatus.PENDING -> android.R.color.holo_orange_light
                    else -> android.R.color.darker_gray
                }
                val textColor = when (leave.status) {
                    EStatus.REJECTED -> android.graphics.Color.WHITE
                    else -> android.graphics.Color.BLACK
                }
                // Chip background/text color
                (status as com.google.android.material.chip.Chip).apply {
                    setChipBackgroundColorResource(bgColorRes)
                    setTextColor(textColor)
                    isClickable = false
                    isCheckable = false
                    contentDescription = "Status: ${leave.status}"
                }

                // Accessibility: summarize the row for TalkBack
                root.contentDescription = buildString {
                    append("Leave request. ")
                    append("Employee: ")
                    append(leave.name)
                    append(". Type: ")
                    append(leave.leaveType.toString())
                    append(". Dates: ")
                    append(rangeText)
                    append(". Status: ")
                    append(leave.status.toString())
                }
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