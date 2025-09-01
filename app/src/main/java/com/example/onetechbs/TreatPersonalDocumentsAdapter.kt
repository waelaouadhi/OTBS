package com.example.onetechbs

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.db.PersonalDocumentResponseDTO
import com.example.onetechbs.db.EDocumentStatus
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class TreatPersonalDocumentsAdapter(
    private val onApprove: (PersonalDocumentResponseDTO) -> Unit,
    private val onReject: (PersonalDocumentResponseDTO) -> Unit,
    private val onProcessing: (PersonalDocumentResponseDTO) -> Unit
) : ListAdapter<PersonalDocumentResponseDTO, TreatPersonalDocumentsAdapter.DocumentViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_document_request, parent, false)
        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(getItem(position), onApprove, onReject, onProcessing)
    }

    class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val employeeNameText: TextView = itemView.findViewById(R.id.text_employee_name)
        private val documentTypeText: TextView = itemView.findViewById(R.id.text_document_type)
        private val notesText: TextView = itemView.findViewById(R.id.text_notes)
        private val requestDateText: TextView = itemView.findViewById(R.id.text_request_date)
        private val statusChip: Chip = itemView.findViewById(R.id.chip_status)
        private val approveBtn: MaterialButton = itemView.findViewById(R.id.button_approve)
        private val rejectBtn: MaterialButton = itemView.findViewById(R.id.button_reject)
        private val processingBtn: MaterialButton = itemView.findViewById(R.id.button_processing)

        fun bind(
            doc: PersonalDocumentResponseDTO,
            onApprove: (PersonalDocumentResponseDTO) -> Unit,
            onReject: (PersonalDocumentResponseDTO) -> Unit,
            onProcessing: (PersonalDocumentResponseDTO) -> Unit
        ) {
            employeeNameText.text = doc.employeeName
            documentTypeText.text = doc.documentType.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercaseChar() }
            notesText.text = "Notes: ${doc.notes}"
            // Render a short request date; if ISO-8601, take first 10 chars (yyyy-MM-dd)
            val shortDate = doc.requestDate.takeIf { it.isNotBlank() }?.let { it.substring(0, kotlin.math.min(10, it.length)) } ?: "--"
            requestDateText.text = "Requested: $shortDate"
            val statusText = doc.status.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercaseChar() }
            statusChip.text = statusText
            // Optional: visual cue by status (soft background colors)
            val color = when (doc.status) {
                EDocumentStatus.PENDING -> "#E3F2FD" // light blue
                EDocumentStatus.PROCESSING -> "#EDE7F6" // light purple
                EDocumentStatus.COMPLETED -> "#E8F5E9" // light green
                EDocumentStatus.REJECTED -> "#FFEBEE" // light red
            }
            statusChip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(color))
            approveBtn.setOnClickListener { onApprove(doc) }
            rejectBtn.setOnClickListener { onReject(doc) }
            processingBtn.setOnClickListener { onProcessing(doc) }
            // Visibility rules:
            // - Approve/Reject visible for PENDING and PROCESSING
            // - Process visible only for PENDING
            val isPending = doc.status == EDocumentStatus.PENDING
            val showApproveReject = doc.status == EDocumentStatus.PENDING || doc.status == EDocumentStatus.PROCESSING
            approveBtn.visibility = if (showApproveReject) View.VISIBLE else View.GONE
            rejectBtn.visibility = if (showApproveReject) View.VISIBLE else View.GONE
            processingBtn.visibility = if (isPending) View.VISIBLE else View.GONE
            // Enabled states align with visibility
            approveBtn.isEnabled = showApproveReject
            rejectBtn.isEnabled = showApproveReject
            processingBtn.isEnabled = isPending
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<PersonalDocumentResponseDTO>() {
        override fun areItemsTheSame(oldItem: PersonalDocumentResponseDTO, newItem: PersonalDocumentResponseDTO): Boolean =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PersonalDocumentResponseDTO, newItem: PersonalDocumentResponseDTO): Boolean =
            oldItem == newItem
    }
}
