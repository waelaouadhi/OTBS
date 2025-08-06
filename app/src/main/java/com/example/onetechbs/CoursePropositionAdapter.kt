package com.example.onetechbs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.db.CoursePropositionResponseDTO
import java.time.format.DateTimeFormatter

class CoursePropositionAdapter(
    private val propositions: List<CoursePropositionResponseDTO>,
    private val isDhr: Boolean,
    private val onApprove: (CoursePropositionResponseDTO) -> Unit,
    private val onReject: (CoursePropositionResponseDTO) -> Unit
) : RecyclerView.Adapter<CoursePropositionAdapter.PropositionViewHolder>() {

    // Track expanded state for each item
    private val expandedPositions = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropositionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course_proposition, parent, false)
        return PropositionViewHolder(view)
    }



    override fun getItemCount(): Int = propositions.size

    fun getPropositionAt(position: Int): CoursePropositionResponseDTO? =
        if (position in propositions.indices) propositions[position] else null

    class PropositionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textViewTitle)
        private val manager: TextView = itemView.findViewById(R.id.textViewManager)
        private val department: TextView = itemView.findViewById(R.id.textViewDepartment)
        private val createdAt: TextView = itemView.findViewById(R.id.textViewCreatedAt)
        private val status: TextView = itemView.findViewById(R.id.textViewStatus)
        private val actionsLayout: LinearLayout = itemView.findViewById(R.id.layoutActions)
        private val btnApprove: Button = itemView.findViewById(R.id.btnApprove)
        private val btnReject: Button = itemView.findViewById(R.id.btnReject)
        private val detailsLayout: LinearLayout = itemView.findViewById(R.id.layoutDetails)
        private val description: TextView = itemView.findViewById(R.id.textViewDescription)
        private val justification: TextView = itemView.findViewById(R.id.textViewJustification)
        private val expectedOutcomes: TextView = itemView.findViewById(R.id.textViewExpectedOutcomes)
        private val provider: TextView = itemView.findViewById(R.id.textViewProvider)
        private val certified: TextView = itemView.findViewById(R.id.textViewCertified)

        fun bind(
            proposition: CoursePropositionResponseDTO,
            isDhr: Boolean,
            onApprove: (CoursePropositionResponseDTO) -> Unit,
            onReject: (CoursePropositionResponseDTO) -> Unit,
            expanded: Boolean
        ) {
            title.text = proposition.title
            manager.text = "Proposed By: ${proposition.managerFullName}"
            department.text = "Department: ${proposition.department}"
            createdAt.text = "Request Date: " + proposition.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            status.text = "Status: ${proposition.status}"

            // Hide approve/reject buttons for swipe UX
            actionsLayout.visibility = View.GONE

            // Bind details
            description.text = "Description: ${proposition.description}"
            justification.text = "Justification: ${proposition.justification}"
            expectedOutcomes.text = "Expected Outcomes: ${proposition.expectedOutcomes}"
            provider.text = "Provider: ${proposition.preferredProvider ?: "-"}"
            certified.text = "Certification Required: ${if (proposition.isCertified) "Yes" else "No"}"
            detailsLayout.visibility = if (expanded) View.VISIBLE else View.GONE
        }
    }

    override fun onBindViewHolder(holder: PropositionViewHolder, position: Int) {
        val proposition = propositions[position]
        val expanded = expandedPositions.contains(position)
        holder.bind(proposition, isDhr, onApprove, onReject, expanded)
        holder.itemView.setOnClickListener {
            if (expanded) {
                expandedPositions.remove(position)
            } else {
                expandedPositions.add(position)
            }
            notifyItemChanged(position)
        }
    }
}
