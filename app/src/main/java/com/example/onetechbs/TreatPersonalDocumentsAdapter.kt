package com.example.onetechbs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.db.PersonalDocumentResponseDTO
import com.example.onetechbs.db.EDocumentStatus

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
        private val statusText: TextView = itemView.findViewById(R.id.text_status)
        private val approveBtn: Button = itemView.findViewById(R.id.button_approve)
        private val rejectBtn: Button = itemView.findViewById(R.id.button_reject)
        private val processingBtn: Button = itemView.findViewById(R.id.button_processing)

        fun bind(
            doc: PersonalDocumentResponseDTO,
            onApprove: (PersonalDocumentResponseDTO) -> Unit,
            onReject: (PersonalDocumentResponseDTO) -> Unit,
            onProcessing: (PersonalDocumentResponseDTO) -> Unit
        ) {
            employeeNameText.text = doc.employeeName
            documentTypeText.text = doc.documentType.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercaseChar() }
            notesText.text = "Notes: ${doc.notes}"
            statusText.text = "Status: ${doc.status.name}".replace('_', ' ').lowercase().replaceFirstChar { it.uppercaseChar() }
            approveBtn.setOnClickListener { onApprove(doc) }
            rejectBtn.setOnClickListener { onReject(doc) }
            processingBtn.setOnClickListener { onProcessing(doc) }
            // Optionally, disable buttons based on status
            val isPending = doc.status == EDocumentStatus.PENDING
            approveBtn.isEnabled = isPending
            rejectBtn.isEnabled = isPending
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
