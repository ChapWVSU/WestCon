package com.example.westcon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.westcon.data.*
import com.example.westcon.ui.UIUtils
import com.example.westcon.ui.WestconPullToRefresh
import com.example.westcon.ui.theme.*
import kotlinx.coroutines.launch

// --- Utility Components ---

@Composable
fun ProfileStatBadge(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .shadow(elevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9ECEF))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = WestconDarkBlue)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.ExtraBold, 
                color = WestconDarkBlue, 
                fontFamily = MomotrustFontFamily,
                textAlign = TextAlign.Center
            )
            Text(
                label, 
                fontSize = 10.sp, 
                color = Color.Gray, 
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MasteryBadgeSmall(level: Int) {
    val (label, color) = when(level) {
        1 -> "Novice" to Color(0xFFADB5BD)
        2 -> "Intermediate" to Color(0xFF4CAF50)
        3 -> "Advanced" to Color(0xFF2196F3)
        4 -> "Expert" to Color(0xFF9C27B0)
        5 -> "Guru" to Color(0xFFFF9800)
        else -> "Novice" to Color(0xFFADB5BD)
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            label, 
            color = color, 
            fontSize = 9.sp, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

// --- Main Header ---

@Composable
fun ProfileHeaderCard(
    profile: UserProfile, 
    isOwnProfile: Boolean = true,
    onIconSelect: (String) -> Unit = {},
    onMessageClick: () -> Unit = {},
    onExchangeClick: () -> Unit = {}
) {
    var showIconPicker by remember { mutableStateOf(false) }

    if (showIconPicker) {
        AlertDialog(
            onDismissRequest = { showIconPicker = false },
            containerColor = White,
            title = { Text("Choose an Avatar", fontFamily = MomotrustFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(UIUtils.availableIcons) { iconName ->
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(if (profile.profileIconName == iconName) WestconYellow.copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.dp, if (profile.profileIconName == iconName) WestconYellow else Color(0xFFE9ECEF), CircleShape)
                                .clickable {
                                    onIconSelect(iconName)
                                    showIconPicker = false
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                UIUtils.getProfileIcon(iconName),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = if (profile.profileIconName == iconName) WestconDarkBlue else Color.Gray
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIconPicker = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F3F5))
                        .border(4.dp, WestconYellow, CircleShape)
                        .padding(4.dp) // Gap between border and icon
                        .clickable(enabled = isOwnProfile) { showIconPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        UIUtils.getProfileIcon(profile.profileIconName),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = WestconDarkBlue
                    )
                }
                Surface(
                    color = WestconDarkBlue,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(30.dp)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .border(3.dp, Color.White, CircleShape)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Verified", tint = Color.White, modifier = Modifier.padding(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                profile.name, 
                fontSize = 24.sp, 
                fontWeight = FontWeight.Bold, 
                color = WestconDarkBlue, 
                fontFamily = MomotrustFontFamily, 
                textAlign = TextAlign.Center
            )
            
            Surface(
                color = Color(0xFF8B7355).copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    "${profile.department} • ${profile.course}", 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = Color(0xFF8B7355), 
                    fontFamily = MomotrustFontFamily, 
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileStatBadge(
                    icon = Icons.Default.History, 
                    label = "Year Level", 
                    value = profile.year,
                    modifier = Modifier.weight(1f)
                )
                val trustScore = if (profile.rating > 0) String.format("%.1f", profile.rating) else "N/A"
                ProfileStatBadge(
                    icon = Icons.Default.Star, 
                    label = "Trust Score", 
                    value = trustScore,
                    modifier = Modifier.weight(1f)
                )
                ProfileStatBadge(
                    icon = Icons.Default.SwapCalls, 
                    label = "Total Swaps", 
                    value = profile.swaps.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            if (!isOwnProfile) {
                Spacer(modifier = Modifier.height(28.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onMessageClick, 
                        modifier = Modifier.weight(1f).height(50.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = WestconDarkBlue), 
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message, 
                            contentDescription = null, 
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Message", 
                            color = Color.White,
                            fontWeight = FontWeight.Bold, 
                            fontFamily = MomotrustFontFamily,
                            fontSize = 14.sp
                        )
                    }
                    OutlinedButton(
                        onClick = onExchangeClick, 
                        modifier = Modifier.weight(1f).height(50.dp), 
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, WestconDarkBlue), 
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz, 
                            contentDescription = null, 
                            modifier = Modifier.size(18.dp), 
                            tint = WestconDarkBlue
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Exchange", 
                            color = WestconDarkBlue, 
                            fontWeight = FontWeight.Bold, 
                            fontFamily = MomotrustFontFamily,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// --- Section Components ---

@Composable
fun EditableAboutSection(
    isEditing: Boolean,
    isOwnProfile: Boolean = true,
    aboutText: String,
    onAboutChange: (String) -> Unit,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9ECEF))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = WestconDarkBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bio", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = WestconDarkBlue, fontFamily = MomotrustFontFamily)
                }
                if (isOwnProfile) {
                    if (isEditing) {
                        Row {
                            IconButton(onClick = onSaveClick) { Icon(Icons.Default.CheckCircle, contentDescription = "Save", tint = Color(0xFF4CAF50)) }
                            IconButton(onClick = onCancelClick) { Icon(Icons.Default.Cancel, contentDescription = "Cancel", tint = Color(0xFFE57373)) }
                        }
                    } else {
                        IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = Color.Gray) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (isEditing) {
                OutlinedTextField(
                    value = aboutText, 
                    onValueChange = onAboutChange, 
                    modifier = Modifier.fillMaxWidth(), 
                    placeholder = { Text("Tell the WestCon community what makes you unique...", color = Color.Gray.copy(alpha = 0.5f)) }, 
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WestconDarkBlue,
                        unfocusedTextColor = WestconDarkBlue,
                        focusedBorderColor = WestconDarkBlue,
                        unfocusedBorderColor = Color(0xFFE9ECEF)
                    )
                )
            } else {
                Text(
                    if (aboutText.isEmpty()) "Share a bit about yourself, your interests, and what you're passionate about!" else aboutText, 
                    fontSize = 14.sp, 
                    color = if (aboutText.isEmpty()) Color.Gray else Color.DarkGray, 
                    lineHeight = 22.sp,
                    fontStyle = if (aboutText.isEmpty()) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditableTeachableSkillsSection(
    isEditing: Boolean,
    isOwnProfile: Boolean = true,
    skills: List<com.example.westcon.data.SkillMastery>,
    newSkillText: String,
    onNewSkillChange: (String) -> Unit,
    onAddSkill: () -> Unit,
    onRemoveSkill: (com.example.westcon.data.SkillMastery) -> Unit,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = WestconDarkBlue),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, contentDescription = null, tint = WestconYellow, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Skills I Can Teach", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp, fontFamily = MomotrustFontFamily)
                }
                if (isOwnProfile) {
                    if (isEditing) {
                        Row {
                            IconButton(onClick = onSaveClick) { Icon(Icons.Default.CheckCircle, contentDescription = "Save", tint = WestconYellow) }
                            IconButton(onClick = onCancelClick) { Icon(Icons.Default.Cancel, contentDescription = "Cancel", tint = Color.White.copy(alpha = 0.7f)) }
                        }
                    } else {
                        IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp)) }
                    }
                }
            }
            
            if (isEditing && isOwnProfile) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newSkillText, onValueChange = onNewSkillChange, modifier = Modifier.weight(1f), placeholder = { Text("Add a skill you've mastered...") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White.copy(alpha = 0.08f), 
                            focusedContainerColor = Color.White.copy(alpha = 0.12f), 
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f), 
                            focusedBorderColor = WestconYellow,
                            focusedTextColor = Color.White, 
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onAddSkill,
                        modifier = Modifier.background(WestconYellow, CircleShape).size(48.dp)
                    ) { Icon(Icons.Default.Add, contentDescription = "Add", tint = WestconDarkBlue) }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (skills.isEmpty()) {
                Text("No skills listed yet. Add something you can share with others!", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            } else {
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    skills.forEach { skill ->
                        Surface(
                            color = Color.White.copy(alpha = 0.12f), 
                            shape = RoundedCornerShape(16.dp), 
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(skill.skillName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MomotrustFontFamily)
                                    if (isEditing) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = WestconYellow, modifier = Modifier.size(16.dp).clickable { onRemoveSkill(skill) })
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = WestconYellow, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val ratingText = if (skill.totalRatings > 0) String.format("%.1f", skill.averageRating) else "New"
                                    Text(ratingText, color = WestconYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    MasteryBadgeSmall(skill.level)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditableLearningSkillsSection(
    isEditing: Boolean,
    isOwnProfile: Boolean = true,
    skills: List<com.example.westcon.data.LearningSkill>,
    newSkillText: String,
    onNewSkillChange: (String) -> Unit,
    onAddSkill: () -> Unit,
    onRemoveSkill: (com.example.westcon.data.LearningSkill) -> Unit,
    onMarkDone: (com.example.westcon.data.LearningSkill) -> Unit,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9ECEF))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = WestconDarkBlue, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Skills I'm Learning", fontWeight = FontWeight.Bold, color = WestconDarkBlue, fontSize = 18.sp, fontFamily = MomotrustFontFamily)
                }
                if (isOwnProfile) {
                    if (isEditing) {
                        Row {
                            IconButton(onClick = onSaveClick) { Icon(Icons.Default.CheckCircle, contentDescription = "Save", tint = Color(0xFF4CAF50)) }
                            IconButton(onClick = onCancelClick) { Icon(Icons.Default.Cancel, contentDescription = "Cancel", tint = Color(0xFFE57373)) }
                        }
                    } else {
                        IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = Color.Gray) }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (skills.isEmpty()) {
                Text("Ready to grow? Skills you exchange for will appear here!", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            } else {
                skills.forEach { skillItem ->
                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(skillItem.skillName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WestconDarkBlue, fontFamily = MomotrustFontFamily)
                                if (isEditing) {
                                    IconButton(onClick = { onRemoveSkill(skillItem) }, modifier = Modifier.size(32.dp).padding(start = 8.dp)) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = Color(0xFFE57373), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (skillItem.rating > 0) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = WestconYellow, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        String.format("%.1f", skillItem.rating),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = WestconDarkBlue,
                                        fontFamily = MomotrustFontFamily
                                    )
                                } else {
                                    Text(
                                        "N/A",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.LightGray,
                                        fontFamily = MomotrustFontFamily
                                    )
                                }
                            }
                        }
                        
                        if (!skillItem.isDone && isOwnProfile && isEditing) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { onMarkDone(skillItem) },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF4CAF50))
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark as Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (skillItem.isDone) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "Completed",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Main Screen ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    userId: String? = null,
    onLogoutClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    onMessageClick: (String, String) -> Unit = { _, _ -> },
    onExchangeClick: (String) -> Unit = {}
) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val currentUid = FirebaseManager.getCurrentUser()?.uid
    val isOwnProfile = userId == null || userId == currentUid

    var isEditingAbout by remember { mutableStateOf(false) }
    var editAboutText by remember { mutableStateOf("") }
    var isEditingTeachableSkills by remember { mutableStateOf(false) }
    val editTeachableSkills = remember { mutableStateListOf<com.example.westcon.data.SkillMastery>() }
    var newTeachableSkill by remember { mutableStateOf("") }
    var isEditingLearningSkills by remember { mutableStateOf(false) }
    val editLearningSkills = remember { mutableStateListOf<LearningSkill>() }
    var newLearningSkill by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        val targetUid = userId ?: currentUid
        if (targetUid != null) {
            val profile = FirebaseManager.getUserProfile(targetUid)
            userProfile = profile
            if (profile != null) {
                editAboutText = profile.about
                editTeachableSkills.clear()
                editTeachableSkills.addAll(profile.skillsToTeach)
                editLearningSkills.clear()
                editLearningSkills.addAll(profile.skillsLearning)
            }
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = WestconDarkBlue, strokeWidth = 3.dp)
        }
    } else if (userProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Failed to load profile", color = Color.Gray, fontFamily = MomotrustFontFamily)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onLogoutClick, colors = ButtonDefaults.buttonColors(containerColor = WestconDarkBlue)) { 
                    Text("Go Back") 
                }
            }
        }
    } else {
        val profile = userProfile!!
        var isRefreshing by remember { mutableStateOf(false) }
        
        fun saveProfile(updatedProfile: UserProfile = profile) {
            scope.launch {
                val finalProfile = updatedProfile.copy(
                    about = editAboutText,
                    skillsToTeach = editTeachableSkills.toList(),
                    skillsLearning = editLearningSkills.toList()
                )
                val result = FirebaseManager.saveUserProfile(finalProfile)
                if (result.isSuccess) {
                    userProfile = finalProfile
                    isEditingAbout = false
                    isEditingTeachableSkills = false
                    isEditingLearningSkills = false
                }
            }
        }

        WestconPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch {
                    val targetUid = userId ?: currentUid
                    if (targetUid != null) {
                        userProfile = FirebaseManager.getUserProfile(targetUid)
                    }
                    kotlinx.coroutines.delay(1000)
                    isRefreshing = false
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
                // Gradient Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(WestconDarkBlue, Color(0xFF002244))
                            )
                        )
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = if (onBackClick != null) 200.dp else 140.dp, bottom = 48.dp)
                ) {
                    item { 
                        ProfileHeaderCard(
                            profile = profile, 
                            isOwnProfile = isOwnProfile,
                            onIconSelect = { newIcon ->
                                saveProfile(profile.copy(profileIconName = newIcon))
                            },
                            onMessageClick = { onMessageClick(profile.uid, profile.name) },
                            onExchangeClick = { onExchangeClick(profile.uid) }
                        ) 
                    }
                
                    item {
                        EditableAboutSection(
                            isEditing = isEditingAbout && isOwnProfile,
                            isOwnProfile = isOwnProfile,
                            aboutText = editAboutText,
                            onAboutChange = { editAboutText = it },
                            onEditClick = { isEditingAbout = true },
                            onSaveClick = { saveProfile() },
                            onCancelClick = { editAboutText = profile.about; isEditingAbout = false }
                        )
                    }

                    item {
                        EditableTeachableSkillsSection(
                            isEditing = isEditingTeachableSkills && isOwnProfile,
                            isOwnProfile = isOwnProfile,
                            skills = editTeachableSkills,
                            newSkillText = newTeachableSkill,
                            onNewSkillChange = { newTeachableSkill = it },
                            onAddSkill = {
                                if (newTeachableSkill.isNotBlank()) {
                                    editTeachableSkills.add(com.example.westcon.data.SkillMastery(skillName = newTeachableSkill.trim()))
                                    newTeachableSkill = ""
                                }
                            },
                            onRemoveSkill = { editTeachableSkills.remove(it) },
                            onEditClick = { isEditingTeachableSkills = true },
                            onSaveClick = { saveProfile() },
                            onCancelClick = {
                                editTeachableSkills.clear()
                                editTeachableSkills.addAll(profile.skillsToTeach)
                                isEditingTeachableSkills = false
                            }
                        )
                    }

                    item {
                        EditableLearningSkillsSection(
                            isEditing = isEditingLearningSkills && isOwnProfile,
                            isOwnProfile = isOwnProfile,
                            skills = editLearningSkills,
                            newSkillText = newLearningSkill,
                            onNewSkillChange = { newLearningSkill = it },
                            onAddSkill = {
                                if (newLearningSkill.isNotBlank()) {
                                    editLearningSkills.add(LearningSkill(skillName = newLearningSkill.trim()))
                                    newLearningSkill = ""
                                }
                            },
                            onRemoveSkill = { editLearningSkills.remove(it) },
                            onMarkDone = { skill ->
                                val idx = editLearningSkills.indexOf(skill)
                                if (idx != -1) {
                                    scope.launch {
                                        // 1. Mark in profile
                                        editLearningSkills[idx] = skill.copy(isDone = true)
                                        saveProfile()
                                        
                                        // 2. Mark in exchange if available
                                        if (skill.exchangeId != null && currentUid != null) {
                                            FirebaseManager.markExchangeDone(skill.exchangeId, currentUid)
                                        }
                                    }
                                }
                            },
                            onEditClick = { isEditingLearningSkills = true },
                            onSaveClick = { saveProfile() },
                            onCancelClick = {
                                editLearningSkills.clear()
                                editLearningSkills.addAll(profile.skillsLearning)
                                isEditingLearningSkills = false
                            }
                        )
                    }

                    if (isOwnProfile) {
                        item {
                            var showLogoutConfirm by remember { mutableStateOf(false) }
                            if (showLogoutConfirm) {
                                AlertDialog(
                                    onDismissRequest = { showLogoutConfirm = false },
                                    containerColor = Color.White,
                                    title = { Text("Log Out", fontWeight = FontWeight.Bold, color = WestconDarkBlue) },
                                    text = { Text("Are you sure you want to log out of your WestCon account?") },
                                    confirmButton = {
                                        Button(
                                            onClick = onLogoutClick,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                                        ) {
                                            Text("Log Out", color = Color.White)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showLogoutConfirm = false }) {
                                            Text("Cancel", color = Color.Gray)
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showLogoutConfirm = true },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9ECEF)),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFE57373))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Log Out", color = Color(0xFFE57373), fontWeight = FontWeight.Bold, fontFamily = MomotrustFontFamily)
                            }
                        }
                    }
                }

                // Top Bar with Back Button (Drawn last to float on top)
                if (onBackClick != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xCC002244), Color.Transparent)
                                )
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = onBackClick,
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 6.dp,
                                tonalElevation = 6.dp,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = WestconDarkBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
