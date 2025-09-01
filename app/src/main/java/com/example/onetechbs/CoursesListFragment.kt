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

    override fun onResume() {
        super.onResume()
        // Refresh to ensure UI reflects latest enrolled/requested states
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
                    val raw = (response.body() ?: emptyList())
                        .sortedByDescending { it.id } // sort newest first

                    // Server is the source of truth: refresh local cache from server flags
                    val prefs = requireContext().getSharedPreferences("training", android.content.Context.MODE_PRIVATE)
                    val serverEnrolled = raw.filter { it.isEnrolled }.map { it.id.toString() }.toSet()
                    val serverRequested = raw.filter { it.isRequested }.map { it.id.toString() }.toSet()
                    prefs.edit()
                        .putStringSet("enrolled_ids", serverEnrolled)
                        .putStringSet("requested_ids", serverRequested)
                        .apply()

                    // Also fetch training requests and override flags so Employee view reflects approvals immediately
                    val trResp = RetrofitClient.trainingService2.getAllTrainingRequests("Bearer $token")
                    val approvedIds: Set<String>
                    val pendingIds: Set<String>
                    val rejectedIds: Set<String>
                    if (trResp.isSuccessful) {
                        val reqs = trResp.body() ?: emptyList()
                        val currentUserId = SharedPreferencesManager.getCurrentUserId(requireContext())
                        approvedIds = reqs.filter { it.status.equals("APPROVED", ignoreCase = true) && it.employeeId == currentUserId }
                            .mapNotNull { it.course?.id?.toString() }
                            .toSet()
                        pendingIds = reqs.filter { it.status.equals("PENDING", ignoreCase = true) && it.employeeId == currentUserId }
                            .mapNotNull { it.course?.id?.toString() }
                            .toSet()
                        rejectedIds = reqs.filter { it.status.equals("REJECTED", ignoreCase = true) && it.employeeId == currentUserId }
                            .mapNotNull { it.course?.id?.toString() }
                            .toSet()
                    } else {
                        approvedIds = emptySet()
                        pendingIds = emptySet()
                        rejectedIds = emptySet()
                    }

                    val finalList = raw.map { c ->
                        val idStr = c.id.toString()
                        when {
                            approvedIds.contains(idStr) -> c.copy(isEnrolled = true, isRequested = false)
                            pendingIds.contains(idStr) -> c.copy(isEnrolled = false, isRequested = true)
                            else -> c
                        }
                    }

                    if (finalList.isEmpty()) {
                        binding.textEmptyCourses.visibility = View.VISIBLE
                    }
                    // Persist cache from finalList for consistency
                    val finalEnrolled = finalList.filter { it.isEnrolled }.map { it.id.toString() }.toSet()
                    val finalRequested = finalList.filter { it.isRequested }.map { it.id.toString() }.toSet()
                    prefs.edit()
                        .putStringSet("enrolled_ids", finalEnrolled)
                        .putStringSet("requested_ids", finalRequested)
                        .apply()
                    // Build per-user status map for adapter
                    val statusMap: Map<Long, String> = finalList.associate { c ->
                        val idStr = c.id.toString()
                        val status = when {
                            approvedIds.contains(idStr) -> "APPROVED"
                            pendingIds.contains(idStr) -> "PENDING"
                            rejectedIds.contains(idStr) -> "REJECTED"
                            else -> null
                        }
                        c.id to (status ?: "")
                    }.filterValues { it.isNotEmpty() }

                    adapter = CoursesAdapter(finalList.toMutableList(), userRole, { course ->
                        onEnrollApproveClicked(course)
                    }, statusMap)
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
                        // Cache locally as requested to disable button immediately
                        val prefs = requireContext().getSharedPreferences("training", android.content.Context.MODE_PRIVATE)
                        val requested = prefs.getStringSet("requested_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
                        requested.add(course.id.toString())
                        prefs.edit().putStringSet("requested_ids", requested).apply()
                        // Force rebind to hide/disable buttons via cache immediately
                        binding.recyclerViewCourses.adapter?.notifyDataSetChanged()
                        fetchCourses()
                    } else {
                        val err = response.errorBody()?.string()
                        if (response.code() == 400 && err?.contains("already requested", ignoreCase = true) == true) {
                            // Mark as requested locally, since server says it's already requested
                            val prefs = requireContext().getSharedPreferences("training", android.content.Context.MODE_PRIVATE)
                            val requested = prefs.getStringSet("requested_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
                            requested.add(course.id.toString())
                            prefs.edit().putStringSet("requested_ids", requested).apply()
                            binding.recyclerViewCourses.adapter?.notifyDataSetChanged()
                            Toast.makeText(requireContext(), "Already requested. Marked as pending.", Toast.LENGTH_SHORT).show()
                            fetchCourses()
                        } else {
                            Toast.makeText(requireContext(), "Failed to enroll: ${err ?: response.code()}", Toast.LENGTH_LONG).show()
                        }
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
