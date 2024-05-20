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
import com.example.hostelzone.databinding.FragmentGrievancesBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*

class GrievancesFragment : Fragment() {

    private var _binding: FragmentGrievancesBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var grievancesAdapter: GrievanceAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val TAG = "GrievancesFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGrievancesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView and its adapter
        recyclerView = binding.recyclerViewGrievances
        grievancesAdapter = GrievanceAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = grievancesAdapter

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
        val fabNewGrievance = view.findViewById<FloatingActionButton>(R.id.fabNewGrievance)
        fabNewGrievance.setOnClickListener {
            // Create a new instance of the NewGrievanceFragment
            val newFragment = NewGrievanceFragment()
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
                    // Degree, course, year, group, and roll number are available, fetch grievances
                    fetchGrievancesByRollNumber(rollNumber, degree, course, year, group)
                } else {
                    Toast.makeText(requireContext(), "Incomplete user data", Toast.LENGTH_SHORT)
                        .show()
                }

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

    private fun fetchGrievancesByRollNumber(
        rollNumber: String,
        degree: String,
        course: String,
        year: String,
        group: String
    ) {
        val grievanceRef = databaseReference
            .child("grievances")
            .child(degree)
            .child(course)
            .child(year)
            .child(group)

        // Query only the grievances associated with the given roll number
        val query = grievanceRef.orderByChild("rollNumber").equalTo(rollNumber)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val grievances = mutableListOf<Grievance>()

                for (grievanceSnapshot in dataSnapshot.children) {
                    // Check if the entry is a placeholder or a complete grievance
                    if (grievanceSnapshot.hasChild("placeholderKey")) {
                        val placeHolderKey =
                            grievanceSnapshot.child("placeholderKey").getValue(String::class.java)
                        val placeHolderValue =
                            grievanceSnapshot.child("placeholderValue").getValue(String::class.java)

                        if (!placeHolderKey.isNullOrEmpty() && !placeHolderValue.isNullOrEmpty()) {

                            val newGrievance = Grievance(
                                grievanceId = "",
                                rollNumber = rollNumber,
                                status = "",
                                time = "",
                                placeHolderKey = placeHolderKey ?: "",
                                placeHolderValue = placeHolderValue ?: ""
                            )
                            grievances.add(newGrievance)
                        }
                    } else {
                        val grievanceId = grievanceSnapshot.child("grievanceId").getValue(String::class.java)
                        val rollNumber = grievanceSnapshot.child("rollNumber").getValue(String::class.java)
                        val status = grievanceSnapshot.child("status").getValue(String::class.java)
                        val time = grievanceSnapshot.child("time").getValue(String::class.java)
                        val date = grievanceSnapshot.child("date").getValue(String::class.java)
                        val grievance = grievanceSnapshot.child("grievance").getValue(String::class.java)
                        // Skip adding if any required field is null
                        if (!grievanceId.isNullOrEmpty() && !rollNumber.isNullOrEmpty() &&
                            !status.isNullOrEmpty() && !time.isNullOrEmpty() && !date.isNullOrEmpty() &&
                            !grievance.isNullOrEmpty()
                        ) {
                            val newGrievance = Grievance(
                                grievanceId = grievanceId,
                                rollNumber = rollNumber,
                                status = status,
                                time = time,
                                date = date,
                                grievance = grievance
                            )
                            grievances.add(newGrievance)
                        }
                    }
                }
                // Update the adapter with fetched grievances
                grievancesAdapter.submitList(grievances)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
                Log.e(TAG, "Error fetching data: ${databaseError.message}")
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

    data class Grievance(
        val grievanceId: String = "", // Unique ID for the grievance
        val grievance: String ="",
        val rollNumber: String = "", // Roll number of the user raising the grievance
        val time: String = "", // Reason for the grievance
        val date: String = "", // Date of the grievance
        val status: String = "", // Status of the grievance
        var placeHolderKey: String = "",
        var placeHolderValue: String = ""
    )

    class GrievanceAdapter :
        RecyclerView.Adapter<GrievanceAdapter.GrievanceViewHolder>() {

        private val grievanceList = mutableListOf<Grievance>()

        fun submitList(newList: List<Grievance>) {
            grievanceList.clear()
            grievanceList.addAll(newList)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GrievanceViewHolder {
            val itemView =
                LayoutInflater.from(parent.context).inflate(R.layout.item_grievance, parent, false)
            return GrievanceViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: GrievanceViewHolder, position: Int) {
            val grievance = grievanceList[position]
            holder.bind(grievance)
        }

        override fun getItemCount(): Int {
            return grievanceList.size
        }

        inner class GrievanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(grievance: Grievance) {
                val grievanceText = "${grievance.grievance}"
                itemView.findViewById<TextView>(R.id.textViewGrievance).text = grievanceText

                val dateText = "${grievance.date}"
                itemView.findViewById<TextView>(R.id.textViewDate).text = dateText

                val statusText = " ${grievance.status} "
                val statusTextView = itemView.findViewById<TextView>(R.id.textViewStatus)
                statusTextView.text = statusText

                // Check for null before setting background
                val backgroundResId = when (grievance.status) {
                    "pending" -> R.drawable.yellow_background
                    "resolved" -> R.drawable.green_background
                    "rejected" -> R.drawable.red_background
                    else -> R.drawable.rounded_corner_background // You can create a default background XML as well
                }
                statusTextView.setBackgroundResource(backgroundResId)
            }
        }
    }
}
