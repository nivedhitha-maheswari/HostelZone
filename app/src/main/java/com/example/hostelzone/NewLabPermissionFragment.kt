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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class NewLabPermissionFragment : Fragment() {
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

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
        initPicker(6, 12, numberPickerHours)
        initPicker(0, 59, numberPickerMinutes)
        initPickerWithString(0, 0, numberPickerAmPm, arrayOf("PM"))

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().reference

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

        // Initialize requestPermissionLauncher in onViewCreated
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted, proceed with location-related operations
                // In this case, fetching and storing location
                // Call fetchAndStoreLocation() here
            } else {
                // Permission is denied, handle the scenario appropriately
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        // Find the "Request Permission" button
        val requestPermissionButton = view.findViewById<Button>(R.id.requestPermissionButton)

        // Handle click event of the "Request Permission" button
        requestPermissionButton.setOnClickListener {
            // Retrieve user ID from SharedPreferences
            val userId = sharedPreferences.getString("userId", "")

            if (!userId.isNullOrEmpty()) {
                // Fetch user data and then store the request
                fetchUserDataAndStoreRequest(userId)
            } else {
                Toast.makeText(requireContext(), "User ID not found in SharedPreferences", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun fetchUserDataAndStoreRequest(userId: String) {
        // Construct database reference to the user node
        val userReference = FirebaseDatabase.getInstance().reference.child("users").child(userId)

        // Retrieve user data
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Check if the dataSnapshot has the "additionalData" node
                val additionalDataSnapshot = dataSnapshot.child("additionalData")
                if (additionalDataSnapshot.exists()) {
                    // Retrieve degree, course, year, and group from additionalData node
                    val degree = additionalDataSnapshot.child("degree").getValue(String::class.java)
                    val course = additionalDataSnapshot.child("course").getValue(String::class.java)
                    val year = additionalDataSnapshot.child("year").getValue(String::class.java)
                    val group = additionalDataSnapshot.child("group").getValue(String::class.java)

                    // If any of the required data is missing, show an error message
                    if (degree.isNullOrEmpty() || course.isNullOrEmpty() || year.isNullOrEmpty() || group.isNullOrEmpty()) {
                        Toast.makeText(requireContext(), "User data incomplete", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Proceed with storing the request using the fetched data
                    storeRequest(userId, degree, course, year, group)
                } else {
                    // Show an error message if the "additionalData" node doesn't exist
                    Toast.makeText(requireContext(), "Additional data not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle onCancelled
            }
        })
    }

    private fun storeRequest(userId: String, degree: String, course: String, year: String, group: String) {
        // Retrieve reason and comments from EditText fields
        val reasonEditText = view?.findViewById<EditText>(R.id.reasonEditText)
        val commentsEditText = view?.findViewById<EditText>(R.id.commentsEditText)
        val reason = reasonEditText?.text.toString().trim()
        val comments = commentsEditText?.text.toString().trim()

        // Get the selected hour and minute from NumberPicker
        val selectedHour = view?.findViewById<NumberPicker>(R.id.numberPickerHours)?.value
        val selectedMinute = view?.findViewById<NumberPicker>(R.id.numberPickerMinutes)?.value

        // Create a Calendar instance and set the selected hour and minute
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour ?: 0)
            set(Calendar.MINUTE, selectedMinute ?: 0)
        }

        // Format the requested time as HH:mm:ss
        val requestedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)

        // Get the current system time
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        // Get the current date
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Construct database reference to the user node
        val userReference = FirebaseDatabase.getInstance().reference.child("users").child(userId)

        // Retrieve user data including roll number
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Retrieve roll number from user data
                val rollNumber = dataSnapshot.child("additionalData").child("rollNumber").getValue(String::class.java)
                val imageUrl = dataSnapshot.child("additionalData").child("image").getValue(String::class.java)

                if (!rollNumber.isNullOrEmpty()) {
                    // Generate a unique ID for the lab permission
                    val labPermissionId = databaseReference.push().key

                    // Create a map for the request data including the labPermissionId and other fields
                    val requestData = mapOf(
                        "labPermissionId" to labPermissionId,
                        "reason" to reason,
                        "comments" to comments,
                        "requestedTime" to requestedTime,
                        "currentTime" to currentTime,
                        "currentDate" to currentDate,
                        "facultyId" to "dummyFacultyId", // Replace with actual faculty ID
                        "rollNumber" to rollNumber,
                        "imageUrl" to imageUrl,
                        "status" to "submitted" // Add the status field with the value "submitted"
                    )

                    // Add the request data to the appropriate location in the database
                    val requestRef = databaseReference
                        .child("requests")
                        .child(degree ?: "")
                        .child(course ?: "")
                        .child(year ?: "")
                        .child(group ?: "")

                    // Add the request data along with the labPermissionId as a child node
                    requestRef.child(labPermissionId ?: "")
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
                    Toast.makeText(requireContext(), "Roll number not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle onCancelled
            }
        })
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
}
