package com.example.onetechbs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.onetechbs.databinding.FragmentInternalDocumentsBinding
import com.example.onetechbs.db.EDocumentCategory
import com.example.onetechbs.db.InternalDocumentRequestDTO
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import com.google.android.material.snackbar.Snackbar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class InternalDocumentsFragment : Fragment() {
    private var _binding: FragmentInternalDocumentsBinding? = null
    private val binding get() = _binding!!

    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null
    private var selectedCategory: EDocumentCategory? = null

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val uri = data?.data
                if (uri != null) {
                    selectedFileUri = uri
                    selectedFileName = getFileName(uri)
                    binding.selectedFileNameTextView.text = selectedFileName
                }
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInternalDocumentsBinding.inflate(inflater, container, false)
        setupUI()
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupUI() {
        // Category Spinner
        val categories = EDocumentCategory.values().map { it.name }
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.categorySpinner.setAdapter(categoryAdapter)
        binding.categorySpinner.inputType = android.text.InputType.TYPE_NULL
        binding.categorySpinner.isFocusable = true
        binding.categorySpinner.isFocusableInTouchMode = false
        binding.categorySpinner.isClickable = true
        binding.categorySpinner.setOnClickListener {
            binding.categorySpinner.showDropDown()
        }
        binding.categorySpinner.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = EDocumentCategory.valueOf(categories[position])
        }

        // File Picker
        binding.filePickerButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/pdf|application/msword|application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            val mimeTypes = arrayOf(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            filePickerLauncher.launch(Intent.createChooser(intent, "Select Document"))
        }

        // Cancel Button
        binding.cancelButton.setOnClickListener {
            clearForm()
        }

        // Submit Button
        binding.submitButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                submitDocument()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun submitDocument() {
        val title = binding.titleEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()
        val category = selectedCategory ?: run {
            Snackbar.make(binding.root, "Please select a category", Snackbar.LENGTH_SHORT).show()
            Log.e("SubmitDebug", "Category not selected")
            return
        }

        val fileUri = selectedFileUri
        if (title.isEmpty()) {
            Snackbar.make(binding.root, "Title is required", Snackbar.LENGTH_SHORT).show()
            Log.e("SubmitDebug", "Title is empty")
            return
        }

        if (fileUri == null) {
            Snackbar.make(binding.root, "Please select a file", Snackbar.LENGTH_SHORT).show()
            Log.e("SubmitDebug", "File URI is null")
            return
        }

        // Show loading
        binding.loadingIndicator.isVisible = true

        // Prepare file part
        val file = createFileFromUri(fileUri) ?: run {
            binding.loadingIndicator.isVisible = false
            Snackbar.make(binding.root, "File error", Snackbar.LENGTH_SHORT).show()
            Log.e("SubmitDebug", "createFileFromUri returned null")
            return
        }

        val mimeType = getMimeType(file)?.toMediaTypeOrNull()
        Log.d("SubmitDebug", "File: ${file.name}, MimeType: $mimeType")
        val requestFile = RequestBody.create(mimeType, file.readBytes())
        val documentPart = MultipartBody.Part.createFormData("document", file.name, requestFile)

        // Prepare text parts
        val titlePart = RequestBody.create("text/plain".toMediaTypeOrNull(), title)
        val descriptionPart = RequestBody.create("text/plain".toMediaTypeOrNull(), description)
        val categoryPart = RequestBody.create("text/plain".toMediaTypeOrNull(), category.name)

        // Get token
        val prefs = SharedPreferencesManager.getInstance(requireContext())
        val token = prefs.getAuthToken()
        val isExpired = prefs.isTokenExpired()

        Log.d("SubmitDebug", "Token: $token")
        Log.d("SubmitDebug", "Is Token Expired: $isExpired")

        if (token.isNullOrEmpty() || isExpired) {
            binding.loadingIndicator.isVisible = false
            Snackbar.make(binding.root, "Authentication token missing or expired. Please log in again.", Snackbar.LENGTH_SHORT).show()
            return
        }

        val apiService = RetrofitClient.documentsService

        try {
            binding.loadingIndicator.isVisible = true
            Log.d("SubmitDebug", "Calling API with token: Bearer $token")

            // Create DTO and serialize as JSON
            val dto = InternalDocumentRequestDTO(title, description, category)
            val gson = com.google.gson.Gson()
            val json = gson.toJson(dto)
            val dataPart = RequestBody.create("application/json".toMediaTypeOrNull(), json)

            val response = apiService.uploadInternalDocument(
                dataPart,
                documentPart,
                "Bearer $token" // Pass token with Bearer prefix
            )

            binding.loadingIndicator.isVisible = false

            if (response.isSuccessful) {
                Log.d("SubmitDebug", "Upload successful")
                Snackbar.make(binding.root, "Document submitted successfully", Snackbar.LENGTH_LONG).show()
                android.widget.Toast.makeText(requireContext(), "Upload completed!", android.widget.Toast.LENGTH_SHORT).show()
                clearForm()
            } else {
                Log.e("SubmitDebug", "Upload failed with status: ${response.code()}, body: ${response.errorBody()?.string()}")
                Snackbar.make(binding.root, "Submission failed: ${response.code()}", Snackbar.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            binding.loadingIndicator.isVisible = false
            Log.e("SubmitDebug", "Exception during upload", e)
            Snackbar.make(binding.root, "Error: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
        }
    }
    private fun clearForm() {
        binding.titleEditText.text?.clear()
        binding.descriptionEditText.text?.clear()
        binding.categorySpinner.setText("")
        binding.selectedFileNameTextView.text = "No file selected"
        selectedFileUri = null
        selectedFileName = null
        selectedCategory = null
    }

    private fun getFileName(uri: Uri): String {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        } ?: uri.lastPathSegment ?: "document"
    }

    private fun createFileFromUri(uri: Uri): File? {
        return try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, getFileName(uri))
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            outputStream.close()
            inputStream?.close()
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "application/octet-stream"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
