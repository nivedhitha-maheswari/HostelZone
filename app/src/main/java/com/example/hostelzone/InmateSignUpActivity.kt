package com.example.hostelzone

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.hostelzone.databinding.ActivityInmateSignUpBinding
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream
import android.widget.ImageView
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class InmateSignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInmateSignUpBinding
    private lateinit var databaseReference: DatabaseReference
    private var selectedImageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInmateSignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseReference = FirebaseDatabase.getInstance().reference.child("users")

        // Set up image selection button click listener
        binding.btnChooseImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }

        // Set up the spinner with options
        val yearAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.year_options,
            android.R.layout.simple_spinner_item
        )
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerYear.adapter = yearAdapter

        val groupAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.groups,
            android.R.layout.simple_spinner_item
        )
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.groupSpinner.adapter = groupAdapter

        val blockAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.block_options,
            android.R.layout.simple_spinner_item
        )
        blockAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBlock.adapter = blockAdapter

        // Handle sign-up button click
        binding.buttonSignUp.setOnClickListener {
            uploadImageToFirebase()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            selectedImageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            binding.imageView.setImageBitmap(selectedImageBitmap)
            binding.imageView.visibility = ImageView.VISIBLE
        }
    }

    private fun uploadImageToFirebase() {
        selectedImageBitmap?.let { bitmap ->
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageData = baos.toByteArray()

            // Firebase Storage reference
            val storageRef = FirebaseStorage.getInstance().reference.child("images/${UUID.randomUUID()}.jpg")

            // Upload image to Firebase Storage
            storageRef.putBytes(imageData)
                .addOnSuccessListener { taskSnapshot ->
                    // Image uploaded successfully, get download URL
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()

                        // Construct additionalData map with image URL and other fields
                        val additionalData = mapOf(
                            "inmateName" to binding.editTextInmateName.text.toString(),
                            "rollNumber" to binding.editTextRollNumber.text.toString(),
                            "course" to binding.spinnerClass.selectedItem.toString(),
                            "year" to binding.spinnerYear.selectedItem.toString(),
                            "group" to binding.groupSpinner.selectedItem.toString(),
                            "block" to binding.spinnerBlock.selectedItem.toString(),
                            "roomNumber" to binding.editTextRoomNumber.text.toString(),
                            "mobileNumber" to binding.editTextMobileNumber.text.toString(),
                            "image" to imageUrl // Store image URL
                        )

                        // Get the username passed from SignUpActivity
                        val username = intent.getStringExtra("username")

                        // Query the database to find the user ID associated with the provided username
                        databaseReference.orderByChild("username").equalTo(username)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        // Loop through the results to find the user ID
                                        for (snapshot in dataSnapshot.children) {
                                            val userId = snapshot.key

                                            // Update additionalData in the database
                                            userId?.let {
                                                databaseReference.child(it).child("additionalData").setValue(additionalData)
                                                    .addOnSuccessListener {
                                                        Toast.makeText(
                                                            this@InmateSignUpActivity,
                                                            "Data saved successfully",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        // Redirect to appropriate page based on user type
                                                        // (If needed, add your logic here)
                                                        startActivity(Intent(this@InmateSignUpActivity, SignInActivity::class.java))
                                                        finish()
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(
                                                            this@InmateSignUpActivity,
                                                            "Failed to save data: ${e.message}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            this@InmateSignUpActivity,
                                            "User not found",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    Toast.makeText(
                                        this@InmateSignUpActivity,
                                        "Database Error: ${databaseError.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            })
                    }.addOnFailureListener { e ->
                        Toast.makeText(
                            this@InmateSignUpActivity,
                            "Failed to get image URL: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        this@InmateSignUpActivity,
                        "Failed to upload image: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } ?: run {
            Toast.makeText(
                this@InmateSignUpActivity,
                "No image selected",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    companion object {
        const val REQUEST_IMAGE_PICK = 1
    }
}
