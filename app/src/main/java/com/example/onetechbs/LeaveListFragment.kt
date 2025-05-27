package com.example.onetechbs

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.Leave
import com.example.onetechbs.LeaveListAdapter
import com.example.onetechbs.R
import com.example.onetechbs.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LeaveListFragment : Fragment() {

    private lateinit var leaveRecyclerView: RecyclerView
    private lateinit var leaveListAdapter: LeaveListAdapter
    private val leaveList = mutableListOf<Leave>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_leave_list, container, false)
        leaveRecyclerView = view.findViewById(R.id.leaveRecyclerView)
        leaveListAdapter = LeaveListAdapter(leaveList)
        leaveRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        leaveRecyclerView.adapter = leaveListAdapter

        fetchLeaves() // Fetch leaves on fragment load
        return view
    }

    private fun fetchLeaves() {
        val token = RetrofitClient.getAuthToken()

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Unauthorized: Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.leaveService.getUserLeaves().enqueue(object : Callback<List<Leave>> {
            override fun onResponse(call: Call<List<Leave>>, response: Response<List<Leave>>) {
                if (response.isSuccessful) {
                    leaveList.clear()
                    leaveList.addAll(response.body() ?: emptyList())
                    leaveListAdapter.notifyDataSetChanged()
                } else {
                    handleError(response)
                }
            }

            override fun onFailure(call: Call<List<Leave>>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("LeaveListFragment", "Error: ${t.message}", t)
            }
        })
    }

    private fun handleError(response: Response<*>) {
        val errorMessage = when (response.code()) {
            401 -> {
                // Optional: Clear token on unauthorized error
                RetrofitClient.setAuthToken("")
                "Unauthorized: Please login again."
            }
            403 -> "Access denied: You don't have permission."
            404 -> "Resource not found."
            else -> "Unexpected error: ${response.message()}"
        }

        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        Log.e("LeaveListFragment", "Error ${response.code()}: ${response.errorBody()?.string()}")
    }
}