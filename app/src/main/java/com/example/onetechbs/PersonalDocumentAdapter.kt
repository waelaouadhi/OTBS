package com.example.onetechbs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.db.PersonalDocumentResponseDTO
import java.time.format.DateTimeFormatter

class PersonalDocumentAdapter(
    private var documents: List<PersonalDocumentResponseDTO>
) : RecyclerView.Adapter<PersonalDocumentAdapter.DocumentViewHolder>() {

    fun updateData(newDocs: List<PersonalDocumentResponseDTO>) {
        documents = newDocs
        android.util.Log.d("PersonalDocumentAdapter", "updateData called with size: ${newDocs.size}")
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_personal_document, parent, false)
        return DocumentViewHolder(view)
    }

    override fun getItemCount(): Int = documents.size

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        android.util.Log.d("PersonalDocumentAdapter", "onBindViewHolder position: $position")
        holder.bind(documents[position])
    }

    class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val typeText: TextView = itemView.findViewById(R.id.text_document_type)
        private val statusText: TextView = itemView.findViewById(R.id.text_status)
        private val dateText: TextView = itemView.findViewById(R.id.text_request_date)
        private val notesText: TextView = itemView.findViewById(R.id.text_notes)

        fun bind(doc: PersonalDocumentResponseDTO) {
            typeText.text = doc.documentType.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercaseChar() }
            statusText.text = doc.status.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercaseChar() }
            dateText.text = "Requested: ${doc.requestDate.format(DateTimeFormatter.ISO_DATE)}"
            notesText.text = "Notes: ${doc.notes}"
        }
    }
}
