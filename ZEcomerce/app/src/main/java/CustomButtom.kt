package com.example.zecomerce
import android.content.Context
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import androidx.appcompat.widget.AppCompatButton

class CustomButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatButton(context, attrs) {

    init {
        // Apply background
        background = context.getDrawable(R.drawable.bg_custom_button)

        // Set click listener to play animation
        setOnClickListener {
            val anim = AnimationUtils.loadAnimation(context, R.anim.button_click)
            startAnimation(anim)

            // Optional: perform click action after animation
            performClick()
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        // Custom logic on click if needed
        return true
    }
}
