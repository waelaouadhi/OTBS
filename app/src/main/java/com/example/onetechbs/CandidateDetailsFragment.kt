package com.example.onetechbs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.onetechbs.db.CandidateResponseDTO
import com.example.onetechbs.db.JobOfferResponseDTO
import com.example.onetechbs.db.ParseResumeRequest
import com.example.onetechbs.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CandidateDetailsFragment : Fragment() {

    private var jobOfferId: Long = -1
    private var candidateId: Long = -1
    private var cvLink: String? = null
    private var fallbackEmail: String? = null
    private var fallbackPhone: String? = null
    private var loadedCandidate: CandidateResponseDTO? = null
    private var loadedJob: JobOfferResponseDTO? = null

    private lateinit var nameTv: TextView
    private lateinit var roleTv: TextView
    private lateinit var emailTv: TextView
    private lateinit var resumeTv: TextView
    private lateinit var scoreTv: TextView
    private lateinit var progress: ProgressBar
    private lateinit var scoreBtn: Button

    // Contact/Actions
    private lateinit var btnDownloadCv: MaterialButton
    private lateinit var btnEmail: MaterialButton
    private lateinit var btnCall: MaterialButton

    // New detailed UI
    private lateinit var overallIndicator: CircularProgressIndicator
    private lateinit var interpretationText: TextView
    private lateinit var skillsBar: LinearProgressIndicator
    private lateinit var expBar: LinearProgressIndicator
    private lateinit var eduBar: LinearProgressIndicator
    private lateinit var certBar: LinearProgressIndicator
    private lateinit var matchedSkillsGroup: ChipGroup
    private lateinit var missingSkillsGroup: ChipGroup
    private lateinit var redFlagsContainer: LinearLayout

    // Percent labels and empty states
    private lateinit var skillsPercent: TextView
    private lateinit var experiencePercent: TextView
    private lateinit var educationPercent: TextView
    private lateinit var certificationsPercent: TextView
    private lateinit var matchedEmpty: TextView
    private lateinit var missingEmpty: TextView
    private lateinit var redFlagsEmpty: TextView

    // Collapsible containers
    private lateinit var breakdownHeader: TextView
    private lateinit var breakdownContent: LinearLayout
    private lateinit var skillsHeader: TextView
    private lateinit var skillsContent: LinearLayout
    private lateinit var redFlagsHeader: TextView
    private lateinit var redFlagsContent: LinearLayout

    // Collapse state
    private var isBreakdownExpanded = true
    private var isSkillsExpanded = true
    private var isRedFlagsExpanded = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            jobOfferId = it.getLong(ARG_JOB_ID)
            candidateId = it.getLong(ARG_CANDIDATE_ID)
            cvLink = it.getString(ARG_CV_LINK)
        }
        savedInstanceState?.let {
            isBreakdownExpanded = it.getBoolean(STATE_BREAKDOWN, true)
            isSkillsExpanded = it.getBoolean(STATE_SKILLS, true)
            isRedFlagsExpanded = it.getBoolean(STATE_REDFLAGS, true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_candidate_details, container, false)
        nameTv = root.findViewById(R.id.name)
        roleTv = root.findViewById(R.id.role)
        emailTv = root.findViewById(R.id.email)
        resumeTv = root.findViewById(R.id.resume)
        scoreTv = root.findViewById(R.id.score)
        progress = root.findViewById(R.id.progressBar)
        scoreBtn = root.findViewById(R.id.btnComputeScore)
        val toolbar: MaterialToolbar = root.findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        toolbar.inflateMenu(R.menu.menu_candidate_details)
        toolbar.setOnMenuItemClickListener { onToolbarItemSelected(it) }

        // Contact/Actions buttons
        btnDownloadCv = root.findViewById(R.id.btnDownloadCv)
        btnEmail = root.findViewById(R.id.btnEmail)
        btnCall = root.findViewById(R.id.btnCall)
        // Keep only Call button; hide Email and Download CV buttons
        btnDownloadCv.visibility = View.GONE
        btnEmail.visibility = View.GONE
        btnCall.setOnClickListener { onCallClicked() }
        // Initially disabled until data is loaded
        btnCall.isEnabled = false

        // New detailed views
        overallIndicator = root.findViewById(R.id.overallScoreIndicator)
        interpretationText = root.findViewById(R.id.interpretationText)
        skillsBar = root.findViewById(R.id.skillsScoreBar)
        expBar = root.findViewById(R.id.experienceScoreBar)
        eduBar = root.findViewById(R.id.educationScoreBar)
        certBar = root.findViewById(R.id.certificationsScoreBar)
        matchedSkillsGroup = root.findViewById(R.id.matchedSkillsGroup)
        missingSkillsGroup = root.findViewById(R.id.missingSkillsGroup)
        redFlagsContainer = root.findViewById(R.id.redFlagsContainer)

        // Percent labels and empties
        skillsPercent = root.findViewById(R.id.skillsPercent)
        experiencePercent = root.findViewById(R.id.experiencePercent)
        educationPercent = root.findViewById(R.id.educationPercent)
        certificationsPercent = root.findViewById(R.id.certificationsPercent)
        matchedEmpty = root.findViewById(R.id.matchedEmpty)
        missingEmpty = root.findViewById(R.id.missingEmpty)
        redFlagsEmpty = root.findViewById(R.id.redFlagsEmpty)

        // Collapsible sections
        breakdownHeader = root.findViewById(R.id.breakdownHeader)
        breakdownContent = root.findViewById(R.id.breakdownContent)
        skillsHeader = root.findViewById(R.id.skillsHeader)
        skillsContent = root.findViewById(R.id.skillsContent)
        redFlagsHeader = root.findViewById(R.id.redFlagsHeader)
        redFlagsContent = root.findViewById(R.id.redFlagsContent)

        breakdownHeader.setOnClickListener { toggleSection(breakdownContent).also { isBreakdownExpanded = breakdownContent.visibility == View.VISIBLE } }
        skillsHeader.setOnClickListener { toggleSection(skillsContent).also { isSkillsExpanded = skillsContent.visibility == View.VISIBLE } }
        redFlagsHeader.setOnClickListener { toggleSection(redFlagsContent).also { isRedFlagsExpanded = redFlagsContent.visibility == View.VISIBLE } }

        // Apply saved expansion state
        breakdownContent.visibility = if (isBreakdownExpanded) View.VISIBLE else View.GONE
        skillsContent.visibility = if (isSkillsExpanded) View.VISIBLE else View.GONE
        redFlagsContent.visibility = if (isRedFlagsExpanded) View.VISIBLE else View.GONE

        loadCandidate()
        loadJobOffer()

        scoreBtn.setOnClickListener { requestScore() }
        return root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_BREAKDOWN, isBreakdownExpanded)
        outState.putBoolean(STATE_SKILLS, isSkillsExpanded)
        outState.putBoolean(STATE_REDFLAGS, isRedFlagsExpanded)
    }

    private fun onToolbarItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> { shareResult(); true }
            R.id.action_refresh -> { requestScore(); true }
            else -> false
        }
    }

    private fun onDownloadCvClicked() {
        val url = cvLink
        if (url.isNullOrBlank()) {
            Snackbar.make(requireView(), "No CV available", Snackbar.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Snackbar.make(requireView(), "Unable to open CV", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun onEmailClicked() {
        val email = loadedCandidate?.candidateInfo?.email?.takeIf { it.isNotBlank() } ?: fallbackEmail
        if (email.isNullOrBlank()) {
            Snackbar.make(requireView(), "No email available", Snackbar.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, "Regarding your application")
        }
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            Snackbar.make(requireView(), "No email app found", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun onCallClicked() {
        val phone = loadedCandidate?.candidateInfo?.phone?.takeIf { it.isNotBlank() } ?: fallbackPhone
        if (phone.isNullOrBlank()) {
            Snackbar.make(requireView(), "No phone available", Snackbar.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phone")
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Snackbar.make(requireView(), "Unable to open dialer", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun shareResult() {
        val score = scoreTv.text?.toString()?.ifBlank { null } ?: return
        val interp = interpretationText.text?.toString()?.ifBlank { null }
        val share = "$score${if (interp != null) " • $interp" else ""}"
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("AI Score", share))
        Snackbar.make(requireView(), "Score copied to clipboard", Snackbar.LENGTH_SHORT).show()
    }

    private fun toggleSection(content: View) {
        content.visibility = if (content.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun loadCandidate() {
        progress.visibility = View.VISIBLE
        RetrofitClient.candidateService.getCandidate(candidateId).enqueue(object : Callback<CandidateResponseDTO> {
            override fun onResponse(
                call: Call<CandidateResponseDTO>, response: Response<CandidateResponseDTO>
            ) {
                progress.visibility = View.GONE
                if (response.isSuccessful) {
                    val c = response.body() ?: return
                    loadedCandidate = c
                    bindCandidate(c)
                    // Fallback contact extraction from built resume text
                    extractContacts(buildResumeText(c))
                    updateContactButtons()
                } else {
                    Snackbar.make(requireView(), "Failed to load candidate: ${response.code()}", Snackbar.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<CandidateResponseDTO>, t: Throwable) {
                progress.visibility = View.GONE
                Snackbar.make(requireView(), t.localizedMessage ?: "Network error", Snackbar.LENGTH_LONG).show()
            }
        })
    }

    private fun loadJobOffer() {
        RetrofitClient.apiService.getJobOfferById(jobOfferId).enqueue(object : Callback<JobOfferResponseDTO> {
            override fun onResponse(call: Call<JobOfferResponseDTO>, response: Response<JobOfferResponseDTO>) {
                if (response.isSuccessful) {
                    loadedJob = response.body()
                }
            }
            override fun onFailure(call: Call<JobOfferResponseDTO>, t: Throwable) {
                // Non-blocking
            }
        })
    }

    private fun bindCandidate(c: CandidateResponseDTO) {
        nameTv.text = c.candidateInfo.name
        val roleGuess = c.skills.technical.firstOrNull() ?: "N/A"
        roleTv.text = roleGuess
        emailTv.text = c.candidateInfo.email
        resumeTv.text = "Uploaded resume available"
    }

    private fun updateContactButtons() {
        val phone = loadedCandidate?.candidateInfo?.phone?.takeIf { it.isNotBlank() } ?: fallbackPhone
        btnCall.isEnabled = !phone.isNullOrBlank()
    }

    private fun buildResumeText(c: CandidateResponseDTO): String {
        val sb = StringBuilder()
        sb.appendLine("Name: ${c.candidateInfo.name}")
        sb.appendLine("Email: ${c.candidateInfo.email}")
        sb.appendLine("Phone: ${c.candidateInfo.phone}")
        sb.appendLine("Location: ${c.candidateInfo.location.city}, ${c.candidateInfo.location.country}")
        if (c.skills.technical.isNotEmpty()) sb.appendLine("Technical Skills: ${c.skills.technical.joinToString(", ")}")
        if (c.skills.soft.isNotEmpty()) sb.appendLine("Soft Skills: ${c.skills.soft.joinToString(", ")}")
        if (c.experience.isNotEmpty()) {
            sb.appendLine("Experience:")
            c.experience.forEach { e ->
                sb.appendLine("- ${e.title} @ ${e.company} (${e.startDate} - ${e.endDate ?: "Present"}) ${e.location}")
                if (e.responsibilities.isNotEmpty()) sb.appendLine("  Responsibilities: ${e.responsibilities.joinToString(", ")}")
                if (e.achievements.isNotEmpty()) sb.appendLine("  Achievements: ${e.achievements.joinToString(", ")}")
            }
        }
        if (c.education.isNotEmpty()) {
            sb.appendLine("Education:")
            c.education.forEach { ed ->
                sb.appendLine("- ${ed.degree} ${ed.fieldOfStudy ?: ""} @ ${ed.institution} (${ed.startDate} - ${ed.endDate ?: ""}) ${ed.location}")
            }
        }
        if (c.certifications.isNotEmpty()) sb.appendLine("Certifications: ${c.certifications.joinToString(", ") { it.name }}")
        if (c.projects.isNotEmpty()) {
            sb.appendLine("Projects:")
            c.projects.forEach { p -> sb.appendLine("- ${p.name}") }
        }
        return sb.toString()
    }

    private fun buildJobDescriptionText(j: JobOfferResponseDTO?): String {
        if (j == null) return ""
        val sb = StringBuilder()
        sb.appendLine("Title: ${j.title}")
        sb.appendLine("Department: ${j.department}")
        sb.appendLine("Summary: ${j.description}")
        j.responsibilities?.takeIf { it.isNotEmpty() }?.let { sb.appendLine("Responsibilities: ${it.joinToString(", ")}") }
        j.qualificationsRequired?.takeIf { it.isNotEmpty() }?.let { sb.appendLine("Qualifications Required: ${it.joinToString(", ")}") }
        j.qualificationsPreferred?.takeIf { it.isNotEmpty() }?.let { sb.appendLine("Qualifications Preferred: ${it.joinToString(", ")}") }
        j.role?.let { sb.appendLine("Role: $it") }
        j.whatWeOffer?.let { sb.appendLine("What We Offer: $it") }
        return sb.toString()
    }

    private fun requestScore() {
        progress.visibility = View.VISIBLE
        scoreBtn.isEnabled = false
        val ai = RetrofitClient.getAiService()
        val resumeText = loadedCandidate?.let { buildResumeText(it) } ?: ""
        // Keep fallbacks up to date
        extractContacts(resumeText)
        val jobDesc = buildJobDescriptionText(loadedJob)
        val req = ParseResumeRequest(
            jobOfferId = jobOfferId,
            candidateId = candidateId,
            resume = resumeText,
            jobDescription = jobDesc
        )
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            try {
                val resp = ai.match(req)
                progress.visibility = View.GONE
                scoreBtn.isEnabled = true
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val mr = body?.match_result

                    // Overall score
                    val scoreRaw = mr?.score
                    val displayPct = scoreRaw?.let { toPercent(it) }
                    overallIndicator.setProgressCompat((displayPct ?: 0.0).toInt(), true)
                    scoreTv.text = displayPct?.let { String.format("%.2f%%", it) } ?: "-"

                    // Interpretation
                    interpretationText.text = mr?.interpretation.orEmpty()

                    // Prefer backend details when available
                    val details = mr?.details.orEmpty()
                    val skillsDet = details["skills_match"]
                    val expDet = details["relevant_experience"]
                    val eduDet = details["education"]
                    val certDet = details["certifications"]

                    fun raw10ToPercent(raw: Double?): Double? = raw?.let { (it.coerceIn(0.0, 10.0)) * 10.0 }

                    val fallback = computeLocalSkillsMatch()
                    val skillsPct = raw10ToPercent(skillsDet?.raw_score) ?: (mr?.skills_score ?: fallback.first)
                    val expPct = raw10ToPercent(expDet?.raw_score) ?: mr?.experience_score
                    val eduPct = raw10ToPercent(eduDet?.raw_score) ?: mr?.education_score
                    val certPct = raw10ToPercent(certDet?.raw_score) ?: mr?.certifications_score

                    setBar(skillsBar, skillsPct, skillsPercent)
                    setBar(expBar, expPct, experiencePercent)
                    setBar(eduBar, eduPct, educationPercent)
                    setBar(certBar, certPct, certificationsPercent)

                    // Skills chips: prefer details.skills_match lists
                    val matchedFromDetails = skillsDet?.matching_skills
                    val missingFromDetails = skillsDet?.missing_skills
                    val matched = when {
                        !matchedFromDetails.isNullOrEmpty() -> matchedFromDetails
                        !mr?.matched_skills.isNullOrEmpty() -> mr?.matched_skills
                        else -> fallback.second
                    }
                    val missing = when {
                        !missingFromDetails.isNullOrEmpty() -> missingFromDetails
                        !mr?.missing_skills.isNullOrEmpty() -> mr?.missing_skills
                        else -> fallback.third
                    }
                    setChips(matchedSkillsGroup, matched, true)
                    setChips(missingSkillsGroup, missing, false)
                    matchedEmpty.visibility = if (matched.isNullOrEmpty()) View.VISIBLE else View.GONE
                    missingEmpty.visibility = if (missing.isNullOrEmpty()) View.VISIBLE else View.GONE

                    // Red flags
                    val rflags = mr?.red_flags
                    setRedFlags(rflags)
                    redFlagsEmpty.visibility = if (rflags.isNullOrEmpty()) View.VISIBLE else View.GONE

                    val warnings = (body?.warnings.orEmpty() + (mr?.warnings.orEmpty()))
                    if (warnings.isNotEmpty()) {
                        val msg = warnings.joinToString("\n").take(300)
                        Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG).show()
                    }
                } else {
                    val err = try { resp.errorBody()?.string() } catch (_: Exception) { null }
                    Snackbar.make(requireView(), "Score failed: ${resp.code()} ${err ?: ""}".trim(), Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                scoreBtn.isEnabled = true
                Snackbar.make(requireView(), e.localizedMessage ?: "Error", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun toPercent(value: Double): Double {
        return if (value <= 1.0) value * 100.0 else value
    }

    private fun setBar(bar: LinearProgressIndicator, raw: Double?, label: TextView) {
        val pct = (raw ?: 0.0).coerceIn(0.0, 100.0)
        bar.setProgressCompat(pct.toInt(), true)
        label.text = String.format("%.0f%%", pct)
        tintByThreshold(bar, pct)
    }

    private fun tintByThreshold(bar: LinearProgressIndicator, pct: Double) {
        val ctx = bar.context
        val color = when {
            pct >= 75 -> requireContext().getColor(R.color.md_theme_primary)
            pct >= 50 -> requireContext().getColor(R.color.md_theme_secondary)
            else -> requireContext().getColor(R.color.md_theme_error)
        }
        bar.setIndicatorColor(color)
    }

    private fun setChips(group: ChipGroup, items: List<String>?, matched: Boolean) {
        group.removeAllViews()
        val ctx = group.context
        items.orEmpty().forEach { text ->
            val chip = Chip(ctx).apply {
                this.text = text
                isCheckable = false
                isClickable = true
                isCloseIconVisible = false
                isChipIconVisible = true
                chipIcon = requireContext().getDrawable(if (matched) R.drawable.ic_check else R.drawable.ic_warning_amber_24)
                chipStrokeWidth = if (matched) 0f else 1f
            }
            chip.setOnClickListener {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Skill", chip.text))
                Snackbar.make(requireView(), "Copied: ${chip.text}", Snackbar.LENGTH_SHORT).show()
            }
            group.addView(chip)
        }
    }

    private fun setRedFlags(flags: List<String>?) {
        redFlagsContainer.removeAllViews()
        if (flags.isNullOrEmpty()) {
            redFlagsContainer.visibility = View.GONE
            return
        }
        redFlagsContainer.visibility = View.VISIBLE
        val ctx = redFlagsContainer.context
        flags.forEach { f ->
            val tv = TextView(ctx).apply {
                text = "• $f"
                setTextColor(resources.getColor(R.color.md_theme_onSurfaceVariant, null))
            }
            redFlagsContainer.addView(tv)
        }
    }

    private fun computeLocalSkillsMatch(): Triple<Double, List<String>, List<String>> {
        val candidateSkills = (loadedCandidate?.skills?.technical ?: emptyList()).map { it.trim().lowercase() }.toSet()
        val jobRequired = (loadedJob?.qualificationsRequired ?: emptyList()).map { it.trim().lowercase() }.toSet()
        val jobPreferred = (loadedJob?.qualificationsPreferred ?: emptyList()).map { it.trim().lowercase() }.toSet()
        val jobSkills = (jobRequired + jobPreferred).filter { it.isNotBlank() }.toSet()
        if (jobSkills.isEmpty() || candidateSkills.isEmpty()) return Triple(0.0, emptyList(), jobSkills.toList())

        val matched = jobSkills.intersect(candidateSkills).toList().sorted()
        val missing = jobSkills.minus(candidateSkills).toList().sorted()
        val pct = (matched.size.toDouble() / jobSkills.size.toDouble()) * 100.0
        val fmtMatched = matched.map { it.replaceFirstChar { ch -> ch.titlecase() } }
        val fmtMissing = missing.map { it.replaceFirstChar { ch -> ch.titlecase() } }
        return Triple(pct, fmtMatched, fmtMissing)
    }

    private fun extractContacts(text: String) {
        if (text.isBlank()) return
        // Basic email regex
        val emailRegex = """[A-Za-z0-9+._%\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""".toRegex()
        val phoneRegex = """(?:(?:\+|00)\d{1,3}[\s.-]?)?(?:\(?\d{2,4}\)[\s.-]?)?\d{3,4}[\s.-]?\d{3,4}""".toRegex()
        fallbackEmail = fallbackEmail ?: emailRegex.find(text)?.value
        // Prefer a longer digit count for phone
        val phoneMatches = phoneRegex.findAll(text).map { it.value }.toList()
        if (fallbackPhone == null && phoneMatches.isNotEmpty()) {
            fallbackPhone = phoneMatches.maxByOrNull { it.filter { ch -> ch.isDigit() }.length }
        }
    }

    companion object {
        private const val ARG_JOB_ID = "arg_job_id"
        private const val ARG_CANDIDATE_ID = "arg_candidate_id"
        private const val ARG_CV_LINK = "arg_cv_link"
        private const val STATE_BREAKDOWN = "state_breakdown"
        private const val STATE_SKILLS = "state_skills"
        private const val STATE_REDFLAGS = "state_redflags"
        fun newInstance(jobOfferId: Long, candidateId: Long): CandidateDetailsFragment {
            return CandidateDetailsFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_JOB_ID, jobOfferId)
                    putLong(ARG_CANDIDATE_ID, candidateId)
                }
            }
        }
        fun newInstance(jobOfferId: Long, candidateId: Long, cvLink: String?): CandidateDetailsFragment {
            return CandidateDetailsFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_JOB_ID, jobOfferId)
                    putLong(ARG_CANDIDATE_ID, candidateId)
                    if (!cvLink.isNullOrBlank()) putString(ARG_CV_LINK, cvLink)
                }
            }
        }
    }
}
