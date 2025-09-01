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
import androidx.core.content.ContextCompat
import android.text.format.DateUtils
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

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

            // Timestamp -> relative time
            binding.timestampTextView.text = formatRelativeTime(notification.createdAt)

            // Color-code card and icon based on type
            val (bgColor, onBgColor) = pickColors(notification.type)
            val ctx = binding.root.context
            binding.root.setCardBackgroundColor(ContextCompat.getColor(ctx, bgColor))
            binding.notificationIcon.setColorFilter(ContextCompat.getColor(ctx, onBgColor))
        }

        private fun formatRelativeTime(createdAt: String): CharSequence {
            return try {
                val odt = OffsetDateTime.parse(createdAt)
                val millis = odt.atZoneSameInstant(ZoneId.systemDefault()).toInstant().toEpochMilli()
                DateUtils.getRelativeTimeSpanString(
                    millis,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )
            } catch (e: DateTimeParseException) {
                createdAt
            }
        }

        private fun pickColors(type: String?): Pair<Int, Int> {
            val t = (type ?: "").uppercase()
            return when {
                t.contains("LEAVE") || t.contains("ABSENCE") -> R.color.md_theme_tertiaryContainer to R.color.md_theme_onTertiaryContainer
                t.contains("MEDICAL") || t.contains("DOCTOR") -> R.color.md_theme_secondaryContainer to R.color.md_theme_onSecondaryContainer
                t.contains("TRAIN") || t.contains("COURSE") || t.contains("LEARNING") -> R.color.md_theme_primaryContainer to R.color.md_theme_onPrimaryContainer
                t.contains("DOC") -> R.color.md_theme_surfaceVariant to R.color.md_theme_onSurfaceVariant
                t.contains("SYSTEM") || t.contains("ALERT") -> R.color.md_theme_errorContainer to R.color.md_theme_onErrorContainer
                else -> R.color.md_theme_surfaceVariant to R.color.md_theme_onSurfaceVariant
            }
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