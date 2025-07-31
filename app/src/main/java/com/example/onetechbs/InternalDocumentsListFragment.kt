package com.example.onetechbs

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onetechbs.databinding.FragmentInternalDocumentsListBinding
import com.example.onetechbs.db.InternalDocumentResponseDTO
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class InternalDocumentsListFragment : Fragment() {
    private var _binding: FragmentInternalDocumentsListBinding? = null
    private val binding get() = _binding!!
    private var adapter: InternalDocumentAdapter? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInternalDocumentsListBinding.inflate(inflater, container, false)
        // Set up toolbar with back arrow and title
        binding.toolbar.title = "Internal Documents"
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        setupRecyclerView()
        fetchDocuments()
        return binding.root
    }

    private fun setupRecyclerView() {
        binding.documentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = InternalDocumentAdapter(emptyList()) { doc ->
            downloadDocument(doc)
        }
        binding.documentsRecyclerView.adapter = adapter
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchDocuments() {
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.emptyTextView.visibility = View.GONE
        val prefs = SharedPreferencesManager.getInstance(requireContext())
        val token = prefs.getAuthToken()
        if (token.isNullOrEmpty()) {
            binding.loadingIndicator.visibility = View.GONE
            Snackbar.make(binding.root, "Authentication token missing. Please log in again.", Snackbar.LENGTH_SHORT).show()
            return
        }
        val apiService = RetrofitClient.documentsService
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            try {
                val response = apiService.getAllInternalDocuments("Bearer $token")
                binding.loadingIndicator.visibility = View.GONE
                if (response.isSuccessful) {
                    val documents = response.body() ?: emptyList()
                    if (documents.isEmpty()) {
                        binding.emptyTextView.visibility = View.VISIBLE
                        adapter?.let { it as InternalDocumentAdapter }.apply {
                            (this as? InternalDocumentAdapter)?.let {
                                it.notifyDataSetChanged()
                            }
                        }
                    } else {
                        binding.emptyTextView.visibility = View.GONE
                        adapter = InternalDocumentAdapter(documents) { doc -> downloadDocument(doc) }
                        binding.documentsRecyclerView.adapter = adapter
                    }
                } else {
                    android.util.Log.e("FetchDocuments", "Failed to load documents: ${response.code()} ${response.errorBody()?.string()}")
                    Snackbar.make(binding.root, "Failed to load documents: ${response.code()}", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.loadingIndicator.visibility = View.GONE
                android.util.Log.e("FetchDocuments", "Exception during fetch", e)
                Snackbar.make(binding.root, "Error: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun downloadDocument(document: InternalDocumentResponseDTO) {
        val fileName = document.title.replace("[^a-zA-Z0-9._-]".toRegex(), "_") + "_" + document.id + ".pdf"
        if (document.document.isEmpty()) {
            Snackbar.make(requireView(), "No file data available for this document.", Snackbar.LENGTH_LONG).show()
            return
        }
        try {
            // Decode base64 string to byte array
            val fileBytes = android.util.Base64.decode(document.document, android.util.Base64.DEFAULT)
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, fileName)
            val fos = java.io.FileOutputStream(file)
            fos.write(fileBytes)
            fos.close()
            Toast.makeText(requireContext(), "File saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            // Optionally, open the file
            val uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), requireContext().packageName + ".provider", file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, "Open document with"))
        } catch (e: Exception) {
            android.util.Log.e("DownloadDocument", "Failed to save/open file", e)
            Snackbar.make(requireView(), "Failed to save/open file: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
