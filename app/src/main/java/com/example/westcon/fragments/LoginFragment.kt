package com.example.westcon.fragments

import androidx.compose.runtime.Composable
import com.example.westcon.ui.screens.LoginScreen
import com.example.westcon.data.FirebaseManager

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginFragment : BaseFragment() {
    @Composable
    override fun ScreenContent() {
        LoginScreen(
            onBackClick = { parentFragmentManager.popBackStack() },
            onLoginSuccess = { uid ->
                lifecycleScope.launch {
                    if (FirebaseManager.isProfileComplete(uid)) {
                        clearBackStackAndNavigate(DashboardFragment())
                    } else {
                        val email = FirebaseManager.getCurrentUser()?.email ?: ""
                        clearBackStackAndNavigate(SignUpStepTwoFragment.newInstance(email, ""))
                    }
                }
            },
            onSignUpClick = { navigateTo(SignUpFragment()) }
        )
    }
}
