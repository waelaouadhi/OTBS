package com.example.onetechbs.adapter

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.onetechbs.R
import com.example.onetechbs.databinding.ItemDoctorManagementBinding
import com.example.onetechbs.db.MedicalVisitResponse

class DoctorManagementAdapter(
    private val onDeleteClicked: (MedicalVisitResponse) -> Unit,
    private val onCardClicked: (MedicalVisitResponse) -> Unit
) : RecyclerView.Adapter<DoctorManagementAdapter.VisitViewHolder>() {

    private val visits = mutableListOf<MedicalVisitResponse>()
    private var swipeToDeleteCallback: SwipeToDeleteCallback? = null
    
    fun attachSwipeToDelete(recyclerView: RecyclerView) {
        swipeToDeleteCallback = SwipeToDeleteCallback(recyclerView.context) { position ->
            onDeleteClicked(visits[position])
        }
        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback!!)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    fun submitList(newList: List<MedicalVisitResponse>) {
        visits.clear()
        visits.addAll(newList)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        if (position in 0 until visits.size) {
            visits.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    inner class VisitViewHolder(private val binding: ItemDoctorManagementBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(visit: MedicalVisitResponse) {
            binding.doctorName.text = "Doctor: ${visit.doctorName}"
            binding.visitDate.text = "Date: ${visit.visitDate}"
            binding.timeRange.text = "Time: ${visit.startTime} - ${visit.endTime}"
            
            // Hide the static delete button since we're using swipe-to-delete
            binding.deleteButton.visibility = View.GONE
            
            itemView.setOnClickListener {
                onCardClicked(visit)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitViewHolder {
        val binding = ItemDoctorManagementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VisitViewHolder(binding)
    }

    override fun getItemCount(): Int = visits.size

    override fun onBindViewHolder(holder: VisitViewHolder, position: Int) {
        holder.bind(visits[position])
    }

    private inner class SwipeToDeleteCallback(
        context: android.content.Context,
        private val onSwiped: (Int) -> Unit
    ) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

        private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.baseline_delete_24)
        private val background = ColorDrawable(Color.RED)
        private val iconMargin = context.resources.getDimensionPixelSize(R.dimen.icon_margin)

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            onSwiped(viewHolder.adapterPosition)
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            val itemView = viewHolder.itemView

            // Draw red background
            background.bounds = when {
                dX > 0 -> { // Swiping right
                    android.graphics.Rect(
                        itemView.left,
                        itemView.top,
                        itemView.left + dX.toInt(),
                        itemView.bottom
                    )
                }
                dX < 0 -> { // Swiping left
                    android.graphics.Rect(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                }
                else -> return
            }
            background.draw(c)

            // Draw delete icon
            deleteIcon?.let { icon ->
                val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                val iconLeft: Int
                val iconRight: Int

                if (dX > 0) { // Swiping right
                    iconLeft = itemView.left + iconMargin
                    iconRight = iconLeft + icon.intrinsicWidth
                } else { // Swiping left
                    iconRight = itemView.right - iconMargin
                    iconLeft = iconRight - icon.intrinsicWidth
                }

                icon.setBounds(iconLeft, iconTop, iconRight, iconTop + icon.intrinsicHeight)
                icon.draw(c)
            }

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }
}