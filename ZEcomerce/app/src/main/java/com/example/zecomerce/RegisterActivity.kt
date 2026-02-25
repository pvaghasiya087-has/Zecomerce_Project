package com.example.zecomerce

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvError: TextView
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance() // Initialize FirebaseAuth

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvError = findViewById(R.id.tvError)
        tvLogin = findViewById(R.id.tvloginAc)
    }

    private fun setupListeners() {
        btnRegister.setOnClickListener {
            validateAndRegister()
        }

        tvLogin.setOnClickListener {
            // Navigate to Login screen
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateAndRegister() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        tvError.text = ""

        when {
            email.isEmpty() -> {
                tvError.text = "Email is required"
                etEmail.requestFocus()
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                tvError.text = "Enter a valid email address"
                etEmail.requestFocus()
            }

            password.isEmpty() -> {
                tvError.text = "Password is required"
                etPassword.requestFocus()
            }

            password.length < 6 -> {
                tvError.text = "Password must be at least 6 characters"
                etPassword.requestFocus()
            }

            confirmPassword.isEmpty() -> {
                tvError.text = "Please confirm your password"
                etConfirmPassword.requestFocus()
            }

            password != confirmPassword -> {
                tvError.text = "Passwords do not match"
                etConfirmPassword.requestFocus()
            }

            else -> {
                // Register with Firebase
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Signup Success", Toast.LENGTH_SHORT).show()
                            Log.d("AUTH", "Signup successful: ${auth.currentUser?.email}")
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Signup Failed: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("AUTH", "Signup error", task.exception)
                        }
                    }
            }
        }
    }
}
