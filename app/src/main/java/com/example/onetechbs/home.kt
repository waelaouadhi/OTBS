package com.example.onetechbs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.onetechbs.databinding.ActivityHomeBinding
import com.google.android.material.navigation.NavigationView
import androidx.fragment.app.FragmentManager
import com.example.onetechbs.DetailsFragment

class home : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        drawerLayout = binding.drawerLayout
        navigationView = binding.navigationView

        setSupportActionBar(binding.toolbar)

        setupBottomNavigation()
        applyRoleBasedUI()

        toggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.toolbar, R.string.open_drawer, R.string.close_drawer
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        setupDrawerNavigation()

        supportFragmentManager.addOnBackStackChangedListener {
            updateToolbarForFragment()
        }

        binding.toolbar.setNavigationOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    private fun setupBottomNavigation() {
        replaceFragment(Homefra()) // Default fragment

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> {
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    replaceFragment(Homefra(), false)
                    updateToolbarForFragment()
                }
                R.id.addLeave -> replaceFragment(AddLeaveFragment())
                R.id.doctor -> replaceFragment(DoctorFragment())

                R.id.notification -> replaceFragment(NotificationFragment())
            }
            true
        }
    }

    private fun setupDrawerNavigation() {
        // Set initial selected item
        navigationView.menu.findItem(R.id.nav_home)?.isChecked = true

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
    R.id.nav_logout -> {
        logoutUser()
    }
                R.id.nav_profile -> replaceFragment(ProfileFragment())
                R.id.nav_home -> replaceFragment(Homefra())
                R.id.nav_leaves -> replaceFragment(LeaveListFragment())
                R.id.nav_doctor -> replaceFragment(DoctorListFragment())

                R.id.nav_notification -> replaceFragment(NotificationFragment())
                R.id.nav_available_jobs -> replaceFragment(AvailableJobsFragment())
                R.id.nav_document -> replaceFragment(DocumentFragment())
                R.id.nav_course_list -> replaceFragment(CoursesListFragment())

                R.id.nav_internal_documents -> {
                    if (isHRD() )replaceFragment(InternalDocumentsFragment())
                    else showAccessDenied()
                }

                R.id.nav_internal_recruiting -> {
                    if (isHR()) replaceFragment(DetailsFragment())
                    else showAccessDenied()
                }

                R.id.nav_course_propositions -> {
                    if (isManager()) replaceFragment(TrainingCoursePropositionsFragment())
                    else showAccessDenied()
                }
                R.id.nav_course_propositions_list -> {

                    if (isManager()|| isHRD()){
                        replaceFragment(TrainingCoursePropositionsListFragment())
                    } else {
                        showAccessDenied()
                    }
            }
                R.id.nav_enrollments -> {

                    if (isHR() || isManager()|| isHRD()) {
                        replaceFragment(EnrollmentsListFragment())
                    } else {
                        showAccessDenied()
                    }
                }


                R.id.nav_doctor_management -> {
                    if (isHR()) replaceFragment(DoctorManagementFragment())
                    else showAccessDenied()
                }
                R.id.nav_create_course -> {

                    val role = getUserRole()
                    if (isHRD())
                        replaceFragment(CreateCourseFragment())
                    else showAccessDenied()
                }

//                R.id.nav_training_management -> {
//                    if (isManager()) replaceFragment(TrainingManagementFragment())
//                    else showAccessDenied()
//                }

                R.id.nav_leaves_management -> {
                    if (isManager() || isHRD()) replaceFragment(HRLeaveManagementFragment())
                    else showAccessDenied()
                }
                R.id.nav_internal_documents_list -> {
                    replaceFragment(InternalDocumentsListFragment())
                }
                R.id.nav_treat_personal_documents -> {
                    val role = getUserRole()
                    if (isHR()|| isHRD()) {
                        replaceFragment(TreatPersonalDocumentsFragment())
                    } else {
                        showAccessDenied()
                    }
                }
                R.id.nav_attendance -> {
                    if (isHR() || isHRD()) {
                        replaceFragment(com.example.onetechbs.attendance.AttendanceFragment())
                    } else {
                        showAccessDenied()
                    }
                }
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun applyRoleBasedUI() {
        val navMenu = navigationView.menu
        val bottomMenu = binding.bottomNavigationView.menu
        val role = getUserRole()

        // Hide all navigation items by default
        hideAllNavItems(navMenu)
        hideAllBottomNavItems(bottomMenu)

        // Show items based on specific role requirements
        when {
            isHR() -> {
                setupHRNavigation(navMenu)
                setupHRBottomNavigation(bottomMenu)
            }
            isManager() -> {
                setupManagerNavigation(navMenu)
                setupManagerBottomNavigation(bottomMenu)
            }
            isHRD() -> {
                setupHRDNavigation(navMenu)
                setupHRDBottomNavigation(bottomMenu)
            }
            isEmployee() -> {
                setupEmployeeNavigation(navMenu)
                setupEmployeeBottomNavigation(bottomMenu)
            }
        }
    }

    private fun hideAllNavItems(navMenu: android.view.Menu) {
        // Hide all drawer navigation items
        navMenu.findItem(R.id.nav_profile)?.isVisible = false
        navMenu.findItem(R.id.nav_leaves)?.isVisible = false
        navMenu.findItem(R.id.nav_available_jobs)?.isVisible = false
        navMenu.findItem(R.id.nav_doctor)?.isVisible = false
        navMenu.findItem(R.id.nav_doctor_management)?.isVisible = false
        navMenu.findItem(R.id.nav_course_list)?.isVisible = false
        navMenu.findItem(R.id.nav_enrollments)?.isVisible = false
        navMenu.findItem(R.id.nav_attendance)?.isVisible = false
        navMenu.findItem(R.id.nav_document)?.isVisible = false
        navMenu.findItem(R.id.nav_internal_documents_list)?.isVisible = false
        navMenu.findItem(R.id.nav_treat_personal_documents)?.isVisible = false
        navMenu.findItem(R.id.nav_internal_recruiting)?.isVisible = false
        navMenu.findItem(R.id.nav_course_propositions)?.isVisible = false
        navMenu.findItem(R.id.nav_course_propositions_list)?.isVisible = false
        navMenu.findItem(R.id.nav_leaves_management)?.isVisible = false
        navMenu.findItem(R.id.nav_create_course)?.isVisible = false
        navMenu.findItem(R.id.nav_internal_documents)?.isVisible = false
        navMenu.findItem(R.id.nav_notification)?.isVisible = false
        navMenu.findItem(R.id.nav_logout)?.isVisible = false
    }

    private fun setupHRNavigation(navMenu: android.view.Menu) {
        // HR: profile, leaves, Available Jobs, Doctor, Doctor Management, course List,
        // Enrolment, Attendance Management, Documents, Internal Documents List,
        // Treat Personal Documents, Internal Recruiting, Notification and logout
        navMenu.findItem(R.id.nav_profile)?.isVisible = true
        navMenu.findItem(R.id.nav_leaves)?.isVisible = true
        navMenu.findItem(R.id.nav_available_jobs)?.isVisible = true
        navMenu.findItem(R.id.nav_doctor)?.isVisible = true
        navMenu.findItem(R.id.nav_doctor_management)?.isVisible = true
        navMenu.findItem(R.id.nav_course_list)?.isVisible = true
        navMenu.findItem(R.id.nav_enrollments)?.isVisible = true
        navMenu.findItem(R.id.nav_attendance)?.isVisible = true
        navMenu.findItem(R.id.nav_document)?.isVisible = true
        navMenu.findItem(R.id.nav_internal_documents_list)?.isVisible = true
        navMenu.findItem(R.id.nav_treat_personal_documents)?.isVisible = true
        navMenu.findItem(R.id.nav_internal_recruiting)?.isVisible = true
        navMenu.findItem(R.id.nav_notification)?.isVisible = true
        navMenu.findItem(R.id.nav_logout)?.isVisible = true
    }

    private fun setupManagerNavigation(navMenu: android.view.Menu) {
        // Manager: profile, leaves, Available Jobs, Doctor, courses list,
        // course Propositions, course proposition list, leave manager,
        // documents, internal Documents list, notification and logout
        navMenu.findItem(R.id.nav_profile)?.isVisible = true
        navMenu.findItem(R.id.nav_leaves)?.isVisible = true
        navMenu.findItem(R.id.nav_available_jobs)?.isVisible = true
        navMenu.findItem(R.id.nav_doctor)?.isVisible = true
        navMenu.findItem(R.id.nav_course_list)?.isVisible = true
        navMenu.findItem(R.id.nav_course_propositions)?.isVisible = true
        navMenu.findItem(R.id.nav_course_propositions_list)?.isVisible = true
        navMenu.findItem(R.id.nav_leaves_management)?.isVisible = true
        navMenu.findItem(R.id.nav_document)?.isVisible = true
        navMenu.findItem(R.id.nav_internal_documents_list)?.isVisible = true
        navMenu.findItem(R.id.nav_notification)?.isVisible = true
        navMenu.findItem(R.id.nav_logout)?.isVisible = true
    }

    private fun setupHRDNavigation(navMenu: android.view.Menu) {
        // HRD: profile, leaves, Available Jobs, Doctor, Enrolments,
        // course Proposition List, course List, Create Course, Attendance management,
        // documents, internal documents, internal documents List,
        // treat personal Documents, notification and logout
        navMenu.findItem(R.id.nav_profile)?.isVisible = true
        navMenu.findItem(R.id.nav_leaves)?.isVisible = true
        navMenu.findItem(R.id.nav_available_jobs)?.isVisible = true
        navMenu.findItem(R.id.nav_doctor)?.isVisible = true
        navMenu.findItem(R.id.nav_enrollments)?.isVisible = true
        navMenu.findItem(R.id.nav_course_propositions_list)?.isVisible = true
        navMenu.findItem(R.id.nav_course_list)?.isVisible = true
        navMenu.findItem(R.id.nav_create_course)?.isVisible = true
        navMenu.findItem(R.id.nav_attendance)?.isVisible = true
        navMenu.findItem(R.id.nav_document)?.isVisible = true
        navMenu.findItem(R.id.nav_internal_documents)?.isVisible = true
        navMenu.findItem(R.id.nav_internal_documents_list)?.isVisible = true
        navMenu.findItem(R.id.nav_treat_personal_documents)?.isVisible = true
        navMenu.findItem(R.id.nav_notification)?.isVisible = true
        navMenu.findItem(R.id.nav_logout)?.isVisible = true
    }

    private fun setupEmployeeNavigation(navMenu: android.view.Menu) {
        // Employee: profile, leaves, Available Jobs, Doctor, course list,
        // internal Documents, documents, notification and logout
        navMenu.findItem(R.id.nav_profile)?.isVisible = true
        navMenu.findItem(R.id.nav_leaves)?.isVisible = true
        navMenu.findItem(R.id.nav_available_jobs)?.isVisible = true
        navMenu.findItem(R.id.nav_doctor)?.isVisible = true
        navMenu.findItem(R.id.nav_course_list)?.isVisible = true
        navMenu.findItem(R.id.nav_internal_documents_list)?.isVisible = true
        navMenu.findItem(R.id.nav_document)?.isVisible = true
        navMenu.findItem(R.id.nav_notification)?.isVisible = true
        navMenu.findItem(R.id.nav_logout)?.isVisible = true
    }

    private fun hideAllBottomNavItems(bottomMenu: android.view.Menu) {
        // Hide all bottom navigation items
        bottomMenu.findItem(R.id.home)?.isVisible = false
        bottomMenu.findItem(R.id.doctor)?.isVisible = false
        bottomMenu.findItem(R.id.addLeave)?.isVisible = false
        bottomMenu.findItem(R.id.profile)?.isVisible = false
        bottomMenu.findItem(R.id.notification)?.isVisible = false
    }

    private fun setupEmployeeBottomNavigation(bottomMenu: android.view.Menu) {
        // Employee: Home, addLeave, Profile and notification
        bottomMenu.findItem(R.id.home)?.isVisible = true
        bottomMenu.findItem(R.id.addLeave)?.isVisible = true
        bottomMenu.findItem(R.id.profile)?.isVisible = true
        bottomMenu.findItem(R.id.notification)?.isVisible = true
    }

    private fun setupManagerBottomNavigation(bottomMenu: android.view.Menu) {
        // Manager: Home, addLeave, Profile and notification
        bottomMenu.findItem(R.id.home)?.isVisible = true
        bottomMenu.findItem(R.id.addLeave)?.isVisible = true
        bottomMenu.findItem(R.id.profile)?.isVisible = true
        bottomMenu.findItem(R.id.notification)?.isVisible = true
    }

    private fun setupHRBottomNavigation(bottomMenu: android.view.Menu) {
        // HR: Home, Doctor is Available, addLeave, Profile, notification
        bottomMenu.findItem(R.id.home)?.isVisible = true
        bottomMenu.findItem(R.id.doctor)?.isVisible = true
        bottomMenu.findItem(R.id.addLeave)?.isVisible = true
        bottomMenu.findItem(R.id.profile)?.isVisible = true
        bottomMenu.findItem(R.id.notification)?.isVisible = true
    }

    private fun setupHRDBottomNavigation(bottomMenu: android.view.Menu) {
        // HRD: Home, addLeave, Profile and notification
        bottomMenu.findItem(R.id.home)?.isVisible = true
        bottomMenu.findItem(R.id.addLeave)?.isVisible = true
        bottomMenu.findItem(R.id.profile)?.isVisible = true
        bottomMenu.findItem(R.id.notification)?.isVisible = true
    }

    private fun getUserRole(): String {
        val prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        return prefs.getString("role", "employee") ?: "employee"
    }

    private fun isHR(): Boolean = getUserRole() == "HR"
    private fun isManager(): Boolean = getUserRole() == "Manager"
    private fun isHRD(): Boolean = getUserRole() == "HRD"
    private fun isEmployee(): Boolean = getUserRole() == "Employee"


    private fun showAccessDenied() {
        Toast.makeText(this, "Access denied for your role", Toast.LENGTH_SHORT).show()
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_layout, fragment)

        if (addToBackStack && fragment !is Homefra) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_layout)
            // If the current fragment is not Homefra and there are fragments in the back stack
            if (currentFragment !is Homefra && supportFragmentManager.backStackEntryCount > 0) {
                // Navigate back to Homefra by popping all fragments up to and including Homefra
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                replaceFragment(Homefra(), false) // Ensure Homefra is the current fragment and not added to stack
            } else {
                // If on Homefra or back stack is empty, allow default back press (exit app)
                super.onBackPressed()
            }
        }
    }
    private fun updateToolbarForFragment() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_layout)
        if (currentFragment is Homefra) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            toggle.isDrawerIndicatorEnabled = true
            toggle.syncState()
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } else {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            toggle.isDrawerIndicatorEnabled = false
            toggle.syncState()
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
    }
    private fun logoutUser() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            // Clear session and navigate to login
            val prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }
    }