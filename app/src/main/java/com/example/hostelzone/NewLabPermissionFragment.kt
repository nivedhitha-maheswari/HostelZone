package com.example.hostelzone

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class NewLabPermissionFragment : Fragment() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_new_lab_permission, container, false)

        // Find NumberPicker views
        val numberPickerHours: NumberPicker = view.findViewById(R.id.numberPickerHours)
        val numberPickerMinutes: NumberPicker = view.findViewById(R.id.numberPickerMinutes)
        val numberPickerAmPm: NumberPicker = view.findViewById(R.id.numberPickerAmPm)

        // Initialize NumberPickers for hours, minutes, and AM/PM
        initPicker(0, 12, numberPickerHours)
        initPicker(0, 59, numberPickerMinutes)
        initPickerWithString(0, 0, numberPickerAmPm, arrayOf("PM"))

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().reference

        // Find the "Request Permission" button
        val requestPermissionButton = view.findViewById<Button>(R.id.requestPermissionButton)

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

        // Handle click event of the "Request Permission" button
        requestPermissionButton.setOnClickListener {
            // Retrieve user ID from SharedPreferences
            val userId = sharedPreferences.getString("userId", "")

            if (!userId.isNullOrEmpty()) {
                // Retrieve reason and comments from EditText fields
                val reasonEditText = view.findViewById<EditText>(R.id.reasonEditText)
                val commentsEditText = view.findViewById<EditText>(R.id.commentsEditText)
                val reason = reasonEditText.text.toString().trim()
                val comments = commentsEditText.text.toString().trim()

                // Get the selected hour and minute from NumberPicker
                val selectedHour = numberPickerHours.value
                val selectedMinute = numberPickerMinutes.value

                // Create a Calendar instance and set the selected hour and minute
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                }

                // Format the requested time as HH:mm:ss
                val requestedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)

                // Get the current system time
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

                // Get the current date
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // Fetch user details from Firebase Database
                fetchUserdetails(userId) { userData ->
                    if (userData != null) {
                        // Create a map for the request data including all user details
                        val requestData = mapOf(
                            "reason" to reason,
                            "comments" to comments,
                            "requestedTime" to requestedTime,
                            "currentTime" to currentTime,
                            "currentDate" to currentDate,
                            "facultyId" to "dummyFacultyId", // Replace with actual faculty ID
                            "rollNumber" to userData.rollNumber,
                            "course" to userData.course,
                            "year" to userData.year,
                            "group" to userData.group,
                            "status" to "submitted" // Add the status field with the value "submitted"
                        )

                        // Add the request data to the "requests" subcollection at the root level
                        databaseReference.child("requests").push()
                            .setValue(requestData)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Permission requested successfully", Toast.LENGTH_SHORT).show()
                                // Replace the current fragment with LabPermissionFragment
                                val labPermissionFragment = LabPermissionFragment()
                                requireActivity().supportFragmentManager.beginTransaction()
                                    .replace(R.id.fragment_container, labPermissionFragment)
                                    .commit()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to request permission", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(requireContext(), "User details not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "User ID not found in SharedPreferences", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun initPicker(min: Int, max: Int, p: NumberPicker) {
        p.minValue = min
        p.maxValue = max
        p.setFormatter { i -> String.format("%02d", i) }
    }

    private fun initPickerWithString(min: Int, max: Int, p: NumberPicker, str: Array<String>) {
        p.minValue = min
        p.maxValue = max
        p.displayedValues = str
    }

    data class UserData(
        val rollNumber: String?,
        val course: String?,
        val year: String?,
        val group: String?
    )

    private fun fetchUserdetails(userId: String, callback: (UserData?) -> Unit) {
        // Construct database reference to the user node
        val userReference = FirebaseDatabase.getInstance().reference.child("users").child(userId)

        // Retrieve user data
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Check if user data exists
                if (dataSnapshot.exists()) {
                    // Retrieve user details from dataSnapshot
                    val rollNumber = dataSnapshot.child("additionalData").child("rollNumber").getValue(String::class.java)
                    val course = dataSnapshot.child("additionalData").child("course").getValue(String::class.java)
                    val year = dataSnapshot.child("additionalData").child("year").getValue(String::class.java)
                    val group = dataSnapshot.child("additionalData").child("group").getValue(String::class.java)

                    // Create a UserData object with retrieved details
                    val userData = UserData(rollNumber, course, year, group)

                    // Pass the userData object to the callback function
                    callback(userData)
                } else {
                    // If user data doesn't exist, pass null to the callback function
                    callback(null)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // If data retrieval is cancelled, pass null to the callback function
                callback(null)
            }
        })
    }

}
