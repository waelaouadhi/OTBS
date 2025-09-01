package com.example.onetechbs

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.CountDownTimer
import com.example.onetechbs.util.SharedPreferencesManager
import android.view.ViewGroup
import android.widget.Toast
import android.widget.ArrayAdapter as AndroidArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModel
import androidx.appcompat.widget.SearchView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.onetechbs.db.CandidateResponseDTO
import com.example.onetechbs.db.JobOfferRequest
import com.example.onetechbs.db.JobOfferResponseDTO
import com.example.onetechbs.network.RetrofitClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.ChipGroup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AvailableJobsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var emptyView: View
    private lateinit var jobAdapter: JobAdapter
    private var isHR = false
    private var rootView: View? = null
    private val appliedJobIds: MutableSet<Long> = mutableSetOf()

    // Filtering state
    private var allJobs: List<JobOfferResponseDTO> = emptyList()
    private var searchJob: Job? = null

    private data class FilterCriteria(
        val query: String = "",
        val department: String? = null,
        val experience: String? = null, // Junior, Mid, Senior
        val status: String? = null      // Open, CLOSED/Finished
    )
    private var currentFilter = FilterCriteria()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_available_jobs, container, false)
            initializeViews()
            setupUserRole()
            setupRecyclerView()
            setupFiltersUI()
            setupShakeToUndo() // Initialize shake detection
            setupSwipeToDelete()
            setupSwipeRefresh()
            loadJobs()
        }
        return rootView
    }

    private fun initializeViews() {
        rootView?.let { view ->
            recyclerView = view.findViewById(R.id.jobsRecyclerView)
            swipeRefreshLayout = view.findViewById(R.id.swipeRefresh)
            emptyView = view.findViewById(R.id.emptyView)
            val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
            toolbar.setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun setupUserRole() {
        val prefs = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val role = prefs.getString("role", "Employee") ?: "Employee"
        // Treat HR and HRD as managers; Employee and Manager are applicants
        isHR = role.equals("HR", ignoreCase = true) || role.equals("HRD", ignoreCase = true)
        // Show status chips only to HR/HRD
        rootView?.findViewById<ChipGroup>(R.id.chipGroupStatus)?.visibility = if (isHR) View.VISIBLE else View.GONE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupRecyclerView() {
        jobAdapter = JobAdapter(
            isHR = isHR,
            onDeleteClick = { job -> handleDeleteJob(job) },
            onUpdateClick = { job -> handleUpdateJob(job) },
            onFinishClick = { job -> handleFinishJob(job) },
            onApplicantsClick = { job -> handleViewApplicants(job) },
            onCancelClick = { job -> confirmAndCancelApplication(job) }
        ).also { it.updateAppliedJobs(appliedJobIds) }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = jobAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupFiltersUI() {
        val searchView = rootView?.findViewById<SearchView>(R.id.searchViewJobs)
        val deptInput = rootView?.findViewById<AutoCompleteTextView>(R.id.autoDepartment)
        val chipExp = rootView?.findViewById<ChipGroup>(R.id.chipGroupExperience)
        val chipStatus = rootView?.findViewById<ChipGroup>(R.id.chipGroupStatus)
        val btnReset = rootView?.findViewById<View>(R.id.btnClearFilters)
        val btnResetEmpty = rootView?.findViewById<View>(R.id.btnClearFiltersEmpty)
        val btnToggleFilters = rootView?.findViewById<android.widget.ImageButton>(R.id.btnToggleFilters)
        val filtersContent = rootView?.findViewById<View>(R.id.filtersContent)

        // Initial visibility of header reset button based on current filter state
        updateClearButtonVisibility(btnReset)

        // Search with debounce
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(250)
                    currentFilter = currentFilter.copy(query = newText.orEmpty())
                    applyFilters()
                    updateClearButtonVisibility(btnReset)
                }
                return true
            }
        })

        // Experience chips single selection
        chipExp?.setOnCheckedStateChangeListener { _, _ ->
            val selected = when (chipExp.checkedChipId) {
                R.id.chipExpJunior -> "Junior"
                R.id.chipExpMid -> "Mid"
                R.id.chipExpSenior -> "Senior"
                else -> null
            }
            currentFilter = currentFilter.copy(experience = selected)
            applyFilters()
            updateClearButtonVisibility(btnReset)
        }

        // Status chips (if visible for HR/HRD) single selection
        chipStatus?.setOnCheckedStateChangeListener { _, _ ->
            val selected = when (chipStatus.checkedChipId) {
                R.id.chipStatusOpen -> "OPEN"
                R.id.chipStatusClosed -> "CLOSED"
                else -> null
            }
            currentFilter = currentFilter.copy(status = selected)
            applyFilters()
            updateClearButtonVisibility(btnReset)
        }

        // Department dropdown
        deptInput?.setOnItemClickListener { parent, _, position, _ ->
            val value = parent.getItemAtPosition(position)?.toString().orEmpty()
            // Treat "All" or empty as no department filter
            val department = value.ifBlank { null }?.takeUnless { it.equals("All", ignoreCase = true) }
            currentFilter = currentFilter.copy(department = department)
            applyFilters()
            updateClearButtonVisibility(btnReset)
        }

        // Reset buttons
        btnReset?.setOnClickListener {
            // Clear UI controls to default
            searchView?.setQuery("", false)
            deptInput?.setText("", false)
            chipExp?.check(R.id.chipExpAll)
            chipStatus?.check(R.id.chipStatusAll)

            // Reset filter model
            currentFilter = currentFilter.copy(
                query = "",
                department = null,
                experience = null,
                status = null
            )
            applyFilters()
            updateClearButtonVisibility(btnReset)
        }

        btnResetEmpty?.setOnClickListener {
            btnReset?.performClick()
        }

        // Collapse/expand filters content and update icon
        btnToggleFilters?.setImageResource(android.R.drawable.arrow_down_float)
        btnToggleFilters?.setOnClickListener { btn ->
            filtersContent?.let { content ->
                val toShow = content.visibility != View.VISIBLE
                content.visibility = if (toShow) View.VISIBLE else View.GONE
                if (toShow) {
                    btnToggleFilters.setImageResource(android.R.drawable.arrow_up_float)
                } else {
                    btnToggleFilters.setImageResource(android.R.drawable.arrow_down_float)
                }
            }
        }
    }

    private fun updateClearButtonVisibility(btnReset: View?) {
        btnReset?.visibility = if (isDefaultFilter()) View.GONE else View.VISIBLE
    }

    private fun isDefaultFilter(): Boolean {
        // Consider default when nothing is set: empty query, no department, experience/status not chosen or at "All"
        val isQueryDefault = currentFilter.query.isNullOrEmpty()
        val isDeptDefault = currentFilter.department.isNullOrEmpty()
        val isExpDefault = currentFilter.experience.isNullOrEmpty()
        val isStatusDefault = currentFilter.status.isNullOrEmpty()
        return isQueryDefault && isDeptDefault && isExpDefault && isStatusDefault
    }

    // --- Shake to Undo State ---
    private var recentlyDeletedJob: JobOfferResponseDTO? = null
    private var recentlyDeletedPosition: Int = -1
    private var undoTimer: CountDownTimer? = null
    private var shakeListenerRegistered = false
    private lateinit var sensorManager: android.hardware.SensorManager
    private lateinit var accelerometer: android.hardware.Sensor
    private var lastShakeTime = 0L

    private val shakeListener = object : android.hardware.SensorEventListener {
        private var lastX = 0f
        private var lastY = 0f
        private var lastZ = 0f
        private val shakeThreshold = 12f
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onSensorChanged(event: android.hardware.SensorEvent?) {
            event?.let {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]
                val now = System.currentTimeMillis()
                if (now - lastShakeTime > 500) {
                    val delta = Math.abs(x + y + z - lastX - lastY - lastZ)
                    if (delta > shakeThreshold) {
                        lastShakeTime = now
                        val vibrator = ContextCompat.getSystemService(requireContext(), android.os.Vibrator::class.java)
                        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Undo Delete?")
                            .setMessage("Shake detected. Restore the deleted job?")
                            .setPositiveButton("Restore") { _, _ -> restoreDeletedJob() }
                            .setNegativeButton("No", null)
                            .setOnDismissListener { unregisterShakeListener() }
                            .show()
                    }
                }
                lastX = x
                lastY = y
                lastZ = z
            }
        }
        override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
    }

    private fun registerShakeListener() {
        if (!shakeListenerRegistered) {
            sensorManager.registerListener(shakeListener, accelerometer, android.hardware.SensorManager.SENSOR_DELAY_UI)
            shakeListenerRegistered = true
        }
    }
    private fun unregisterShakeListener() {
        if (shakeListenerRegistered) {
            sensorManager.unregisterListener(shakeListener)
            shakeListenerRegistered = false
        }
    }
    private fun temporarilyRemoveJob(job: JobOfferResponseDTO, position: Int) {
        recentlyDeletedJob = job
        recentlyDeletedPosition = position
        val mutableList = jobAdapter.currentList.toMutableList()
        mutableList.removeAt(position)
        jobAdapter.submitList(mutableList)
        registerShakeListener()
        undoTimer?.cancel()
        undoTimer = object : android.os.CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onFinish() {
                // If not restored, actually delete from backend
                recentlyDeletedJob?.let { jobToDelete ->
                    handleDeleteJob(jobToDelete)
                }
                unregisterShakeListener()
                recentlyDeletedJob = null
                recentlyDeletedPosition = -1
            }
        }.start()
    }
    private fun restoreDeletedJob() {
        recentlyDeletedJob?.let { job ->
            val mutableList = jobAdapter.currentList.toMutableList()
            val pos = if (recentlyDeletedPosition in 0..mutableList.size) recentlyDeletedPosition else 0
            mutableList.add(pos, job)
            jobAdapter.submitList(mutableList)
            recyclerView.post {
                recyclerView.scrollToPosition(pos)
                val holder = recyclerView.findViewHolderForAdapterPosition(pos)
                holder?.itemView?.apply {
                    alpha = 0f
                    animate().alpha(1f).setDuration(300).start()
                }
            }
            com.google.android.material.snackbar.Snackbar.make(recyclerView, "Job restored", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
            recentlyDeletedJob = null
            recentlyDeletedPosition = -1
        }
        unregisterShakeListener()
        undoTimer?.cancel()
    }

    private fun setupShakeToUndo() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        accelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)!!
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                return if (isHR) {
                    super.getMovementFlags(recyclerView, viewHolder)
                } else {
                    0 // Disable swipe for non-HR users
                }
            }
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val job = jobAdapter.currentList[position]
                val vibrator = ContextCompat.getSystemService(requireContext(), android.os.Vibrator::class.java)
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))

                val itemView = viewHolder.itemView

                if (direction == ItemTouchHelper.LEFT) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Delete Job?")
                        .setMessage("Are you sure you want to delete this job?")
                        .setPositiveButton("Delete") { _, _ ->
                            itemView.animate()
                                .alpha(0f)
                                .setDuration(200)
                                .withEndAction {
                                    temporarilyRemoveJob(job, position)
                                    itemView.alpha = 1f
                                }
                                .start()
                        }
                        .setNegativeButton("Cancel") { _, _ ->
                            jobAdapter.notifyItemChanged(position)
                        }
                        .setOnCancelListener {
                            jobAdapter.notifyItemChanged(position)
                        }
                        .show()
                } else if (direction == ItemTouchHelper.RIGHT) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Finish Job?")
                        .setMessage("Are you sure you want to finish this job?")
                        .setPositiveButton("Finish") { _, _ ->
                            itemView.animate()
                                .alpha(0f)
                                .setDuration(200)
                                .withEndAction {
                                    handleFinishJob(job)
                                    itemView.alpha = 1f
                                }
                                .start()
                        }
                        .setNegativeButton("Cancel") { _, _ ->
                            jobAdapter.notifyItemChanged(position)
                        }
                        .setOnCancelListener {
                            jobAdapter.notifyItemChanged(position)
                        }
                        .show()
                }
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
                val iconMargin: Int
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    if (dX < 0) { // Swipe left for delete
                        val background = ColorDrawable(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark))
                        background.setBounds(
                            itemView.right + dX.toInt(),
                            itemView.top,
                            itemView.right,
                            itemView.bottom
                        )
                        background.draw(c)
                        val icon = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_delete_24)
                        icon?.let {
                            iconMargin = (itemView.height - it.intrinsicHeight) / 2
                            val iconTop = itemView.top + iconMargin
                            val iconBottom = iconTop + it.intrinsicHeight
                            val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                            val iconRight = itemView.right - iconMargin
                            it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            it.draw(c)
                        }
                    } else if (dX > 0) { // Swipe right for finish
                        val background = ColorDrawable(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                        background.setBounds(
                            itemView.left,
                            itemView.top,
                            itemView.left + dX.toInt(),
                            itemView.bottom
                        )
                        background.draw(c)
                        val icon = ContextCompat.getDrawable(requireContext(), R.drawable.baseline_check_circle_24)
                        icon?.let {
                            iconMargin = (itemView.height - it.intrinsicHeight) / 2
                            val iconTop = itemView.top + iconMargin
                            val iconBottom = iconTop + it.intrinsicHeight
                            val iconLeft = itemView.left + iconMargin
                            val iconRight = iconLeft + it.intrinsicWidth
                            it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            it.draw(c)
                        }
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.apply {
            setColorSchemeResources(R.color.purple_500)
            setOnRefreshListener { loadJobs() }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadJobs() {
        showLoading(true)
        val prefs = SharedPreferencesManager.getInstance(requireContext())
        val token = prefs.getAuthToken()
        if (token.isNullOrEmpty() || prefs.isTokenExpired()) {
            showLoading(false)
            showSessionExpiredDialog()
            return
        }
        val apiService = RetrofitClient.getJobClient(token).create(com.example.onetechbs.network.ApiService::class.java)
        apiService.getAllJobOffers()
            .enqueue(object : Callback<List<JobOfferResponseDTO>> {
                override fun onResponse(
                    call: Call<List<JobOfferResponseDTO>>,
                    response: Response<List<JobOfferResponseDTO>>
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val jobs = response.body() ?: emptyList()
                        allJobs = jobs
                        // Populate department dropdown dynamically
                        populateDepartmentsDropdown(jobs)
                        applyFilters()
                    } else {
                        if (response.code() == 401) {
                            showSessionExpiredDialog()
                            return
                        }
                        handleApiError(response.code())
                    }
                }

                override fun onFailure(call: Call<List<JobOfferResponseDTO>>, t: Throwable) {
                    showLoading(false)
                    handleNetworkError(t)
                }
            })
    }

    private fun showLoading(show: Boolean) {
        swipeRefreshLayout.isRefreshing = show
    }

    private fun applyFilters() {
        val q = currentFilter.query.trim().lowercase()
        val dept = currentFilter.department?.lowercase()
        val exp = currentFilter.experience?.lowercase()
        val status = currentFilter.status?.lowercase()

        val filtered = allJobs.filter { job ->
            val fields = listOf(
                job.title,
                job.department,
                job.description,
                job.role
            ).map { it?.lowercase().orEmpty() } +
                (job.responsibilities ?: emptyList()).map { it.lowercase() } +
                (job.qualificationsRequired ?: emptyList()).map { it.lowercase() } +
                (job.qualificationsPreferred ?: emptyList()).map { it.lowercase() }

            val matchesQuery = q.isEmpty() || fields.any { it.contains(q) }
            val matchesDept = dept == null || job.department?.equals(currentFilter.department, true) == true
            val td = "${job.title} ${job.description}".lowercase()
            val inferredLevel = when {
                td.contains("junior") -> "junior"
                td.contains("senior") -> "senior"
                td.contains("mid") || td.contains("middle") || td.contains("intermediate") -> "mid"
                else -> null
            }
            val matchesExp = exp == null || (inferredLevel != null && inferredLevel.equals(exp, true))
            val matchesStatus = status == null ||
                job.status?.equals(currentFilter.status, true) == true ||
                // Some backends use "Finished" instead of CLOSED
                (currentFilter.status.equals("CLOSED", true) && job.status.equals("Finished", true))

            matchesQuery && matchesDept && matchesExp && matchesStatus
        }

        jobAdapter.submitList(filtered)
        updateEmptyState(filtered.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun populateDepartmentsDropdown(jobs: List<JobOfferResponseDTO>) {
        val deptInput = rootView?.findViewById<AutoCompleteTextView>(R.id.autoDepartment) ?: return
        val depts = buildList {
            add("All departments")
            addAll(jobs.mapNotNull { it.department }.distinct().sorted())
        }
        val adapter = AndroidArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, depts)
        deptInput.setAdapter(adapter)
    }

    // --- Placeholder action handlers to resolve unresolved references ---
    private fun handleDeleteJob(job: JobOfferResponseDTO) {
        // TODO: replace with API call to delete job and refresh list
        Toast.makeText(requireContext(), "Delete job: ${'$'}{job.title}", Toast.LENGTH_SHORT).show()
        // Optimistically remove from adapter list
        val list = jobAdapter.currentList.toMutableList()
        val idx = list.indexOfFirst { it.id == job.id }
        if (idx >= 0) {
            list.removeAt(idx)
            jobAdapter.submitList(list)
            updateEmptyState(list.isEmpty())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleUpdateJob(job: JobOfferResponseDTO) {
        // Build a small edit dialog with key fields
        val ctx = requireContext()
        val container = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 20, 40, 0)
        }
        fun editText(hintText: String, prefill: String, multiline: Boolean = false): android.widget.EditText {
            return android.widget.EditText(ctx).apply {
                hint = hintText
                setText(prefill)
                if (multiline) {
                    minLines = 3
                    maxLines = 6
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                }
            }
        }
        val etTitle = editText("Title", job.title)
        val etDepartment = editText("Department", job.department)
        val etRole = editText("Role", job.role ?: "")
        val etDescription = editText("Description", job.description, multiline = true)
        val responsibilitiesPrefill = (job.responsibilities ?: emptyList()).joinToString("\n")
        val etResponsibilities = editText("Responsibilities (one per line)", responsibilitiesPrefill, multiline = true)
        val qualificationsPrefill = buildList {
            addAll(job.qualificationsRequired ?: emptyList())
            addAll(job.qualificationsPreferred ?: emptyList())
        }.joinToString("\n")
        val etQualifications = editText("Qualifications (one per line)", qualificationsPrefill, multiline = true)
        val cbInternal = android.widget.CheckBox(ctx).apply {
            text = "Internal only"
            isChecked = job.isInternal
        }
        container.addView(etTitle)
        container.addView(etDepartment)
        container.addView(etRole)
        container.addView(etDescription)
        container.addView(etResponsibilities)
        container.addView(etQualifications)
        container.addView(cbInternal)

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Edit job")
            .setView(container)
            .setNegativeButton("Close", null)
            .setPositiveButton("Save") { _, _ ->
                // Basic validation
                val title = etTitle.text.toString().trim()
                val dept = etDepartment.text.toString().trim()
                val desc = etDescription.text.toString().trim()
                val role = etRole.text.toString().trim()
                if (title.isEmpty() || dept.isEmpty() || desc.isEmpty()) {
                    Toast.makeText(ctx, "Required fields are missing", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val responsibilities = etResponsibilities.text.toString().lines().filter { it.isNotBlank() }.joinToString("; ")
                val qualifications = etQualifications.text.toString().lines().filter { it.isNotBlank() }.joinToString("; ")

                val request = JobOfferRequest(
                    title = title,
                    department = dept,
                    description = desc,
                    responsibilities = responsibilities,
                    qualifications = qualifications,
                    role = role,
                    isInternal = cbInternal.isChecked
                )

                val prefs = SharedPreferencesManager.getInstance(ctx)
                val token = prefs.getAuthToken()
                if (token.isNullOrEmpty() || prefs.isTokenExpired()) {
                    showSessionExpiredDialog()
                    return@setPositiveButton
                }
                // Use job service without auto Authorization and pass token explicitly
                val api = RetrofitClient.getJobService(ctx)
                api.updateJobOffer(job.id.toString(), request, "Bearer $token")
                    .enqueue(object : retrofit2.Callback<Void> {
                        override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {
                            if (response.isSuccessful) {
                                // Update the item locally
                                val list = jobAdapter.currentList.toMutableList()
                                val idx = list.indexOfFirst { it.id == job.id }
                                if (idx >= 0) {
                                    val updated = list[idx].copy(
                                        title = title,
                                        department = dept,
                                        description = desc,
                                        role = role
                                    )
                                    list[idx] = updated
                                    jobAdapter.submitList(list)
                                }
                                com.google.android.material.snackbar.Snackbar.make(recyclerView, "Job updated", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                            } else {
                                if (response.code() == 401) {
                                    showSessionExpiredDialog()
                                    return
                                }
                                handleApiError(response.code())
                            }
                        }

                        override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                            handleNetworkError(t)
                        }
                    })
            }
            .show()
    }

    private fun handleFinishJob(job: JobOfferResponseDTO) {
        // TODO: call backend to mark as finished, then refresh
        Toast.makeText(requireContext(), "Finish job: ${'$'}{job.title}", Toast.LENGTH_SHORT).show()
        // Update status locally if present
        val list = jobAdapter.currentList.toMutableList()
        val idx = list.indexOfFirst { it.id == job.id }
        if (idx >= 0) {
            val updated = list[idx].copy(status = "Finished")
            list[idx] = updated
            jobAdapter.submitList(list)
        }
    }

    private fun handleViewApplicants(job: JobOfferResponseDTO) {
        // Navigate to candidates list for this job
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_layout,
                CandidateListFragment.newInstance(job.id)
            )
            .addToBackStack(null)
            .commit()
    }

    private fun confirmAndCancelApplication(job: JobOfferResponseDTO) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel Application")
            .setMessage("Are you sure you want to cancel your application for this job?")
            .setPositiveButton("Cancel") { _, _ ->
                // TODO: call backend to cancel application
                Toast.makeText(requireContext(), "Application canceled", Toast.LENGTH_SHORT).show()
                // Track canceled state locally so buttons update
                appliedJobIds.remove(job.id ?: -1L)
                jobAdapter.updateAppliedJobs(appliedJobIds)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showSessionExpiredDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Session Expired")
            .setMessage("Your session has expired. Please log in again.")
            .setPositiveButton("Close") { _, _ -> /* TODO: navigate to login */ }
            .show()
    }

    private fun handleApiError(code: Int) {
        Log.e("AvailableJobs", "API error: code=${'$'}code")
        Toast.makeText(requireContext(), "An error occurred", Toast.LENGTH_SHORT).show()
    }

    private fun handleNetworkError(t: Throwable) {
        Log.e("AvailableJobs", "Network error", t)
        Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
    }
}