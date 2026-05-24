package com.example.westcon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.westcon.data.*
import com.example.westcon.ui.theme.WestconDarkBlue
import com.example.westcon.ui.theme.WestconYellow
import com.example.westcon.ui.theme.MomotrustFontFamily
import com.example.westcon.ui.theme.White
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExchangeDialog(targetPost: com.example.westcon.data.SkillPost, onDismiss: () -> Unit) {
    var offeredSkill by remember { mutableStateOf(if (targetPost.postType == "FIND") targetPost.title else "") }
    var wantedSkill by remember { mutableStateOf(if (targetPost.postType == "SHARE") targetPost.title else "") }
    var isLoading by remember { mutableStateOf(false) }
    var currentUserProfile by remember { mutableStateOf<com.example.westcon.data.UserProfile?>(null) }
    var authorProfile by remember { mutableStateOf<com.example.westcon.data.UserProfile?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val currentUser = FirebaseManager.getCurrentUser()
        if (currentUser != null) {
            currentUserProfile = FirebaseManager.getUserProfile(currentUser.uid)
        }
        authorProfile = FirebaseManager.getUserProfile(targetPost.authorUid)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = { Text("Skill Exchange", fontWeight = FontWeight.Bold, color = WestconDarkBlue, fontFamily = MomotrustFontFamily) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (targetPost.postType == "SHARE") {
                    Text(
                        "You want to learn '${targetPost.title}' from ${targetPost.authorName}.",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                    
                    if (currentUserProfile != null && currentUserProfile!!.skillsToTeach.isNotEmpty()) {
                        Text("Offer one of your skills in exchange:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WestconDarkBlue)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            currentUserProfile!!.skillsToTeach.forEach { skill ->
                                val isSelected = offeredSkill.equals(skill.skillName, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { offeredSkill = skill.skillName },
                                    label = { Text(skill.skillName, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WestconYellow,
                                        selectedLabelColor = WestconDarkBlue,
                                        containerColor = Color(0xFFF1F5F9),
                                        labelColor = Color.Gray
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = offeredSkill,
                        onValueChange = { offeredSkill = it },
                        label = { Text("Offer this skill") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WestconDarkBlue,
                            unfocusedTextColor = WestconDarkBlue,
                            focusedBorderColor = WestconDarkBlue,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                        )
                    )
                } else {
                    // FIND Post
                    Text(
                        "${targetPost.authorName} is looking for '${targetPost.title}'. Select which of your skills you'll teach them and what you want to learn in return.",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )

                    // 1. Offer one of YOUR skills (The one the author is looking for)
                    if (currentUserProfile != null && currentUserProfile!!.skillsToTeach.isNotEmpty()) {
                        Text("Confirm which of your skills you are offering:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WestconDarkBlue)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            currentUserProfile!!.skillsToTeach.forEach { skill ->
                                val isSelected = offeredSkill.equals(skill.skillName, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { offeredSkill = skill.skillName },
                                    label = { Text(skill.skillName, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WestconYellow,
                                        selectedLabelColor = WestconDarkBlue,
                                        containerColor = Color(0xFFF1F5F9),
                                        labelColor = Color.Gray
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = offeredSkill,
                        onValueChange = { offeredSkill = it },
                        label = { Text("Offer this skill") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WestconDarkBlue,
                            unfocusedTextColor = WestconDarkBlue,
                            focusedBorderColor = WestconDarkBlue,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Select a skill from the AUTHOR to learn
                    if (authorProfile != null && authorProfile!!.skillsToTeach.isNotEmpty()) {
                        Text("Select a skill from ${targetPost.authorName} you want to learn:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WestconDarkBlue)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            authorProfile!!.skillsToTeach.forEach { skill ->
                                val isSelected = wantedSkill.equals(skill.skillName, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { wantedSkill = skill.skillName },
                                    label = { Text(skill.skillName, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WestconYellow,
                                        selectedLabelColor = WestconDarkBlue,
                                        containerColor = Color(0xFFF1F5F9),
                                        labelColor = Color.Gray
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (offeredSkill.isNotBlank() && wantedSkill.isNotBlank()) {
                        isLoading = true
                        scope.launch {
                            val currentUser = FirebaseManager.getCurrentUser()
                            val profile = currentUserProfile ?: (currentUser?.let { FirebaseManager.getUserProfile(it.uid) })
                            
                            val notificationContent = if (targetPost.postType == "SHARE") {
                                "${profile?.name ?: "Someone"} wants to learn '${targetPost.title}' from you and offered to teach '${offeredSkill.trim()}'!"
                            } else {
                                "${profile?.name ?: "Someone"} offered to teach you '${targetPost.title}' and wants to learn '${wantedSkill.trim()}' from you!"
                            }

                            val notification = com.example.westcon.data.Notification(
                                receiverUid = targetPost.authorUid,
                                type = "SKILL_EXCHANGE",
                                title = if (targetPost.postType == "SHARE") "Skill Request" else "Skill Offer",
                                content = notificationContent,
                                senderUid = currentUser?.uid,
                                senderName = profile?.name,
                                senderIconName = profile?.profileIconName ?: "Person",
                                senderDept = profile?.department,
                                skillOffered = offeredSkill.trim(),
                                skillWanted = wantedSkill.trim()
                            )
                            
                            FirebaseManager.sendNotification(notification)
                            isLoading = false
                            onDismiss()
                        }
                    }
                },
                enabled = !isLoading && offeredSkill.isNotBlank() && wantedSkill.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = WestconDarkBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Send Request", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun RateUserDialog(
    otherUserName: String,
    exchange: SkillExchange,
    currentUid: String,
    onDismiss: () -> Unit,
    onRateSubmitted: (Double, Double, String, String) -> Unit,
    onMarkDone: () -> Unit
) {
    var teachingRating by remember { mutableDoubleStateOf(5.0) }
    var learningRating by remember { mutableDoubleStateOf(5.0) }
    
    // Skill logic:
    // Skill THEY taught ME (I rate their Teaching efficiency)
    val skillTheyTaught = if (exchange.requesterUid == currentUid) exchange.skillWanted else exchange.skillOffered
    // Skill I taught THEM (I rate their Learning progress)
    val skillTheyLearned = if (exchange.requesterUid == currentUid) exchange.skillOffered else exchange.skillWanted
    
    val iMarkedDone = if (exchange.requesterUid == currentUid) exchange.requesterMarkedDone else exchange.responderMarkedDone
    val otherUserMarkedDone = if (exchange.requesterUid == currentUid) exchange.responderMarkedDone else exchange.requesterMarkedDone
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Session Feedback",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = WestconDarkBlue,
                    fontFamily = MomotrustFontFamily
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (!iMarkedDone) {
                    Button(
                        onClick = onMarkDone,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WestconYellow),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Mark My Part as Done", color = WestconDarkBlue, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            "You've marked this session as complete!",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section 1: Teaching Efficiency
                Text(
                    "How well did $otherUserName teach you?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WestconDarkBlue,
                    textAlign = TextAlign.Center
                )
                Text("Skill: $skillTheyTaught", fontSize = 13.sp, color = WestconYellow, fontWeight = FontWeight.ExtraBold)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(horizontalArrangement = Arrangement.Center) {
                    repeat(5) { index ->
                        IconButton(onClick = { if (otherUserMarkedDone) teachingRating = (index + 1).toDouble() }, enabled = otherUserMarkedDone) {
                            Icon(
                                imageVector = if (index < teachingRating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (index < teachingRating) WestconYellow else Color.LightGray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 2: Learning Progress
                Text(
                    "How was $otherUserName's learning progress?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = WestconDarkBlue,
                    textAlign = TextAlign.Center
                )
                Text("Skill: $skillTheyLearned", fontSize = 13.sp, color = WestconYellow, fontWeight = FontWeight.ExtraBold)

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.Center) {
                    repeat(5) { index ->
                        IconButton(onClick = { if (otherUserMarkedDone) learningRating = (index + 1).toDouble() }, enabled = otherUserMarkedDone) {
                            Icon(
                                imageVector = if (index < learningRating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (index < learningRating) WestconYellow else Color.LightGray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                if (!otherUserMarkedDone) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Waiting for $otherUserName to mark as done before you can rate.",
                        fontSize = 11.sp,
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Later", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = { onRateSubmitted(teachingRating, learningRating, skillTheyTaught, skillTheyLearned) },
                        enabled = otherUserMarkedDone,
                        modifier = Modifier.weight(1.5f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WestconDarkBlue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Submit Feedback", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
