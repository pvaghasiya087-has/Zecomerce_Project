package com.example.zecomerceuser


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Make BottomNavigationView respect system gesture / safe area
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            view.updatePadding(
                bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
            insets
        }

        // Bottom navigation item selection
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_orders -> loadFragment(OrderFragment())
                R.id.nav_cart -> loadFragment(CartFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }
        // 🔥 HANDLE INTENT FROM ProceedOrderActivity
        val openOrders = intent.getBooleanExtra("OPEN_ORDERS", false)
        val openCart = intent.getBooleanExtra("OPEN_CART", false)


        if (openOrders) {
            bottomNav.selectedItemId = R.id.nav_orders
        }
        else if (openCart) {
            bottomNav.selectedItemId = R.id.nav_cart
        }
        else {
            bottomNav.selectedItemId = R.id.nav_home
        }

    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
