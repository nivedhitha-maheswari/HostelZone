package com.example.hostelzone

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
            // Fetch roll number from Firebase using userId
            fetchRollNumberFromFirebase(userId)
        } else {
            Toast.makeText(requireContext(), "User ID not found in SharedPreferences", Toast.LENGTH_SHORT).show()
        }

        // Initialize Firebase components
        databaseReference = FirebaseDatabase.getInstance().reference.child("requests")
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
    }

    private fun fetchRollNumberFromFirebase(userId: String) {
        // Construct database reference to the user node
        val userReference = FirebaseDatabase.getInstance().reference.child("users").child(userId)

        // Retrieve roll number from user data
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val rollNumber = dataSnapshot.child("additionalData").child("rollNumber").getValue(String::class.java)
                if (!rollNumber.isNullOrEmpty()) {
                    // Roll number is available, fetch lab permissions by roll number
                    fetchLabPermissionsByRollNumber(rollNumber)
                } else {
                    Toast.makeText(requireContext(), "Roll number not found for the user", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching roll number: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchLabPermissionsByRollNumber(rollNumber: String) {
        // Construct database reference to the lab permissions node

        // Query lab permissions by roll number
        databaseReference.orderByChild("rollNumber").equalTo(rollNumber).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val labPermissions = mutableListOf<LabPermission>()
                for (snapshot in dataSnapshot.children) {
                    val labPermission = snapshot.getValue(LabPermission::class.java)
                    labPermission?.let { labPermissions.add(it) }
                }
                // Update the adapter with fetched lab permissions
                labPermissionAdapter.submitList(labPermissions)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
                Log.e(TAG, "Error fetching data: ${databaseError.message}")
                // Display a message to the user
                Toast.makeText(requireContext(), "Error fetching data: ${databaseError.message}", Toast.LENGTH_SHORT).show()
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
    )

    class LabPermissionAdapter : RecyclerView.Adapter<LabPermissionAdapter.LabPermissionViewHolder>() {

        private val labPermissionList = mutableListOf<LabPermission>()

        fun submitList(newList: List<LabPermission>) {
            labPermissionList.clear()
            labPermissionList.addAll(newList)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabPermissionViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_request, parent, false)
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
                itemView.findViewById<TextView>(R.id.textViewStatus).text = statusText
            }
        }

    }
}
