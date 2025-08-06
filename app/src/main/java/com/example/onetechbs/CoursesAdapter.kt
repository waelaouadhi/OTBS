package com.example.onetechbs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.db.CourseResponseDTO

class CoursesAdapter(
    private val courses: List<CourseResponseDTO>,
    private val userRole: String?,
    private val onEnrollApproveClick: (CourseResponseDTO) -> Unit
) : RecyclerView.Adapter<CoursesAdapter.CourseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        holder.bind(course, userRole, onEnrollApproveClick)
    }

    override fun getItemCount(): Int = courses.size

    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textViewCourseTitle)
        private val description: TextView = itemView.findViewById(R.id.textViewCourseDescription)
        private val certification: TextView = itemView.findViewById(R.id.textViewCourseCertification)
        private val cost: TextView = itemView.findViewById(R.id.textViewCourseCost)
        private val btnEnrollApprove: Button = itemView.findViewById(R.id.btnEnrollApprove)
        private val status: TextView = itemView.findViewById(R.id.textViewCourseStatus)

        fun bind(course: CourseResponseDTO, userRole: String?, onEnrollApproveClick: (CourseResponseDTO) -> Unit) {
            title.text = course.title
            description.text = "Description: ${course.description}"
            certification.text = "Certification: ${if (course.hasCertification) "Yes" else "No"}"
            cost.text = "Cost: ${course.cost?.toString() ?: "-"}"

            // Role-based logic
            if (userRole == "Employee" || userRole == "Manager" || userRole == "HR") {
                btnEnrollApprove.visibility = View.VISIBLE
                btnEnrollApprove.isEnabled = true
                when {
                    course.isEnrolled -> {
                        btnEnrollApprove.visibility = View.GONE
                        status.text = "Enrolled"
                    }
                    course.isRequested -> {
                        btnEnrollApprove.text = "Requested"
                        btnEnrollApprove.isEnabled = false
                        status.text = "Request Pending"
                    }
                    else -> {
                        btnEnrollApprove.text = "Enroll"
                        btnEnrollApprove.isEnabled = true
                        status.text = ""
                        btnEnrollApprove.setOnClickListener { onEnrollApproveClick(course) }
                    }
                }
            } else {
                btnEnrollApprove.visibility = View.GONE
                status.text = ""
            }
        }
    }
}
