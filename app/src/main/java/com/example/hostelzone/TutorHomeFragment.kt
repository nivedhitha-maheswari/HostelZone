package com.example.hostelzone

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

class TutorHomeFragment : Fragment(R.layout.fragment_tutor_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the TextViews by their IDs
        val pendingCardView = view.findViewById<androidx.cardview.widget.CardView>(R.id.pendingCardView)
        val requestCardView = view.findViewById<androidx.cardview.widget.CardView>(R.id.requestListCardView)

        // Set OnClickListener for Grievance TextView
        pendingCardView.setOnClickListener {
            // Navigate to GrievanceFragment
            navigateToFragment(TutorPendingRequestFragment())
        }

        // Set OnClickListener for Permission TextView
        requestCardView.setOnClickListener {
            // Navigate to LabPermissionFragment
            navigateToFragment(RequestListFragment())
        }
    }

    private fun navigateToFragment(fragment: Fragment) {
        // Get the FragmentManager
        val fragmentManager: FragmentManager = requireActivity().supportFragmentManager
        // Begin a new transaction
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        // Replace the current fragment with the new one
        fragmentTransaction.replace(R.id.fragment_container, fragment)
        // Add the transaction to the back stack (optional)
        fragmentTransaction.addToBackStack(null)
        // Commit the transaction
        fragmentTransaction.commit()
    }
}
