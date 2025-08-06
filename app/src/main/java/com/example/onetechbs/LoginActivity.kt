package com.example.onetechbs

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.onetechbs.db.AuthRequest
import com.example.onetechbs.db.EmployeeResponse
import com.example.onetechbs.db.JwtResponse
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import com.example.onetechbs.websocket.WebSocketService
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var forgotPwdButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var blurOverlay: View

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameEditText = findViewById(R.id.outlinedTextField)
        passwordEditText = findViewById(R.id.editTextTextPassword)
        loginButton = findViewById(R.id.login)
        forgotPwdButton = findViewById(R.id.forgotpwd)
        progressBar = findViewById(R.id.loginProgressBar)
        blurOverlay = findViewById(R.id.blurOverlay)

        // Prevent spaces in username input
        usernameEditText.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
            if (source.contains(" ")) "" else null
        })

        loginButton.setOnClickListener {
            if (validateInputs()) {
                progressBar.visibility = View.VISIBLE
                blurOverlay.visibility = View.VISIBLE
                loginButton.isEnabled = false
                forgotPwdButton.isEnabled = false
                forgotPwdButton.alpha = 0.5f
                forgotPwdButton.isEnabled = false
                forgotPwdButton.alpha = 0.5f

                val username = usernameEditText.text.toString().trim()
                val password = passwordEditText.text.toString()

                val authRequest = AuthRequest(username = username, password = password)

                RetrofitClient.authService.login(authRequest).enqueue(object : Callback<JwtResponse> {
                    override fun onResponse(call: Call<JwtResponse>, response: Response<JwtResponse>) {
                        progressBar.visibility = View.GONE
                        blurOverlay.visibility = View.GONE
                        loginButton.isEnabled = true
                        if (response.isSuccessful) {
                            val jwtResponse = response.body()
                            val spm = SharedPreferencesManager.getInstance(this@LoginActivity)

                            jwtResponse?.let { jwt ->
                                val now = System.currentTimeMillis()
                                val accessExpirationTimestamp = now + (jwt.accessExpiration ?: 0L) // expiration duration in ms
                                val refreshExpirationTimestamp = now + (jwt.refreshExpiration ?: 0L)

                                spm.saveAuthToken(jwt.accessToken ?: "", accessExpirationTimestamp)
                                spm.saveRefreshToken(jwt.refreshToken ?: "", refreshExpirationTimestamp)

                                Log.d("LoginActivity", "Saved accessToken: ${jwt.accessToken}")
                                Log.d("LoginActivity", "Saved accessExpiration (timestamp): $accessExpirationTimestamp")
                                Log.d("LoginActivity", "Saved refreshToken: ${jwt.refreshToken}")
                                Log.d("LoginActivity", "Saved refreshExpiration (timestamp): $refreshExpirationTimestamp")
                            }

                            RetrofitClient.setAuthToken(spm.getAuthToken() ?: "")

                            val prefs = getSharedPreferences("auth", MODE_PRIVATE)
                            prefs.edit().apply {
                                putString("accessToken", jwtResponse?.accessToken)
                                putString("refreshToken", jwtResponse?.refreshToken)
                                putLong("accessExpiration", System.currentTimeMillis() + (jwtResponse?.accessExpiration ?: 0L))
                                putLong("refreshExpiration", System.currentTimeMillis() + (jwtResponse?.refreshExpiration ?: 0L))
                                apply()
                            }

                            RetrofitClient.employeeService.getEmployeeByUsername(username).enqueue(object : Callback<EmployeeResponse> {
                                override fun onResponse(call: Call<EmployeeResponse>, response: Response<EmployeeResponse>) {
                                    if (response.isSuccessful) {
                                        val employee = response.body()
                                        prefs.edit().apply {
                                            putString("userId", employee?.id) // Save employee ID as String
                                            putString("firstName", employee?.firstName)
                                            putString("lastName", employee?.lastName)
                                            putString("email", employee?.email)
                                            putString("jobTitle", employee?.jobTitle)
                                            putString("role", employee?.role)
                                            putString("username", employee?.username)
                                            apply()
                                        }
                                        // Save user role to SharedPreferencesManager for role-based UI
                                        employee?.role?.let { role ->
                                            SharedPreferencesManager.getInstance(this@LoginActivity).saveUserRole(role)
                                            Log.d("LoginActivity", "Saved user role to SharedPreferencesManager: $role")
                                        }
                                        Log.d("LoginActivity", "Starting WebSocketService with token: ${jwtResponse?.accessToken}")
                                        startWebSocketService(jwtResponse?.accessToken ?: "")
                                        startActivity(Intent(this@LoginActivity, home::class.java))
                                        finish()
                                    } else {
                                        Snackbar.make(loginButton, "Failed to fetch employee data", Snackbar.LENGTH_LONG)
                                            .setBackgroundTint(ContextCompat.getColor(this@LoginActivity, com.google.android.material.R.color.design_default_color_error))
                                            .show()
                                    }
                                }

                                override fun onFailure(call: Call<EmployeeResponse>, t: Throwable) {
                                    Snackbar.make(loginButton, "Error: ${t.message}", Snackbar.LENGTH_LONG)
                                        .setBackgroundTint(ContextCompat.getColor(this@LoginActivity, com.google.android.material.R.color.design_default_color_error))
                                        .show()
                                }
                            })

                        } else {
                            Snackbar.make(loginButton, "Incorrect username or password", Snackbar.LENGTH_LONG)
                                .setBackgroundTint(ContextCompat.getColor(this@LoginActivity, com.google.android.material.R.color.design_default_color_error))
                                .show()
                            passwordEditText.text?.clear()
                        }
                    }

                    override fun onFailure(call: Call<JwtResponse>, t: Throwable) {
                        progressBar.visibility = View.GONE
                        blurOverlay.visibility = View.GONE
                        loginButton.isEnabled = true
                        forgotPwdButton.isEnabled = true
                        forgotPwdButton.alpha = 1.0f
                        Snackbar.make(loginButton, "Network error: ${t.localizedMessage}", Snackbar.LENGTH_LONG)
                            .setBackgroundTint(ContextCompat.getColor(this@LoginActivity, com.google.android.material.R.color.design_default_color_error))
                            .show()
                    }
                })
            }
        }

        forgotPwdButton.setOnClickListener {
            val intent = Intent(this, ftp::class.java)
            startActivity(intent)
        }
    }

    private fun validateInputs(): Boolean {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        if (username.isEmpty()) {
            usernameEditText.error = "Username is required"
            return false
        }

        if (username.contains(" ")) {
            usernameEditText.error = "Username must not contain spaces"
            return false
        }

        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            return false
        }

        if (password.length < 6) {
            passwordEditText.error = "Password must be at least 6 characters"
            return false
        }

        return true
    }
    private fun startWebSocketService(accessToken: String) {
        val serviceIntent = Intent(this, WebSocketService::class.java)
        serviceIntent.putExtra("authToken", accessToken)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}