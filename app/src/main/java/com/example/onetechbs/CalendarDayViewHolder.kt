package com.example.onetechbs

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.kizitonwose.calendar.view.ViewContainer

class CalendarDayViewHolder(view: View) : ViewContainer(view) {
    val dayNumber: TextView = view.findViewById(R.id.dayNumber)
    val eventDotsContainer: LinearLayout = view.findViewById(R.id.eventDotsContainer)
    val dayCellRoot: CardView = view.findViewById(R.id.dayCellRoot)
    val eventIndicator: View = view.findViewById(R.id.eventIndicator)
    val todayIndicator: View = view.findViewById(R.id.todayIndicator)
}
