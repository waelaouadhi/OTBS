package com.example.onetechbs

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onetechbs.databinding.FragmentCoursesListBinding
import com.example.onetechbs.db.CourseResponseDTO
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import kotlinx.coroutines.launch

class CoursesListFragment : Fragment() {
    private var _binding: FragmentCoursesListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CoursesAdapter
    private var userRole: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoursesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbarCoursesList.setNavigationOnClickListener { requireActivity().onBackPressed() }
        userRole = SharedPreferencesManager.getInstance(requireContext()).getUserRole()
        setupRecyclerView()
        fetchCourses()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewCourses.layoutManager = LinearLayoutManager(requireContext())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchCourses() {
        binding.progressBarCourses.visibility = View.VISIBLE
        binding.textEmptyCourses.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val token = SharedPreferencesManager.getInstance(requireContext()).getAuthToken()
                val response = RetrofitClient.trainingService2.getAllCourses("Bearer $token")
                if (response.isSuccessful) {
                    val courses = response.body() ?: emptyList()
                    if (courses.isEmpty()) {
                        binding.textEmptyCourses.visibility = View.VISIBLE
                    }
                    adapter = CoursesAdapter(courses, userRole) { course ->
                        onEnrollApproveClicked(course)
                    }
                    binding.recyclerViewCourses.adapter = adapter
                } else {
                    Toast.makeText(requireContext(), "Failed to load courses", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarCourses.visibility = View.GONE
            }
        }
    }

    private fun onEnrollApproveClicked(course: CourseResponseDTO) {
        if (userRole == "Employee") {
            binding.progressBarCourses.visibility = View.VISIBLE
            lifecycleScope.launch {
                try {
                    val token = com.example.onetechbs.util.SharedPreferencesManager.getInstance(requireContext()).getAuthToken()
                    val response = com.example.onetechbs.network.RetrofitClient.trainingService2.enrollInCourse(course.id, "Bearer $token")
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Enrollment request sent!", Toast.LENGTH_SHORT).show()
                        fetchCourses() // Refresh list to update status
                    } else {
                        Toast.makeText(requireContext(), "Failed to enroll: ${response.errorBody()?.string() ?: response.code()}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    binding.progressBarCourses.visibility = View.GONE
                }
            }
        } else {
            Toast.makeText(requireContext(), "Only employees can enroll.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
