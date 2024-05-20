package com.example.hostelzone

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.hostelzone.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TutorProfileFragment : Fragment() {
    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var facultyIdTextView: TextView
    private lateinit var degreeTextView: TextView
    private lateinit var courseTextView: TextView
    private lateinit var yearTextView: TextView
    private lateinit var groupTextView: TextView
    private lateinit var mobileNumberTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_tutor_profile, container, false)

        profileImageView = view.findViewById(R.id.profileImageView)
        nameTextView = view.findViewById(R.id.textViewName)
        facultyIdTextView = view.findViewById(R.id.textViewFacultyId)
        degreeTextView = view.findViewById(R.id.textViewDegree)
        courseTextView = view.findViewById(R.id.textViewCourse)
        yearTextView = view.findViewById(R.id.textViewYear)
        groupTextView = view.findViewById(R.id.textViewGroup)
        mobileNumberTextView = view.findViewById(R.id.textViewMobileNumber)

        // Fetch user ID from SharedPreferences
        val sharedPreferences = requireActivity().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", null)

        userId?.let { uid ->
            // Firebase reference
            val userReference = FirebaseDatabase.getInstance().reference.child("users").child(uid)

            // Read data from Firebase
            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val tutorName = dataSnapshot.child("additionalData").child("tutorName").getValue(String::class.java)
                    val facultyId = dataSnapshot.child("additionalData").child("facultyId").getValue(String::class.java)
                    val tutoringDegree = dataSnapshot.child("additionalData").child("tutoringDegree").getValue(String::class.java)
                    val tutoringClass = dataSnapshot.child("additionalData").child("tutoringClass").getValue(String::class.java)
                    val tutoringClassYear = dataSnapshot.child("additionalData").child("tutoringClassYear").getValue(String::class.java)
                    val tutoringClassGroup = dataSnapshot.child("additionalData").child("tutoringClassGroup").getValue(String::class.java)
                    val mobileNumber = dataSnapshot.child("additionalData").child("mobileNumber").getValue(String::class.java)
                    val profileImageURL = dataSnapshot.child("additionalData").child("image").getValue(String::class.java)

                    // Populate UI elements
                    nameTextView.text = tutorName
                    facultyIdTextView.text = facultyId
                    degreeTextView.text = tutoringDegree
                    courseTextView.text = tutoringClass
                    yearTextView.text = tutoringClassYear
                    groupTextView.text = tutoringClassGroup
                    mobileNumberTextView.text = mobileNumber

                    // Load profile image using Glide
                    profileImageURL?.let {
                        Glide.with(requireContext())
                            .load(profileImageURL)
                            .placeholder(R.drawable.prof) // Placeholder image while loading
                            .error(R.drawable.prof) // Error image if loading fails
                            .into(profileImageView)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle database error
                    Toast.makeText(requireContext(), "Database Error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        return view
    }
}
