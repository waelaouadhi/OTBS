package com.example.onetechbs

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.onetechbs.databinding.ActivityFtpBinding


class ftp : AppCompatActivity() {
    private lateinit var binding: ActivityFtpBinding  // ViewBinding instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inflate ViewBinding
        binding = ActivityFtpBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // ✅ Handle Button Click - Navigate to Next Activity
        binding.send.setOnClickListener {  // Use the correct button ID
            val intent = Intent(this, OTP::class.java) // Replace with your actual activity
            startActivity(intent)
        }

        // Apply window insets correctly
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}