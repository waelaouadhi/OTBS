package com.example.onetechbs

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_splash) // Make sure this is your layout

        setupFullScreen()

        // ✅ Animate fade-out of splash screen layout
        val splashScreenView = findViewById<View>(R.id.main)
        splashScreenView.alpha = 1f

        lifecycleScope.launch {
            delay(3000) // Show splash for 3 seconds
            splashScreenView.animate().alpha(0f).duration = 1000 // 1-second fade out
            delay(1000) // Wait for fade-out
            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupFullScreen() {
        val rootView = findViewById<View>(R.id.main)
        rootView?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        } ?: run {
            Log.e("SplashActivity", "Root view (main) is null. Check your XML layout.")
        }
    }
}
