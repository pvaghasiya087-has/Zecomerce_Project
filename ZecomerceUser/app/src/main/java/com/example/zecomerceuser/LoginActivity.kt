package com.example.zecomerceuser


import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : BaseActivity() {
    private lateinit var auth:FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth= FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val tvError = findViewById<TextView>(R.id.tvError)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

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

                else -> {

                    auth.signInWithEmailAndPassword(email,password).addOnCompleteListener{ task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Login Success", Toast.LENGTH_SHORT).show()
                            Log.d("AUTH", "Login successful: ${auth.currentUser?.email}")
                            // TODO: Navigate to HomeActivity
                            startActivity(Intent(this, MainActivity2::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            Log.e("AUTH", "Login error", task.exception)
                        }         }




                }
            }
        }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()

            // startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}
