package com.example.onetechbs

import android.R
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.onetechbs.databinding.FragmentDocumentBinding
import com.example.onetechbs.db.EDocumentType
import com.example.onetechbs.db.PersonalDocumentRequestDTO
import com.example.onetechbs.network.ApiService
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import kotlinx.coroutines.launch

class DocumentFragment : Fragment() {

    private var _binding: FragmentDocumentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocumentBinding.inflate(inflater, container, false)
        // Set up toolbar with back arrow and title
        binding.toolbar.title = "Document Request"
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Populate the spinner with EDocumentType enum values
        val documentTypes = EDocumentType.values()
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_item,
            documentTypes.map { it.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercaseChar() } }
        )
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spinnerDocuments.adapter = adapter

        // 2. Set up RecyclerView for personal documents
        val documentAdapter = PersonalDocumentAdapter(emptyList(), this::downloadDocument)
        binding.recyclerPersonalDocuments.adapter = documentAdapter
        binding.recyclerPersonalDocuments.setHasFixedSize(true)
        binding.recyclerPersonalDocuments.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        fun fetchPersonalDocuments() {
            try {
                // Check if binding or RecyclerView is null
                if (_binding == null) {
                    Log.e("DocumentFragment", "[LAYOUT] Binding is null in fetchPersonalDocuments!")
                }
                if (binding.recyclerPersonalDocuments == null) {
                    Log.e("DocumentFragment", "[LAYOUT] RecyclerView is null in fetchPersonalDocuments!")
                }
            } catch (e: Exception) {
                Log.e("DocumentFragment", "[LAYOUT] Exception during layout check: ${e.localizedMessage}")
            }
            binding.textDocumentsHeader.text = "Loading..."
            binding.recyclerPersonalDocuments.visibility = View.GONE
            lifecycleScope.launch {
                try {
                    val prefs = com.example.onetechbs.util.SharedPreferencesManager.getInstance(requireContext())
                    val token = prefs.getAuthToken()
                    Log.d("DocumentFragment", "[GET] Token: $token")
                    Log.d("DocumentFragment", "[GET] Authorization header: Bearer $token")
                    if (token.isNullOrEmpty()) {
                        binding.textDocumentsHeader.text = "Authentication error: missing token."
                        return@launch
                    }
                    // Use the centralized Retrofit client
                    val response = RetrofitClient.documentService.getPersonalDocuments("Bearer $token")
                    Log.d("DocumentFragment", "[GET] Response code: ${'$'}{response.code()}")
                    Log.d("DocumentFragment", "[GET] Response message: ${'$'}{response.message()}")
                    // --- END: Logging HTTP communication ---
                    if (response.isSuccessful) {
                        val docs = response.body() ?: emptyList()
                        documentAdapter.updateData(docs)
                        val msg = if (docs.isEmpty()) "No requested documents yet." else "Your Requested Documents"
                        binding.textDocumentsHeader.text = msg
                        Log.e("DocumentFragment", "[UI] $msg")
                        binding.recyclerPersonalDocuments.visibility = View.VISIBLE
                    } else {
                        val msg = "Failed to load documents (${response.code()})"
                        binding.textDocumentsHeader.text = msg
                        Log.e("DocumentFragment", "[UI] $msg")
                    }
                } catch (e: Exception) {
                    val msg = "Error loading documents: ${e.localizedMessage}"
                    binding.textDocumentsHeader.text = msg
                    Log.e("DocumentFragment", "[UI] $msg")
                }
            }
        }

        fetchPersonalDocuments()

        // 3. Handle button click
        binding.buttonRequestDocument.setOnClickListener {
            val selectedIndex = binding.spinnerDocuments.selectedItemPosition
            val selectedType = documentTypes[selectedIndex]
            val notes = binding.editNote.text.toString().trim()

            if (notes.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a note.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = PersonalDocumentRequestDTO(
                documentType = selectedType,
                notes = notes
            )

            // Disable button while processing
            binding.buttonRequestDocument.isEnabled = false

            // 3. Launch API call inside coroutine
            lifecycleScope.launch {
                try {
                    val prefs = SharedPreferencesManager.getInstance(requireContext())
                    val token = prefs.getAuthToken()

                    if (token.isNullOrEmpty()) {
                        Toast.makeText(requireContext(), "Authentication error: token not found.", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    Log.d("DocumentFragment", "Auth token: $token")
                    Log.d("DocumentFragment", "Auth header: Bearer $token")
                    // Use the centralized Retrofit client
                    val response = RetrofitClient.documentService.sendPersonalDocumentRequest("Bearer $token", request)
                    // --- END: Logging HTTP communication ---

                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Request sent successfully!", Toast.LENGTH_LONG).show()
                        binding.editNote.setText("")
                        binding.spinnerDocuments.setSelection(0)
                    } else {
                        Toast.makeText(requireContext(), "Failed: ${response.code()} - ${response.message()}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    binding.buttonRequestDocument.isEnabled = true
                }
            }
        }
    }

    // Function to handle document download
    private fun downloadDocument(doc: com.example.onetechbs.db.PersonalDocumentResponseDTO) {
        android.util.Log.d("DownloadDebug", "downloadDocument CALLED for docId=${doc.id}, url=${doc.document}")
        Toast.makeText(requireContext(), "Download button pressed", Toast.LENGTH_SHORT).show()
        try {
            val base64 = doc.document
            if (base64.isNullOrEmpty()) {
                android.util.Log.e("DownloadDebug", "No document data found for docId=${doc.id}")
                Toast.makeText(requireContext(), "No document data found.", Toast.LENGTH_SHORT).show()
                return
            }
            // Prompt user for file type: PDF or DOC
            val fileTypes = arrayOf("PDF", "DOC")
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Choose file type to save:")
                .setItems(fileTypes) { _, which ->
                    val ext = if (which == 0) "pdf" else "doc"
                    val fileName = "${doc.documentType.name}_${doc.id}.$ext"
                    val success = saveBase64ToFile(base64, fileName)
                    if (success != null) {
                        Toast.makeText(requireContext(), "Saved as ${success.absolutePath}", Toast.LENGTH_LONG).show()
                        android.util.Log.d("DownloadDebug", "File saved: ${success.absolutePath}")
                    } else {
                        Toast.makeText(requireContext(), "Failed to save file.", Toast.LENGTH_LONG).show()
                        android.util.Log.e("DownloadDebug", "Failed to save file for docId=${doc.id}")
                    }
                }
                .show()
        } catch (e: Exception) {
            android.util.Log.e("DownloadDebug", "Exception in downloadDocument: ${e.localizedMessage}", e)
            Toast.makeText(requireContext(), "Download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveBase64ToFile(base64: String, fileName: String): java.io.File? {
        return try {
            val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, fileName)
            java.io.FileOutputStream(file).use { it.write(bytes) }
            file
        } catch (e: Exception) {
            android.util.Log.e("DownloadDebug", "Failed to save file: ${e.localizedMessage}", e)
            null
        }
    }

    override fun onDestroyView() {
        // Re-enable the button if it exists in the binding
        _binding?.buttonRequestDocument?.isEnabled = true
        super.onDestroyView()
        _binding = null
    }
}
