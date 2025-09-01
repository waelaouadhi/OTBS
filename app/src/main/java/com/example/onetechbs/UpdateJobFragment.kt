package com.example.onetechbs

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.onetechbs.db.JobOfferRequest
import com.example.onetechbs.db.JobOfferResponseDTO
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UpdateJobFragment : Fragment() {

    private var jobId: String = ""
    private lateinit var titleInput: TextInputEditText
    private lateinit var departmentInput: AutoCompleteTextView
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var responsibilitiesInput: TextInputEditText
    private lateinit var qualificationsInput: TextInputEditText
    private lateinit var roleInput: TextInputEditText

    private val departments = listOf("HR", "Engineering", "Marketing", "Sales", "Finance", "Operations")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            jobId = it.getString(ARG_JOB_ID, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_update_job, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        titleInput = view.findViewById(R.id.titleInput)
        departmentInput = view.findViewById(R.id.departmentInput)
        descriptionInput = view.findViewById(R.id.descriptionInput)
        responsibilitiesInput = view.findViewById(R.id.responsibilitiesInput)
        qualificationsInput = view.findViewById(R.id.qualificationsInput)
        roleInput = view.findViewById(R.id.roleInput)

        // Setup department dropdown
        setupDepartmentDropdown()

        // Load job details
        loadJobDetails()

        // Setup button clicks
        view.findViewById<View>(R.id.updateButton).setOnClickListener {
            if (validateInputs()) {
                updateJob()
            }
        }

        view.findViewById<View>(R.id.cancelButton).setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun setupDepartmentDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, departments)
        departmentInput.setAdapter(adapter)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadJobDetails() {
        val idLong = jobId.toLongOrNull()
        if (idLong == null) {
            showError("Invalid or missing Job ID")
            return
        }

        val prefs = SharedPreferencesManager.getInstance(requireContext())
        val token = prefs.getAuthToken()
        if (token.isNullOrEmpty() || prefs.isTokenExpired()) {
            showError("Please log in again to continue")
            return
        }
        val api = RetrofitClient.getJobClient(token).create(com.example.onetechbs.network.ApiService::class.java)
        api.getJobOfferById(idLong)
            .enqueue(object : Callback<JobOfferResponseDTO> {
                override fun onResponse(
                    call: Call<JobOfferResponseDTO>,
                    response: Response<JobOfferResponseDTO>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { job ->
                            populateFields(job)
                        }
                    } else {
                        showError("Failed to load job details: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<JobOfferResponseDTO>, t: Throwable) {
                    showError("Error: ${t.localizedMessage}")
                }
            })
    }

    private fun populateFields(job: JobOfferResponseDTO) {
        titleInput.setText(job.title)
        departmentInput.setText(job.department)
        descriptionInput.setText(job.description)
        val respList: List<String> = job.responsibilities ?: emptyList<String>()
        responsibilitiesInput.setText(respList.joinToString(separator = "\n"))

        val qualsList: List<String> = mutableListOf<String>().apply {
            addAll(job.qualificationsRequired ?: emptyList<String>())
            addAll(job.qualificationsPreferred ?: emptyList<String>())
        }.toList()
        qualificationsInput.setText(qualsList.joinToString(separator = "\n"))
        roleInput.setText(job.role ?: "")
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (titleInput.text.isNullOrBlank()) {
            titleInput.error = "Title is required"
            isValid = false
        }

        if (departmentInput.text.isNullOrBlank()) {
            departmentInput.error = "Department is required"
            isValid = false
        }

        if (descriptionInput.text.isNullOrBlank()) {
            descriptionInput.error = "Description is required"
            isValid = false
        }

        if (responsibilitiesInput.text.isNullOrBlank()) {
            responsibilitiesInput.error = "Responsibilities are required"
            isValid = false
        }

        if (qualificationsInput.text.isNullOrBlank()) {
            qualificationsInput.error = "Qualifications are required"
            isValid = false
        }

        if (roleInput.text.isNullOrBlank()) {
            roleInput.error = "Role is required"
            isValid = false
        }

        return isValid
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateJob() {
        val updatedJob = JobOfferRequest(
            title = titleInput.text.toString(),
            department = departmentInput.text.toString(),
            description = descriptionInput.text.toString(),
            responsibilities = responsibilitiesInput.text.toString(),
            qualifications = qualificationsInput.text.toString(),
            role = roleInput.text.toString(),
            isInternal = true // You might want to make this configurable
        )

        val idLong = jobId.toLongOrNull() ?: run {
            showError("Invalid Job ID")
            return
        }
        val prefs = SharedPreferencesManager.getInstance(requireContext())
        val token = prefs.getAuthToken()
        if (token.isNullOrEmpty() || prefs.isTokenExpired()) {
            showError("Please log in again to continue")
            return
        }
        val api = RetrofitClient.getJobService(requireContext())
        api.updateJobOffer(
            id = jobId,
            jobOfferRequestDTO = updatedJob,
            token = "Bearer $token"
        )
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        showSuccess("Job updated successfully")
                        requireActivity().supportFragmentManager.popBackStack()
                    } else {
                        val err = try { response.errorBody()?.string() } catch (e: Exception) { null }
                        if (response.code() == 401) {
                            showError("Unauthorized (401). ${err ?: "Please log in again or contact admin."}")
                        } else {
                            showError("Failed to update job: ${response.code()} ${err ?: ""}")
                        }
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    showError("Error updating job: ${t.localizedMessage}")
                }
            })
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val ARG_JOB_ID = "job_id"

        fun newInstance(jobId: String) = UpdateJobFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_JOB_ID, jobId)
            }
        }
    }
} 