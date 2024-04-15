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
import com.example.hostelzone.databinding.FragmentGrievancesBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*

class GrievancesFragment : Fragment() {

    private var _binding: FragmentGrievancesBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var grievanceAdapter: GrievanceAdapter
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
        grievanceAdapter = GrievanceAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = grievanceAdapter

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
        databaseReference = FirebaseDatabase.getInstance().reference.child("grievances")
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

    private fun fetchRollNumberFromFirebase(userId: String) {
        // Construct database reference to the user node
        val userReference = FirebaseDatabase.getInstance().reference.child("users").child(userId)

        // Retrieve roll number from user data
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val rollNumber = dataSnapshot.child("additionalData").child("rollNumber").getValue(String::class.java)
                if (!rollNumber.isNullOrEmpty()) {
                    // Roll number is available, fetch grievances by roll number
                    fetchGrievancesByRollNumber(rollNumber)
                } else {
                    Toast.makeText(requireContext(), "Roll number not found for the user", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching roll number: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchGrievancesByRollNumber(rollNumber: String) {
        databaseReference.orderByChild("rollNumber").equalTo(rollNumber).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val grievances = mutableListOf<Grievance>()
                for (snapshot in dataSnapshot.children) {
                    val grievance = snapshot.getValue(Grievance::class.java)
                    grievance?.let { grievances.add(it) }
                }
                // Update the adapter with fetched grievances
                grievanceAdapter.submitList(grievances)
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

    data class Grievance(
        val grievanceId: String = "", // Unique ID for the grievance
        val rollNumber: String = "", // Roll number of the user submitting the grievance
        val date: String = "",
        val time: String = "",
        val grievance: String = "",
        val status: String = ""
    )

    class GrievanceAdapter : RecyclerView.Adapter<GrievanceAdapter.GrievanceViewHolder>() {

        private val grievanceList = mutableListOf<Grievance>()

        fun submitList(newList: List<Grievance>) {
            grievanceList.clear()
            grievanceList.addAll(newList)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GrievanceViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_grievance, parent, false)
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
                itemView.findViewById<TextView>(R.id.textViewStatus).text = statusText
            }
        }

    }
}
