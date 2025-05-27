package com.example.onetechbs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView


class LeaveListAdapter(private val leaveList: List<Leave>) : RecyclerView.Adapter<LeaveListAdapter.LeaveViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaveViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_leave, parent, false)
        return LeaveViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaveViewHolder, position: Int) {
        val leave = leaveList[position]
        holder.tvLeaveType.text = leave.type
        holder.tvLeaveDates.text = "${leave.startDate} - ${leave.endDate}"
        holder.tvLeaveStatus.text = leave.status

        // Set different status colors
        when (leave.status) {
            "Approved" -> holder.tvLeaveStatus.setTextColor(holder.itemView.context.getColor(R.color.green))
            "Pending" -> holder.tvLeaveStatus.setTextColor(holder.itemView.context.getColor(R.color.orange))
            else -> holder.tvLeaveStatus.setTextColor(holder.itemView.context.getColor(R.color.red))
        }
    }

    override fun getItemCount(): Int = leaveList.size

    class LeaveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLeaveType: TextView = itemView.findViewById(R.id.tvLeaveType)
        val tvLeaveDates: TextView = itemView.findViewById(R.id.tvLeaveDates)
        val tvLeaveStatus: TextView = itemView.findViewById(R.id.tvLeaveStatus)
    }
}