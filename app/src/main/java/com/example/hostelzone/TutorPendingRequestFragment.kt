package com.example.hostelzone

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hostelzone.databinding.FragmentTutorPendingRequestBinding
import com.google.firebase.database.*
import java.util.Locale

class TutorPendingRequestFragment : Fragment() {

    private var _binding: FragmentTutorPendingRequestBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RequestAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var tutorUserId: String
    private lateinit var tutoringDegree: String
    private lateinit var tutoringClass: String
    private lateinit var tutoringClassYear: String
    private lateinit var tutoringClassGroup: String

    companion object {
        private const val TAG = "TutorPendingRequestFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTutorPendingRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.recyclerViewRequests
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = RequestAdapter()
        recyclerView.adapter = adapter

        sharedPreferences = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        tutorUserId = sharedPreferences.getString("userId", "") ?: ""

        if (tutorUserId.isNotEmpty()) {
            fetchTutoringClassFromFirebase(tutorUserId)
        } else {
            Toast.makeText(requireContext(), "User ID not found in SharedPreferences", Toast.LENGTH_SHORT).show()
        }
        databaseReference = FirebaseDatabase.getInstance().reference
    }

    private fun fetchTutoringClassFromFirebase(userId: String) {
        val userReference = FirebaseDatabase.getInstance().reference.child("users").child(userId)
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                tutoringDegree = dataSnapshot.child("additionalData").child("tutoringDegree").getValue(String::class.java) ?: ""
                tutoringClass = dataSnapshot.child("additionalData").child("tutoringClass").getValue(String::class.java) ?: ""
                tutoringClassGroup = dataSnapshot.child("additionalData").child("tutoringClassGroup").getValue(String::class.java) ?: ""
                tutoringClassYear = dataSnapshot.child("additionalData").child("tutoringClassYear").getValue(String::class.java) ?: ""

                if (tutoringDegree.isNotEmpty() && tutoringClass.isNotEmpty() && tutoringClassGroup.isNotEmpty() && tutoringClassYear.isNotEmpty()) {
                    fetchRequests(tutoringDegree, tutoringClass, tutoringClassGroup, tutoringClassYear)

                } else {
                    Toast.makeText(requireContext(), "Tutoring class information not found for the user", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching tutoring class information: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchRequests(tutoringDegree: String, tutoringClass: String, tutoringClassGroup: String, tutoringClassYear: String) {
        val requestRef = databaseReference
            .child("requests")
            .child(tutoringDegree)
            .child(tutoringClass)
            .child(tutoringClassYear)
            .child(tutoringClassGroup)

        requestRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requests = mutableListOf<Request>()

                for (requestSnapshot in snapshot.children) {
                    val requestId = requestSnapshot.key ?: ""
                    val reason = requestSnapshot.child("reason").value.toString()
                    val requestedTime = requestSnapshot.child("requestedTime").value.toString()
                    val rollNumber = requestSnapshot.child("rollNumber").value.toString()
                    val imageUrl = requestSnapshot.child("photoData").value.toString()
                    val latitude = requestSnapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    val longitude = requestSnapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                    val status = requestSnapshot.child("status").value.toString()

                    if (status == "submitted") {
                        val request =
                            Request(requestId, rollNumber, reason, requestedTime, imageUrl,latitude,longitude)
                        requests.add(request)
                    }
                }

                adapter.setRequests(requests)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to fetch requests: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class Request(val requestId: String, val rollNumber: String, val reason: String, val requestedTime: String, val imageUrl: String?,val latitude: Double, val longitude: Double)

    inner class RequestAdapter ( private var requests: MutableList<Request> = mutableListOf()):
        RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {



        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tutor_new_request_list, parent, false)
            return RequestViewHolder(view, tutoringDegree, tutoringClass, tutoringClassYear, tutoringClassGroup)
        }

        override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
            holder.bind(requests[position])
        }

        override fun getItemCount(): Int {
            return requests.size
        }

        fun setRequests(requests: List<Request>) {
            this.requests.clear()
            this.requests.addAll(requests)
            notifyDataSetChanged()
        }

        fun clearRequests() {
            requests.clear()
            notifyDataSetChanged()
        }


        private fun handleLocation(latitude: Double, longitude: Double) {
            val address = getAddressFromLocation(latitude, longitude)
            val locationString =
                "$address"
            view?.findViewById<TextView>(R.id.textViewLocation)?.text = locationString
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

        inner class RequestViewHolder(itemView: View, val tutoringDegree: String, val tutoringClass: String, val tutoringClassYear: String, val tutoringClassGroup: String) : RecyclerView.ViewHolder(itemView) {
            init {
                // Find the forward request and reject buttons
                val forwardButton = itemView.findViewById<Button>(R.id.forwardRequestButton)
                val rejectButton = itemView.findViewById<Button>(R.id.rejectRequestButton)

                // Set click listeners for the buttons
                forwardButton.setOnClickListener {
                    val requestId = requests[adapterPosition].requestId

                    val requestRef = databaseReference
                        .child("requests")
                        .child(tutoringDegree)
                        .child(tutoringClass)
                        .child(tutoringClassYear)
                        .child(tutoringClassGroup)
                    updateRequestStatus(requestId,requestRef, "forwarded")
                    reloadPage()
                }

                rejectButton.setOnClickListener {
                    val requestId = requests[adapterPosition].requestId
                    val requestRef = databaseReference
                        .child("requests")
                        .child(tutoringDegree)
                        .child(tutoringClass)
                        .child(tutoringClassYear)
                        .child(tutoringClassGroup)
                    updateRequestStatus(requestId, requestRef, "declined")
                    reloadPage()
                }
            }

            fun bind(request: Request) {
                val requestId = request.requestId
                if (request.rollNumber.isNotEmpty()) {

                    itemView.findViewById<TextView>(R.id.textViewrequestRollNumber).text =
                        request.rollNumber
                }

                itemView.findViewById<TextView>(R.id.textViewrequestReason).text = request.reason
                itemView.findViewById<TextView>(R.id.textviewRequestedTime).text = request.requestedTime

                // Load image into ImageView using Glide
                val imageView = itemView.findViewById<ImageView>(R.id.imageViewProfile)
                Glide.with(itemView.context)
                    .load(request.imageUrl)
                    .into(imageView)

                if (request.rollNumber.isNotEmpty()) {

                    handleLocation(request.latitude.toDouble(), request.longitude.toDouble())
                }
                if (request.latitude != 0.0 && request.longitude != 0.0) {
                    val address = getAddressFromLocation(request.latitude, request.longitude)
                    itemView.findViewById<TextView>(R.id.textViewLocation).text = address
                } else {
                    itemView.findViewById<TextView>(R.id.textViewLocation).text = "Location not available"
                }
            }

            private fun reloadPage() {
                clearRequests()
                fetchTutoringClassFromFirebase(tutorUserId)
            }

            private fun updateRequestStatus(requestId: String, requestRef: DatabaseReference, newStatus: String) {
                val statusUpdate = mapOf("status" to newStatus)
                requestRef.child(requestId).updateChildren(statusUpdate)
                    .addOnSuccessListener {
                        Toast.makeText(
                            itemView.context,
                            "Request $newStatus ",
                            Toast.LENGTH_SHORT
                        ).show()
                        reloadPage()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            itemView.context,
                            "Failed to update status: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        reloadPage()
                    }
            }


        }
    }


    data class UserData(
        val course: String = "",
        val year: String = "",
        val group: String = "",
        val imageUrl: String? = null
    ) {
        // No-argument constructor required by Firebase
        constructor() : this("", "", "", null)
    }
}