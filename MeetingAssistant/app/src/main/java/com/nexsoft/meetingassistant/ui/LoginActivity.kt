package com.nexsoft.meetingassistant.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nexsoft.meetingassistant.api.ApiClient
import com.nexsoft.meetingassistant.databinding.ActivityLoginBinding
import com.nexsoft.meetingassistant.models.LoginRequest
import com.nexsoft.meetingassistant.models.LoginResponse
import com.nexsoft.meetingassistant.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Check if already logged in
        if (sessionManager.isLoggedIn()) {
            ApiClient.setToken(sessionManager.getToken())
            navigateToDashboard()
            return
        }

        // Password toggle is now handled by TextInputLayout in XML
        setupLoginButton()
    }



    private fun setupLoginButton() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString()?.trim() ?: ""

            if (username.isEmpty()) {
                binding.etUsername.error = "Username harus diisi"
                binding.etUsername.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.etPassword.error = "Password harus diisi"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }

            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Loading..."

            val loginRequest = LoginRequest(username, password)
            ApiClient.apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Login"

                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse != null) {
                            sessionManager.saveAuthData(
                                token = loginResponse.accessToken,
                                role = loginResponse.role,
                                userId = loginResponse.userId,
                                userName = loginResponse.name
                            )
                            ApiClient.setToken(loginResponse.accessToken)
                            navigateToDashboard()
                        } else {
                            Toast.makeText(this@LoginActivity, "Login gagal: Response kosong", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorMsg = try {
                            response.errorBody()?.string() ?: "Login gagal"
                        } catch (e: Exception) {
                            "Login gagal: ${response.code()}"
                        }
                        Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Login"
                    Toast.makeText(this@LoginActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }
}
