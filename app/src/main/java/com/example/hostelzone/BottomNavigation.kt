package com.example.hostelzone

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class BottomNavigation : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_navigation)

        bottomNavigationView = findViewById(R.id.bottom_navigation)


        val logout_button = findViewById<ImageView>(R.id.logout_button)
        logout_button.setOnClickListener{
           logout()
        }

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {

                R.id.bottom_home -> {
                    navigateToFragment(HomeFragment())
                    true
                }
                R.id.bottom_greivances -> {
                    navigateToFragment(GrievancesFragment())
                    true
                }
                R.id.bottom_labpermission -> {
                    navigateToFragment(LabPermissionFragment())
                    true
                }
                R.id.bottom_profile -> {
                    navigateToFragment(InmateProfileFragment())
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
        finish()
    }
}
