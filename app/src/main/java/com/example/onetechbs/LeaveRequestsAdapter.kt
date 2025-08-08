package com.example.onetechbs

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.os.EnvironmentCompat
import androidx.core.view.isVisible
import com.example.onetechbs.R
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.db.ELeaveType
import com.example.onetechbs.db.EStatus
import com.example.onetechbs.db.Leave
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale

// Replace LeaveResponse with your updated DTO class if needed
class LeaveRequestsAdapter(
    private val onItemClick: (Leave) -> Unit,
    private val onApprove: (Leave) -> Unit,
    private val onReject: (Leave) -> Unit,
    private val onRequestPermission: () -> Unit
) : ListAdapter<Leave, LeaveRequestsAdapter.LeaveViewHolder>(LeaveDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leave_request, parent, false)
        return LeaveViewHolder(view, onApprove, onReject, onRequestPermission)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: LeaveViewHolder, position: Int) {
        val leave = getItem(position)
        holder.bind(leave)
        holder.itemView.setOnClickListener { onItemClick(leave) }
    }

    class LeaveViewHolder(
        itemView: View,
        private val onApprove: (Leave) -> Unit,
        private val onReject: (Leave) -> Unit,
        private val onRequestPermission: () -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val employeeName: TextView = itemView.findViewById(R.id.employeeName)
        private val leaveType: TextView = itemView.findViewById(R.id.leaveType)
        private val dateRange: TextView = itemView.findViewById(R.id.dateRange)
        private val status: TextView = itemView.findViewById(R.id.status)
        private val btnApprove: MaterialButton = itemView.findViewById(R.id.btnApprove)
        private val btnReject: MaterialButton = itemView.findViewById(R.id.btnReject)
        private val userRole: String? = com.example.onetechbs.util.SharedPreferencesManager.getInstance(itemView.context).getUserRole()
        private val btnDownloadAttachment: MaterialButton = itemView.findViewById(R.id.btnDownloadAttachment)

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(leave: Leave) {
            // Reset card appearance
            itemView.alpha = 1.0f
            itemView.setBackgroundColor(Color.WHITE)

            try {
                // Extract employee name from userDn (e.g., "uid=john.doe,ou=users,dc=example,dc=com" -> "john.doe")
                val name = leave.userDn.split(",")[0].split("=")[1]
                employeeName.text = name
            } catch (e: Exception) {
                employeeName.text = leave.userDn
            }

            // Handle null leaveType
            leaveType.text = leave.leaveType?.name ?: "N/A"

            // Format date range
            dateRange.text = "${leave.startDate.format(dateFormatter)} - ${leave.endDate.format(dateFormatter)}"

            // Handle null status
            status.text = leave.status?.name ?: "EN_ATTENTE"

            // Show/Hide action buttons based on user role
            val isEmployee = userRole?.uppercase()?.contains("EMPLOYEE") == true
            val isPending = leave.status == EStatus.PENDING
            val isApproved = leave.status == EStatus.APPROVED
            val isRejected = leave.status == EStatus.REJECTED

            btnApprove.isVisible = !isEmployee && isPending
            btnReject.isVisible = !isEmployee && isPending

            // Color mapping for status
            val statusColor = when (leave.status) {
                EStatus.PENDING -> Color.parseColor("#FFA500") // Orange
                EStatus.APPROVED -> Color.parseColor("#008000") // Green
                EStatus.REJECTED -> Color.parseColor("#FF0000") // Red
                else -> Color.parseColor("#FFA500")
            }
            status.setTextColor(statusColor)

            // Card UI: fade and color for processed
            if (!isPending) {
                itemView.animate().alpha(0.5f).setDuration(400).start()
                itemView.setBackgroundColor(Color.parseColor("#F0F0F0"))
            } else {
                itemView.animate().alpha(1.0f).setDuration(400).start()
                itemView.setBackgroundColor(Color.WHITE)
            }

            // Show download button only for sick leave with attachment
            btnDownloadAttachment.visibility = if (leave.leaveType == ELeaveType.MALADIE && !leave.attachment.isNullOrEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Set up click listeners
            btnApprove.setOnClickListener {
                // Optimistically update UI
                status.text = "APPROUVÉE"
                status.setTextColor(Color.parseColor("#008000"))
                btnApprove.visibility = View.GONE
                btnReject.visibility = View.GONE
                itemView.animate().alpha(0.5f).setDuration(400).start()
                itemView.setBackgroundColor(Color.parseColor("#F0F0F0"))
                onApprove(leave)
            }

            btnReject.setOnClickListener {
                // Optimistically update UI
                status.text = "REFUSÉE"
                status.setTextColor(Color.parseColor("#FF0000"))
                btnApprove.visibility = View.GONE
                btnReject.visibility = View.GONE
                itemView.animate().alpha(0.5f).setDuration(400).start()
                itemView.setBackgroundColor(Color.parseColor("#F0F0F0"))
                onReject(leave)
            }

            btnDownloadAttachment.setOnClickListener {
                if (leave.attachment != null) {
                    // Check for storage permission
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        val permission = ContextCompat.checkSelfPermission(
                            itemView.context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                        if (permission != PackageManager.PERMISSION_GRANTED) {
                            onRequestPermission()
                            return@setOnClickListener
                        }
                    }

                    try {
                        // Decode base64 string to byte array
                        val decodedBytes = Base64.decode(leave.attachment, Base64.DEFAULT)
                        
                        // Create a file in the Downloads directory
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val fileName = "medical_certificate_${leave.userDn}.pdf"
                        val file = File(downloadsDir, fileName)
                        
                        // Write the bytes to the file
                        FileOutputStream(file).use { outputStream ->
                            outputStream.write(decodedBytes)
                        }
                        
                        // Notify the media scanner to make the file visible
                        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        mediaScanIntent.data = Uri.fromFile(file)
                        itemView.context.sendBroadcast(mediaScanIntent)
                        
                        // Get the FileProvider URI
                        val fileUri = FileProvider.getUriForFile(
                            itemView.context,
                            "${itemView.context.packageName}.fileprovider",
                            file
                        )
                        
                        // Open the PDF file using FileProvider
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(fileUri, "application/pdf")
                            flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        
                        // Grant temporary read permission to the PDF viewer app
                        val chooserIntent = Intent.createChooser(intent, "Open PDF")
                        val resInfoList = itemView.context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                        for (resolveInfo in resInfoList) {
                            itemView.context.grantUriPermission(
                                resolveInfo.activityInfo.packageName,
                                fileUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                        
                        itemView.context.startActivity(chooserIntent)
                        
                        Toast.makeText(itemView.context, "Certificate downloaded successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(itemView.context, "Error downloading certificate: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("LeaveRequestsAdapter", "Error downloading certificate", e)
                    }
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
    }

    private class LeaveDiffCallback : DiffUtil.ItemCallback<Leave>() {
        override fun areItemsTheSame(oldItem: Leave, newItem: Leave): Boolean {
            return oldItem.id == newItem.id
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Leave, newItem: Leave): Boolean {
            return oldItem == newItem
        }
    }
}