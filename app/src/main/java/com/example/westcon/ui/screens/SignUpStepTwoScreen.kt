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
import com.example.westcon.data.FirebaseManager
import com.example.westcon.data.UserProfile
import com.example.westcon.ui.SignUpTextField
import com.example.westcon.ui.FooterSection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.example.westcon.ui.UIUtils
import kotlinx.coroutines.delay

@Composable
fun SignUpStepTwoScreen(
    email: String = "",
    password: String = "",
    onNextClick: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var selectedIcon by remember { mutableStateOf("Person") }
    var username by remember { mutableStateOf("") }
    var college by remember { mutableStateOf("CICT") }
    var program by remember { mutableStateOf("BSCS") }
    var year by remember { mutableStateOf("1") }
    
    // Step 2 skills
    val predefinedSkills = listOf("React & Next.js", "Python Basics", "Academic Writing", "UI/UX Logic")
    val selectedSkills = remember { mutableStateListOf<String>() }
    var otherSkill by remember { mutableStateOf("") }
    var showOtherField by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var resendCooldown by remember { mutableIntStateOf(0) }
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
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
            // App Icon Branding
            Icon(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = null,
                tint = WestconYellow,
                modifier = Modifier.size(110.dp).offset(x = (-10).dp)
            )
            
            Spacer(modifier = Modifier.height(10.dp))

            if (step == 0) {
                Text(
                    text = "Pick your\nprofile avatar",
                    color = Color.White,
                    fontSize = 42.sp,
                    lineHeight = 48.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MomotrustFontFamily
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(UIUtils.availableIcons) { iconName ->
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(if (selectedIcon == iconName) WestconYellow.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
                                .border(2.dp, if (selectedIcon == iconName) WestconYellow else Color.Transparent, CircleShape)
                                .clickable { selectedIcon = iconName }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                UIUtils.getProfileIcon(iconName),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = if (selectedIcon == iconName) WestconYellow else Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = { step = 1 },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF001229))
                ) {
                    Text("Continue", color = WestconYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = MomotrustFontFamily)
                }
            } else if (step == 1) {
                Text(
                    text = "Before we begin,\ntell us about\nyourself!",
                    color = Color.White,
                    fontSize = 42.sp,
                    lineHeight = 48.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MomotrustFontFamily
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }

                Spacer(modifier = Modifier.height(30.dp))

                SignUpTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username",
                    icon = R.drawable.person
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        DropdownLabel("College")
                        CustomDropdown(listOf("CICT", "CON", "COE", "CAS"), college) { college = it }
                    }
                    Column(modifier = Modifier.weight(1.2f)) {
                        DropdownLabel("Program")
                        CustomDropdown(listOf("BSIT", "BSCS", "BSIS"), program) { program = it }
                    }
                    Column(modifier = Modifier.weight(0.8f)) {
                        DropdownLabel("Year Lvl.")
                        CustomDropdown(listOf("1", "2", "3", "4"), year) { year = it }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = {
                        if (username.isEmpty()) {
                            errorMessage = "Please enter a username"
                            return@Button
                        }
                        scope.launch {
                            isLoading = true
                            if (FirebaseManager.checkUsernameExists(username)) {
                                errorMessage = "Username already taken"
                                isLoading = false
                            } else {
                                errorMessage = null
                                isLoading = false
                                step = 2
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF001229))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = WestconYellow, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Next", color = WestconYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = MomotrustFontFamily)
                    }
                }
            } else if (step == 2) {
                Text(
                    text = "What skills can\nyou share with\nothers?",
                    color = Color.White,
                    fontSize = 42.sp,
                    lineHeight = 48.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MomotrustFontFamily
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text("Select skills you can teach:", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    predefinedSkills.forEach { skill ->
                        val isSelected = selectedSkills.contains(skill)
                        FilterChip(
                            selected = isSelected,
                            onClick = { if (isSelected) selectedSkills.remove(skill) else selectedSkills.add(skill) },
                            label = { Text(skill) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WestconYellow,
                                selectedLabelColor = Color.Black,
                                containerColor = Color.White.copy(alpha = 0.1f),
                                labelColor = Color.White
                            )
                        )
                    }
                    
                    FilterChip(
                        selected = showOtherField,
                        onClick = { showOtherField = !showOtherField },
                        label = { Text("Others") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WestconYellow,
                            selectedLabelColor = Color.Black,
                            containerColor = Color.White.copy(alpha = 0.1f),
                            labelColor = Color.White
                        )
                    )
                }
                
                if (showOtherField) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SignUpTextField(
                        value = otherSkill,
                        onValueChange = { otherSkill = it },
                        label = "Type your skill...",
                        icon = R.drawable.tdesign_education_filled
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = { step = 3 },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF001229))
                ) {
                    Text("Review Details", color = WestconYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = MomotrustFontFamily)
                }
                
                TextButton(
                    onClick = { step = 1 },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Go Back", color = Color.White.copy(alpha = 0.7f), fontFamily = MomotrustFontFamily)
                }
            } else if (step == 3) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Please confirm\nyour details",
                        color = Color.White,
                        fontSize = 42.sp,
                        lineHeight = 48.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MomotrustFontFamily
                    )
                    
                    Spacer(modifier = Modifier.height(30.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Email", fontSize = 12.sp, color = Color.Gray)
                            Text(email, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WestconDarkBlue)

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Password", fontSize = 12.sp, color = Color.Gray)
                            Text("•".repeat(password.length), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WestconDarkBlue)

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Username", fontSize = 12.sp, color = Color.Gray)
                            Text(username, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WestconDarkBlue)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text("Education", fontSize = 12.sp, color = Color.Gray)
                            Text("$college | $program - Year $year", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WestconDarkBlue)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text("Skills I Can Teach", fontSize = 12.sp, color = Color.Gray)
                            val finalSkills = selectedSkills.toMutableList()
                            if (showOtherField && otherSkill.isNotBlank()) {
                                finalSkills.add(otherSkill.trim())
                            }
                            if (finalSkills.isEmpty()) {
                                Text("None selected", fontSize = 16.sp, color = Color.DarkGray)
                            } else {
                                Text(finalSkills.joinToString(", "), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WestconDarkBlue)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    if (errorMessage != null) {
                        val isSuccess = errorMessage!!.contains("successful")
                        Text(
                            errorMessage!!, 
                            color = if (isSuccess) Color(0xFF4CAF50) else Color.Red, 
                            fontSize = 12.sp, 
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                val finalSkillsToSave = selectedSkills.toMutableList()
                                if (showOtherField && otherSkill.isNotBlank()) {
                                    finalSkillsToSave.add(otherSkill.trim())
                                }

                                val profile = UserProfile(
                                    uid = FirebaseManager.getCurrentUser()?.uid ?: "",
                                    name = username,
                                    email = email,
                                    profileIconName = selectedIcon,
                                    department = college,
                                    course = program,
                                    year = year,
                                    skillsToTeach = finalSkillsToSave.map { com.example.westcon.data.SkillMastery(skillName = it) }
                                )
                                
                                val profileResult = FirebaseManager.saveUserProfile(profile)
                                isLoading = false
                                if (profileResult.isSuccess) {
                                    onNextClick()
                                } else {
                                    errorMessage = "Failed to save profile: ${profileResult.exceptionOrNull()?.message}"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF001229))
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = WestconYellow, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Confirm & Finish", color = WestconYellow, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = MomotrustFontFamily)
                        }
                    }
                    
                    TextButton(
                        onClick = { step = 2 },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        enabled = !isLoading
                    ) {
                        Text("Go Back to Edit", color = Color.White.copy(alpha = 0.7f), fontFamily = MomotrustFontFamily)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
        FooterSection(Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun DropdownLabel(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = MomotrustFontFamily,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDropdown(options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor(),
            shape = RoundedCornerShape(15.dp),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MomotrustFontFamily
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onOptionSelected(selectionOption)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
