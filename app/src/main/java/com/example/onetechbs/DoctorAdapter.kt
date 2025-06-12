//package com.example.onetechbs
//
//import android.app.TimePickerDialog
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import android.widget.Toast
//import androidx.recyclerview.widget.RecyclerView
//import com.example.onetechbs.db.MedicalVisitRequest
//import com.example.onetechbs.network.RetrofitClient
//import com.google.android.material.button.MaterialButton
//import com.google.android.material.chip.Chip
//import com.google.android.material.imageview.ShapeableImageView
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import java.text.SimpleDateFormat
//import java.util.*
//
//class DoctorAdapter(
//    private val doctors: List<Doctor>,
//    private val onAppointmentConfirmed: () -> Unit
//) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {
//
//    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
//
//    inner class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val doctorImageView: ShapeableImageView = itemView.findViewById(R.id.doctorImageView)
//        val doctorNameTextView: TextView = itemView.findViewById(R.id.doctorNameTextView)
//        val specialtyTextView: TextView = itemView.findViewById(R.id.specialtyTextView)
//        val availabilityChip: Chip = itemView.findViewById(R.id.availabilityChip)
//        val scheduleButton: MaterialButton = itemView.findViewById(R.id.scheduleButton)
//        val selectedTimeTextView: TextView = itemView.findViewById(R.id.selectedTimeTextView)
//        val confirmButton: MaterialButton = itemView.findViewById(R.id.confirmButton)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_doctor, parent, false)
//        return DoctorViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
//        val doctor = doctors[position]
//
//        holder.doctorNameTextView.text = doctor.name
//        holder.specialtyTextView.text = doctor.specialty
//
//        // Update availability chip with time range
//        val availabilityText = if (doctor.isAvailable) {
//            "Available ${formatTimeRange(doctor.availabilityStart, doctor.availabilityEnd)}"
//        } else {
//            "Not Available"
//        }
//        holder.availabilityChip.text = availabilityText
//        holder.availabilityChip.isEnabled = doctor.isAvailable
//
//        // Reset views state
//        holder.selectedTimeTextView.visibility = View.GONE
//        holder.confirmButton.visibility = View.GONE
//        holder.scheduleButton.isEnabled = doctor.isAvailable
//
//        holder.scheduleButton.setOnClickListener {
//            showTimePicker(holder, doctor)
//        }
//
//        holder.confirmButton.setOnClickListener {
//            confirmAppointment(holder, doctor)
//        }
//    }
//
//    override fun getItemCount() = doctors.size
//
//    private fun showTimePicker(holder: DoctorViewHolder, doctor: Doctor) {
//        val calendar = Calendar.getInstance()
//        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
//        val currentMinute = calendar.get(Calendar.MINUTE)
//
//        // Create custom time picker dialog
//        val timePickerDialog = TimePickerDialog(
//            holder.itemView.context,
//            { _, hourOfDay, minute ->
//                val selectedTime = Calendar.getInstance().apply {
//                    set(Calendar.HOUR_OF_DAY, hourOfDay)
//                    set(Calendar.MINUTE, minute)
//                }
//
//                // Check if selected time is within availability range
//                if (isTimeInRange(selectedTime.time, doctor.availabilityStart, doctor.availabilityEnd)) {
//                    val formattedTime = timeFormat.format(selectedTime.time)
//                    holder.selectedTimeTextView.text = "Selected time: $formattedTime"
//                    holder.selectedTimeTextView.visibility = View.VISIBLE
//                    holder.confirmButton.visibility = View.VISIBLE
//                } else {
//                    Toast.makeText(
//                        holder.itemView.context,
//                        "Please select a time between ${formatTimeRange(doctor.availabilityStart, doctor.availabilityEnd)}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            },
//            currentHour,
//            currentMinute,
//            true // 24-hour format
//        )
//
//        // Set time picker to show only available hours
//        timePickerDialog.setOnShowListener {
//            val timePicker = timePickerDialog.javaClass.getDeclaredField("mTimePicker")
//            timePicker.isAccessible = true
//            val picker = timePicker.get(timePickerDialog)
//
//            // Set min and max time
//            val minTime = doctor.availabilityStart
//            val maxTime = doctor.availabilityEnd
//
//            // Set time picker properties
//            val setMinTime = picker.javaClass.getDeclaredMethod("setMinTime", Int::class.java, Int::class.java)
//            setMinTime.isAccessible = true
//            setMinTime.invoke(picker, minTime.hours, minTime.minutes)
//
//            val setMaxTime = picker.javaClass.getDeclaredMethod("setMaxTime", Int::class.java, Int::class.java)
//            setMaxTime.isAccessible = true
//            setMaxTime.invoke(picker, maxTime.hours, maxTime.minutes)
//        }
//
//        timePickerDialog.show()
//    }
//
//    private fun confirmAppointment(holder: DoctorViewHolder, doctor: Doctor) {
//        val selectedTime = holder.selectedTimeTextView.text.toString()
//            .replace("Selected time: ", "")
//
//        val request = MedicalVisitRequest(
//            doctorName = doctor.name,
//            visitDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
//            startTime = selectedTime,
//            endTime = calculateEndTime(selectedTime)
//        )
//
//        CoroutineScope(Dispatchers.Main).launch {
//            try {
//                val response = RetrofitClient.medService.submitMedicalVisit(request)
//                if (response.isSuccessful) {
//                    Toast.makeText(
//                        holder.itemView.context,
//                        "Appointment scheduled successfully!",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    onAppointmentConfirmed()
//                } else {
//                    val errorBody = response.errorBody()?.string()
//                    Toast.makeText(
//                        holder.itemView.context,
//                        "Error: ${errorBody ?: response.message()}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            } catch (e: Exception) {
//                Toast.makeText(
//                    holder.itemView.context,
//                    "Network error: ${e.localizedMessage}",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//    }
//
//    private fun calculateEndTime(startTime: String): String {
//        try {
//            val time = timeFormat.parse(startTime)
//            val calendar = Calendar.getInstance()
//            calendar.time = time
//            calendar.add(Calendar.HOUR, 1) // Assuming 1-hour appointments
//            return timeFormat.format(calendar.time)
//        } catch (e: Exception) {
//            return startTime
//        }
//    }
//
//    private fun formatTimeRange(start: TimeOfDay, end: TimeOfDay): String {
//        return "${String.format("%02d:%02d", start.hours, start.minutes)} - " +
//               "${String.format("%02d:%02d", end.hours, end.minutes)}"
//    }
//
//    private fun isTimeInRange(time: Date, start: TimeOfDay, end: TimeOfDay): Boolean {
//        val calendar = Calendar.getInstance()
//        calendar.time = time
//        val hour = calendar.get(Calendar.HOUR_OF_DAY)
//        val minute = calendar.get(Calendar.MINUTE)
//
//        val timeInMinutes = hour * 60 + minute
//        val startInMinutes = start.hours * 60 + start.minutes
//        val endInMinutes = end.hours * 60 + end.minutes
//
//        return timeInMinutes in startInMinutes..endInMinutes
//    }
//}
//
//data class Doctor(
//    val name: String,
//    val specialty: String,
//    val isAvailable: Boolean,
//    val imageResId: Int,
//    val availabilityStart: TimeOfDay,
//    val availabilityEnd: TimeOfDay
//)
//
//data class TimeOfDay(
//    val hours: Int,
//    val minutes: Int
//)