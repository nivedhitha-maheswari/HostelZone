package com.example.hostelzone

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class NewGrievanceFragment : Fragment() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_new_grievance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().reference

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

        // Find views
        val grievanceEditText = view.findViewById<EditText>(R.id.grievanceEditText)
        val submitButton = view.findViewById<Button>(R.id.submitGrievanceButton)

        // Handle submit button click
        submitButton.setOnClickListener {
            val grievance = grievanceEditText.text.toString().trim()
            if (grievance.isNotEmpty()) {
                // Retrieve the userId from SharedPreferences
                val userId = sharedPreferences.getString("userId", "")
                if (!userId.isNullOrEmpty()) {
                    // Proceed with fetching user data and submitting the grievance
                    fetchUserDataAndSubmitGrievance(userId, grievance)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "User ID not found in SharedPreferences",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please enter a grievance", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun fetchUserDataAndSubmitGrievance(userId: String, grievance: String) {
        // Construct database reference to the user node
        val userReference = FirebaseDatabase.getInstance().reference.child("users").child(userId)

        // Retrieve user data
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Check if user data exists
                if (dataSnapshot.exists()) {
                    // Retrieve roll number from user data
                    val rollNumber = dataSnapshot.child("additionalData").child("rollNumber")
                        .getValue(String::class.java)

                    if (!rollNumber.isNullOrEmpty()) {
                        // Proceed with submitting the grievance
                        submitGrievance(userId,rollNumber, grievance)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Roll number not found for the user",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "User data not found in the database",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Error fetching user data: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

   private fun submitGrievance(userId: String,rollNumber: String, grievance: String) {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentDate: String = sdfDate.format(Date())
        val currentTime: String = sdfTime.format(Date())

        // Retrieve degree, course, year, and group from the user data
        val userReference = FirebaseDatabase.getInstance().reference.child("users").child(userId)
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val degree = dataSnapshot.child("additionalData").child("degree").getValue(String::class.java)
                val course = dataSnapshot.child("additionalData").child("course").getValue(String::class.java)
                val year = dataSnapshot.child("additionalData").child("year").getValue(String::class.java)
                val group = dataSnapshot.child("additionalData").child("group").getValue(String::class.java)

                if (degree != null && course != null && year != null && group != null) {
                    val grievanceId = databaseReference
                        .child("grievances")
                        .child(degree)
                        .child(course)
                        .child(year)
                        .child(group)
                        .push()
                        .key

                    val grievanceData = mapOf(
                        "grievanceId" to grievanceId,
                        "rollNumber" to rollNumber,
                        "date" to currentDate,
                        "time" to currentTime,
                        "grievance" to grievance,
                        "status" to "pending"
                    )

                    grievanceId?.let {
                        databaseReference
                            .child("grievances")
                            .child(degree)
                            .child(course)
                            .child(year)
                            .child(group)
                            .child(it)
                            .setValue(grievanceData)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Grievance submitted successfully", Toast.LENGTH_SHORT).show()
                                // Replace the current fragment with GrievancesFragment
                                val fragmentManager = requireActivity().supportFragmentManager
                                val fragmentTransaction = fragmentManager.beginTransaction()
                                val newFragment = GrievancesFragment()
                                fragmentTransaction.replace(R.id.fragment_container, newFragment)
                                fragmentTransaction.commit()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to submit grievance", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(requireContext(), "Incomplete user data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching user data: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}