package com.example.hostelzone

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hostelzone.databinding.ActivityTutorSignUpBinding
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.UUID

class TutorSignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorSignUpBinding
    private lateinit var databaseReference: DatabaseReference
    private var selectedImageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorSignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseReference = FirebaseDatabase.getInstance().reference.child("users")

        binding.btnChooseImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }

        // Set up the spinner with options
        val tutoringClassAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.class_options,
            android.R.layout.simple_spinner_item
        )
        tutoringClassAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTutoringClass.adapter = tutoringClassAdapter

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
            binding.tutorImageView.setImageBitmap(selectedImageBitmap)
            binding.tutorImageView.visibility = ImageView.VISIBLE
        }
    }

    private fun uploadImageToFirebase() {
        selectedImageBitmap?.let { bitmap ->
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageData = baos.toByteArray()

            val tutorName = binding.editTextTutorName.text.toString()
            val facultyId = binding.editTextFacultyId.text.toString()
            val tutoringClass = binding.spinnerTutoringClass.selectedItem.toString()
            val tutoringClassYear = binding.spinnerTutoringClassYear.selectedItem.toString()
            val tutoringClassGroup = binding.spinnerTutoringClassGroup.selectedItem.toString()
            val mobileNumber = binding.editTextMobileNumber.text.toString()

            // Firebase Storage reference
            val storageRef = FirebaseStorage.getInstance().reference.child("images/${UUID.randomUUID()}.jpg")

            // Upload image to Firebase Storage
            storageRef.putBytes(imageData)
                .addOnSuccessListener { taskSnapshot ->
                    // Image uploaded successfully, get download URL
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()

                        // Construct additionalData map
                        val additionalData = mapOf(
                            "tutorName" to tutorName,
                            "facultyId" to facultyId,
                            "tutoringClass" to tutoringClass,
                            "tutoringClassYear" to tutoringClassYear,
                            "tutoringClassGroup" to tutoringClassGroup,
                            "mobileNumber" to mobileNumber,
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
                                                            this@TutorSignUpActivity,
                                                            "Data saved successfully",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        // Redirect to appropriate page based on user type
                                                        // (If needed, add your logic here)
                                                        startActivity(Intent(this@TutorSignUpActivity, SignInActivity::class.java))
                                                        finish()
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(
                                                            this@TutorSignUpActivity,
                                                            "Failed to save data: ${e.message}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            this@TutorSignUpActivity,
                                            "User not found",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    Toast.makeText(
                                        this@TutorSignUpActivity,
                                        "Database Error: ${databaseError.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            })
                    }.addOnFailureListener { e ->
                        Toast.makeText(
                            this@TutorSignUpActivity,
                            "Failed to get image URL: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        this@TutorSignUpActivity,
                        "Failed to upload image: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } ?: run {
            Toast.makeText(
                this@TutorSignUpActivity,
                "No image selected",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        const val REQUEST_IMAGE_PICK = 1
    }
}
