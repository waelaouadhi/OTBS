package com.example.onetechbs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.onetechbs.databinding.FragmentDocumentBinding

class DocumentFragment : Fragment() {

    private var _binding: FragmentDocumentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocumentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Spinner
        val documents = resources.getStringArray(R.array.document_types)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, documents)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDocuments.adapter = adapter

        // Setup Button
        binding.buttonRequestDocument.setOnClickListener {
            val selectedDocument = binding.spinnerDocuments.selectedItem.toString()
            Toast.makeText(requireContext(), "Request for '$selectedDocument' sent successfully!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
