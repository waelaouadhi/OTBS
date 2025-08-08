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
    private val onUpdateClicked: (MedicalVisitResponse) -> Unit,
    private val onCardClicked: (MedicalVisitResponse) -> Unit
) : RecyclerView.Adapter<DoctorManagementAdapter.VisitViewHolder>() {

    private val visits = mutableListOf<MedicalVisitResponse>()
    private var swipeCallback: SwipeCallback? = null
    
    fun attachSwipeActions(recyclerView: RecyclerView) {
        swipeCallback = SwipeCallback(recyclerView.context) { position, action ->
            when (action) {
                SwipeAction.DELETE -> onDeleteClicked(visits[position])
                SwipeAction.UPDATE -> onUpdateClicked(visits[position])
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeCallback!!)
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

    enum class SwipeAction {
        DELETE, UPDATE
    }

    private inner class SwipeCallback(
        context: android.content.Context,
        private val onSwiped: (Int, SwipeAction) -> Unit
    ) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

        private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.baseline_delete_24)
        private val editIcon = ContextCompat.getDrawable(context, R.drawable.baseline_edit_24)
        private val deleteBackground = ColorDrawable(Color.RED)
        private val updateBackground = ColorDrawable(ContextCompat.getColor(context, R.color.colorPrimary))
        private val iconMargin = context.resources.getDimensionPixelSize(R.dimen.icon_margin)

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val action = if (direction == ItemTouchHelper.LEFT) SwipeAction.DELETE else SwipeAction.UPDATE
            onSwiped(viewHolder.adapterPosition, action)
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

            when {
                dX > 0 -> { // Swiping right - UPDATE
                    // Draw blue background for update
                    updateBackground.bounds = android.graphics.Rect(
                        itemView.left,
                        itemView.top,
                        itemView.left + dX.toInt(),
                        itemView.bottom
                    )
                    updateBackground.draw(c)

                    // Draw edit icon
                    editIcon?.let { icon ->
                        val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + icon.intrinsicWidth

                        icon.setBounds(iconLeft, iconTop, iconRight, iconTop + icon.intrinsicHeight)
                        icon.draw(c)
                    }
                }
                dX < 0 -> { // Swiping left - DELETE
                    // Draw red background for delete
                    deleteBackground.bounds = android.graphics.Rect(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    deleteBackground.draw(c)

                    // Draw delete icon
                    deleteIcon?.let { icon ->
                        val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                        val iconRight = itemView.right - iconMargin
                        val iconLeft = iconRight - icon.intrinsicWidth

                        icon.setBounds(iconLeft, iconTop, iconRight, iconTop + icon.intrinsicHeight)
                        icon.draw(c)
                    }
                }
                else -> return
            }

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }
}