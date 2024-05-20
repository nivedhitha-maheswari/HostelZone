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
import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.provider.MediaStore
import android.provider.MediaStore.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices.getFusedLocationProviderClient
import java.io.ByteArrayOutputStream
import android.util.Base64
import com.google.firebase.storage.FirebaseStorage


class NewLabPermissionFragment : Fragment() {
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private val REQUEST_IMAGE_CAPTURE = 1
    private var encodedPhoto: String? = null


    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

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
        initPicker(6, 9, numberPickerHours)
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
                getLastLocation()
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

        // Find the "Get Location" button
        val getLocationButton = view.findViewById<Button>(R.id.getLocationButton)

        // Handle click event of the "Get Location" button
        getLocationButton.setOnClickListener {
            requestLocationPermission()
        }
        val takePhotoButton = view.findViewById<Button>(R.id.getCameraButton)


        takePhotoButton.setOnClickListener {
            dispatchTakePictureIntent()
        }

        return view
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                uploadImageToFirebaseStorage(imageBitmap)
            } else {
                Toast.makeText(requireContext(), "Failed to capture image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImageToFirebaseStorage(bitmap: Bitmap) {
        val resizedBitmap = resizeBitmap(bitmap, 800, 800) // Resize the bitmap to desired dimensions

        val storageRef = FirebaseStorage.getInstance().reference
        val imagesRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

        val baos = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = imagesRef.putBytes(data)
        uploadTask.addOnSuccessListener { taskSnapshot ->
            imagesRef.downloadUrl.addOnSuccessListener { uri ->
                // Image uploaded successfully, save the download URL to the database

                encodedPhoto = uri.toString()

                view?.findViewById<ImageView>(R.id.photoImageView)?.setImageBitmap(resizedBitmap)

            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to get image URL", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
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



        val reason = reasonEditText?.text.toString().trim()
        if (reason.isEmpty()) {
            reasonEditText?.error = "Reason is required"
            return
        }
        if (encodedPhoto.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please upload a photo", Toast.LENGTH_SHORT).show()
            return
        }

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

        if (latitude == 0.0 || longitude == 0.0) {
            Toast.makeText(requireContext(), "Location coordinates are required", Toast.LENGTH_SHORT).show()
            return
        }

        // Retrieve user data including roll number and image URL
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Retrieve roll number and image URL from user data
                val rollNumber = dataSnapshot.child("additionalData").child("rollNumber").getValue(String::class.java)
                val imageUrl = dataSnapshot.child("additionalData").child("image").getValue(String::class.java)

                if (!rollNumber.isNullOrEmpty()) {
                    // Generate a unique ID for the lab permission
                    val labPermissionId = databaseReference.push().key

                    // Create a map for the request data including the labPermissionId and other fields
                    val requestData = mapOf(
                        "labPermissionId" to labPermissionId,
                        "reason" to reason,
                        "requestedTime" to requestedTime,
                        "currentTime" to currentTime,
                        "currentDate" to currentDate,
                        "latitude" to latitude, // Add latitude
                        "longitude" to longitude, // Add longitude
                        "facultyId" to "dummyFacultyId", // Replace with actual faculty ID
                        "rollNumber" to rollNumber,
                        "imageUrl" to imageUrl,
                        "photoData" to encodedPhoto, // Store the Firebase Storage download URL
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

                            // Load the image into the photoImageView
                            encodedPhoto?.let { encodedPhoto ->
                                val decodedBitmap = base64ToBitmap(encodedPhoto)
                                view?.findViewById<ImageView>(R.id.photoImageView)?.setImageBitmap(decodedBitmap)
                            }

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

    private fun base64ToBitmap(encodedString: String?): Bitmap? {
        if (encodedString.isNullOrEmpty()) {
            return null
        }

        return try {
            val decodedBytes = Base64.decode(encodedString, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        }
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

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission already granted, proceed with location fetching
            getLastLocation()
        } else {
            // Permission not granted, request permission from the user
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    // Assign values to latitude and longitude
                    latitude = location.latitude
                    longitude = location.longitude

                    handleLocation(location)
                } else {
                    view?.findViewById<TextView>(R.id.locationTextView)?.text = "Location not available"
                }
            }
            .addOnFailureListener { e ->
                view?.findViewById<TextView>(R.id.locationTextView)?.text = "Error getting location: ${e.message}"
            }
    }


    private fun handleLocation(location: android.location.Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        val address = getAddressFromLocation(latitude, longitude)
        val locationString =
            "Latitude: $latitude\nLongitude: $longitude\nAddress: $address"
        view?.findViewById<TextView>(R.id.locationTextView)?.text = locationString
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

        return if (addresses != null && addresses.isNotEmpty()) {
            val address: Address = addresses[0]
            address.getAddressLine(0) ?: "Address line not available"
        } else {
            "Address not found"
        }
    }
}