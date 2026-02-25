package com.example.zecomerce

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen splash
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(R.layout.activity_splash)

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val progressBar2 = findViewById<ProgressBar>(R.id.progressBar2)

        ObjectAnimator.ofInt(progressBar2, "progress", 0, 100).apply {
            duration = 5500
            interpolator = DecelerateInterpolator()
            start()
        }

        ObjectAnimator.ofInt(progressBar, "progress", 0, 100).apply {
            duration = 3000
            interpolator = DecelerateInterpolator()
            start()
        }

        // Delay for splash animation
        Handler(Looper.getMainLooper()).postDelayed({
            navigateNext()
        }, 3500)
    }

    private fun navigateNext() {
        val user = FirebaseAuth.getInstance().currentUser

        val intent = if (user == null) {
            // User NOT logged in → Login screen
            Intent(this, LoginActivity::class.java)
        } else {
            // User logged in → Main screen
            Intent(this, MainActivity2::class.java)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
