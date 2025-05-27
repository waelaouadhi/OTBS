package com.example.onetechbs

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.onetechbs.databinding.ActivityNewPasswordBinding

class NewPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivityNewPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the submit button click listener
        binding.submitNewPasswordBtn.setOnClickListener {
            val newPassword = binding.newPassword.text.toString()
            val confirmPassword = binding.confirmPassword.text.toString()

            if (newPassword == confirmPassword) {
                // Optionally: handle password change logic (e.g., save to database, update user data)

                // Show success message
                Toast.makeText(this, "Password changed successfully!", Toast.LENGTH_SHORT).show()

                // Navigate to HomeActivity
                val intent = Intent(this, home::class.java) // Replace with your actual HomeActivity class
                startActivity(intent)

                // Finish the current activity so the user can't navigate back to the new password screen
                finish()
            } else {
                // Show error message if passwords don't match
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}