package com.example.onetechbs
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.databinding.ItemJobOfferBinding
import com.example.onetechbs.db.JobOfferResponseDTO

class JobOfferAdapter : RecyclerView.Adapter<JobOfferAdapter.JobOfferViewHolder>() {

    private val jobOffers = mutableListOf<JobOfferResponseDTO>()

    fun submitList(list: List<JobOfferResponseDTO>) {
        jobOffers.clear()
        jobOffers.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobOfferViewHolder {
        val binding = ItemJobOfferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JobOfferViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JobOfferViewHolder, position: Int) {
        holder.bind(jobOffers[position])
    }

    override fun getItemCount(): Int = jobOffers.size

    class JobOfferViewHolder(private val binding: ItemJobOfferBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(jobOffer: JobOfferResponseDTO) {
            binding.titleTextView.text = jobOffer.title
            binding.departmentTextView.text = jobOffer.department
            binding.descriptionTextView.text = jobOffer.description
        }
    }
}