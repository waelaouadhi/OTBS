package com.example.onetechbs

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.onetechbs.databinding.ActivityHomeBinding
import com.google.android.material.navigation.NavigationView

class home : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        drawerLayout = binding.drawerLayout
        navigationView = binding.navigationView

        setupBottomNavigation()
        applyRoleBasedUI()

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.open_drawer, R.string.close_drawer
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        setupDrawerNavigation()
    }

    private fun setupBottomNavigation() {
        replaceFragment(Homefra()) // Default fragment

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> replaceFragment(Homefra())
                R.id.addLeave -> replaceFragment(AddLeaveFragment())
                R.id.doctor -> replaceFragment(DoctorFragment())
                R.id.training -> replaceFragment(TrainingFragment())
                R.id.notification -> replaceFragment(NotificationFragment())
            }
            true
        }
    }

    private fun setupDrawerNavigation() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> replaceFragment(Homefra())
                R.id.nav_leaves -> replaceFragment(LeaveListFragment())
                R.id.nav_doctor -> replaceFragment(DoctorListFragment())
                R.id.nav_training -> replaceFragment(TrainingListFragment())
                R.id.nav_notification -> replaceFragment(NotificationFragment())
                R.id.nav_available_jobs -> replaceFragment(AvailableJobsFragment())

                R.id.nav_internal_recruiting -> {
                    if (isHR()) replaceFragment(InternalRecruitingFragment())
                    else showAccessDenied()
                }

                R.id.nav_review_applications -> {
                    if (isHR()) replaceFragment(ReviewApplicationsFragment())
                    else showAccessDenied()
                }

                R.id.nav_doctor_management -> {
                    if (isHR()) replaceFragment(DoctorManagementFragment())
                    else showAccessDenied()
                }

                R.id.nav_training_management -> {
                    if (isManager()) replaceFragment(TrainingManagementFragment())
                    else showAccessDenied()
                }

                R.id.nav_leaves_management -> {
                    if (isManager()) replaceFragment(HRLeaveManagementFragment())
                    else showAccessDenied()
                }
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun applyRoleBasedUI() {
        val role = getUserRole()

        val navMenu = navigationView.menu

        // Default: hide all role-specific items
        navMenu.findItem(R.id.nav_internal_recruiting)?.isVisible = false
        navMenu.findItem(R.id.nav_review_applications)?.isVisible = false
        navMenu.findItem(R.id.nav_doctor_management)?.isVisible = false
        navMenu.findItem(R.id.nav_training_management)?.isVisible = false
        navMenu.findItem(R.id.nav_leaves_management)?.isVisible = false

        // Bottom nav role-specific visibility
        binding.bottomNavigationView.menu.findItem(R.id.training)?.isVisible = role == "Manager"
        binding.bottomNavigationView.menu.findItem(R.id.doctor)?.isVisible = role == "HR"

        // Show items based on role
        if (role == "HR") {
            navMenu.findItem(R.id.nav_internal_recruiting)?.isVisible = true
            navMenu.findItem(R.id.nav_review_applications)?.isVisible = true
            navMenu.findItem(R.id.nav_doctor_management)?.isVisible = true
        } else if (role == "Manager") {
            navMenu.findItem(R.id.nav_training_management)?.isVisible = true
            navMenu.findItem(R.id.nav_leaves_management)?.isVisible = true
        }
    }

    private fun getUserRole(): String {
        val prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        return prefs.getString("role", "employee") ?: "employee"
    }

    private fun isHR(): Boolean = getUserRole() == "HR"
    private fun isManager(): Boolean = getUserRole() == "Manager"

    private fun showAccessDenied() {
        Toast.makeText(this, "Access denied for your role", Toast.LENGTH_SHORT).show()
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_layout, fragment)
            .commit()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}