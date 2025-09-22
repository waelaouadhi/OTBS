package com.example.onetechbs

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.GestureDetector
import android.widget.TextView
import android.widget.Toast
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.onetechbs.databinding.FragmentHomefraBinding
import com.example.onetechbs.db.LeaveBalanceResponse
import com.example.onetechbs.db.CalendarEvent
import com.example.onetechbs.db.EventType
import com.example.onetechbs.db.EventStatus
import com.example.onetechbs.CalendarDayViewHolder
import com.example.onetechbs.CalendarViewModel
import com.example.onetechbs.model.Holiday
import com.example.onetechbs.network.HolidayApiService
import com.example.onetechbs.network.RetrofitClient

import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import android.graphics.Color
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.content.ContextCompat
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import android.animation.AnimatorListenerAdapter
import android.util.Base64
import android.util.TypedValue
import org.json.JSONObject

class Homefra : Fragment() {
    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<androidx.appcompat.widget.Toolbar?>(R.id.toolbar)?.visibility = View.VISIBLE
    }
    
    override fun onPause() {
        super.onPause()
        requireActivity().findViewById<androidx.appcompat.widget.Toolbar?>(R.id.toolbar)?.visibility = View.GONE
    }

    private var _binding: FragmentHomefraBinding? = null
    private val binding get() = _binding!!

    // TextViews that actually exist in the layout
    private var firstNameTextView: TextView? = null
    private var lastNameTextView: TextView? = null
    private var roleTextView: TextView? = null
    private var availableLeaveTextView: TextView? = null
    private var leaveUsedTextView: TextView? = null

    private val calendarViewModel: CalendarViewModel by viewModels()
    private var holidays = setOf<LocalDate>() // This will be populated from the API.
    private lateinit var calendarEventService: CalendarEventService
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var calendarView: CalendarView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomefraBinding.inflate(inflater, container, false)

        // Initialize the TextViews that exist in the layout
        firstNameTextView = binding.firstNameTextView
        lastNameTextView = binding.lastNameTextView
        roleTextView = binding.roleTextView
        availableLeaveTextView = binding.root.findViewById(R.id.Availebal_leave)
        leaveUsedTextView = binding.root.findViewById(R.id.leave_used)

        // Retrieve employee data from SharedPreferences
        val prefs = requireActivity().getSharedPreferences("auth", AppCompatActivity.MODE_PRIVATE)
        val firstName = prefs.getString("firstName", "Unknown")
        val lastName = prefs.getString("lastName", "Unknown")
        val role = prefs.getString("role", "Unknown")
        val username = prefs.getString("username", null)

        // Display employee data in existing TextViews
        firstNameTextView?.text = "$firstName"
        lastNameTextView?.text = "$lastName"
        roleTextView?.text = "$role"

        // Load profile image initially if username exists
        username?.let { fetchAndSetProfileImage(it) }

        // Listen for profile photo updates from ProfileFragment
        parentFragmentManager.setFragmentResultListener(
            "profile_photo_updated",
            viewLifecycleOwner
        ) { _, _ ->
            username?.let { fetchAndSetProfileImage(it) }
        }

        // Fetch leave balance from backend and update the UI
        fetchLeaveBalance()

        // Initialize calendar event service
        calendarEventService = CalendarEventService(requireContext())
        
        // Load dynamic events from appointments and leaves
        loadCalendarEvents()

        // Fetch public holidays (temporarily using mock data due to API rate limits)
        // fetchHolidays()
        loadMockHolidays()

        // --- CalendarView integration ---
        val calendarFrame = binding.root.findViewById<FrameLayout>(R.id.CalendarFrame)
        calendarView = CalendarView(requireContext())
        // Calculate the height of 6 rows of dates to set a fixed height for the calendar.
        // This is a major performance optimization to prevent the calendar from measuring all months at once.
        val dayHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, resources.displayMetrics).toInt()
        val calendarHeight = dayHeight * 6

        calendarView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            calendarHeight
        )
        calendarFrame.addView(calendarView)

        val currentMonth = YearMonth.now()
        calendarView.setup(currentMonth.minusMonths(12), currentMonth.plusMonths(12), DayOfWeek.MONDAY)
        calendarView.scrollToMonth(currentMonth)
        
        // Enhanced swipe gesture configuration
        setupEnhancedSwipeNavigation()
        
        // Set the custom day layout
        calendarView.dayViewResource = R.layout.calendar_day_layout
        calendarView.monthHeaderResource = R.layout.calendar_month_header

        // Month header binder with navigation
        calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<CalendarMonthHeaderViewHolder> {
            override fun create(view: View): CalendarMonthHeaderViewHolder {
                return CalendarMonthHeaderViewHolder(view)
            }
            override fun bind(container: CalendarMonthHeaderViewHolder, data: CalendarMonth) {
                val monthName = data.yearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
                val year = data.yearMonth.year
                container.monthYearText.text = "$monthName $year"
                
                // Previous month navigation
                container.previousMonthButton.setOnClickListener {
                    val previousMonth = data.yearMonth.minusMonths(1)
                    animateMonthTransition(previousMonth, isForward = false)
                }
                
                // Next month navigation
                container.nextMonthButton.setOnClickListener {
                    val nextMonth = data.yearMonth.plusMonths(1)
                    animateMonthTransition(nextMonth, isForward = true)
                }
                
                // Today button navigation
                container.todayButton.setOnClickListener {
                    val currentMonth = YearMonth.now()
                    animateMonthTransition(currentMonth, isForward = true)
                }
                
                // Month/Year text click for date picker (optional enhancement)
                container.monthYearText.setOnClickListener {
                    // Could add a month/year picker dialog here in the future
                    Toast.makeText(requireContext(), "Navigate to: $monthName $year", Toast.LENGTH_SHORT).show()
                }
            }
        }

        calendarView.dayBinder = object : MonthDayBinder<CalendarDayViewHolder> {
            override fun create(view: View): CalendarDayViewHolder {
                return CalendarDayViewHolder(view)
            }
            override fun bind(container: CalendarDayViewHolder, data: CalendarDay) {
                container.dayNumber.text = data.date.dayOfMonth.toString()
                container.eventDotsContainer.removeAllViews()
                
                // Reset all indicators
                container.eventIndicator.visibility = View.GONE
                container.todayIndicator.visibility = View.GONE

                if (data.position != DayPosition.MonthDate) {
                    // Days from previous/next month
                    container.dayCellRoot.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.md_theme_background))
                    container.dayNumber.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onBackground))
                    container.dayNumber.alpha = 0.3f
                    container.dayCellRoot.isClickable = false
                    container.dayCellRoot.cardElevation = 0f
                    return
                }
                
                // Reset for current month days
                container.dayNumber.alpha = 1.0f
                container.dayCellRoot.cardElevation = 2f
                container.dayCellRoot.isClickable = true

                val events = calendarViewModel.events.value.filter { it.date == data.date }
                val isToday = data.date == LocalDate.now()
                val isWeekend = data.date.dayOfWeek == DayOfWeek.SATURDAY || data.date.dayOfWeek == DayOfWeek.SUNDAY
                val isHoliday = holidays.contains(data.date)
                
                // Show today indicator
                if (isToday) {
                    container.todayIndicator.visibility = View.VISIBLE
                }

                // Enhanced styling with brand colors
                when {
                    isToday -> {
                        container.dayCellRoot.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
                        container.dayNumber.setTextColor(Color.WHITE)
                        container.dayCellRoot.cardElevation = 6f
                    }
                    isHoliday || isWeekend -> {
                        container.dayCellRoot.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.md_theme_background))
                        container.dayNumber.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onBackground))
                        container.dayNumber.alpha = 0.6f
                    }
                    events.any { it.type == EventType.VACATION && it.status == EventStatus.APPROVED } -> {
                        container.dayCellRoot.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.green))
                        container.dayNumber.setTextColor(Color.WHITE)
                        container.dayCellRoot.cardElevation = 4f
                    }
                    events.any { it.type == EventType.SICK } -> {
                        container.dayCellRoot.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
                        container.dayNumber.setTextColor(Color.WHITE)
                        container.dayCellRoot.cardElevation = 4f
                    }
                    events.any { it.type == EventType.DOCTOR } -> {
                        container.dayCellRoot.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark))
                        container.dayNumber.setTextColor(Color.WHITE)
                        container.dayCellRoot.cardElevation = 4f
                    }
                    events.any { it.type == EventType.TRAINING } -> {
                        container.dayCellRoot.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorAccent))
                        container.dayNumber.setTextColor(Color.WHITE)
                        container.dayCellRoot.cardElevation = 4f
                    }
                    else -> {
                        container.dayCellRoot.setCardBackgroundColor(Color.WHITE)
                        container.dayNumber.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark))
                    }
                }

                // Add colored dots for each event
                events.forEach { event ->
                    val dot = View(requireContext())
                    dot.layoutParams = LinearLayout.LayoutParams(12, 12).apply {
                        setMargins(2, 0, 2, 0)
                    }
                    dot.background = when (event.type) {
                        EventType.VACATION -> ContextCompat.getDrawable(requireContext(), R.drawable.dot_green)
                        EventType.SICK -> ContextCompat.getDrawable(requireContext(), R.drawable.dot_red)
                        EventType.DOCTOR -> ContextCompat.getDrawable(requireContext(), R.drawable.dot_blue)
                        EventType.TRAINING -> ContextCompat.getDrawable(requireContext(), R.drawable.dot_yellow)
                        EventType.HOLIDAY -> ContextCompat.getDrawable(requireContext(), R.drawable.dot_red) // Red dot for holidays
                    }
                    container.eventDotsContainer.addView(dot)
                }

                // Click listener for day cell
                container.dayCellRoot.setOnClickListener {
                    if (events.isNotEmpty()) {
                        // Show existing events
                        val eventDescriptions = events.joinToString("\n") { 
                            if (it.title.isNotEmpty()) it.title else it.type.name 
                        }
                        Toast.makeText(requireContext(), eventDescriptions, Toast.LENGTH_SHORT).show()
                    } else {
                        // No events - offer to add leave
                        showAddLeaveDialog(data.date)
                    }
                }
            }
        }

        // Observe events and refresh calendar
        viewLifecycleOwner.lifecycleScope.launch {
            calendarViewModel.events.collect {
                calendarView.notifyCalendarChanged()
            }
        }

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchLeaveBalance() {
        // Log the token being sent for debugging
        println("Sending token: ${RetrofitClient.getAuthToken()}") // Access the token using getter

        // Fetch the leave balance using the leave-service API
        RetrofitClient.leaveService.getLeaveBalance().enqueue(object : Callback<LeaveBalanceResponse> {
            override fun onResponse(call: Call<LeaveBalanceResponse>, response: Response<LeaveBalanceResponse>) {
                if (response.isSuccessful) {
                    val leaveBalance = response.body()
                    leaveBalance?.let {
                        // Set the available leave and used leave text views
                        availableLeaveTextView?.text = it.remainingLeave.toString()
                        leaveUsedTextView?.text = (it.totalLeave - it.remainingLeave).toString() // Leave used = total - remaining
                    }
                } else {
                    // Log the response code and message for debugging
                    Toast.makeText(requireContext(), "Failed to fetch leave balance. Code: ${response.code()} Message: ${response.message()}", Toast.LENGTH_SHORT).show()
                    println("Response Code: ${response.code()}")
                    println("Response Message: ${response.message()}")
                    // Optional: Log the response body to see the details
                    response.errorBody()?.let {
                        println("Error Body: ${it.string()}")
                    }
                }
            }

            override fun onFailure(call: Call<LeaveBalanceResponse>, t: Throwable) {
                // Log detailed error information
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                t.printStackTrace()  // Print the stack trace for more details
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadCalendarEvents() {
        lifecycleScope.launch {
            try {
                val events = calendarEventService.fetchAllCalendarEvents()
                calendarViewModel.setEvents(events)
            } catch (e: Exception) {
                Log.e("Homefra", "Error loading calendar events", e)
                // Show error message or fallback behavior
                Toast.makeText(requireContext(), "Failed to load calendar events", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAddLeaveDialog(selectedDate: LocalDate) {
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
        val formattedDate = selectedDate.format(dateFormatter)
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Add Leave Event")
            .setMessage("No events found for $formattedDate.\n\nWould you like to add a leave request for this date?")
            .setPositiveButton("Add Leave") { _, _ ->
                navigateToAddLeave(selectedDate)
            }
            .setNegativeButton("Cancel", null)
            .setIcon(R.drawable.ic_calendar_add)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun navigateToAddLeave(selectedDate: LocalDate) {
        try {
            // Navigate to AddLeaveFragment with the selected date using FragmentManager
            val addLeaveFragment = AddLeaveFragment().apply {
                arguments = Bundle().apply {
                    putString("selected_date", selectedDate.toString())
                }
            }
            
            // Use the parent activity's fragment manager to replace fragment
            (activity as? home)?.let { homeActivity ->
                homeActivity.supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_layout, addLeaveFragment)
                    .addToBackStack(null)
                    .commit()
            } ?: run {
                Toast.makeText(requireContext(), "Unable to navigate to add leave", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("Homefra", "Error navigating to AddLeave", e)
            Toast.makeText(requireContext(), "Unable to open leave request form", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupEnhancedSwipeNavigation() {
        // Create custom gesture detector for enhanced swipe responsiveness
        gestureDetector = GestureDetectorCompat(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100
            
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                
                // Check if horizontal swipe is more dominant than vertical
                if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY)) {
                    if (kotlin.math.abs(diffX) > SWIPE_THRESHOLD && kotlin.math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Swipe right - go to previous month
                            navigateToPreviousMonth()
                        } else {
                            // Swipe left - go to next month
                            navigateToNextMonth()
                        }
                        return true
                    }
                }
                return false
            }
            
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                // Allow native scrolling behavior but with enhanced responsiveness
                return false
            }
        })
        
        // Apply touch listener to calendar view for gesture detection
        calendarView.setOnTouchListener { view, event ->
            gestureDetector.onTouchEvent(event)
            // Return false to allow native calendar scrolling to continue
            false
        }
        
        // Configure calendar for smoother scrolling
        calendarView.apply {
            // Enable smooth scrolling
            isNestedScrollingEnabled = true
            
            // Enhanced scroll configuration for better performance
            scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
            isScrollbarFadingEnabled = true
            
            // Add month scroll listener for better feedback
            monthScrollListener = { calendarMonth ->
                // Add subtle visual feedback during scroll
                animateMonthHeaderTransition()
                
                // Enhanced haptic feedback
                try {
                    view?.performHapticFeedback(
                        android.view.HapticFeedbackConstants.CLOCK_TICK,
                        android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
                } catch (e: Exception) {
                    // Fallback haptic feedback
                    view?.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                }
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun navigateToPreviousMonth() {
        try {
            val currentMonth = calendarView.findFirstVisibleMonth()?.yearMonth ?: YearMonth.now()
            val previousMonth = currentMonth.minusMonths(1)
            animateMonthTransition(previousMonth, isForward = false)
        } catch (e: Exception) {
            Log.e("Homefra", "Error navigating to previous month", e)
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun navigateToNextMonth() {
        try {
            val currentMonth = calendarView.findFirstVisibleMonth()?.yearMonth ?: YearMonth.now()
            val nextMonth = currentMonth.plusMonths(1)
            animateMonthTransition(nextMonth, isForward = true)
        } catch (e: Exception) {
            Log.e("Homefra", "Error navigating to next month", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun animateMonthTransition(targetMonth: YearMonth, isForward: Boolean) {
        // Create smooth transition with custom timing and easing
        val calendarFrame = binding.root.findViewById<FrameLayout>(R.id.CalendarFrame)
        
        // Pre-transition animation - subtle scale and fade
        val scaleAnimator = ObjectAnimator.ofFloat(calendarFrame, "scaleX", 1.0f, 0.98f).apply {
            duration = 150
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val fadeAnimator = ObjectAnimator.ofFloat(calendarFrame, "alpha", 1.0f, 0.85f).apply {
            duration = 150
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Slide animation based on direction
        val slideDistance = 50f * (if (isForward) -1 else 1)
        val slideAnimator = ObjectAnimator.ofFloat(calendarFrame, "translationX", 0f, slideDistance).apply {
            duration = 150
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Pre-transition animation set
        val preTransitionSet = AnimatorSet().apply {
            playTogether(scaleAnimator, fadeAnimator, slideAnimator)
        }
        
        // Post-transition restore animation
        val restoreScaleAnimator = ObjectAnimator.ofFloat(calendarFrame, "scaleX", 0.98f, 1.0f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
        }
        
        val restoreFadeAnimator = ObjectAnimator.ofFloat(calendarFrame, "alpha", 0.85f, 1.0f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
        }
        
        val restoreSlideAnimator = ObjectAnimator.ofFloat(calendarFrame, "translationX", slideDistance, 0f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
        }
        
        val postTransitionSet = AnimatorSet().apply {
            playTogether(restoreScaleAnimator, restoreFadeAnimator, restoreSlideAnimator)
        }
        
        // Execute animation sequence
        preTransitionSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Perform the actual month transition
                calendarView.smoothScrollToMonth(targetMonth)
                
                // Add slight delay for smooth transition feel
                calendarFrame.postDelayed({
                    postTransitionSet.start()
                    
                    // Enhanced haptic feedback
                    try {
                        calendarFrame.performHapticFeedback(
                            android.view.HapticFeedbackConstants.VIRTUAL_KEY,
                            android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                        )
                    } catch (e: Exception) {
                        // Fallback haptic feedback
                        calendarFrame.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                }, 100)
            }
        })
        
        preTransitionSet.start()
    }

    private fun animateMonthHeaderTransition() {
        // Add subtle animation to month header during transitions
        val calendarFrame = binding.root.findViewById<FrameLayout>(R.id.CalendarFrame)
        
        // Create a gentle pulse effect
        val pulseAnimator = ObjectAnimator.ofFloat(calendarFrame, "scaleY", 1.0f, 1.02f, 1.0f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
        }
        
        pulseAnimator.start()
    }

    private fun fetchAndSetProfileImage(username: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.getUserService(requireContext())
                val resp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    service.getProfilePicture(username)
                }
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body == null) {
                        Log.d("Homefra", "No profile image (null body)")
                    } else {
                        val ct = body.contentType()?.toString()?.lowercase()
                        try {
                            if (ct != null && ct.contains("application/json")) {
                                val text = body.string()
                                val json = JSONObject(text)
                                val b64 = json.optString("picture", "")
                                if (b64.isNotBlank()) {
                                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                                    if (bytes.isNotEmpty()) {
                                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        if (bmp != null) {
                                            binding.profileImg.setImageBitmap(bmp)
                                            Log.d("Homefra", "Loaded profile image from JSON (${bytes.size} bytes)")
                                        } else {
                                            Log.w("Homefra", "Failed to decode profile image from JSON bytes (${bytes.size}).")
                                        }
                                    } else {
                                        Log.d("Homefra", "JSON picture field empty after Base64 decode")
                                    }
                                } else {
                                    Log.d("Homefra", "JSON response missing 'picture' field or blank")
                                }
                            } else {
                                val bytes = body.bytes()
                                if (bytes.isNotEmpty()) {
                                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    if (bmp != null) {
                                        binding.profileImg.setImageBitmap(bmp)
                                        Log.d("Homefra", "Loaded profile image (${bytes.size} bytes)")
                                    } else {
                                        Log.w("Homefra", "Failed to decode profile image bytes (${bytes.size}).")
                                    }
                                } else {
                                    Log.d("Homefra", "No profile image (empty body)")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("Homefra", "Error parsing/decoding profile image response", e)
                        }
                    }
                } else {
                    Log.e("Homefra", "getProfilePicture failed: code=${resp.code()} message=${resp.message()}")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Fragment/view likely destroyed; no-op
                Log.d("Homefra", "Image load cancelled")
            } catch (e: Exception) {
                Log.e("Homefra", "Failed to load profile image", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadMockHolidays() {
        Log.d("Homefra", "Loading mock holidays for testing.")
        val mockHolidays = listOf(
            CalendarEvent(date = LocalDate.of(2025, 1, 1), type = EventType.HOLIDAY, status = EventStatus.APPROVED, title = "New Year's Day"),
            CalendarEvent(date = LocalDate.of(2025, 3, 20), type = EventType.HOLIDAY, status = EventStatus.APPROVED, title = "Independence Day"),
            CalendarEvent(date = LocalDate.of(2025, 4, 9), type = EventType.HOLIDAY, status = EventStatus.APPROVED, title = "Martyrs' Day"),
            CalendarEvent(date = LocalDate.of(2025, 5, 1), type = EventType.HOLIDAY, status = EventStatus.APPROVED, title = "Labour Day"),
            CalendarEvent(date = LocalDate.of(2025, 7, 25), type = EventType.HOLIDAY, status = EventStatus.APPROVED, title = "Republic Day")
        )

        val currentEvents = calendarViewModel.events.value.toMutableList()
        currentEvents.addAll(mockHolidays)
        calendarViewModel.setEvents(currentEvents)

        holidays = mockHolidays.map { it.date }.toSet()
        Toast.makeText(requireContext(), "Showing mock holidays", Toast.LENGTH_SHORT).show()
    }


    // NOTE: Temporarily disabled due to API rate limiting.
    // Re-enable this function and the call in onCreateView when the API is accessible.
    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchHolidays() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val holidayService = RetrofitClient.getHolidayService()
                val response = holidayService.getHolidays(
                    apiKey = "5zdb1nzjLyJIZ9w3ezSHZw==AOZIvaXvNT5ZBHG0",
                    country = "TN",
                    year = 2025 ,
                    type = "public_holiday"
                )

                if (response.isSuccessful) {
                    val holidayList = response.body() ?: emptyList()
                    Log.d("Homefra", "Fetched ${holidayList.size} holidays.")

                    val holidayEvents = holidayList.mapNotNull { holiday ->
                        try {
                            val date = LocalDate.parse(holiday.date, DateTimeFormatter.ISO_LOCAL_DATE)
                            Log.d("Homefra", "Holiday: ${holiday.name} on $date")
                            CalendarEvent(
                                title = holiday.name,
                                date = date,
                                type = EventType.HOLIDAY,
                                status = EventStatus.APPROVED // Holidays are always approved
                            )
                        } catch (e: Exception) {
                            Log.e("Homefra", "Error parsing holiday date: ${holiday.date}", e)
                            null
                        }
                    }

                    // Add holidays to the view model
                    val currentEvents = calendarViewModel.events.value.toMutableList()
                    currentEvents.addAll(holidayEvents)
                    calendarViewModel.setEvents(currentEvents)

                    // Update the local holidays set for styling if needed
                    holidays = holidayEvents.map { it.date }.toSet()

                } else {
                    Log.e("Homefra", "Failed to fetch holidays: ${response.code()} - ${response.message()}")
                    Toast.makeText(requireContext(), "Failed to load holidays", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Homefra", "Error fetching holidays", e)
                Toast.makeText(requireContext(), "An error occurred while fetching holidays", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // Clean up TextView references
        firstNameTextView = null
        lastNameTextView = null
        roleTextView = null
        availableLeaveTextView = null
        leaveUsedTextView = null
    }
}