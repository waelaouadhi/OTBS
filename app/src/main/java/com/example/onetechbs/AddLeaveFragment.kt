package com.example.onetechbs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.appbar.MaterialToolbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.db.MessageResponse
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddLeaveFragment : Fragment() {

    private lateinit var btnUploadCertificate: MaterialButton
    private lateinit var btnSelectDate: ImageButton
    private lateinit var tvSelectedDate: TextView
    private lateinit var leaveCategoryInput: MaterialAutoCompleteTextView
    private lateinit var btnSubmit: MaterialButton
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var tvSelectedFile: TextView

    private var startDate: String? = null
    private var endDate: String? = null
    private var selectedFile: File? = null
    private val takenDateRanges = listOf(
        Pair("2025-05-01", "2025-05-05"),
        Pair("2025-06-10", "2025-06-15")
    )

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }

    interface OnLeaveSubmittedListener {
        fun onLeaveSubmitted(category: String, startDate: String, endDate: String)
    }

    private var leaveSubmittedListener: OnLeaveSubmittedListener? = null

    fun setOnLeaveSubmittedListener(listener: (String, String, String) -> Unit) {
        this.leaveSubmittedListener = object : OnLeaveSubmittedListener {
            override fun onLeaveSubmitted(category: String, startDate: String, endDate: String) {
                listener(category, startDate, endDate)
            }
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_add_leave, container, false)

        btnUploadCertificate = view.findViewById(R.id.btnUploadCertificate)
        btnSelectDate = view.findViewById(R.id.btnSelectDate)
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate)
        leaveCategoryInput = view.findViewById(R.id.spinnerLeaveCategory)
        btnSubmit = view.findViewById(R.id.submitbtn1)
        progressBar = view.findViewById(R.id.progressBar)
        tvSelectedFile = view.findViewById(R.id.tvSelectedFile)

        // toolbar back navigation
        view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        btnUploadCertificate.visibility = View.GONE
        progressBar.visibility = View.GONE

        val categories = listOf(
            "Choose your leave category",
            "Sick Leave",
            "Annual Leave",
            "Maternity Leave",
            "Paternity Leave",
            "Unpaid Leave",
            "Bereavement Leave",
            "Remote Work",
            "Authorization"
        )

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        leaveCategoryInput.setAdapter(adapter)
        leaveCategoryInput.setText(categories[0], false)

        leaveCategoryInput.setOnItemClickListener { _, _, position, _ ->
            val selected = categories[position]
            btnUploadCertificate.visibility = if (selected == "Sick Leave") View.VISIBLE else View.GONE
        }

        btnUploadCertificate.setOnClickListener {
            getContent.launch("application/pdf")
        }

        btnSelectDate.setOnClickListener {
            val today = Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis
            val constraintsBuilder = CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now())

            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .setCalendarConstraints(constraintsBuilder.build())
                .setSelection(androidx.core.util.Pair(today, today))
                .build()

            picker.show(parentFragmentManager, "date_picker")
            picker.addOnPositiveButtonClickListener { selection ->
                val selectedStart = convertMillisToDate(selection.first)
                val selectedEnd = convertMillisToDate(selection.second)

                if (isDateRangeTaken(selectedStart, selectedEnd)) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Unavailable Range")
                        .setMessage("The selected leave period overlaps with an existing one. Please choose another range.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@addOnPositiveButtonClickListener
                }

                startDate = selectedStart
                endDate = selectedEnd
                tvSelectedDate.text = "From $startDate to $endDate"
            }
        }

        btnSubmit.setOnClickListener {
            val category = leaveCategoryInput.text.toString()
            if (category == "Choose your leave category" || !validateLeaveDates(startDate, endDate)) {
                Toast.makeText(requireContext(), "Please correct the dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (category == "Sick Leave" && selectedFile == null) {
                Toast.makeText(requireContext(), "Please attach a medical certificate", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val leaveType = mapCategoryToLeaveType(category)
            val leaveTypeBody = leaveType.toRequestBody("text/plain".toMediaTypeOrNull())
            val startDateBody = startDate!!.toRequestBody("text/plain".toMediaTypeOrNull())
            val endDateBody = endDate!!.toRequestBody("text/plain".toMediaTypeOrNull())

            val startHourly = null
            val endHourly = null
            val filePart = selectedFile?.let {
                MultipartBody.Part.createFormData(
                    "attachment",
                    it.name,
                    it.asRequestBody("application/pdf".toMediaTypeOrNull())
                )
            }

            progressBar.visibility = View.VISIBLE
            progressBar.setProgress(0, true)

            RetrofitClient.leaveService.applyLeave(
                leaveTypeBody, startDateBody, endDateBody, startHourly, endHourly, filePart
            ).enqueue(object : Callback<MessageResponse> {
                override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        leaveSubmittedListener?.onLeaveSubmitted(category, startDate!!, endDate!!)
                        Toast.makeText(requireContext(), "Leave submitted", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        return view
    }

    private fun handleSelectedFile(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, "medical_certificate_${System.currentTimeMillis()}.pdf")
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            selectedFile = file
            tvSelectedFile.text = "Selected: ${file.name}"
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error selecting file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertMillisToDate(millis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        return formatter.format(calendar.time)
    }

    private fun mapCategoryToLeaveType(category: String): String {
        return when (category) {
            "Sick Leave" -> "MALADIE"
            "Annual Leave" -> "ANNUEL"
            "Maternity Leave" -> "MATERNITÉ"
            "Paternity Leave" -> "PATERNITÉ"
            "Unpaid Leave" -> "SANS_SOLDE"
            "Bereavement Leave" -> "DÉCÈS"
            "Remote Work" -> "TÉLÉTRAVAIL"
            "Authorization" -> "AUTORISATION"
            else -> "AUTORISATION"
        }
    }

    private fun isDateRangeTaken(start: String, end: String): Boolean {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedStart = format.parse(start)
        val selectedEnd = format.parse(end)

        return takenDateRanges.any { (takenStartStr, takenEndStr) ->
            val takenStart = format.parse(takenStartStr)
            val takenEnd = format.parse(takenEndStr)
            selectedStart <= takenEnd && selectedEnd >= takenStart
        }
    }

    private fun validateLeaveDates(start: String?, end: String?): Boolean {
        if (start == null || end == null) return false

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDateParsed = format.parse(start)
        val endDateParsed = format.parse(end)

        // Check if start date is before or equal to end date
        if (startDateParsed.after(endDateParsed)) {
            Toast.makeText(requireContext(), "Start date must be before end date", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check if the range is already taken
        if (isDateRangeTaken(start, end)) {
            Toast.makeText(requireContext(), "Selected range overlaps with an existing leave", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}