package com.example.hostelzone

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hostelzone.databinding.FragmentTutorPendingRequestBinding
import com.google.firebase.database.*

class TutorPendingRequestFragment : Fragment() {

    private var _binding: FragmentTutorPendingRequestBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RequestAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

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
        val userId = sharedPreferences.getString("userId", "")
        if (!userId.isNullOrEmpty()) {
            fetchTutoringClassFromFirebase(userId)
        } else {
            Toast.makeText(requireContext(), "User ID not found in SharedPreferences", Toast.LENGTH_SHORT).show()
        }
        databaseReference = FirebaseDatabase.getInstance().reference.child("requests")
    }

    private fun fetchTutoringClassFromFirebase(userId: String) {
        val userReference = FirebaseDatabase.getInstance().reference.child("users").child(userId)
        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val tutoringClass = dataSnapshot.child("additionalData").child("tutoringClass").getValue(String::class.java)
                val tutoringClassGroup = dataSnapshot.child("additionalData").child("tutoringClassGroup").getValue(String::class.java)
                val tutoringClassYear = dataSnapshot.child("additionalData").child("tutoringClassYear").getValue(String::class.java)

                if (!tutoringClass.isNullOrEmpty() && !tutoringClassGroup.isNullOrEmpty() && !tutoringClassYear.isNullOrEmpty()) {
                    fetchRequests(tutoringClass, tutoringClassGroup, tutoringClassYear)
                } else {
                    Toast.makeText(requireContext(), "Tutoring class information not found for the user", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching tutoring class information: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchRequests(tutoringClass: String, tutoringClassGroup: String, tutoringClassYear: String) {
        val query = databaseReference.orderByChild("course").equalTo(tutoringClass)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val requests = mutableListOf<Request>()
                if (snapshot.exists()) {
                    for (requestSnapshot in snapshot.children) {
                        val requestId = requestSnapshot.key ?: ""
                        val course = requestSnapshot.child("course").value.toString()
                        val year = requestSnapshot.child("year").value.toString()
                        val group = requestSnapshot.child("group").value.toString()
                        val status = requestSnapshot.child("status").value.toString()
                        if (course == tutoringClass && year == tutoringClassYear && group == tutoringClassGroup && status == "submitted") {
                            val rollNumber = requestSnapshot.child("rollNumber").value.toString()
                            val reason = requestSnapshot.child("reason").value.toString()
                            val requestedTime = requestSnapshot.child("requestedTime").value.toString()
                            val request = Request(requestId, rollNumber, reason, requestedTime)
                            requests.add(request)
                        }
                    }
                    adapter.setRequests(requests)
                } else {
                    Toast.makeText(requireContext(), "No requests found for tutoring class: $tutoringClass", Toast.LENGTH_SHORT).show()
                }
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

    data class Request(val requestId: String, val rollNumber: String, val reason: String, val requestedTime: String)

    inner class RequestAdapter : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {
        private var requests: MutableList<Request> = mutableListOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tutor_new_request_list, parent, false)
            return RequestViewHolder(view)
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

        inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(request: Request) {
                val requestId = request.requestId

                itemView.findViewById<TextView>(R.id.textViewrequestRollNumber).text = request.rollNumber
                itemView.findViewById<TextView>(R.id.textViewrequestReason).text = request.reason
                itemView.findViewById<TextView>(R.id.textviewRequestedTime).text = request.requestedTime

                val forwardButton = itemView.findViewById<Button>(R.id.forwardRequestButton)
                val rejectButton = itemView.findViewById<Button>(R.id.rejectRequestButton)

                forwardButton.setOnClickListener {
                    updateRequestStatus(requestId, "forwarded")
                    // Reload the page after button click
                    reloadPage()
                }

                rejectButton.setOnClickListener {
                    updateRequestStatus(requestId, "declined")
                    // Reload the page after button click
                    reloadPage()
                }
            }

            private fun reloadPage() {
                // Clear the existing list and fetch data again to refresh the page
                clearRequests()
                // Fetch data again to refresh the page
                sharedPreferences = requireContext().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
                val userId = sharedPreferences.getString("userId", "")
                if (!userId.isNullOrEmpty()) {
                    fetchTutoringClassFromFirebase(userId)
                } else {
                    Toast.makeText(requireContext(), "User ID not found in SharedPreferences", Toast.LENGTH_SHORT).show()
                }
            }

            private fun updateRequestStatus(requestId: String, newStatus: String) {
                val requestRef = databaseReference.child(requestId)
                requestRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            requestRef.child("status").setValue(newStatus)
                                .addOnSuccessListener {
                                    Toast.makeText(itemView.context, "Request $newStatus successfully", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(itemView.context, "Failed to update status: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(itemView.context, "Request $requestId not found", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(itemView.context, "Error updating status: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }
}
