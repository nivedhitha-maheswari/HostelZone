package com.example.hostelzone

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hostelzone.databinding.FragmentLabPermissionBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*

class LabPermissionFragment : Fragment() {

    private var _binding: FragmentLabPermissionBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var labPermissionAdapter: LabPermissionAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val TAG = "LabPermissionFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLabPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView and its adapter
        recyclerView = binding.recyclerViewRequests
        labPermissionAdapter = LabPermissionAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = labPermissionAdapter

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

        // Fetch userId from SharedPreferences
        val userId = sharedPreferences.getString("userId", "")
        if (!userId.isNullOrEmpty()) {
            // Fetch roll number, degree, course, year, and group from Firebase using userId
            fetchUserDataFromFirebase(userId)
        } else {
            Toast.makeText(
                requireContext(),
                "User ID not found in SharedPreferences",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Initialize Firebase components
        databaseReference = FirebaseDatabase.getInstance().reference
        val fabNewLabPermission = view.findViewById<FloatingActionButton>(R.id.fabNewRequest)
        fabNewLabPermission.setOnClickListener {
            // Create a new instance of the NewLabPermissionFragment
            val newFragment = NewLabPermissionFragment()
            // Get the FragmentManager
            val fragmentManager = requireActivity().supportFragmentManager
            // Begin a new transaction
            val fragmentTransaction = fragmentManager.beginTransaction()
            // Replace the current fragment with the new one
            fragmentTransaction.replace(R.id.fragment_container, newFragment)
            // Add the transaction to the back stack (optional)
            fragmentTransaction.addToBackStack(null)
            // Commit the transaction
            fragmentTransaction.commit()
        }
        Toast.makeText(requireContext(), "View created", Toast.LENGTH_SHORT).show()

    }

    private fun fetchUserDataFromFirebase(userId: String) {
        // Construct database reference to the user node
        val userReference = FirebaseDatabase.getInstance().reference.child("users").child(userId)

        // Retrieve user data
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val degree = dataSnapshot.child("additionalData").child("degree")
                    .getValue(String::class.java)
                val course = dataSnapshot.child("additionalData").child("course")
                    .getValue(String::class.java)
                val year =
                    dataSnapshot.child("additionalData").child("year").getValue(String::class.java)
                val group =
                    dataSnapshot.child("additionalData").child("group").getValue(String::class.java)
                val rollNumber = dataSnapshot.child("additionalData").child("rollNumber")
                    .getValue(String::class.java)

                if (!degree.isNullOrEmpty() && !course.isNullOrEmpty() && !year.isNullOrEmpty() && !group.isNullOrEmpty() && !rollNumber.isNullOrEmpty()) {
                    // Degree, course, year, group, and roll number are available, fetch lab permissions
                    fetchLabPermissionsByRollNumber(rollNumber, degree, course, year, group)
                } else {
                    Toast.makeText(requireContext(), "Incomplete user data", Toast.LENGTH_SHORT)
                        .show()
                }
                val userDataMessage = "Received Degree: $degree\n" +
                        "Received Course: $course\n" +
                        "Received Year: $year\n" +
                        "Received Group: $group\n" +
                        "Received Roll Number: $rollNumber"
                Toast.makeText(requireContext(), userDataMessage, Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Error fetching user data: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    @SuppressLint("RestrictedApi")
    private fun fetchLabPermissionsByRollNumber(
        rollNumber: String,
        degree: String,
        course: String,
        year: String,
        group: String
    ) {
        val requestRef = databaseReference
            .child("requests")
            .child(degree)
            .child(course)
            .child(year)
            .child(group)

        Toast.makeText(requireContext(), "Request Ref Path: ${requestRef.path}", Toast.LENGTH_SHORT)
            .show()

        // Fetch all requests under the specified group
        requestRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val labPermissions = mutableListOf<LabPermission>()

                for (requestSnapshot in dataSnapshot.children) {
                    val labPermission = requestSnapshot.getValue(LabPermission::class.java)
                    labPermission?.let {
                        // Filter requests by roll number
                        if (it.rollNumber == rollNumber) {
                            labPermissions.add(it)
                        }
                    }
                }

                // Update the adapter with fetched lab permissions
                labPermissionAdapter.submitList(labPermissions)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
                Log.e(TAG, "Error fetching data: ${databaseError.message}")
                // Display a message to the user
                Toast.makeText(
                    requireContext(),
                    "Error fetching data: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class LabPermission(
        val labPermissionId: String = "", // Unique ID for the lab permission
        val rollNumber: String = "", // Roll number of the user requesting the lab permission
        val reason: String = "", // Reason for lab permission request
        val requestedTime: String = "", // Requested time for lab permission
        val status: String = "" // Status of lab permission request
    ) {
        // Override toString() method to provide a meaningful string representation
        override fun toString(): String {
            return "LabPermission(labPermissionId='$labPermissionId', rollNumber='$rollNumber', reason='$reason', requestedTime='$requestedTime', status='$status')"
        }
    }
    class LabPermissionAdapter :
        RecyclerView.Adapter<LabPermissionAdapter.LabPermissionViewHolder>() {

        private val labPermissionList = mutableListOf<LabPermission>()

        fun submitList(newList: List<LabPermission>) {
            labPermissionList.clear()
            labPermissionList.addAll(newList)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabPermissionViewHolder {
            val itemView =
                LayoutInflater.from(parent.context).inflate(R.layout.item_request, parent, false)
            return LabPermissionViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: LabPermissionViewHolder, position: Int) {
            val labPermission = labPermissionList[position]
            holder.bind(labPermission)
        }

        override fun getItemCount(): Int {
            return labPermissionList.size
        }

        inner class LabPermissionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(labPermission: LabPermission) {
                val reasonText = "${labPermission.reason}"
                itemView.findViewById<TextView>(R.id.textViewReason).text = reasonText

                val timeText = "${labPermission.requestedTime}"
                itemView.findViewById<TextView>(R.id.textViewTime).text = timeText

                val statusText = " ${labPermission.status} "
                val statusTextView = itemView.findViewById<TextView>(R.id.textViewStatus)
                statusTextView.text = statusText

                // Check for null before setting background
                val backgroundResId = when (labPermission.status) {
                    "forwarded" -> R.drawable.yellow_background
                    "approved" -> R.drawable.green_background
                    "declined" -> R.drawable.red_background
                    else -> R.drawable.rounded_corner_background // You can create a default background XML as well
                }
                statusTextView.setBackgroundResource(backgroundResId)
            }
        }
    }
}
