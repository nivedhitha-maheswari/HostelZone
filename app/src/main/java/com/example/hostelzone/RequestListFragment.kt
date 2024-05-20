package com.example.hostelzone

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hostelzone.databinding.FragmentRequestListBinding
import com.google.firebase.database.*

class RequestListFragment : Fragment() {

    private var _binding: FragmentRequestListBinding? = null
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
        _binding = FragmentRequestListBinding.inflate(inflater, container, false)
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
                val requests = mutableListOf<Request>() // Use the Request class defined in RequestListFragment

                for (requestSnapshot in snapshot.children) {
                    val requestId = requestSnapshot.key ?: ""
                    val reason = requestSnapshot.child("reason").value.toString()
                    val requestedTime = requestSnapshot.child("requestedTime").value.toString()
                    val status = requestSnapshot.child("status").value.toString()

                    val rollNumber = requestSnapshot.child("rollNumber").value.toString()
                    val imageUrl = requestSnapshot.child("imageUrl").value.toString()

                    val request =
                        Request( // Use the Request class defined in RequestListFragment
                            requestId,
                            rollNumber,
                            reason,
                            requestedTime,
                            status,
                            imageUrl)
                    requests.add(request)
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

    data class Request(val requestId: String, val rollNumber: String, val reason: String, val requestedTime: String, val status: String, val imageUrl: String?)

    inner class RequestAdapter : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {
        private var requests: List<Request> = emptyList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tutor_request_list, parent, false)
            return RequestViewHolder(view)
        }

        override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
            holder.bind(requests[position])
        }

        override fun getItemCount(): Int {
            return requests.size
        }

        fun setRequests(requests: List<Request>) {
            this.requests = requests
            notifyDataSetChanged()
        }

        inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(request: Request) {
                val rollnumberText = "${request.rollNumber}"
                val reasonText = "${request.reason}"
                val timeText = "${request.requestedTime}"
                val statusText = " ${request.status} "

                itemView.findViewById<TextView>(R.id.textViewrequestRollNumber)?.text = rollnumberText
                itemView.findViewById<TextView>(R.id.textViewrequestReason)?.text = reasonText
                itemView.findViewById<TextView>(R.id.textviewRequestedDate)?.text = timeText
                itemView.findViewById<TextView>(R.id.textviewRequestedTime)?.text = timeText // Updated to match the XML

                val statusTextView = itemView.findViewById<TextView>(R.id.textViewrequestStatus)
                statusTextView?.text = statusText

                // Check for null before setting background
                val backgroundResId = when (request.status) {
                    "forwarded" -> R.drawable.green_background
                    "declined" -> R.drawable.red_background
                    else -> R.drawable.rounded_corner_background // You can create a default background XML as well
                }
                statusTextView?.setBackgroundResource(backgroundResId)

                // Load image into ImageView using Glide if imageUrl is not null
                val imageView = itemView.findViewById<ImageView>(R.id.imageViewProfile)
                if (!request.imageUrl.isNullOrEmpty()) {
                    Glide.with(itemView.context)
                        .load(request.imageUrl)
                        .into(imageView)
                } else {
                    imageView.setImageResource(R.drawable.stu_girl)
                }
            }
        }

    }
}
