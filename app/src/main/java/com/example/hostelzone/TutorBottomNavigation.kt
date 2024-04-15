package com.example.hostelzone

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class TutorBottomNavigation : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tutor_bottom_navigation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val logout_button = findViewById<ImageView>(R.id.logout_button)
        logout_button.setOnClickListener {
            logout()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.tutor_bottom_navigation)

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bottom_home -> {
                    navigateToFragment(HomeFragment())
                    true
                }
                R.id.bottom_new_request -> {
                    navigateToFragment(TutorPendingRequestFragment())
                    true
                }
                R.id.bottom_request_list -> {
                    navigateToFragment(RequestListFragment())
                    true
                }
                else -> false
            }
        }

        // Initially select the home fragment
        bottomNavigationView.selectedItemId = R.id.bottom_home
    }

    private fun navigateToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun logout() {
        val sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        // Navigate to login screen
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish() // Finish the current activity to prevent going back to the profile screen
    }

    private fun AppCompatActivity.enableEdgeToEdge() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }
}
