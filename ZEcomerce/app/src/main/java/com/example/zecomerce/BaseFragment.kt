package com.example.zecomerce

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {

    private var noInternetLayout: RelativeLayout? = null
    private var btnRetry: Button? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initNoInternetLayout(view)
        checkInternet()
    }

    override fun onResume() {
        super.onResume()
        checkInternet()
    }

    private fun initNoInternetLayout(view: View) {
        noInternetLayout = view.findViewById(R.id.noInternetLayout)
        btnRetry = view.findViewById(R.id.btnRetryInternet)
        btnRetry?.setOnClickListener {
            checkInternet()
        }
    }

    private fun checkInternet() {
        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            noInternetLayout?.visibility = View.VISIBLE
        } else {
            noInternetLayout?.visibility = View.GONE
        }
    }
}
