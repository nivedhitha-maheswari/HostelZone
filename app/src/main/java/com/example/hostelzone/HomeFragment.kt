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

        return view
    }

    private fun fetchUserDataFromFirebase(userId: String, view: View) {
        // Initialize Firebase components
        databaseReference = FirebaseDatabase.getInstance().reference.child("users").child(userId)

        // Retrieve user data
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Check if additionalData exists for the user
                val additionalDataSnapshot = dataSnapshot.child("additionalData")
                if (additionalDataSnapshot.exists()) {
                    // Extract image data from additionalData
                    val imageData = additionalDataSnapshot.child("image").getValue(String::class.java)
                    if (!imageData.isNullOrEmpty()) {
                        // Decode Base64 string to bitmap and display in ImageView
                        val decodedImageBytes = Base64.decode(imageData, Base64.DEFAULT)
                        val decodedImage = android.graphics.BitmapFactory.decodeByteArray(decodedImageBytes, 0, decodedImageBytes.size)
                        imageView = view.findViewById(R.id.imageView)
                        imageView.setImageBitmap(decodedImage)
                        imageView.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(requireContext(), "Image data not found for the user", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Additional data not found for the user", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching user data: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
