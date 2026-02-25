package com.example.zecomerce

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

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Make BottomNavigationView respect system gesture / safe area
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            view.updatePadding(
                bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
            insets
        }

        // Default fragment
        replaceFragment(ItemsFragment())

        // Bottom navigation item selection
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_items -> replaceFragment(ItemsFragment())
                R.id.nav_orders -> replaceFragment(OrderFragment())
                R.id.nav_offers -> replaceFragment(OfferFragment())
                R.id.nav_profile -> replaceFragment(ProfileFragment())
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
