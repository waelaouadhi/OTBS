package com.example.onetechbs

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.kizitonwose.calendar.view.ViewContainer

class CalendarMonthHeaderViewHolder(view: View) : ViewContainer(view) {
    val monthYearText: TextView = view.findViewById(R.id.monthYearText)
    val previousMonthButton: ImageButton = view.findViewById(R.id.previousMonthButton)
    val nextMonthButton: ImageButton = view.findViewById(R.id.nextMonthButton)
    val todayButton: MaterialButton = view.findViewById(R.id.todayButton)
}
