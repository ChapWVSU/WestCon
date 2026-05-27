package com.example.westcon

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import com.example.westcon.fragments.OnboardingFragment
import com.example.westcon.fragments.DashboardFragment
import com.example.westcon.data.FirebaseManager
import com.google.firebase.FirebaseApp

import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.westcon.fragments.SignUpStepTwoFragment

import com.example.westcon.ui.theme.WestconYellow

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Makes the UI draw behind the status bar for that full-screen background effect
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            val container = findViewById<android.view.ViewGroup>(R.id.fragment_container)
            
            // Create a temporary loading view
            val loadingView = ComposeView(this).apply {
                setContent {
                    Box(
                        modifier = Modifier.fillMaxSize().background(com.example.westcon.ui.theme.WestconDarkBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = WestconYellow)
                    }
                }
            }
            container.addView(loadingView)

            lifecycleScope.launch {
                try {
                    val user = FirebaseManager.getCurrentUser()
                    val startFragment = if (user != null && FirebaseManager.isEmailVerified()) {
                        if (FirebaseManager.isProfileComplete(user.uid)) {
                            DashboardFragment()
                        } else {
                            // Logged in but profile not saved - redirect to Step Two
                            SignUpStepTwoFragment.newInstance(user.email ?: "", "")
                        }
                    } else {
                        // If logged in but not verified, log out to be safe
                        if (FirebaseManager.isUserLoggedIn()) FirebaseManager.logout()
                        OnboardingFragment()
                    }

                    // Remove loading view and add fragment
                    container.removeView(loadingView)
                    supportFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace(R.id.fragment_container, startFragment)
                    }
                } catch (e: Exception) {
                    // Fallback to onboarding on error
                    container.removeView(loadingView)
                    FirebaseManager.logout()
                    supportFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace(R.id.fragment_container, OnboardingFragment())
                    }
                }
            }
        }
    }
}
