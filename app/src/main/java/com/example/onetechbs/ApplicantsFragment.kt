package com.example.onetechbs

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class ApplicantsFragment : Fragment() {

    private var jobId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            jobId = it.getLong(ARG_JOB_ID, 0L)
            Log.d("ApplicantsFragment", "Received jobId in fragment: $jobId")
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_applicants, container, false)
        val applicantsContainer = view.findViewById<LinearLayout>(R.id.applicantsContainer)

        val applicants = fetchApplicantsForJob(jobId)

        for (applicant in applicants) {
            val textView = TextView(requireContext())
            textView.text = applicant
            textView.textSize = 18f
            textView.setPadding(16, 16, 16, 16)
            applicantsContainer.addView(textView)
        }

        return view
    }

    private fun fetchApplicantsForJob(jobId: Long): List<String> {
        Log.d("ApplicantsFragment", "Fetching applicants for jobId: $jobId")
        // TODO: Replace with real DB fetch
        return listOf("User A", "User B", "User C")
    }

    companion object {
        private const val ARG_JOB_ID = "job_id"

        fun newInstance(jobId: Long) = ApplicantsFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_JOB_ID, jobId)
            }
        }
    }
}