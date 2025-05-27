package com.example.onetechbs

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.onetechbs.db.AuthRequest
import com.example.onetechbs.db.EmployeeResponse
import com.example.onetechbs.db.JwtResponse
import com.example.onetechbs.network.RetrofitClient
import com.example.onetechbs.util.SharedPreferencesManager
import com.example.onetechbs.websocket.WebSocketService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var forgotPwdButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameEditText = findViewById(R.id.outlinedTextField)
        passwordEditText = findViewById(R.id.editTextTextPassword)
        loginButton = findViewById(R.id.login)
        forgotPwdButton = findViewById(R.id.forgotpwd)

        // Prevent spaces in username input
        usernameEditText.filters = arrayOf(InputFilter { source, _, _, _, _, _ ->
            if (source.contains(" ")) "" else null
        })

        loginButton.setOnClickListener {
            if (validateInputs()) {
                val username = usernameEditText.text.toString().trim()
                val password = passwordEditText.text.toString()

                val authRequest = AuthRequest(username = username, password = password)

                RetrofitClient.authService.login(authRequest).enqueue(object : Callback<JwtResponse> {
                    override fun onResponse(call: Call<JwtResponse>, response: Response<JwtResponse>) {
                        if (response.isSuccessful) {
                            val jwtResponse = response.body()
                            val spm = SharedPreferencesManager.getInstance(this@LoginActivity)
                            jwtResponse?.let { jwt ->
                                spm.saveAuthToken(jwt.accessToken ?: "", jwt.accessExpiration ?: 0)
                                spm.saveRefreshToken(jwt.refreshToken ?: "", jwt.refreshExpiration ?: 0)
                            }

                            RetrofitClient.setAuthToken(spm.getAuthToken() ?: "")

                            val prefs = getSharedPreferences("auth", MODE_PRIVATE)
                            prefs.edit().apply {
                                putString("accessToken", jwtResponse?.accessToken)
                                putString("refreshToken", jwtResponse?.refreshToken)
                                putLong("accessExpiration", jwtResponse?.accessExpiration ?: 0)
                                putLong("refreshExpiration", jwtResponse?.refreshExpiration ?: 0)

                                apply()
                            }

                            RetrofitClient.employeeService.getEmployeeByUsername(username).enqueue(object : Callback<EmployeeResponse> {
                                override fun onResponse(call: Call<EmployeeResponse>, response: Response<EmployeeResponse>) {
                                    if (response.isSuccessful) {
                                        val employee = response.body()
                                        prefs.edit().apply {
                                            putString("firstName", employee?.firstName)
                                            putString("lastName", employee?.lastName)
                                            putString("email", employee?.email)
                                            putString("jobTitle", employee?.jobTitle)
                                            putString("role", employee?.role)
                                            putString("username", employee?.username)
                                            apply()
                                        }
                                        Log.d("LoginActivity", "Starting WebSocketService with token: ${jwtResponse?.accessToken}")
                                        startWebSocketService(jwtResponse?.accessToken ?: "")
                                        startActivity(Intent(this@LoginActivity, home::class.java))
                                        finish()
                                    } else {
                                        Toast.makeText(this@LoginActivity, "Failed to fetch employee data", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onFailure(call: Call<EmployeeResponse>, t: Throwable) {
                                    Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                                }
                            })

                        } else {
                            Toast.makeText(this@LoginActivity, "Login failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<JwtResponse>, t: Throwable) {
                        Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        Log.e("LoginActivity", "Login error: ${t.message}", t)
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
