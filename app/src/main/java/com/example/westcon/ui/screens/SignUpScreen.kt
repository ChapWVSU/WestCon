package com.example.westcon.ui.screens

import com.example.westcon.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.westcon.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.westcon.data.FirebaseManager
import com.example.westcon.ui.SignUpTextField
import com.example.westcon.ui.FooterSection

@Composable
fun RegisterScreen(onJoinClick: (String, String) -> Unit, onBackClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var isVerificationPending by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isVerificationPending) {
        while (isVerificationPending) {
            delay(3000) // Poll every 3 seconds
            val reloadResult = FirebaseManager.reloadUser()
            if (reloadResult.isSuccess && FirebaseManager.isEmailVerified()) {
                isVerificationPending = false
                onJoinClick(email, password) // Proceed automatically when verified
            }
        }
    }

    LaunchedEffect(resendCooldown) {
        while (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_login),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .statusBarsPadding()
                .padding(top = 110.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = null,
                tint = WestconYellow,
                modifier = Modifier.size(130.dp)
            )

            Spacer(modifier = Modifier.height(height = 20.dp))
            Text(
                text = "Welcome,\nTaga-WEST!",
                modifier = Modifier
                    .padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
                    .offset(y = (-30).dp),
                color = Color.White,
                fontSize = 42.sp,
                lineHeight = 46.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MomotrustFontFamily
            )

            if (errorMessage != null) {
                val isSuccess = errorMessage!!.contains("sent")
                Text(
                    errorMessage!!, 
                    color = if (isSuccess) Color(0xFF4CAF50) else Color.Red, 
                    fontSize = 12.sp, 
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(0.dp))

            // Fields
            SignUpTextField(
                value = email,
                onValueChange = { if (!isVerificationPending) email = it },
                label = "WVSU email",
                icon = R.drawable.email
            )

            if (isVerificationPending) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = WestconYellow.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WestconYellow)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = WestconYellow,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "waiting for email confirmation...",
                                fontSize = 13.sp,
                                color = WestconYellow,
                                fontWeight = FontWeight.Bold,
                                fontFamily = MomotrustFontFamily
                            )
                        }
                        
                        if (resendCooldown > 0) {
                            Text(
                                "You can resend the link in ${resendCooldown}s",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                modifier = Modifier.padding(start = 28.dp, top = 4.dp),
                                fontFamily = MomotrustFontFamily
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Password fields
            SignUpTextField(
                value = password,
                onValueChange = { if (!isVerificationPending) password = it },
                label = "Password",
                icon = R.drawable.lock,
                isPassword = true,
                showEyeIcon = true
            )
            if (!isVerificationPending) {
                SignUpTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Re-enter password",
                    icon = R.drawable.lock,
                    isPassword = true,
                    showEyeIcon = false
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (!isVerificationPending) {
                Button(
                    onClick = {
                        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                            errorMessage = "Please fill all fields"
                            return@Button
                        }
                        if (!email.endsWith("@wvsu.edu.ph")) {
                            errorMessage = "Please use your WVSU email (@wvsu.edu.ph)"
                            return@Button
                        }
                        if (password != confirmPassword) {
                            errorMessage = "Passwords do not match"
                            return@Button
                        }
                        
                        val hasUppercase = password.any { it.isUpperCase() }
                        val hasLowercase = password.any { it.isLowerCase() }
                        val hasDigit = password.any { it.isDigit() }
                        val hasSpecial = password.any { !it.isLetterOrDigit() }
                        
                        if (password.length < 8 || !hasUppercase || !hasLowercase || !hasDigit || !hasSpecial) {
                            errorMessage = "Password must be at least 8 characters and contain 1 uppercase, 1 lowercase, 1 number, and 1 special character"
                            return@Button
                        }
                        
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            val authResult = FirebaseManager.signUp(email, password)
                            if (authResult.isSuccess) {
                                val verResult = FirebaseManager.sendEmailVerification()
                                isLoading = false
                                if (verResult.isSuccess) {
                                    isVerificationPending = true
                                    resendCooldown = 60
                                    errorMessage = "Verification email sent. Please check your inbox and spam folder."
                                } else {
                                    errorMessage = "Account created, but failed to send verification email."
                                }
                            } else {
                                isLoading = false
                                val exception = authResult.exceptionOrNull()
                                errorMessage = when {
                                    exception?.message?.contains("email address is already in use") == true -> 
                                        "This email is already registered"
                                    exception?.message?.contains("badly formatted") == true ->
                                        "Invalid email format"
                                    else -> exception?.message ?: "Signup failed"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF001229))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = WestconYellow, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Join WESTCON", color = WestconYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = MomotrustFontFamily)
                    }
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            val result = FirebaseManager.sendEmailVerification()
                            if (result.isSuccess) {
                                resendCooldown = 60
                                errorMessage = "Verification email resent!"
                            } else {
                                errorMessage = "Failed to resend. Please wait."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = resendCooldown == 0 && !isLoading,
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF001229))
                ) {
                    Text(
                        if (resendCooldown > 0) "Wait ${resendCooldown}s to Resend" else "Resend Verification Email", 
                        color = if (resendCooldown > 0) Color.Gray else WestconYellow, 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold, 
                        fontFamily = MomotrustFontFamily
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = { 
                        isVerificationPending = false
                        FirebaseManager.logout()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cancel Registration", color = Color.Red.copy(alpha = 0.8f), fontFamily = MomotrustFontFamily)
                }
            }

            if (!isVerificationPending) {
                TextButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Go Back", color = Color.White.copy(alpha = 0.7f), fontFamily = MomotrustFontFamily)
                }
            }
        }
        FooterSection(Modifier.align(Alignment.BottomCenter))
    }
}
