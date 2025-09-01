package com.example.onetechbs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.db.CourseResponseDTO
import java.util.Locale

class CoursesAdapter(
    private val courses: MutableList<CourseResponseDTO>,
    private val userRole: String?,
    private val onEnrollApproveClick: (CourseResponseDTO) -> Unit,
    // Map of courseId -> status (APPROVED, PENDING, REJECTED) for current user
    private val userStatuses: Map<Long, String> = emptyMap()
) : RecyclerView.Adapter<CoursesAdapter.CourseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        val explicitStatus = userStatuses[course.id]?.uppercase(Locale.ROOT)
        holder.bind(course, userRole, onEnrollApproveClick, explicitStatus)
    }

    override fun getItemCount(): Int = courses.size

    // Update helpers to reflect server/client state immediately
    fun markRequested(courseId: Long) {
        val index = courses.indexOfFirst { it.id == courseId }
        if (index >= 0) {
            courses[index] = courses[index].copy(isRequested = true, isEnrolled = false)
            notifyItemChanged(index)
        }
    }

    fun markEnrolled(courseId: Long) {
        val index = courses.indexOfFirst { it.id == courseId }
        if (index >= 0) {
            courses[index] = courses[index].copy(isEnrolled = true, isRequested = false)
            notifyItemChanged(index)
        }
    }

    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textViewCourseTitle)
        private val description: TextView = itemView.findViewById(R.id.textViewCourseDescription)
        private val certification: TextView = itemView.findViewById(R.id.textViewCourseCertification)
        private val cost: TextView = itemView.findViewById(R.id.textViewCourseCost)
        private val btnEnrollApprove: Button = itemView.findViewById(R.id.btnEnrollApprove)
        private val status: TextView = itemView.findViewById(R.id.textViewCourseStatus)

        fun bind(
            course: CourseResponseDTO,
            userRole: String?,
            onEnrollApproveClick: (CourseResponseDTO) -> Unit,
            explicitStatus: String?
        ) {
            title.text = course.title
            description.text = "Description: ${course.description}"
            certification.text = "Certification: ${if (course.hasCertification) "Yes" else "No"}"
            // Hide cost in UI
            cost.visibility = View.GONE

            // Reset recycled views to a neutral state
            btnEnrollApprove.setOnClickListener(null)
            btnEnrollApprove.visibility = View.GONE
            btnEnrollApprove.isEnabled = false
            status.text = ""

            // Determine enrolled/requested state with server as source of truth, then reconcile local cache
            val prefs = itemView.context.getSharedPreferences("training", android.content.Context.MODE_PRIVATE)
            val enrolledSet = (prefs.getStringSet("enrolled_ids", emptySet()) ?: emptySet()).toMutableSet()
            val requestedSet = (prefs.getStringSet("requested_ids", emptySet()) ?: emptySet()).toMutableSet()

            // Reconcile cache with server flags:
            // - If server says enrolled -> ensure in enrolledSet and not in requestedSet
            // - If server says not requested -> ensure it's removed from requestedSet
            // Server truth always wins over cache.
            val idStr = course.id.toString()
            if (course.isEnrolled) {
                enrolledSet.add(idStr)
                requestedSet.remove(idStr)
            }
            if (!course.isRequested) {
                requestedSet.remove(idStr)
            }
            // Persist any reconciliation changes
            prefs.edit()
                .putStringSet("enrolled_ids", enrolledSet)
                .putStringSet("requested_ids", requestedSet)
                .apply()

            val isEnrolledOrCached = course.isEnrolled || enrolledSet.contains(idStr)
            val isRequestedOrCached = course.isRequested || requestedSet.contains(idStr)

            // If APPROVED or enrolled, hide button and show Approved
            if (explicitStatus == "APPROVED" || isEnrolledOrCached) {
                btnEnrollApprove.visibility = View.GONE
                status.visibility = View.VISIBLE
                status.text = "Approved"
                status.setTextColor(itemView.resources.getColor(android.R.color.holo_green_dark))
                return
            }

            // If request pending (explicit or cached), show disabled Requested
            if (explicitStatus == "PENDING" || isRequestedOrCached) {
                btnEnrollApprove.visibility = View.VISIBLE
                btnEnrollApprove.text = "Requested"
                btnEnrollApprove.isEnabled = false
                status.visibility = View.VISIBLE
                status.text = "Pending"
                status.setTextColor(itemView.resources.getColor(android.R.color.holo_orange_dark))
                return
            }

            // If REJECTED, show status and hide button
            if (explicitStatus == "REJECTED") {
                btnEnrollApprove.visibility = View.GONE
                status.visibility = View.VISIBLE
                status.text = "Rejected"
                status.setTextColor(itemView.resources.getColor(android.R.color.holo_red_dark))
                return
            }

            // Role-based logic: only Employees can enroll
            val canEnroll = userRole?.equals("Employee", ignoreCase = true) == true
            if (canEnroll) {
                btnEnrollApprove.visibility = View.VISIBLE
                btnEnrollApprove.text = "Enroll"
                btnEnrollApprove.isEnabled = true
                status.visibility = View.VISIBLE
                status.text = ""
                status.setTextColor(itemView.resources.getColor(android.R.color.darker_gray))
                btnEnrollApprove.setOnClickListener { onEnrollApproveClick(course) }
            } else {
                btnEnrollApprove.visibility = View.GONE
                status.visibility = View.GONE
            }
        }
    }
}
