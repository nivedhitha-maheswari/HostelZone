package com.example.hostelzone

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

class InmateProfileFragment : Fragment() {
    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var rollNumberTextView: TextView
    private lateinit var degreeTextView: TextView
    private lateinit var courseTextView: TextView
    private lateinit var yearTextView: TextView
    private lateinit var groupTextView: TextView
    private lateinit var mobileNumberTextView: TextView
    private lateinit var blockTextView: TextView
    private lateinit var roomNumberTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_inmate_profile, container, false)

        profileImageView = view.findViewById(R.id.profileImageView)
        nameTextView = view.findViewById(R.id.textViewName)
        rollNumberTextView = view.findViewById(R.id.textViewRollNumber)
        degreeTextView = view.findViewById(R.id.textViewDegree)
        courseTextView = view.findViewById(R.id.textViewCourse)
        yearTextView = view.findViewById(R.id.textViewYear)
        groupTextView = view.findViewById(R.id.textViewGroup)
        mobileNumberTextView = view.findViewById(R.id.textViewMobileNumber)
        blockTextView = view.findViewById(R.id.textViewBlock)
        roomNumberTextView = view.findViewById(R.id.textViewRoomNumber)

        // Fetch user ID from SharedPreferences
        val sharedPreferences = requireActivity().getSharedPreferences("myPrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", null)

        // Fetch user data from additionalData using userId
        fetchUserDataFromFirebase(userId)

        return view
    }

    private fun fetchUserDataFromFirebase(userId: String?) {
        userId?.let {
            val userReference = FirebaseDatabase.getInstance().reference.child("users").child(userId)
            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val name = dataSnapshot.child("additionalData").child("inmateName").getValue(String::class.java)
                    val degree = dataSnapshot.child("additionalData").child("degree").getValue(String::class.java)
                    val course = dataSnapshot.child("additionalData").child("course").getValue(String::class.java)
                    val year = dataSnapshot.child("additionalData").child("year").getValue(String::class.java)
                    val group = dataSnapshot.child("additionalData").child("group").getValue(String::class.java)
                    val rollNumber = dataSnapshot.child("additionalData").child("rollNumber").getValue(String::class.java)
                    val profileImageURL = dataSnapshot.child("additionalData").child("image").getValue(String::class.java)
                    val mobileNumber = dataSnapshot.child("additionalData").child("mobileNumber").getValue(String::class.java) ?: "" // Assuming mobileNumber is also fetched
                    val block = dataSnapshot.child("additionalData").child("block").getValue(String::class.java) ?: "" // Assuming block is also fetched
                    val roomNumber = dataSnapshot.child("additionalData").child("roomNumber").getValue(String::class.java) ?: "" // Assuming roomNumber is


                    // Check if all necessary data is present
                    if (!degree.isNullOrEmpty() && !course.isNullOrEmpty() && !year.isNullOrEmpty() &&
                        !group.isNullOrEmpty() && !rollNumber.isNullOrEmpty() && !profileImageURL.isNullOrEmpty()) {

                        // Create ProfileData object with fetched data
                        val profileData = ProfileData(
                            name = name ?: "",
                            rollNumber = rollNumber ?: "",
                            degree = degree ?: "",
                            course = course ?: "",
                            year = year ?: "",
                            group = group ?: "",
                            mobileNumber = mobileNumber,
                            block = block,
                            roomNumber = roomNumber,
                            profileImageURL = profileImageURL
                        )

                        // Populate UI with ProfileData
                        populateUI(profileData)
                    } else {
                        // Handle incomplete user data
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle database error
                }
            })
        }
    }

    // Function to populate UI with ProfileData
    private fun populateUI(profileData: ProfileData) {
        nameTextView.text = profileData.name
        rollNumberTextView.text = profileData.rollNumber
        degreeTextView.text = profileData.degree
        courseTextView.text = profileData.course
        yearTextView.text = profileData.year
        groupTextView.text = profileData.group
        mobileNumberTextView.text = profileData.mobileNumber
        blockTextView.text = profileData.block
        roomNumberTextView.text = profileData.roomNumber

        // Load profile image using Glide
        val profileImageURL = profileData.profileImageURL
        profileImageURL?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.stu_girl) // Placeholder image while loading
                .error(R.drawable.stu_girl) // Error image if loading fails
                .into(profileImageView)
        }
    }
}

data class ProfileData(
    val name: String,
    val rollNumber: String,
    val degree: String,
    val course: String,
    val year: String,
    val group: String,
    val mobileNumber: String,
    val block: String,
    val roomNumber: String,
    val profileImageURL: String?
)
