package com.example.westcon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.westcon.R
import com.example.westcon.ui.theme.*

@Composable
fun SignUpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: Int,
    isPassword: Boolean = false,
    showEyeIcon: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = Color.Gray) },
        leadingIcon = {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp), tint = WestconDarkBlue)
        },
        trailingIcon = {
            if (isPassword && showEyeIcon) {
                val image = if (passwordVisible)
                    painterResource(id = R.drawable.secret)
                else
                    painterResource(id = R.drawable.secret_on)

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(painter = image, contentDescription = null, modifier = Modifier.size(24.dp), tint = WestconDarkBlue)
                }
            }
        },
        visualTransformation = if (isPassword && !passwordVisible)
            PasswordVisualTransformation()
        else
            VisualTransformation.None,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(50),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = WestconYellow,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        ),
        singleLine = true
    )
}

@Composable
fun FooterSection(modifier: Modifier) {
    Column(
        modifier = modifier.padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Privacy Policy", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontFamily = MomotrustFontFamily)
            Text("  •  ", color = Color.White.copy(alpha = 0.8f))
            Text("Terms of Service", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontFamily = MomotrustFontFamily)
        }
    }
}
