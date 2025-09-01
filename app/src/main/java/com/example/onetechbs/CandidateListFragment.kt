package com.example.onetechbs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.db.CandidateResponseDTO
import com.example.onetechbs.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class CandidateListFragment : Fragment() {

    private var jobOfferId: Long = -1
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var progressBar: ProgressBar
    private lateinit var sortDropdown: MaterialAutoCompleteTextView

    private var allCandidates: List<CandidateResponseDTO> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        jobOfferId = arguments?.getLong(ARG_JOB_ID) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_candidate_list, container, false)
        recyclerView = root.findViewById(R.id.candidatesRecyclerView)
        emptyView = root.findViewById(R.id.emptyView)
        progressBar = root.findViewById(R.id.progressBar)
        sortDropdown = root.findViewById(R.id.sortDropdown)
        val toolbar: MaterialToolbar = root.findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = CandidatesAdapter(emptyList()) { candidate ->
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_layout,
                    CandidateDetailsFragment.newInstance(jobOfferId, candidate.id)
                )
                .addToBackStack(null)
                .commit()
        }

        // Setup sort dropdown
        sortDropdown.setSimpleItems(resources.getStringArray(R.array.candidate_sort_options))
        sortDropdown.setOnItemClickListener { _, _, position, _ ->
            applySorting(position)
        }

        loadCandidates()
        return root
    }

    private fun loadCandidates() {
        progressBar.visibility = View.VISIBLE
        val prefs = com.example.onetechbs.util.SharedPreferencesManager.getInstance(requireContext())
        val token = prefs.getAuthToken()
        if (token.isNullOrEmpty() || prefs.isTokenExpired()) {
            progressBar.visibility = View.GONE
            Snackbar.make(requireView(), "Session expired", Snackbar.LENGTH_LONG).show()
            return
        }
        val authHeader = "Bearer $token"
        RetrofitClient.candidateService.listCandidates(authHeader).enqueue(object : Callback<List<CandidateResponseDTO>> {
            override fun onResponse(
                call: Call<List<CandidateResponseDTO>>, response: Response<List<CandidateResponseDTO>>
            ) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    allCandidates = response.body().orEmpty()
                    // Default sort: Newest first
                    sortDropdown.setText(getString(R.string.candidate_sort_newest), false)
                    applySorting(0)
                    emptyView.visibility = if (allCandidates.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Snackbar.make(requireView(), "Error: ${response.code()}", Snackbar.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<List<CandidateResponseDTO>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Snackbar.make(requireView(), t.localizedMessage ?: "Network error", Snackbar.LENGTH_LONG).show()
            }
        })
    }

    private fun applySorting(position: Int) {
        val sorted = when (position) {
            0 -> allCandidates.sortedByDescending { safeMillis(it.createdAt) }
            1 -> allCandidates.sortedBy { safeMillis(it.createdAt) }
            else -> allCandidates
        }
        (recyclerView.adapter as CandidatesAdapter).submit(sorted)
    }

    private fun safeMillis(iso: String): Long {
        // Try a few common ISO patterns from backend
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
        )
        for (p in patterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(iso)?.time ?: 0L
            } catch (_: ParseException) { }
        }
        return 0L
    }

    private class CandidatesAdapter(
        private var items: List<CandidateResponseDTO>,
        val onClick: (CandidateResponseDTO) -> Unit
    ) : RecyclerView.Adapter<CandidatesAdapter.VH>() {

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.name)
            val email: TextView = itemView.findViewById(R.id.email)
            val phone: TextView? = itemView.findViewById(R.id.phone)
            val departmentChip: Chip? = itemView.findViewById(R.id.departmentChip)
            val statusChip: Chip? = itemView.findViewById(R.id.statusChip)
            val appliedDate: TextView? = itemView.findViewById(R.id.appliedDate)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_candidate_row, parent, false)
            return VH(v)
        }
        override fun getItemCount(): Int = items.size
        override fun onBindViewHolder(holder: VH, position: Int) {
            val c = items[position]
            holder.name.text = c.candidateInfo.name
            holder.email.text = c.candidateInfo.email
            holder.phone?.apply {
                text = c.candidateInfo.phone
                visibility = if (c.candidateInfo.phone.isNullOrBlank()) View.GONE else View.VISIBLE
            }
            // No department/status in DTO; hide chips
            holder.departmentChip?.visibility = View.GONE
            holder.statusChip?.visibility = View.GONE

            holder.appliedDate?.apply {
                text = formatAppliedDate(c.createdAt)
                visibility = View.VISIBLE
            }

            holder.itemView.setOnClickListener { onClick(c) }
        }
        fun submit(list: List<CandidateResponseDTO>) {
            items = list
            notifyDataSetChanged()
        }

        private fun safeMillis(iso: String): Long {
            val patterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX"
            )
            for (p in patterns) {
                try {
                    val sdf = SimpleDateFormat(p, Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    return sdf.parse(iso)?.time ?: 0L
                } catch (_: ParseException) { }
            }
            return 0L
        }

        private fun formatAppliedDate(iso: String): String {
            val millis = try { safeMillis(iso) } catch (_: Exception) { 0L }
            return if (millis > 0L) {
                val out = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                out.timeZone = TimeZone.getDefault()
                "Applied · ${out.format(Date(millis))}"
            } else {
                "Applied"
            }
        }
    }

    companion object {
        private const val ARG_JOB_ID = "arg_job_id"
        fun newInstance(jobOfferId: Long): CandidateListFragment {
            return CandidateListFragment().apply {
                arguments = Bundle().apply { putLong(ARG_JOB_ID, jobOfferId) }
            }
        }
    }
}
