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
import com.example.westcon.ui.FooterSection

@Composable
fun LandingScreen(onSignUpClick: () -> Unit, onLoginClick: () -> Unit) {
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
                .padding(top = 220.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.icon),
                    contentDescription = null,
                    tint = WestconYellow,
                    modifier = Modifier.size(110.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("WESTCON", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFamily = MomotrustFontFamily)
                    Text("THE OFFICIAL STUDENT SKILL\nMARKETPLACE", color = Color.White, fontSize = 10.sp, lineHeight = 12.sp, fontFamily = MomotrustFontFamily)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("WANT TO CONNECT WITH FELLOW\nTAGA-WESTS?", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp, fontFamily = MomotrustFontFamily)

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onSignUpClick,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WestconDarkBlue)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.email), null, tint = WestconYellow, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Sign up with Email", color = WestconYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = MomotrustFontFamily)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.email),
                        contentDescription = null,
                        tint = WestconDarkBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Login with Email",
                        color = WestconDarkBlue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MomotrustFontFamily
                    )
                }
            }
        }

        FooterSection(Modifier.align(Alignment.BottomCenter))
    }
}
