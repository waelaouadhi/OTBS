package com.example.onetechbs

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.onetechbs.databinding.ActivityOtpBinding

class OTP : AppCompatActivity() {
    private lateinit var binding: ActivityOtpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Use ViewBinding
        binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle system insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Submit button logic
        binding.submitBtn.setOnClickListener {
            val code = binding.otp1.text.toString() +
                    binding.otp2.text.toString() +
                    binding.otp3.text.toString() +
                    binding.otp4.text.toString()

            if (code.length == 4) {
                // Optional: Show OTP code
                Toast.makeText(this, "Entered OTP: $code", Toast.LENGTH_SHORT).show()

                // Navigate to NewPasswordActivity
                val intent = Intent(this, NewPasswordActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Please enter all 4 digits", Toast.LENGTH_SHORT).show()
            }
        }
    }
}