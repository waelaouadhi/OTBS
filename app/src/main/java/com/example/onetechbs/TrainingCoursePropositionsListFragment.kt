package com.example.onetechbs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.onetechbs.databinding.FragmentTrainingCoursePropositionsListBinding

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.onetechbs.db.CoursePropositionResponseDTO
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

class TrainingCoursePropositionsListFragment : Fragment() {
    private var _binding: FragmentTrainingCoursePropositionsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CoursePropositionAdapter
    private var isDhr: Boolean = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTrainingCoursePropositionsListBinding.inflate(inflater, container, false)
        // Setup toolbar with back navigation
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        setupRecyclerView()
        fetchUserRoleAndLoadPropositions()
        return binding.root
    }

    private fun setupRecyclerView() {
        binding.recyclerViewPropositions.layoutManager = LinearLayoutManager(requireContext())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchUserRoleAndLoadPropositions() {
        val prefs = com.example.onetechbs.util.SharedPreferencesManager.getInstance(requireContext())
        val storedRole = prefs.getUserRole()
        val role = getUserRole(requireContext())
        Log.d("PropositionRole", "Stored role: $storedRole, Detected role: $role")
        Toast.makeText(requireContext(), "Detected role: $role (stored: $storedRole)", Toast.LENGTH_LONG).show()
        isDhr = role == "DHR"
        if (isDhr) {
            val itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT or androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
                override fun onMove(recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, target: androidx.recyclerview.widget.RecyclerView.ViewHolder): Boolean = false
                override fun getSwipeDirs(recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder): Int {
                    val position = viewHolder.adapterPosition
                    val proposition = (binding.recyclerViewPropositions.adapter as? CoursePropositionAdapter)?.getPropositionAt(position)
                    // Only allow swipes for PENDING items; disable for APPROVED/REJECTED
                    return if (proposition?.status?.name == "PENDING") {
                        super.getSwipeDirs(recyclerView, viewHolder)
                    } else {
                        0
                    }
                }
                override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val proposition = (binding.recyclerViewPropositions.adapter as? CoursePropositionAdapter)?.getPropositionAt(position)
                    if (proposition != null && proposition.status.name == "PENDING") {
                        if (direction == androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
                            handleStatusChange(proposition, "APPROVED")
                        } else if (direction == androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
                            handleStatusChange(proposition, "REJECTED")
                        }
                    } else {
                        binding.recyclerViewPropositions.adapter?.notifyItemChanged(position)
                    }
                }
                override fun onChildDraw(
                    c: android.graphics.Canvas,
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    // Avoid drawing swipe background/icons for non-PENDING items (swipe disabled)
                    val position = viewHolder.adapterPosition
                    val proposition = (binding.recyclerViewPropositions.adapter as? CoursePropositionAdapter)?.getPropositionAt(position)
                    if (proposition?.status?.name != "PENDING") {
                        super.onChildDraw(c, recyclerView, viewHolder, 0f, dY, actionState, false)
                        return
                    }
                    val itemView = viewHolder.itemView
                    val context = itemView.context
                    val iconMargin = (itemView.height - 72) / 2 // icon size 72
                    val approveIcon = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_approve)
                    val rejectIcon = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_reject)
                    val paint = android.graphics.Paint()
                    if (dX > 0) { // Swiping right (approve)
                        paint.color = android.graphics.Color.parseColor("#4CAF50") // Green
                        c.drawRect(
                            itemView.left.toFloat(),
                            itemView.top.toFloat(),
                            itemView.left + dX,
                            itemView.bottom.toFloat(),
                            paint
                        )
                        approveIcon?.let {
                            val iconTop = itemView.top + (itemView.height - it.intrinsicHeight) / 2
                            val iconLeft = itemView.left + iconMargin
                            val iconRight = iconLeft + it.intrinsicWidth
                            val iconBottom = iconTop + it.intrinsicHeight
                            it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            it.draw(c)
                        }
                    } else if (dX < 0) { // Swiping left (reject)
                        paint.color = android.graphics.Color.parseColor("#F44336") // Red
                        c.drawRect(
                            itemView.right + dX,
                            itemView.top.toFloat(),
                            itemView.right.toFloat(),
                            itemView.bottom.toFloat(),
                            paint
                        )
                        rejectIcon?.let {
                            val iconTop = itemView.top + (itemView.height - it.intrinsicHeight) / 2
                            val iconRight = itemView.right - iconMargin
                            val iconLeft = iconRight - it.intrinsicWidth
                            val iconBottom = iconTop + it.intrinsicHeight
                            it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            it.draw(c)
                        }
                    }
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                }
            })
            itemTouchHelper.attachToRecyclerView(binding.recyclerViewPropositions)
        }
        fetchPropositions()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchPropositions() {
        // Show loading indicator
        binding.progressBarPropositions?.visibility = View.VISIBLE
        binding.recyclerViewPropositions.visibility = View.GONE
        binding.textEmptyPropositions?.visibility = View.GONE
        lifecycleScope.launch {
            val prefs = SharedPreferencesManager.getInstance(requireContext())
            val token = prefs.getAuthToken()
            if (token.isNullOrEmpty() || prefs.isTokenExpired()) {
                binding.progressBarPropositions?.visibility = View.GONE
                Toast.makeText(requireContext(), "Authentication token missing or expired. Please log in again.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                val response = RetrofitClient.trainingService2.getAllCoursePropositions("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val propositions = response.body()!!
                    if (propositions.isEmpty()) {
                        binding.textEmptyPropositions?.visibility = View.VISIBLE
                        binding.recyclerViewPropositions.visibility = View.GONE
                    } else {
                        adapter = CoursePropositionAdapter(
                            propositions,
                            isDhr,
                            onApprove = { proposition -> handleStatusChange(proposition, "APPROVED") },
                            onReject = { proposition -> handleStatusChange(proposition, "REJECTED") }
                        )
                        binding.recyclerViewPropositions.adapter = adapter
                        binding.recyclerViewPropositions.visibility = View.VISIBLE
                        binding.textEmptyPropositions?.visibility = View.GONE
                        binding.progressBarPropositions?.visibility = View.GONE
                    }
                } else {
                    binding.textEmptyPropositions?.visibility = View.VISIBLE
                    binding.textEmptyPropositions?.text = "Failed to load propositions."
                    binding.recyclerViewPropositions.visibility = View.GONE
                    binding.progressBarPropositions?.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to load propositions", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace() // ← This logs full stack trace to console
                Log.e("PropositionListError", "Exception: ${e.message}")
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                binding.progressBarPropositions?.visibility = View.GONE
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleStatusChange(proposition: CoursePropositionResponseDTO, status: String) {
        val role = getUserRole(requireContext())
        if (role != "DHR") {
            Toast.makeText(requireContext(), "Only DHR can approve or reject propositions.", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val prefs = com.example.onetechbs.util.SharedPreferencesManager.getInstance(requireContext())
            val token = prefs.getAuthToken()
            try {
                val response = when (status.uppercase()) {
                    "APPROVED" -> RetrofitClient.trainingService2.approveCourseProposition(proposition.id, "Bearer $token")
                    "REJECTED" -> RetrofitClient.trainingService2.rejectCourseProposition(proposition.id, "Bearer $token")
                    else -> null
                }
                if (response != null && response.isSuccessful) {
                    Toast.makeText(requireContext(), "Proposition ${status.lowercase().capitalize()}!", Toast.LENGTH_SHORT).show()
                    fetchPropositions() // Refresh list
                } else {
                    Toast.makeText(requireContext(), "Failed to update status", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dummy function for demo; replace with real role fetch logic
    private fun getUserRole(context: Context): String {
        val prefs = com.example.onetechbs.util.SharedPreferencesManager.getInstance(context)
        val role = prefs.getUserRole()?.uppercase()
        return if (role == "DHR" || role == "HRD") "DHR" else "MANAGER"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

