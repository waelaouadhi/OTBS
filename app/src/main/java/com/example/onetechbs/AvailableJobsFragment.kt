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
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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
        isHR = prefs.getString("role", "employee") == "HR"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupRecyclerView() {
        jobAdapter = JobAdapter(
            isHR = isHR,
            onDeleteClick = { job -> handleDeleteJob(job) },
            onUpdateClick = { job -> handleUpdateJob(job) },
            onFinishClick = { job -> handleFinishJob(job) },
            onApplicantsClick = { job -> handleViewApplicants(job) }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = jobAdapter
            setHasFixedSize(true)
        }
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
                        .setTitle(getString(R.string.delete_job_title))
                        .setMessage(getString(R.string.delete_job_message))
                        .setPositiveButton(getString(R.string.delete)) { _, _ ->
                            itemView.animate()
                                .alpha(0f)
                                .setDuration(200)
                                .withEndAction {
                                    temporarilyRemoveJob(job, position)
                                    itemView.alpha = 1f
                                }
                                .start()
                        }
                        .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                            jobAdapter.notifyItemChanged(position)
                        }
                        .setOnCancelListener {
                            jobAdapter.notifyItemChanged(position)
                        }
                        .show()
                } else if (direction == ItemTouchHelper.RIGHT) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.finish_job_title))
                        .setMessage(getString(R.string.finish_job_message))
                        .setPositiveButton(getString(R.string.finish)) { _, _ ->
                            itemView.animate()
                                .alpha(0f)
                                .setDuration(200)
                                .withEndAction {
                                    finishJob(job.id.toString())
                                    itemView.alpha = 1f
                                }
                                .start()
                        }
                        .setNegativeButton(getString(R.string.cancel)) { _, _ ->
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
        val recruitingService = RetrofitClient.getRecruitingService(requireContext())
        recruitingService.getAllJobOffers()
            .enqueue(object : Callback<List<JobOfferResponseDTO>> {
                override fun onResponse(
                    call: Call<List<JobOfferResponseDTO>>,
                    response: Response<List<JobOfferResponseDTO>>
                ) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val jobs = response.body() ?: emptyList()
                        updateJobsList(jobs)
                    } else {
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

    private fun updateJobsList(jobs: List<JobOfferResponseDTO>) {
        jobAdapter.submitList(jobs)
        updateEmptyState(jobs.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun handleUpdateJob(job: JobOfferResponseDTO) {
        val updateFragment = UpdateJobFragment.newInstance(job.id.toString())
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_layout, updateFragment)
            .addToBackStack(null)
            .commit()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleFinishJob(job: JobOfferResponseDTO) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.finish_job_title))
            .setMessage(getString(R.string.finish_job_message))
            .setPositiveButton(getString(R.string.finish)) { _, _ ->
                finishJob(job.id.toString())
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun finishJob(jobId: String) {
        val token = SharedPreferencesManager.getInstance(requireContext()).getAuthToken()

        if (token.isNullOrEmpty()) {
            showSessionExpiredDialog()
            return
        }

        showLoading(true)

        val recruitingService = RetrofitClient.getRecruitingService(requireContext())
        recruitingService.toggleJobOfferStatus(
            jobId = jobId,
            status = "CLOSED", // must be one of: OPEN, CLOSED, CONVERTED_TO_EXTERNAL, CONVERTED_TO_INTERNAL
            token = "Bearer $token"
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                showLoading(false)
                if (response.isSuccessful) {
                    showSuccess(getString(R.string.job_finished_success))
                    loadJobs()
                } else if (response.code() == 401) {
                    showSessionExpiredDialog()
                } else {
                    handleApiError(response.code())
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showLoading(false)
                handleNetworkError(t)
            }
        })
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleDeleteJob(job: JobOfferResponseDTO) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_job_title))
            .setMessage(getString(R.string.delete_job_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteJob(job.id.toString())
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun deleteJob(jobId: String) {
        showLoading(true)
        val recruitingService = RetrofitClient.getRecruitingService(requireContext())
        recruitingService.deleteJobOffer(jobId)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        showSuccess(getString(R.string.job_deleted_success))
                        loadJobs()
                    } else {
                        handleApiError(response.code())
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    showLoading(false)
                    handleNetworkError(t)
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.O)

    private fun handleViewApplicants(job: JobOfferResponseDTO) {
        val prefsManager = SharedPreferencesManager.getInstance(requireContext())

        if (prefsManager.isTokenExpired()) {
            showSessionExpiredDialog()
            return
        }

        val token = prefsManager.getAuthToken()
        if (token.isNullOrEmpty()) {
            showSessionExpiredDialog()
            return
        }

        val authHeader = "Bearer $token"
        showLoading(true)

        RetrofitClient.candidateService.listCandidates(authHeader).enqueue(object : Callback<List<CandidateResponseDTO>> {
            override fun onResponse(
                call: Call<List<CandidateResponseDTO>>,
                response: Response<List<CandidateResponseDTO>>
            ) {
                showLoading(false)
                if (response.isSuccessful) {
                    val candidates = response.body() ?: emptyList()
                    showCandidatesDialog(candidates)
                } else if (response.code() == 401) {
                    showSessionExpiredDialog()
                } else {
                    handleApiError(response.code())
                }
            }

            override fun onFailure(call: Call<List<CandidateResponseDTO>>, t: Throwable) {
                showLoading(false)
                handleNetworkError(t)
            }
        })
    }private fun showCandidatesDialog(candidates: List<CandidateResponseDTO>) {
        if (candidates.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.applicants))
                .setMessage(getString(R.string.no_applicants))
                .setPositiveButton(getString(R.string.close), null)
                .show()
            return
        }

        val message = buildString {
            append("Showing all candidates.\n\n") // visual cue
            candidates.forEach { candidate ->
                append("${candidate.candidateInfo.name} - ${candidate.candidateInfo.email}\n")
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.applicants))
            .setMessage(message)
            .setPositiveButton(getString(R.string.close), null)
            .show()
    }
    private fun handleApiError(code: Int) {
        val message = when (code) {
            401 -> getString(R.string.error_unauthorized)
            403 -> getString(R.string.error_forbidden)
            404 -> getString(R.string.error_not_found)
            else -> getString(R.string.error_generic)
        }
        showError(message)
    }

    private fun handleNetworkError(throwable: Throwable) {
        Log.e(TAG, "Network error", throwable)
        val message = when {
            throwable.message?.contains("timeout") == true -> getString(R.string.error_timeout)
            !isNetworkAvailable() -> getString(R.string.error_no_internet)
            else -> getString(R.string.error_network)
        }
        showError(message)
    }
    private fun showSessionExpiredDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Session Expired")
            .setMessage("Your session has expired. Please log in again.")
            .setCancelable(false)
            .setPositiveButton("Login") { _, _ ->
                SharedPreferencesManager.getInstance(requireContext()).clearAuthData()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .show()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return actNw.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        showMessage(message)
    }

    private fun showSuccess(message: String) {
        showMessage(message)
    }

    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterShakeListener()
        undoTimer?.cancel()
        rootView = null
    }

    companion object {
        private const val TAG = "AvailableJobsFragment"
    }
}