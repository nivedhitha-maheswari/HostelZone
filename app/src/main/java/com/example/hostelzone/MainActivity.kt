package com.example.hostelzone

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    // Database reference
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().reference

        // Delayed action to navigate to SignInActivity after 3 seconds
        Handler().postDelayed({
            if (isLoggedIn()) {
                redirectToDashboard()
            } else {
                startActivity(Intent(this, SignInActivity::class.java))
                finish() // Finish the MainActivity only if not redirected
            }
        }, 1500)

        // Rest of your code remains the same...
    }

    private fun isLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.contains("userId")
        Log.d("MainActivity", "isLoggedIn: $isLoggedIn")
        return isLoggedIn
    }


    private fun redirectToDashboard() {
        // Redirect to the dashboard activity based on user type
        val userId = getSharedPreferences("myPrefs", MODE_PRIVATE).getString("userId", null)
        val userType = getSharedPreferences("myPrefs", MODE_PRIVATE).getString("userType", null)


        if (userId != null && userType != null) {
            val intent = when (userType) {
                "Inmate" -> Intent(this, BottomNavigation::class.java)
                "Tutor" -> Intent(this, TutorBottomNavigation::class.java)
                else -> Intent(this, MainActivity::class.java) // Replace with appropriate activity
            }
            startActivity(intent)
            finish() // Finish the MainActivity
        }
    }
}

