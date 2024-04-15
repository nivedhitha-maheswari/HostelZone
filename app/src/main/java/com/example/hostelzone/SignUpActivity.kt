package com.example.hostelzone

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.hostelzone.databinding.ActivitySignUpBinding
import com.google.firebase.database.*

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("users")

        // Set up the spinner with user types
        val userTypeAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.user_types,
            android.R.layout.simple_spinner_item
        )
        userTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.userTypeSpinner.adapter = userTypeAdapter

        binding.proceedbutton.setOnClickListener {
            val signupUsername = binding.signUpUsername.text.toString()
            val signupPassword = binding.signUpPassword.text.toString()

            // Call signupUser function with username, password, and selected user type
            signupUser(signupUsername, signupPassword)
        }

        binding.loginRedirect.setOnClickListener {
            startActivity(Intent(this@SignUpActivity, SignInActivity::class.java))
            finish()
        }
    }

    private fun signupUser(username: String, password: String) {
        val userType = binding.userTypeSpinner.selectedItem.toString()
        val additionalData: MutableMap<String, Any>? = mutableMapOf()

        if (username.isNotEmpty() && password.isNotEmpty()) {
            databaseReference.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            val id = databaseReference.push().key

                            val userData = UserData(id, username, password, userType, additionalData)
                            databaseReference.child(id!!).setValue(userData)

                            // Create additionalData based on user type
                            when (userType) {
                                "Inmate" -> {
                                    additionalData?.put("inmateData", "Additional data for Inmate")
                                }
                                "Tutor" -> {
                                    additionalData?.put("tutorData", "Additional data for Tutor")
                                }
                                "Supervisor" -> {
                                    additionalData?.put("supervisorData", "Additional data for Supervisor")
                                }
                                "Warden" -> {
                                    additionalData?.put("wardenData", "Additional data for Warden")
                                }
                            }

                            // Update userData in the database
                            databaseReference.child(id).setValue(userData)

                            // Start the appropriate activity
                            val intent = when (userType) {
                                "Inmate" -> Intent(this@SignUpActivity, InmateSignUpActivity::class.java)
                                "Tutor" -> Intent(this@SignUpActivity, TutorSignUpActivity::class.java)
                                else -> null
                            }

                            intent?.apply {
                                putExtra("username", username)
                                putExtra("password", password)
                                startActivity(this)
                            }
                            finish()
                        } else {
                            Toast.makeText(
                                this@SignUpActivity,
                                "User already exists",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(
                            this@SignUpActivity,
                            "Database Error: ${databaseError.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        } else {
            Toast.makeText(this@SignUpActivity, "All fields are mandatory", Toast.LENGTH_SHORT).show()
        }
    }
}
