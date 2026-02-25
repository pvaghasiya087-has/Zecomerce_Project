package com.example.zecomerceuser

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etFullName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvError: TextView
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
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
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateAndRegister() {
        val fullName = etFullName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        tvError.text = ""

        when {
            fullName.isEmpty() -> showError("Full name is required", etFullName)
            !fullName.matches(Regex("^[a-zA-Z ]{8,}$")) ->
                showError("Full name must contain only letters and minimum 8 characters", etFullName)

            phone.isEmpty() -> showError("Phone number is required", etPhone)
            !phone.matches(Regex("^[0-9]{10}$")) -> showError("Enter valid 10-digit phone number", etPhone)

            email.isEmpty() -> showError("Email is required", etEmail)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showError("Enter a valid email address", etEmail)

            password.isEmpty() -> showError("Password is required", etPassword)
            !password.matches(
                Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@\$!%*#?&])[A-Za-z\\d@\$!%*#?&]{8,}$")
            ) -> showError(
                "Password must be at least 8 characters and include letter, number & special character",
                etPassword
            )


            confirmPassword.isEmpty() -> showError("Please confirm your password", etConfirmPassword)
            password != confirmPassword -> showError("Passwords do not match", etConfirmPassword)

            else -> registerUser(fullName, phone, email, password, confirmPassword)
        }
    }

    private fun showError(message: String, field: EditText) {
        tvError.text = message
        field.requestFocus()
    }

    private fun registerUser(
        fullName: String,
        phone: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        btnRegister.isEnabled = false

        // 1️⃣ Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user!!
                val uid = firebaseUser.uid

                // 2️⃣ Firestore – NEW DOCUMENT in userdetail
                val userData = hashMapOf(
                    "uid" to uid,
                    "fullName" to fullName,
                    "phone" to phone,
                    "email" to email,
                    "password" to password,
                    "confirmPassword" to confirmPassword,
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("userdetail")
                    .document(uid) // NEW DOCUMENT
                    .set(userData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        // ❌ Rollback auth user if Firestore fails
                        firebaseUser.delete()
                        tvError.text = "Failed to save user data"
                        btnRegister.isEnabled = true
                    }
            }
            .addOnFailureListener {
                tvError.text = it.message
                btnRegister.isEnabled = true
            }
    }
}
