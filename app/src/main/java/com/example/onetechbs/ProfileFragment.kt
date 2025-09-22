package com.example.onetechbs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Toast
import android.util.Log
import android.util.Base64
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import org.json.JSONObject
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.content.res.AppCompatResources
import com.example.onetechbs.databinding.FragmentProfileBinding
import com.example.onetechbs.db.UserResponseDTO
import com.example.onetechbs.network.RetrofitClient
import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment() {

    companion object {
        private const val TAG = "ProfileFragment"
    }

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var currentUser: UserResponseDTO? = null
    private var selectedImageUri: Uri? = null
    private var lastPickedImageUri: Uri? = null

    private val cropActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val croppedUri = result.data?.getParcelableExtra<Uri>(CropActivity.EXTRA_CROPPED_URI)
            if (croppedUri != null) {
                selectedImageUri = croppedUri
                _binding?.profileImage?.setImageURI(croppedUri)
                Log.d(TAG, "Image cropped successfully: uri=$croppedUri")
            } else {
                Log.e(TAG, "Cropped URI is null")
            }
        } else {
            Log.d(TAG, "Crop was cancelled")
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            Log.d(TAG, "Image selected, launching custom cropper: uri=$uri")
            lastPickedImageUri = uri
            val intent = CropActivity.newIntent(requireContext(), uri)
            cropActivityLauncher.launch(intent)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        setupUi()
        loadUser()
        return binding.root
    }

    private fun setupUi() {
        // Toolbar back navigation
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.changePhotoButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.saveButton.setOnClickListener {
            Log.d(TAG, "Save clicked")
            attemptUpdate()
        }
    }

    private fun loadUser() {
        val prefs = requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
        val username = prefs.getString("username", null)
        if (username.isNullOrBlank()) {
            Toast.makeText(requireContext(), "No username found in session", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Username missing from SharedPreferences while loading user")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val service = RetrofitClient.getUserService(requireContext())
            try {
                Log.d(TAG, "Fetching user by username=$username")
                val resp = withContext(Dispatchers.IO) { service.getUserByUsername(username) }
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body != null) {
                        currentUser = body
                        Log.d(TAG, "User loaded: id=${body.id}, email=${body.email}")
                        if (_binding != null) {
                            bindUser(body)
                        }
                        if (_binding != null) {
                            loadProfilePicture(username)
                        }
                    }
                } else {
                    Log.e(TAG, "getUserByUsername failed: code=${resp.code()} message=${resp.message()}")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "loadUser cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user", e)
                if (isAdded) Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                // No loader UI
            }
        }
    }

    private suspend fun loadProfilePicture(username: String) {
        if (_binding == null) return
        val service = RetrofitClient.getUserService(requireContext())
        try {
            Log.d(TAG, "Fetching profile picture for $username")
            val picResp = withContext(Dispatchers.IO) { service.getProfilePicture(username) }
            if (picResp.isSuccessful) {
                val body = picResp.body()
                if (body == null) {
                    Log.d(TAG, "No profile picture returned (null body)")
                    return
                }
                val ct = body.contentType()?.toString()?.lowercase()
                try {
                    if (ct != null && ct.contains("application/json")) {
                        val text = body.string()
                        val json = JSONObject(text)
                        var b64 = json.optString("picture", "")
                        if (b64.isNotBlank()) {
                            val commaIdx = b64.indexOf(',')
                            if (commaIdx != -1) b64 = b64.substring(commaIdx + 1)
                            val bytes = Base64.decode(b64, Base64.DEFAULT)
                            if (bytes.isNotEmpty()) {
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (bmp != null) {
                                    _binding?.profileImage?.setImageBitmap(bmp)
                                    Log.d(TAG, "Profile picture bound from JSON (${bytes.size} bytes)")
                                } else {
                                    Log.w(TAG, "Failed to decode bitmap from JSON-decoded bytes")
                                }
                            } else {
                                Log.d(TAG, "Decoded JSON image bytes empty")
                            }
                        } else {
                            Log.d(TAG, "JSON response missing/blank 'picture' field")
                        }
                    } else {
                        val bytes = body.bytes()
                        if (bytes.isNotEmpty()) {
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            _binding?.profileImage?.setImageBitmap(bmp)
                            Log.d(TAG, "Profile picture bound (${bytes.size} bytes)")
                        } else {
                            Log.d(TAG, "No profile picture returned or empty body")
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "loadProfilePicture cancelled")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing/decoding profile picture response", e)
                }
            } else {
                Log.e(TAG, "getProfilePicture failed: code=${picResp.code()} message=${picResp.message()}")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.d(TAG, "Fetching profile picture cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching profile picture", e)
        }
    }

    private fun bindUser(user: UserResponseDTO) {
        val b = _binding ?: return
        // Top identity
        b.nameText.text = "${user.firstName} ${user.lastName}"
        b.emailText.text = user.email
        b.departmentText.text = user.department
        b.roleChip.text = user.role

        // Editable fields
        b.jobTitleInput.setText(user.jobTitle)
        b.phone1Input.setText(user.phoneNumber1)
        b.phone2Input.setText(user.phoneNumber2 ?: "")
    }

    private fun attemptUpdate() {
        val u = currentUser ?: return
        val jobTitle = binding.jobTitleInput.text?.toString()?.trim().orEmpty()
        val phone1 = binding.phone1Input.text?.toString()?.trim().orEmpty()
        val phone2 = binding.phone2Input.text?.toString()?.trim()?.ifBlank { null }

        if (jobTitle.isBlank()) {
            binding.jobTitleInputLayout.error = "Required"
            Log.w(TAG, "Validation failed: jobTitle is blank")
            return
        } else binding.jobTitleInputLayout.error = null

        if (phone1.isBlank()) {
            binding.phone1InputLayout.error = "Required"
            Log.w(TAG, "Validation failed: phone1 is blank")
            return
        } else binding.phone1InputLayout.error = null

        Log.d(TAG, "Preparing update: jobTitle='${jobTitle}', phone1='${phone1}', phone2='${phone2}', hasImage=${selectedImageUri != null}")
        viewLifecycleOwner.lifecycleScope.launch {
            _binding?.saveButton?.isEnabled = false
            try {
                val service = RetrofitClient.getUserService(requireContext())

                // Some backends may return nulls for non-null Kotlin fields; guard defensively
                val firstNameStr = (u.firstName as String?)?.trim().orEmpty()
                val lastNameStr = (u.lastName as String?)?.trim().orEmpty()
                val emailStr = (u.email as String?)?.trim().orEmpty()

                if (firstNameStr.isBlank() || lastNameStr.isBlank() || emailStr.isBlank()) {
                    Log.w(TAG, "Identity fields contain null/blank values: first='${firstNameStr}', last='${lastNameStr}', email='${emailStr}'")
                }

                val firstName = firstNameStr.toRequestBody("text/plain".toMediaTypeOrNull())
                val lastName = lastNameStr.toRequestBody("text/plain".toMediaTypeOrNull())
                val email = emailStr.toRequestBody("text/plain".toMediaTypeOrNull())
                val jobTitleRb = jobTitle.toRequestBody("text/plain".toMediaTypeOrNull())
                val phone1Rb = phone1.toRequestBody("text/plain".toMediaTypeOrNull())
                val phone2Rb: RequestBody? = phone2?.toRequestBody("text/plain".toMediaTypeOrNull())
                val genderRb: RequestBody? = (u.gender as String?)?.toRequestBody("text/plain".toMediaTypeOrNull())
                val birthDateRb: RequestBody? = (u.birthDate as String?)?.toRequestBody("text/plain".toMediaTypeOrNull())

                val picturePart: MultipartBody.Part? = selectedImageUri?.let { uri ->
                    Log.d(TAG, "Creating image multipart from uri=$uri")
                    createImagePartFromUri(uri)
                }

                Log.d(TAG, "Calling updateUserInfo() ...")
                val resp = withContext(Dispatchers.IO) {
                    service.updateUserInfo(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        jobTitle = jobTitleRb,
                        phoneNumber1 = phone1Rb,
                        phoneNumber2 = phone2Rb,
                        gender = genderRb,
                        birthDate = birthDateRb,
                        picture = picturePart
                    )
                }

                Log.d(TAG, "updateUserInfo() response: code=${resp.code()} success=${resp.isSuccessful}")
                if (resp.isSuccessful) {
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                    // Notify other fragments (e.g., Homefra) to refresh profile image if a new one was uploaded
                    if (selectedImageUri != null) {
                        parentFragmentManager.setFragmentResult(
                            "profile_photo_updated",
                            Bundle().apply { putLong("ts", System.currentTimeMillis()) }
                        )
                        Log.d(TAG, "Emitted fragment result: profile_photo_updated")
                    }
                    // Refresh data
                    loadUser()
                } else {
                    val errorBody = try { resp.errorBody()?.string() } catch (e: Exception) { null }
                    Log.e(TAG, "Update failed: code=${resp.code()} message=${resp.message()} body=${errorBody}")
                    Toast.makeText(requireContext(), "Update failed: ${resp.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "Save flow cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Exception during update", e)
                if (isAdded) Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                _binding?.saveButton?.isEnabled = true
                Log.d(TAG, "Save flow finished")
            }
        }
    }

    private fun createImagePartFromUri(uri: Uri): MultipartBody.Part? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("profile_pic", ".tmp", requireContext().cacheDir)
            FileOutputStream(tempFile).use { out ->
                inputStream.copyTo(out)
            }
            val mimeType = requireContext().contentResolver.getType(uri) ?: "image/*"
            val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            Log.d(TAG, "Temp image created: ${tempFile.absolutePath}, size=${tempFile.length()} bytes, mime=${mimeType}")
            MultipartBody.Part.createFormData("picture", tempFile.name, requestBody)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create image multipart", e)
            null
        }
    }

    override fun onResume() {
        super.onResume()
        // Using fragment's own toolbar; no changes to Activity toolbar here to avoid conflicts
    }

    override fun onPause() {
        super.onPause()
        // No-op
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
