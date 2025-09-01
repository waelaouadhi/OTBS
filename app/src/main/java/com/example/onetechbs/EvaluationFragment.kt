package com.example.onetechbs

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.db.CriterionDTO
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class EvaluationFragment : Fragment() {
    
    private val viewModel: JobOfferCreationViewModel by activityViewModels()
    
    private lateinit var criteriaRecyclerView: RecyclerView
    private lateinit var totalWeightText: TextView
    private lateinit var saveButton: MaterialButton
    private lateinit var criteriaAdapter: CriteriaAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_evaluation, container, false)
        
        initViews(view)
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        return view
    }
    
    private fun initViews(view: View) {
        criteriaRecyclerView = view.findViewById(R.id.rvCriteria)
        totalWeightText = view.findViewById(R.id.tvTotalWeight)
        saveButton = view.findViewById(R.id.btnSaveAndProceed)
    }
    
    private fun setupRecyclerView() {
        criteriaAdapter = CriteriaAdapter { index, weight ->
            viewModel.updateCriterionWeight(index, weight)
        }
        criteriaRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        criteriaRecyclerView.adapter = criteriaAdapter
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.criteria.collect { criteria ->
                criteriaAdapter.updateCriteria(criteria)
                updateTotalWeight(criteria)
            }
        }
    }
    
    private fun setupClickListeners() {
        saveButton.setOnClickListener {
            if (validateWeights()) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_layout, ReviewFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
    
    private fun updateTotalWeight(criteria: List<CriterionDTO>) {
        val total = criteria.sumOf { it.weight }
        totalWeightText.text = "Total Weight: $total%"
        
        // Change color based on validity
        totalWeightText.setTextColor(
            if (total == 100) {
                requireContext().getColor(android.R.color.holo_green_dark)
            } else {
                requireContext().getColor(android.R.color.holo_red_dark)
            }
        )
    }
    
    private fun validateWeights(): Boolean {
        val total = viewModel.criteria.value.sumOf { it.weight }
        return if (total != 100) {
            Toast.makeText(
                requireContext(),
                "Total weight must equal 100%. Current total: $total%",
                Toast.LENGTH_LONG
            ).show()
            false
        } else {
            true
        }
    }
}

class CriteriaAdapter(
    private val onWeightChanged: (Int, Int) -> Unit
) : RecyclerView.Adapter<CriteriaAdapter.CriteriaViewHolder>() {
    
    private var criteria = listOf<CriterionDTO>()
    
    fun updateCriteria(newCriteria: List<CriterionDTO>) {
        criteria = newCriteria
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CriteriaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_criterion, parent, false)
        return CriteriaViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: CriteriaViewHolder, position: Int) {
        holder.bind(criteria[position], position)
    }
    
    override fun getItemCount() = criteria.size
    
    inner class CriteriaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.tvCriterionName)
        private val weightInput: TextInputEditText = itemView.findViewById(R.id.etWeight)
        
        fun bind(criterion: CriterionDTO, position: Int) {
            nameText.text = criterion.name
            weightInput.setText(criterion.weight.toString())
            
            weightInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val weight = s.toString().toIntOrNull() ?: 0
                    if (weight >= 0 && weight <= 100) {
                        onWeightChanged(position, weight)
                    }
                }
            })
        }
    }
}
