package com.example.onetechbs

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.databinding.ItemNotificationBinding
import com.example.onetechbs.db.Notification

class NotificationAdapter : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        // item_notification.xml root is now a LinearLayout, not a CardView
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = getItem(position)
        // Log the notification data
        Log.d("NotificationAdapter", "Binding notification: ${notification.title}, ${notification.message}")
        holder.bind(notification)
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: Notification) {
            // Binding the data to the UI components (TextViews)
            binding.titleTextView.text = notification.title
            binding.messageTextView.text = notification.message
        }
    }

    // DiffUtil callback to compare items for efficient updates
    class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            // Compare items by their unique id
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            // Compare the contents of the two notifications
            return oldItem == newItem
        }
    }
}