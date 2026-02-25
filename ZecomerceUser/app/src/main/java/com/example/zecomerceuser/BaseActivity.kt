package com.example.zecomerceuser

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    private var noInternetLayout: RelativeLayout? = null
    private var btnRetry: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        checkInternet()
    }

    fun checkInternet() {
        if (!NetworkUtils.isInternetAvailable(this)) {
            showNoInternetLayout()
        } else {
            hideNoInternetLayout()
        }
    }

    private fun showNoInternetLayout() {
        if (noInternetLayout == null) {
            noInternetLayout = findViewById(R.id.noInternetLayout)
            btnRetry = findViewById(R.id.btnRetryInternet)

            btnRetry?.setOnClickListener {
                checkInternet()
            }
        }
        noInternetLayout?.visibility = View.VISIBLE
    }

    private fun hideNoInternetLayout() {
        noInternetLayout?.visibility = View.GONE
    }
}
