package com.example.onetechbs

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.onetechbs.db.JobOfferResponseDTO
import com.example.onetechbs.network.ResumeService
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class JobOfferDetailsFragment : Fragment() {

    private lateinit var jobOffer: JobOfferResponseDTO
    private var resumeUri: Uri? = null
    private lateinit var resumeService: ResumeService

    companion object {
        private const val ARG_JOB_OFFER = "job_offer"

        fun newInstance(jobOffer: JobOfferResponseDTO): JobOfferDetailsFragment {
            return JobOfferDetailsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_JOB_OFFER, jobOffer)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            jobOffer = it.getParcelable(ARG_JOB_OFFER)
                ?: throw IllegalArgumentException("Job offer is required")
        }
        resumeService = ResumeService(RetrofitClient.apiService)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_job_offer_details, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvJobTitle).text = jobOffer.title
        view.findViewById<TextView>(R.id.tvJobDepartment).text = jobOffer.department
        view.findViewById<TextView>(R.id.tvJobDescription).text = jobOffer.description

        val btnUploadResume: MaterialButton = view.findViewById(R.id.btnUploadResume)
        val btnApply: MaterialButton = view.findViewById(R.id.btnApply)

        btnUploadResume.setOnClickListener {
            openFilePicker()
        }

        btnApply.setOnClickListener {
            if (resumeUri != null) {
                uploadResume(requireContext())
            } else {
                Toast.makeText(context, "Please upload a resume first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    resumeUri = uri
                    Toast.makeText(context, "Resume uploaded successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
        }
        getContent.launch(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun uploadResume(context: Context) {
        resumeUri?.let { uri ->
            val contentResolver = context.contentResolver
    
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val fileName = getFileName(uri, contentResolver) ?: "resume.pdf"
                val fileBytes = inputStream?.readBytes()
    
                if (fileBytes != null) {
                    val requestBody = fileBytes.toRequestBody("application/pdf".toMediaTypeOrNull())
                    val resumePart = MultipartBody.Part.createFormData(
                        "resume",
                        fileName,
                        requestBody
                    )
    
                    // Get auth token
                    val prefsManager = SharedPreferencesManager.getInstance(requireContext())
                    val token = prefsManager.getAuthToken()
                    
                    if (token.isNullOrEmpty()) {
                        Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
                        return@let
                    }
    
                    lifecycleScope.launch {
                        try {
                            val response = RetrofitClient.apiService.createApplication(
                                jobOfferId = jobOffer.id,
                                resume = resumePart,
                                authHeader = "Bearer $token"
                            )
                            
                            if (response.isSuccessful) {
                                Toast.makeText(
                                    context,
                                    "Application submitted successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Submission failed: ${response.message()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Error: ${e.localizedMessage}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Could not read resume file", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to upload resume: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("Range")
    private fun getFileName(uri: Uri, contentResolver: ContentResolver): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1 && cut != null) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}