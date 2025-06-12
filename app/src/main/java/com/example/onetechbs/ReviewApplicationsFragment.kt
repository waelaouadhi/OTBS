package com.example.onetechbs
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.onetechbs.databinding.FragmentReviewApplicationsBinding

class ReviewApplicationsFragment : Fragment() {

    private var _binding: FragmentReviewApplicationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReviewApplicationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textView.text = "Review Applications Section"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}