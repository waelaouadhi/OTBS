package com.example.onetechbs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.db.TrainingRequestResponseDTO

class EnrollmentsAdapter(
    private val enrollments: List<TrainingRequestResponseDTO>,
    private val userRole: String?,
    private val onApprove: (TrainingRequestResponseDTO) -> Unit,
    private val onReject: (TrainingRequestResponseDTO) -> Unit
) : RecyclerView.Adapter<EnrollmentsAdapter.EnrollmentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnrollmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training_request, parent, false)
        return EnrollmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: EnrollmentViewHolder, position: Int) {
        val enrollment = enrollments[position]
        holder.bind(enrollment, userRole, onApprove, onReject)
    }

    override fun getItemCount(): Int = enrollments.size

    class EnrollmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val employeeName: TextView = itemView.findViewById(R.id.textViewEmployeeName)
        private val courseTitle: TextView = itemView.findViewById(R.id.textViewCourseTitle)
        private val status: TextView = itemView.findViewById(R.id.textViewRequestStatus)
        private val btnApprove: Button = itemView.findViewById(R.id.btnApprove)
        private val btnReject: Button = itemView.findViewById(R.id.btnReject)
        private val rejectionReason: TextView = itemView.findViewById(R.id.textViewRejectionReason)

        fun bind(
            enrollment: TrainingRequestResponseDTO,
            userRole: String?,
            onApprove: (TrainingRequestResponseDTO) -> Unit,
            onReject: (TrainingRequestResponseDTO) -> Unit
        ) {
            employeeName.text = enrollment.employeeFullName ?: enrollment.employeeId ?: ""
            courseTitle.text = enrollment.course?.title ?: ""
            status.text = "Status: ${enrollment.status ?: ""}"

            // Show rejection reason if rejected
            if (enrollment.status == "REJECTED" && !enrollment.rejectionReason.isNullOrEmpty()) {
                rejectionReason.visibility = View.VISIBLE
                rejectionReason.text = "Reason: ${enrollment.rejectionReason}"
            } else {
                rejectionReason.visibility = View.GONE
            }

            // Only show approve/reject for PENDING and HR/HRD
            if ((userRole == "HR" || userRole == "HRD") && enrollment.status == "PENDING") {
                btnApprove.visibility = View.VISIBLE
                btnReject.visibility = View.VISIBLE
                btnApprove.isEnabled = true
                btnReject.isEnabled = true
                btnApprove.setOnClickListener { onApprove(enrollment) }
                btnReject.setOnClickListener { onReject(enrollment) }
            } else {
                btnApprove.visibility = View.GONE
                btnReject.visibility = View.GONE
            }
        }
    }
}
