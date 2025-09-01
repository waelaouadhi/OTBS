package com.example.onetechbs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.db.CriterionDTO
import com.example.onetechbs.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import android.util.Log
import com.example.onetechbs.util.SharedPreferencesManager

class ReviewFragment : Fragment() {
    
    private val viewModel: JobOfferCreationViewModel by activityViewModels()
    private val TAG = "ReviewFragment"
    
    private lateinit var titleText: TextView
    private lateinit var summaryText: TextView
    private lateinit var departmentText: TextView
    private lateinit var responsibilitiesText: TextView
    private lateinit var requiredQualificationsText: TextView
    private lateinit var preferredQualificationsText: TextView
    private lateinit var whatWeOfferText: TextView
    private lateinit var criteriaRecyclerView: RecyclerView
    private lateinit var submitButton: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var criteriaAdapter: ReviewCriteriaAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_review, container, false)
        
        initViews(view)
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        return view
    }
    
    private fun initViews(view: View) {
        titleText = view.findViewById(R.id.tvTitle)
        summaryText = view.findViewById(R.id.tvSummary)
        departmentText = view.findViewById(R.id.tvDepartment)
        responsibilitiesText = view.findViewById(R.id.tvResponsibilities)
        requiredQualificationsText = view.findViewById(R.id.tvRequiredQualifications)
        preferredQualificationsText = view.findViewById(R.id.tvPreferredQualifications)
        whatWeOfferText = view.findViewById(R.id.tvWhatWeOffer)
        criteriaRecyclerView = view.findViewById(R.id.rvCriteria)
        submitButton = view.findViewById(R.id.btnSubmit)
        progressBar = view.findViewById(R.id.progressBar)
        // Toolbar back navigation
        view.findViewById<MaterialToolbar?>(R.id.toolbar)?.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
    
    private fun setupRecyclerView() {
        criteriaAdapter = ReviewCriteriaAdapter()
        criteriaRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        criteriaRecyclerView.adapter = criteriaAdapter
    }
    
    private fun setupObservers() {
        // Initialize API service (point to 8088 job-offers) with JWT from SharedPreferencesManager
        val token = SharedPreferencesManager.getInstance(requireContext()).getAuthToken()
        val masked = token?.take(10)?.padEnd(10, '*') ?: "<null>"
        Log.d(TAG, "JWT from SharedPreferencesManager: $masked")
        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Authentication required. Please sign in again.", Toast.LENGTH_LONG).show()
            // Disable submit to avoid 401 spam
            submitButton.isEnabled = false
            return
        }
        val apiService = RetrofitClient.getJobClient(token).create(com.example.onetechbs.network.ApiService::class.java)
        viewModel.setApiService(apiService)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.generatedTitle.collect { title ->
                titleText.text = title
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.summary.collect { summary ->
                summaryText.text = summary
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.generatedDepartment.collect { department ->
                departmentText.text = department
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.responsibilities.collect { responsibilities ->
                responsibilitiesText.text = responsibilities.joinToString("\n• ", "• ")
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.requiredQualifications.collect { qualifications ->
                requiredQualificationsText.text = qualifications.joinToString("\n• ", "• ")
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.preferredQualifications.collect { qualifications ->
                preferredQualificationsText.text = qualifications.joinToString("\n• ", "• ")
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.whatWeOffer.collect { offer ->
                whatWeOfferText.text = offer
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.criteria.collect { criteria ->
                criteriaAdapter.updateCriteria(criteria)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                Log.d(TAG, "Submission loading: $isLoading")
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                submitButton.isEnabled = !isLoading
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Log.e(TAG, "Submission error: $it")
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSubmissionSuccessful.collect { isSuccessful ->
                Log.i(TAG, "Submission success state: $isSuccessful")
                if (isSuccessful) {
                    Toast.makeText(requireContext(), "Job offer created successfully!", Toast.LENGTH_LONG).show()
                    // Clear back stack and navigate to the first phase without NavController
                    parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_layout, DetailsFragment())
                        .commit()
                    viewModel.resetSubmissionState()
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        submitButton.setOnClickListener {
            Log.d(TAG, "Submit button clicked - triggering submitJobOffer()")
            viewModel.submitJobOffer()
        }
    }
}

class ReviewCriteriaAdapter : RecyclerView.Adapter<ReviewCriteriaAdapter.ReviewCriteriaViewHolder>() {
    
    private var criteria = listOf<CriterionDTO>()
    
    fun updateCriteria(newCriteria: List<CriterionDTO>) {
        criteria = newCriteria
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewCriteriaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review_criterion, parent, false)
        return ReviewCriteriaViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ReviewCriteriaViewHolder, position: Int) {
        holder.bind(criteria[position])
    }
    
    override fun getItemCount() = criteria.size
    
    class ReviewCriteriaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.tvCriterionName)
        private val weightText: TextView = itemView.findViewById(R.id.tvWeight)
        
        fun bind(criterion: CriterionDTO) {
            nameText.text = criterion.name
            weightText.text = "${criterion.weight}%"
        }
    }
}
