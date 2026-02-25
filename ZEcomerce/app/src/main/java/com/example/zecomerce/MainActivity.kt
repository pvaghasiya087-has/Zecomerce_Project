package com.example.zecomerce

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth





class MainActivity : AppCompatActivity() {

    private lateinit var auth:FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        auth= FirebaseAuth.getInstance()

        val email=findViewById<EditText>(R.id.editTextText)
        val password=findViewById<EditText>(R.id.editTextText2)
        val signButton=findViewById<Button>(R.id.btnSignup)
        val GsignButton=findViewById<Button>(R.id.btnGoogle)

        signButton.setOnClickListener{
            var uemail:String=email.text.toString().trim()
            var upassword:String=password.text.toString().trim()
            auth.createUserWithEmailAndPassword(uemail,upassword).addOnCompleteListener{ task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Signup Success", Toast.LENGTH_SHORT).show()
                    Log.d("AUTH", "Signup successful: ${auth.currentUser?.email}")
                } else {
                    Toast.makeText(this, "Signup Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    Log.e("AUTH", "Signup error", task.exception)
                }
            }
        }


        val btnLogin=findViewById<CustomButton>(R.id.btnLogin)
        btnLogin.setOnClickListener{
           var uemail:String=email.text.toString().trim()
           var upassword:String=password.text.toString().trim()
            auth.signInWithEmailAndPassword(uemail,upassword).addOnCompleteListener{ task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login Success", Toast.LENGTH_SHORT).show()
                    Log.d("AUTH", "Login successful: ${auth.currentUser?.email}")
                    var i=Intent(this, List_menu::class.java)
                    startActivity(i)
                } else {
                    Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    Log.e("AUTH", "Login error", task.exception)
                }         }
        }



    }
    }
