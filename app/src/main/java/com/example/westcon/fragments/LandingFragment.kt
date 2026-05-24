package com.example.westcon.fragments

import androidx.compose.runtime.Composable
import com.example.westcon.ui.screens.LandingScreen

class LandingFragment : BaseFragment() {
    @Composable
    override fun ScreenContent() {
        LandingScreen(
            onSignUpClick = { navigateTo(SignUpFragment()) },
            onLoginClick = { navigateTo(LoginFragment()) }
        )
    }
}
