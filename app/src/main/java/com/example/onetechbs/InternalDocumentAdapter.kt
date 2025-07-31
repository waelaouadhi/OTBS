package com.example.onetechbs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.db.InternalDocumentResponseDTO
import com.example.onetechbs.R

class InternalDocumentAdapter(
    private val documents: List<InternalDocumentResponseDTO>,
    private val onDownloadClick: (InternalDocumentResponseDTO) -> Unit
) : RecyclerView.Adapter<InternalDocumentAdapter.DocumentViewHolder>() {

    inner class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        val categoryTextView: TextView = itemView.findViewById(R.id.categoryTextView)
        val downloadButton: Button = itemView.findViewById(R.id.downloadButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_internal_document, parent, false)
        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val document = documents[position]
        holder.titleTextView.text = document.title
        holder.descriptionTextView.text = document.description
        holder.categoryTextView.text = document.category
        holder.downloadButton.setOnClickListener {
            onDownloadClick(document)
        }
    }

    override fun getItemCount(): Int = documents.size
}
