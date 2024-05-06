package com.example.hostelzone

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var imageView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

        // Fetch userId from SharedPreferences
        val userId = sharedPreferences.getString("userId", "")
        if (!userId.isNullOrEmpty()) {
            // Fetch user data from Firebase using userId
            fetchUserDataFromFirebase(userId, view)
        } else {
            Toast.makeText(requireContext(), "User ID not found in SharedPreferences", Toast.LENGTH_SHORT).show()
        }

        // Initialize imageView
        imageView = view.findViewById(R.id.imageView) // Replace R.id.imageView with the actual ID of your ImageView

        return view
    }

    private fun fetchUserDataFromFirebase(userId: String, view: View) {
        // Initialize Firebase components
        databaseReference = FirebaseDatabase.getInstance().reference.child("users").child(userId)

        // Check if the fragment is attached to the activity
        if (!isAdded) {
            return
        }

        // Retrieve user data
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!isAdded) {
                    // Fragment is not attached, return
                    return
                }

                // Retrieve user data from dataSnapshot
                val userData = dataSnapshot.getValue(User::class.java)

                // Update UI with user data
                if (userData != null) {
                    // Example: Update ImageView using Glide
                    Glide.with(requireContext())
                        .load(userData.imageUrl)
                        .into(imageView)
                } else {
                    // Handle case where user data is null
                    Toast.makeText(requireContext(), "User data is null", Toast.LENGTH_SHORT).show()
                }
            }



            override fun onCancelled(databaseError: DatabaseError) {
                // Check if the fragment is still attached before accessing the context
                if (!isAdded) {
                    // Fragment is not attached, return
                    return
                }

                // Handle the onCancelled event
                Toast.makeText(requireContext(), "Error fetching user data: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    data class User(
        val imageUrl: String? // Assuming imageUrl is a String property in your Firebase data
    ) {
        constructor() : this(null) // No-argument constructor
    }


}
