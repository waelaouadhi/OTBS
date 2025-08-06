package com.example.onetechbs

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.onetechbs.db.CourseRequestDTO
import com.google.android.material.appbar.MaterialToolbar
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal

class CreateCourseFragment : Fragment() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_course, container, false)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbarCreateCourse)
        val editTextTitle = view.findViewById<EditText>(R.id.editTextTitle)
        val editTextDescription = view.findViewById<EditText>(R.id.editTextDescription)
        val switchCertification = view.findViewById<Switch>(R.id.switchCertification)
        val editTextCost = view.findViewById<EditText>(R.id.editTextCost)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitCourse)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBarCreateCourse)

        toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        btnSubmit.setOnClickListener {
            val title = editTextTitle.text.toString().trim()
            val description = editTextDescription.text.toString().trim()
            val hasCertification = switchCertification.isChecked
            val costText = editTextCost.text.toString().trim()
            val cost = costText.toBigDecimalOrNull()

            if (title.isEmpty() || description.isEmpty() || cost == null) {
                Toast.makeText(requireContext(), "Please fill all fields correctly.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnSubmit.isEnabled = false

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val prefs = SharedPreferencesManager.getInstance(requireContext())
                    val token = prefs.getAuthToken()
                    val dto = CourseRequestDTO(title, description, hasCertification, cost)
                    val response = RetrofitClient.trainingService2.createCourse("Bearer $token", dto)
                    progressBar.visibility = View.GONE
                    btnSubmit.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Course created!", Toast.LENGTH_SHORT).show()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    } else {
                        Toast.makeText(requireContext(), "Failed to create course.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    progressBar.visibility = View.GONE
                    btnSubmit.isEnabled = true
                    Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return view
    }
}
