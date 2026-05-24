package com.example.westcon.fragments

import androidx.compose.runtime.Composable
import com.example.westcon.ui.screens.LoginScreen

class LoginFragment : BaseFragment() {
    @Composable
    override fun ScreenContent() {
        LoginScreen(
            onBackClick = { parentFragmentManager.popBackStack() },
            onLoginSuccess = { clearBackStackAndNavigate(DashboardFragment()) },
            onSignUpClick = { navigateTo(SignUpFragment()) }
        )
    }
}
