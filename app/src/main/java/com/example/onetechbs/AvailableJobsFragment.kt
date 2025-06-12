package com.example.onetechbs

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.content.Intent
import com.example.onetechbs.util.SharedPreferencesManager
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.onetechbs.db.CandidateResponseDTO
import com.example.onetechbs.db.JobOfferResponseDTO
import com.example.onetechbs.network.RetrofitClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AvailableJobsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var emptyView: View
    private lateinit var jobAdapter: JobAdapter
    private var isHR = false
    private var rootView: View? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_available_jobs, container, false)
            initializeViews()
            setupUserRole()
            setupRecyclerView()
            setupSwipeRefresh()
            loadJobs()
        }
        return rootView
    }

    private fun initializeViews() {
        rootView?.let { view ->
            recyclerView = view.findViewById(R.id.jobsRecyclerView)
            swipeRefreshLayout = view.findViewById(R.id.swipeRefresh)
            emptyView = view.findViewById(R.id.emptyView)
        }
    }

    private fun setupUserRole() {
        val prefs = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE)
        isHR = prefs.getString("role", "employee") == "HR"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupRecyclerView() {
        jobAdapter = JobAdapter(
            isHR = isHR,
            onDeleteClick = { job -> handleDeleteJob(job) },
            onUpdateClick = { job -> handleUpdateJob(job) },
            onFinishClick = { job -> handleFinishJob(job) },
            onApplicantsClick = { job -> handleViewApplicants(job) }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = jobAdapter
            setHasFixedSize(true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.apply {
            setColorSchemeResources(R.color.purple_500)
            setOnRefreshListener { loadJobs() }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadJobs() {
        showLoading(true)
        RetrofitClient.recruitingService.getAllJobOffers()
            .enqueue(object : Callback<List<JobOfferResponseDTO>> {
                override fun onResponse(
                    call: Call<List<JobOfferResponseDTO>>,
                    response: Response<List<JobOfferResponseDTO>>
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val jobs = response.body() ?: emptyList()
                        updateJobsList(jobs)
                    } else {
                        handleApiError(response.code())
                    }
                }

                override fun onFailure(call: Call<List<JobOfferResponseDTO>>, t: Throwable) {
                    showLoading(false)
                    handleNetworkError(t)
                }
            })
    }

    private fun showLoading(show: Boolean) {
        swipeRefreshLayout.isRefreshing = show
    }

    private fun updateJobsList(jobs: List<JobOfferResponseDTO>) {
        jobAdapter.submitList(jobs)
        updateEmptyState(jobs.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun handleUpdateJob(job: JobOfferResponseDTO) {
        val updateFragment = UpdateJobFragment.newInstance(job.id.toString())
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_layout, updateFragment)
            .addToBackStack(null)
            .commit()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleFinishJob(job: JobOfferResponseDTO) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.finish_job_title))
            .setMessage(getString(R.string.finish_job_message))
            .setPositiveButton(getString(R.string.finish)) { _, _ ->
                finishJob(job.id.toString())
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun finishJob(jobId: String) {
        showLoading(true)
        RetrofitClient.recruitingService.toggleJobOfferStatus(jobId, "FINISHED")
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        showSuccess(getString(R.string.job_finished_success))
                        loadJobs()
                    } else {
                        handleApiError(response.code())
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    showLoading(false)
                    handleNetworkError(t)
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleDeleteJob(job: JobOfferResponseDTO) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_job_title))
            .setMessage(getString(R.string.delete_job_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteJob(job.id.toString())
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun deleteJob(jobId: String) {
        showLoading(true)
        RetrofitClient.recruitingService.deleteJobOffer(jobId)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        showSuccess(getString(R.string.job_deleted_success))
                        loadJobs()
                    } else {
                        handleApiError(response.code())
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    showLoading(false)
                    handleNetworkError(t)
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleViewApplicants(job: JobOfferResponseDTO) {
        val prefsManager = SharedPreferencesManager.getInstance(requireContext())
        
        // Check token expiration
        if (prefsManager.isTokenExpired()) {
            // Token expired, redirect to login
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            return
        }
        
        showLoading(true)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = RetrofitClient.apiService.listCandidates()
                showLoading(false)
                
                if (response.isSuccessful) {
                    val candidates = response.body() ?: emptyList()
                    showCandidatesDialog(candidates)
                } else if (response.code() == 401) {
                    // Token became invalid during the request
                    prefsManager.clearAuthData()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    handleApiError(response.code())
                }
            } catch (e: Exception) {
                showLoading(false)
                handleNetworkError(e)
            }
        }
    }

    private fun showCandidatesDialog(candidates: List<CandidateResponseDTO>) {
        if (candidates.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.applicants))
                .setMessage(getString(R.string.no_applicants))
                .setPositiveButton(getString(R.string.close), null)
                .show()
            return
        }

        val candidatesList = candidates.joinToString("\n") { candidate ->
            "${candidate.candidateInfo.name} - ${candidate.candidateInfo.email}"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.applicants))
            .setMessage(candidatesList)
            .setPositiveButton(getString(R.string.close), null)
            .show()
    }

    private fun handleApiError(code: Int) {
        val message = when (code) {
            401 -> getString(R.string.error_unauthorized)
            403 -> getString(R.string.error_forbidden)
            404 -> getString(R.string.error_not_found)
            else -> getString(R.string.error_generic)
        }
        showError(message)
    }

    private fun handleNetworkError(throwable: Throwable) {
        Log.e(TAG, "Network error", throwable)
        val message = when {
            throwable.message?.contains("timeout") == true -> getString(R.string.error_timeout)
            !isNetworkAvailable() -> getString(R.string.error_no_internet)
            else -> getString(R.string.error_network)
        }
        showError(message)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return actNw.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        showMessage(message)
    }

    private fun showSuccess(message: String) {
        showMessage(message)
    }

    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rootView = null
    }

    companion object {
        private const val TAG = "AvailableJobsFragment"
    }
}