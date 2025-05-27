package com.example.onetechbs

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.db.MessageResponse
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class AddLeaveFragment : Fragment() {

    private lateinit var btnUploadCertificate: Button
    private lateinit var btnSelectDate: ImageButton
    private lateinit var tvSelectedDate: TextView
    private lateinit var spinnerLeaveCategory: Spinner
    private lateinit var btnSubmit: AppCompatImageButton

    private var startDate: String? = null
    private var endDate: String? = null
    private val takenDateRanges = listOf(
        Pair("2025-05-01", "2025-05-05"),
        Pair("2025-06-10", "2025-06-15")
    )
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_add_leave, container, false)

        btnUploadCertificate = view.findViewById(R.id.btnUploadCertificate)
        btnSelectDate = view.findViewById(R.id.btnSelectDate)
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate)
        spinnerLeaveCategory = view.findViewById(R.id.spinnerLeaveCategory)
        btnSubmit = view.findViewById(R.id.submitbtn1)

        btnUploadCertificate.visibility = View.GONE

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

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerLeaveCategory.adapter = adapter

        spinnerLeaveCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = parent.getItemAtPosition(position).toString()
                btnUploadCertificate.visibility = if (selected == "Sick Leave") View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
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
            val category = spinnerLeaveCategory.selectedItem.toString()
            if (category == "Choose your leave category" || !validateLeaveDates(startDate, endDate)) {
                Toast.makeText(requireContext(), "Please correct the dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener

            }

            val leaveType = mapCategoryToLeaveType(category)
            val leaveTypeBody = leaveType.toRequestBody("text/plain".toMediaTypeOrNull())
            val startDateBody = startDate!!.toRequestBody("text/plain".toMediaTypeOrNull())
            val endDateBody = endDate!!.toRequestBody("text/plain".toMediaTypeOrNull())

            val startHourly = null
            val endHourly = null
            val filePart: MultipartBody.Part? = null

            RetrofitClient.leaveService.applyLeave(
                leaveTypeBody, startDateBody, endDateBody, startHourly, endHourly, filePart
            ).enqueue(object : Callback<MessageResponse> {
                override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                    if (response.isSuccessful) {
                        leaveSubmittedListener?.onLeaveSubmitted(category, startDate!!, endDate!!)
                        Toast.makeText(requireContext(), "Leave submitted", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                    Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        return view
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