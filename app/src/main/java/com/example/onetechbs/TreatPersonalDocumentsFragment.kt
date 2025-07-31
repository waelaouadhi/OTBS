package com.example.onetechbs

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onetechbs.databinding.FragmentTreatPersonalDocumentsBinding
import com.example.onetechbs.db.EDocumentStatus
import com.example.onetechbs.db.PersonalDocumentResponseDTO
import com.example.onetechbs.network.ApiService
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class TreatPersonalDocumentsFragment : Fragment() {
    private var _binding: FragmentTreatPersonalDocumentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TreatPersonalDocumentsAdapter
    private var fileUri: Uri? = null
    private var pickedFileCallback: ((Uri?) -> Unit)? = null

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        pickedFileCallback?.invoke(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTreatPersonalDocumentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = TreatPersonalDocumentsAdapter(
            onApprove = { doc -> showApproveDialog(doc) },
            onReject = { doc -> showRejectDialog(doc) },
            onProcessing = { doc -> showProcessingDialog(doc) }
        )
        binding.recyclerTreatDocuments.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTreatDocuments.adapter = adapter
        fetchDocuments()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchDocuments() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val prefs = com.example.onetechbs.util.SharedPreferencesManager.getInstance(requireContext())
                val token = prefs.getAuthToken()
                android.util.Log.d("TreatPersonalDocuments", "[GET] Token: $token")
                android.util.Log.d("TreatPersonalDocuments", "[GET] Authorization header: Bearer $token")
                if (token.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "Authentication error: missing token.", Toast.LENGTH_LONG).show()
                    return@launch
                }
                val logging = okhttp3.logging.HttpLoggingInterceptor()
                logging.level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
                val okHttpClient = okhttp3.OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor { chain ->
                        val req = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer $token")
                            .build()
                        android.util.Log.d("TreatPersonalDocuments", "[GET] Outgoing request: ${'$'}{req.method} ${'$'}{req.url}")
                        android.util.Log.d("TreatPersonalDocuments", "[GET] Request headers: ${'$'}{req.headers}")
                        chain.proceed(req)
                    }
                    .build()
                val retrofit = retrofit2.Retrofit.Builder()
                    .baseUrl("http://172.31.4.102:8093/") // Use your DOCUMENTS_BASE_URL
                    .client(okHttpClient)
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()
                val api = retrofit.create(com.example.onetechbs.network.ApiService::class.java)
                val response = api.getAllReceivedPersonalDocumentRequests("Bearer $token")
                android.util.Log.d("TreatPersonalDocuments", "[GET] Response code: ${'$'}{response.code()}")
                android.util.Log.d("TreatPersonalDocuments", "[GET] Response message: ${'$'}{response.message()}")
                if (response.isSuccessful) {
                    val docs = response.body() ?: emptyList()
                    adapter.submitList(docs)
                } else {
                    Toast.makeText(requireContext(), "Failed to fetch documents: ${'$'}{response.code()} ${'$'}{response.message()}", Toast.LENGTH_LONG).show()
                    android.util.Log.e("TreatPersonalDocuments", "Failed to fetch documents: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${'$'}{e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showApproveDialog(doc: PersonalDocumentResponseDTO) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_approve_document, null)
        val editNotes = dialogView.findViewById<android.widget.EditText>(R.id.edit_approve_notes)
        val pickFileBtn = dialogView.findViewById<android.widget.Button>(R.id.button_pick_file)
        val fileNameText = dialogView.findViewById<android.widget.TextView>(R.id.text_file_name)
        var selectedUri: Uri? = null
        pickFileBtn.setOnClickListener {
            pickedFileCallback = { uri ->
                selectedUri = uri
                fileNameText.text = uri?.lastPathSegment ?: "No file selected"
            }
            pickFileLauncher.launch("application/pdf")
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialogView.findViewById<android.widget.Button>(R.id.button_approve_confirm).setOnClickListener {
            val notes = editNotes.text.toString()
            processDocument(doc, EDocumentStatus.COMPLETED, notes, selectedUri)
            dialog.dismiss()
        }
        dialogView.findViewById<android.widget.Button>(R.id.button_approve_cancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showRejectDialog(doc: PersonalDocumentResponseDTO) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_reject_document, null)
        val editNotes = dialogView.findViewById<android.widget.EditText>(R.id.edit_reject_notes)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialogView.findViewById<android.widget.Button>(R.id.button_reject_confirm).setOnClickListener {
            val notes = editNotes.text.toString()
            if (notes.isBlank()) {
                Toast.makeText(requireContext(), "Rejection notes required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            processDocument(doc, EDocumentStatus.REJECTED, notes, null)
            dialog.dismiss()
        }
        dialogView.findViewById<android.widget.Button>(R.id.button_reject_cancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showProcessingDialog(doc: PersonalDocumentResponseDTO) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_processing_document, null)
        val editNotes = dialogView.findViewById<android.widget.EditText>(R.id.edit_processing_notes)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialogView.findViewById<android.widget.Button>(R.id.button_processing_confirm).setOnClickListener {
            val notes = editNotes.text.toString()
            if (notes.isBlank()) {
                Toast.makeText(requireContext(), "Processing notes required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            processDocument(doc, EDocumentStatus.PROCESSING, notes, null)
            dialog.dismiss()
        }
        dialogView.findViewById<android.widget.Button>(R.id.button_processing_cancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun processDocument(doc: PersonalDocumentResponseDTO, status: EDocumentStatus, notes: String, fileUri: Uri?) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val prefs = com.example.onetechbs.util.SharedPreferencesManager.getInstance(requireContext())
                val token = prefs.getAuthToken()
                android.util.Log.d("TreatPersonalDocuments", "Token before process API: $token")
                val logging = okhttp3.logging.HttpLoggingInterceptor()
                logging.level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
                val okHttpClient = okhttp3.OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor { chain ->
                        val req = chain.request().newBuilder()
                            .addHeader("Authorization", "Bearer $token")
                            .build()
                        android.util.Log.d("TreatPersonalDocuments", "[PROCESS] Outgoing request: ${'$'}{req.method} ${'$'}{req.url}")
                        android.util.Log.d("TreatPersonalDocuments", "[PROCESS] Request headers: ${'$'}{req.headers}")
                        chain.proceed(req)
                    }
                    .build()
                val retrofit = retrofit2.Retrofit.Builder()
                    .baseUrl("http://172.31.4.102:8093/") // <-- Fixed: do not duplicate /api/v1/
                    .client(okHttpClient)
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()
                val api = retrofit.create(com.example.onetechbs.network.ApiService::class.java)
                val processRequest = com.example.onetechbs.db.PersonalDocumentProcessRequest(status, notes)
                val gson = com.google.gson.Gson()
                val json = gson.toJson(processRequest)
                val dataPart = RequestBody.create("application/json".toMediaTypeOrNull(), json)
                var filePart: MultipartBody.Part? = null
                if (fileUri != null) {
                    val inputStream = requireContext().contentResolver.openInputStream(fileUri)
                    val fileBytes = inputStream?.readBytes()
                    val fileName = fileUri.lastPathSegment ?: "document.pdf"
                    if (fileBytes != null) {
                        val reqBody = RequestBody.create("application/pdf".toMediaTypeOrNull(), fileBytes)
                        filePart = MultipartBody.Part.createFormData("document", fileName, reqBody)
                    }
                }
                android.util.Log.d("TreatPersonalDocuments", "About to call processPersonalDocumentRequest with docId=${doc.id}, status=$status, notes=$notes, fileUri=$fileUri, dataJson=$json")
                val response = api?.processPersonalDocumentRequest(
                    doc.id,
                    dataPart,
                    filePart,
                    "Bearer $token"
                )
                if (response != null) {
                    android.util.Log.d("TreatPersonalDocuments", "processPersonalDocumentRequest response: code=${response.code()}, message=${response.message()}, errorBody=${response.errorBody()?.string()}")
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Request processed!", Toast.LENGTH_SHORT).show()
                        fetchDocuments()
                    } else {
                        Toast.makeText(requireContext(), "Failed: ${response.code()} - ${response.message()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${'$'}{e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
