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
        recyclerView = binding.recyclerViewRequests
        labPermissionAdapter = LabPermissionAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = labPermissionAdapter
        sharedPreferences = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", "")
        if (!userId.isNullOrEmpty()) {
            fetchUserDataFromFirebase(userId)
        } else {
            Toast.makeText(
                requireContext(),
                "User ID not found in SharedPreferences",
                Toast.LENGTH_SHORT
            ).show()
        }
        databaseReference = FirebaseDatabase.getInstance().reference
        val fabNewLabPermission = view.findViewById<FloatingActionButton>(R.id.fabNewRequest)
        fabNewLabPermission.setOnClickListener {
            val newFragment = NewLabPermissionFragment()
            val fragmentManager = requireActivity().supportFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.fragment_container, newFragment)
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
        }
    }

    private fun fetchUserDataFromFirebase(userId: String) {
        val userReference = FirebaseDatabase.getInstance().reference.child("users").child(userId)
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val degree = dataSnapshot.child("additionalData").child("degree").getValue(String::class.java)
                val course = dataSnapshot.child("additionalData").child("course").getValue(String::class.java)
                val year = dataSnapshot.child("additionalData").child("year").getValue(String::class.java)
                val group = dataSnapshot.child("additionalData").child("group").getValue(String::class.java)
                val rollNumber = dataSnapshot.child("additionalData").child("rollNumber").getValue(String::class.java)
                if (!degree.isNullOrEmpty() && !course.isNullOrEmpty() && !year.isNullOrEmpty() && !group.isNullOrEmpty() && !rollNumber.isNullOrEmpty()) {
                    fetchLabPermissionsByRollNumber(rollNumber, degree, course, year, group)
                } else {
                    Toast.makeText(requireContext(), "Incomplete user data", Toast.LENGTH_SHORT).show()
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
        val query = requestRef.orderByChild("rollNumber").equalTo(rollNumber)


        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val requests = mutableListOf<LabPermission>()
                for (requestSnapshot in dataSnapshot.children) {
                    if (requestSnapshot.hasChild("placeholderKey")) {
                        val placeHolderKey =
                            requestSnapshot.child("placeholderKey").getValue(String::class.java)
                        val placeHolderValue =
                            requestSnapshot.child("placeholderValue").getValue(String::class.java)
                        if (!placeHolderKey.isNullOrEmpty() && !placeHolderValue.isNullOrEmpty()) {

                            val newRequest = LabPermission(
                                labPermissionId = "",
                                rollNumber = rollNumber,
                                status = "",
                                requestedTime = "",
                                placeHolderKey = placeHolderKey ?: "",
                                placeHolderValue = placeHolderValue ?: ""
                            )
                            requests.add(newRequest)
                        }
                    }else {
                        val labPermissionId =
                            requestSnapshot.child("labPermissionId").getValue(String::class.java)
                        val rollNumber =
                            requestSnapshot.child("rollNumber").getValue(String::class.java)
                        val reason = requestSnapshot.child("reason").getValue(String::class.java)
                        val requestedTime =
                            requestSnapshot.child("requestedTime").getValue(String::class.java)
                        val currentTime =
                            requestSnapshot.child("currentTime").getValue(String::class.java)
                        val status = requestSnapshot.child("status").getValue(String::class.java)

                        if (!labPermissionId.isNullOrEmpty() && !rollNumber.isNullOrEmpty() &&
                            !reason.isNullOrEmpty() && !requestedTime.isNullOrEmpty() &&
                            !currentTime.isNullOrEmpty() && !status.isNullOrEmpty()
                        ) {
                            val newRequest = LabPermission(
                                labPermissionId = labPermissionId ?: "",
                                rollNumber = rollNumber ?: "",
                                reason = reason ?: "",
                                requestedTime = requestedTime ?: "",
                                currentTime = currentTime ?: "",
                                status = status ?: ""
                            )
                            requests.add(newRequest)
                        }
                    }
                }
                labPermissionAdapter.submitList(requests)
            }

            override fun onCancelled(databaseError: DatabaseError) {
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

    data class LabPermission(
        val labPermissionId: String = "",
        val rollNumber: String = "",
        val reason: String = "",
        val requestedTime: String = "",
        val currentTime: String = "",
        val status: String = "",
        var placeHolderKey: String = "",
        var placeHolderValue: String = ""

    ) {
        override fun toString(): String {
            return "LabPermission(labPermissionId='$labPermissionId', rollNumber='$rollNumber', reason='$reason', requestedTime='$requestedTime',currentTime='$currentTime', status='$status')"
        }
    }

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

                val currentTimeText = "${labPermission.currentTime}"
                itemView.findViewById<TextView>(R.id.textViewAtTime).text = currentTimeText

                val statusText = " ${labPermission.status} "
                val statusTextView = itemView.findViewById<TextView>(R.id.textViewStatus)
                statusTextView.text = statusText

                // Check for null before setting background
                val backgroundResId = when (labPermission.status) {
                    "forwarded"-> R.drawable.yellow_background
                    "approved" -> R.drawable.green_background
                    "declined" -> R.drawable.red_background
                    else -> R.drawable.rounded_corner_background // You can create a default background XML as well
                }
                statusTextView.setBackgroundResource(backgroundResId)
            }
        }
    }
}