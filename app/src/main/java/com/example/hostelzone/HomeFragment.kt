package com.example.hostelzone

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the TextViews by their IDs
        val grievanceCardView = view.findViewById<androidx.cardview.widget.CardView>(R.id.grievanceCardView)
        val permissionCardView = view.findViewById<androidx.cardview.widget.CardView>(R.id.requestCardView)

        // Set OnClickListener for Grievance TextView
        grievanceCardView.setOnClickListener {
            // Navigate to GrievanceFragment
            navigateToFragment(GrievancesFragment())
        }

        // Set OnClickListener for Permission TextView
        permissionCardView.setOnClickListener {
            // Navigate to LabPermissionFragment
            navigateToFragment(LabPermissionFragment())
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
