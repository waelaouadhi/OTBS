package com.example.onetechbs


import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.onetechbs.databinding.FragmentHomefraBinding
import com.example.onetechbs.db.LeaveBalanceResponse
import com.example.onetechbs.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Homefra : Fragment() {

    private var _binding: FragmentHomefraBinding? = null
    private val binding get() = _binding!!

    // TextViews for displaying employee information
    private var firstNameTextView: TextView? = null
    private var lastNameTextView: TextView? = null
    private var emailTextView: TextView? = null
    private var jobTitleTextView: TextView? = null
    private var roleTextView: TextView? = null
    private var leaveStatusTextView: TextView? = null
    private var availableLeaveTextView: TextView? = null
    private var leaveUsedTextView: TextView? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomefraBinding.inflate(inflater, container, false)

        // Initialize the TextViews for displaying employee information
        firstNameTextView = binding.firstNameTextView
        lastNameTextView = binding.lastNameTextView
        roleTextView = binding.roleTextView

        availableLeaveTextView = binding.root.findViewById(R.id.Availebal_leave) // For displaying available leave
        leaveUsedTextView = binding.root.findViewById(R.id.leave_used) // For displaying used leave

        // Retrieve employee data from SharedPreferences
        val prefs = requireActivity().getSharedPreferences("auth", AppCompatActivity.MODE_PRIVATE)
        val firstName = prefs.getString("firstName", "Unknown")
        val lastName = prefs.getString("lastName", "Unknown")
        val email = prefs.getString("email", "Unknown")
        val jobTitle = prefs.getString("jobTitle", "Unknown")
        val role = prefs.getString("role", "Unknown")

        // Display employee data in TextViews
        firstNameTextView?.text = " $firstName"
        lastNameTextView?.text = "$lastName"
        emailTextView?.text = "Email: $email"
        jobTitleTextView?.text = "Job Title: $jobTitle"
        roleTextView?.text = "$role"

        // Fetch leave balance from backend and update the UI
        fetchLeaveBalance()

        // Handle the button click for submitting leave
        binding.buttonsub.setOnClickListener {
            val addLeaveFragment = AddLeaveFragment()
            binding.buttonsub.visibility = View.GONE // Hide the button after clicking

            // Set the listener for the AddLeaveFragment
            addLeaveFragment.setOnLeaveSubmittedListener { category, startDate, endDate ->
                // After leave is submitted, update leave status in Homefra
                updateLeaveStatus(category.toString(), startDate.toString(), endDate.toString())
            }

            // Navigate to AddLeaveFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.frameLayoutContainer, addLeaveFragment)
                .addToBackStack(null)
                .commit()


        }

        return binding.root

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchLeaveBalance() {
        // Log the token being sent for debugging
        println("Sending token: ${RetrofitClient.getAuthToken()}") // Access the token using getter

        // Fetch the leave balance using the leave-service API
        RetrofitClient.leaveService.getLeaveBalance().enqueue(object : Callback<LeaveBalanceResponse> {
            override fun onResponse(call: Call<LeaveBalanceResponse>, response: Response<LeaveBalanceResponse>) {
                if (response.isSuccessful) {
                    val leaveBalance = response.body()
                    leaveBalance?.let {
                        // Set the available leave and used leave text views
                        availableLeaveTextView?.text = it.remainingLeave.toString()
                        leaveUsedTextView?.text = (it.totalLeave - it.remainingLeave).toString() // Leave used = total - remaining
                    }
                } else {
                    // Log the response code and message for debugging
                    Toast.makeText(requireContext(), "Failed to fetch leave balance. Code: ${response.code()} Message: ${response.message()}", Toast.LENGTH_SHORT).show()
                    println("Response Code: ${response.code()}")
                    println("Response Message: ${response.message()}")
                    // Optional: Log the response body to see the details
                    response.errorBody()?.let {
                        println("Error Body: ${it.string()}")
                    }
                }
            }

            override fun onFailure(call: Call<LeaveBalanceResponse>, t: Throwable) {
                // Log detailed error information
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                t.printStackTrace()  // Print the stack trace for more details
            }
        })
    }
    private fun updateLeaveStatus(category: String, startDate: String?, endDate: String?) {
        val leaveStatus = "Leave Category: $category\nStart Date: $startDate\nEnd Date: $endDate"

        // Update the text of the TextView
        leaveStatusTextView?.text = leaveStatus
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        leaveStatusTextView = null  // Avoid memory leaks
    }
}