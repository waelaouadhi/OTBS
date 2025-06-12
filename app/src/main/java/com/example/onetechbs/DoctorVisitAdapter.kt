package com.example.onetechbs

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.databinding.ItemDoctorBinding
import com.example.onetechbs.db.MedicalVisitResponse
import java.text.SimpleDateFormat
import java.util.*

class DoctorVisitAdapter(
    private val onVisitSelected: (MedicalVisitResponse, String) -> Unit
) : ListAdapter<MedicalVisitResponse, DoctorVisitAdapter.DoctorViewHolder>(DoctorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val binding = ItemDoctorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DoctorViewHolder(binding, onVisitSelected)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DoctorViewHolder(
        private val binding: ItemDoctorBinding,
        private val onVisitSelected: (MedicalVisitResponse, String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(doctor: MedicalVisitResponse) {
            binding.apply {
                doctorNameTextView.text = doctor.doctorName
                specialtyTextView.text = "Visit Date: ${doctor.visitDate}"

                availabilityChip.apply {
                    text = "Time: ${doctor.startTime} - ${doctor.endTime}"
                    setChipBackgroundColorResource(R.color.available_color)
                }

                doctorImageView.setImageResource(R.drawable.doctor2)

                // Reset views
                selectedTimeTextView.visibility = View.GONE
                confirmButton.visibility = View.GONE
                timeSlotSpinner.visibility = View.GONE

                scheduleButton.setOnClickListener {
                    populateTimeSpinner(doctor)
                }
            }
        }

        private fun generateTimeSlots(start: String, end: String): List<String> {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            val slots = mutableListOf<String>()

            try {
                val startCal = Calendar.getInstance().apply {
                    time = format.parse(start)!!
                }
                val endCal = Calendar.getInstance().apply {
                    time = format.parse(end)!!
                }

                while (startCal.before(endCal)) {
                    slots.add(format.format(startCal.time))
                    startCal.add(Calendar.MINUTE, 30)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return slots
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun populateTimeSpinner(doctor: MedicalVisitResponse) {
            val context = binding.root.context
            val timeSlots = generateTimeSlots(doctor.startTime, doctor.endTime)

            if (timeSlots.isEmpty()) {
                Toast.makeText(context, "No available time slots.", Toast.LENGTH_SHORT).show()
                return
            }

            // Show UI
            binding.timeSlotSpinner.visibility = View.VISIBLE
            binding.selectedTimeTextView.visibility = View.VISIBLE
            binding.confirmButton.visibility = View.VISIBLE

            val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, timeSlots)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.timeSlotSpinner.adapter = spinnerAdapter

            // Default selected
            val defaultSelected = timeSlots[0]
            binding.selectedTimeTextView.text = "Selected time: $defaultSelected"

            binding.timeSlotSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selectedTime = parent.getItemAtPosition(position) as String
                    binding.selectedTimeTextView.text = "Selected time: $selectedTime"

                    binding.confirmButton.setOnClickListener {
                        confirmAppointment(doctor, selectedTime)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun confirmAppointment(doctor: MedicalVisitResponse, selectedTime: String) {
            val context = binding.root.context
            val today = Calendar.getInstance()

            val visitDateParsed = try {
                dateFormat.parse(doctor.visitDate)
            } catch (e: Exception) {
                null
            }

            if (visitDateParsed == null) {
                Toast.makeText(context, "Invalid visit date format.", Toast.LENGTH_SHORT).show()
                return
            }

            val visitDateCal = Calendar.getInstance().apply {
                time = visitDateParsed
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val todayDateCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (visitDateCal.before(todayDateCal)) {
                Toast.makeText(context, "Visit date must be today or a future date.", Toast.LENGTH_SHORT).show()
                return
            }

            val isToday = visitDateCal == todayDateCal
            if (isToday) {
                val now = timeFormat.format(today.time)
                if (selectedTime <= now) {
                    Toast.makeText(context, "Start time must be in the future.", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            onVisitSelected(doctor, selectedTime)
        }
    }

    private class DoctorDiffCallback : DiffUtil.ItemCallback<MedicalVisitResponse>() {
        override fun areItemsTheSame(oldItem: MedicalVisitResponse, newItem: MedicalVisitResponse): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MedicalVisitResponse, newItem: MedicalVisitResponse): Boolean =
            oldItem == newItem
    }
}