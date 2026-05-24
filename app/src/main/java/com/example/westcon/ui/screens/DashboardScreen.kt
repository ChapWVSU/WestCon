package com.example.westcon.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.westcon.data.*
import com.example.westcon.data.FirebaseManager
import com.example.westcon.ui.theme.*
import kotlinx.coroutines.launch

import com.example.westcon.ui.UIUtils
import com.example.westcon.ui.WestconPullToRefresh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNotificationClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onMessageClick: (String, String, String) -> Unit = { _, _, _ -> },
    onProfileClick: (String) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showPostSkillDialog by remember { mutableStateOf(false) }
    var showPostFreedomDialog by remember { mutableStateOf(false) }
    var skillToExchange by remember { mutableStateOf<com.example.westcon.data.SkillPost?>(null) }
    
    val notifications by FirebaseManager.getNotifications().collectAsState(initial = emptyList())
    val hasUnread = notifications.any { !it.isActuallyRead }

    Scaffold(
        topBar = { 
            val title = when(selectedTab) {
                1 -> "Freedom Wall"
                2 -> "Messages"
                3 -> "Profile"
                else -> "WestCon"
            }
            DashboardTopBar(
                title = title,
                showLogo = selectedTab == 0,
                onNotificationClick = onNotificationClick, 
                onSearchClick = onSearchClick,
                hasNotifications = hasUnread
            ) 
        },
        bottomBar = { DashboardBottomNav(selectedTab) { selectedTab = it } },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    onClick = { 
                        if (selectedTab == 0) showPostSkillDialog = true 
                        else if (selectedTab == 1) showPostFreedomDialog = true
                    },
                    containerColor = if (selectedTab == 1) WestconYellow else WestconDarkBlue,
                    contentColor = if (selectedTab == 1) WestconDarkBlue else Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Post")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> HomeFeed(
                    onPostClick = { showPostSkillDialog = true },
                    onExchangeClick = { skillToExchange = it },
                    onMessageClick = { chatId, authorName, otherUid -> onMessageClick(chatId, authorName, otherUid) },
                    onProfileClick = onProfileClick
                )
                1 -> FreedomWallScreen(onProfileClick = onProfileClick)
                2 -> MessageScreen(onMessageClick = onMessageClick)
                3 -> ProfileScreen(onLogoutClick = onLogoutClick)
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Screen $selectedTab Coming Soon", fontFamily = MomotrustFontFamily)
                    }
                }
            }
        }
    }

    if (showPostSkillDialog) {
        PostSkillDialog(onDismiss = { showPostSkillDialog = false })
    }

    if (showPostFreedomDialog) {
        PostFreedomDialog(onDismiss = { showPostFreedomDialog = false })
    }

    if (skillToExchange != null) {
        var existingExchange by remember { mutableStateOf<SkillExchange?>(null) }
        var checkingExchange by remember { mutableStateOf(true) }
        val currentUid = FirebaseManager.getCurrentUser()?.uid ?: ""

        LaunchedEffect(skillToExchange) {
            checkingExchange = true
            existingExchange = FirebaseManager.getActiveExchange(currentUid, skillToExchange!!.authorUid)
            checkingExchange = false
        }

        if (!checkingExchange) {
            if (existingExchange != null) {
                AlertDialog(
                    onDismissRequest = { skillToExchange = null },
                    containerColor = White,
                    title = { Text("Active Exchange Found", fontWeight = FontWeight.Bold, color = WestconDarkBlue) },
                    text = { Text("You already have an active exchange with ${skillToExchange!!.authorName}. Please mark it as done before starting a new one.") },
                    confirmButton = {
                        Button(onClick = { skillToExchange = null }, colors = ButtonDefaults.buttonColors(containerColor = WestconDarkBlue)) {
                            Text("Got it")
                        }
                    }
                )
            } else {
                ExchangeDialog(
                    targetPost = skillToExchange!!,
                    onDismiss = { skillToExchange = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExchangeDialog(targetPost: com.example.westcon.data.SkillPost, onDismiss: () -> Unit) {
    var offeredSkill by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf<com.example.westcon.data.UserProfile?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val currentUser = FirebaseManager.getCurrentUser()
        if (currentUser != null) {
            userProfile = FirebaseManager.getUserProfile(currentUser.uid)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = { Text("Exchange Skills", fontWeight = FontWeight.Bold, color = WestconDarkBlue, fontFamily = MomotrustFontFamily) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "You want to learn '${targetPost.title}' from ${targetPost.authorName}.",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                
                if (userProfile != null && userProfile!!.skillsToTeach.isNotEmpty()) {
                    Text("Choose from your skills:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WestconDarkBlue)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        userProfile!!.skillsToTeach.forEach { skill ->
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
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color.Transparent,
                                    selectedBorderColor = WestconYellow
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                OutlinedTextField(
                    value = offeredSkill,
                    onValueChange = { offeredSkill = it },
                    label = { Text("Or type a new skill to teach") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WestconDarkBlue,
                        unfocusedTextColor = WestconDarkBlue,
                        focusedBorderColor = WestconDarkBlue,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (offeredSkill.isNotBlank()) {
                        isLoading = true
                        scope.launch {
                            val currentUser = FirebaseManager.getCurrentUser()
                            val profile = userProfile ?: (currentUser?.let { FirebaseManager.getUserProfile(it.uid) })
                            
                            // SYNC LOGIC: Check if this skill exists on profile
                            val existingSkill = profile?.skillsToTeach?.find { it.skillName.equals(offeredSkill.trim(), ignoreCase = true) }
                            
                            // If new skill, add to profile automatically
                            if (existingSkill == null && profile != null) {
                                val updatedSkills = profile.skillsToTeach.toMutableList().apply {
                                    add(com.example.westcon.data.SkillMastery(skillName = offeredSkill.trim(), level = 1))
                                }
                                FirebaseManager.saveUserProfile(profile.copy(skillsToTeach = updatedSkills))
                            }

                            val notification = com.example.westcon.data.Notification(
                                receiverUid = targetPost.authorUid,
                                type = "SKILL_EXCHANGE",
                                title = "New Exchange Request",
                                content = "${profile?.name ?: "Someone"} wants to exchange skills!",
                                senderUid = currentUser?.uid,
                                senderName = profile?.name,
                                senderIconName = profile?.profileIconName ?: "Person",
                                senderDept = profile?.department,
                                skillOffered = offeredSkill.trim(),
                                skillWanted = targetPost.title
                            )
                            
                            FirebaseManager.sendNotification(notification)
                            isLoading = false
                            onDismiss()
                        }
                    }
                },
                enabled = !isLoading && offeredSkill.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WestconDarkBlue,
                    contentColor = Color.White,
                    disabledContainerColor = WestconDarkBlue.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                ),
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostSkillDialog(onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Technology") }
    var isLoading by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf<com.example.westcon.data.UserProfile?>(null) }
    val scope = rememberCoroutineScope()
    
    val categories = listOf("Technology", "Academics", "Arts", "Language", "Sports", "Others")

    LaunchedEffect(Unit) {
        val currentUser = FirebaseManager.getCurrentUser()
        if (currentUser != null) {
            userProfile = FirebaseManager.getUserProfile(currentUser.uid)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.clip(RoundedCornerShape(28.dp)),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            color = White,
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Share a Skill",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = WestconDarkBlue,
                        fontFamily = MomotrustFontFamily
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
                
                // Profile Skills Section
                if (userProfile != null && userProfile!!.skillsToTeach.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Choose from your skills:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WestconDarkBlue)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            userProfile!!.skillsToTeach.forEach { skill ->
                                val isSelected = title.equals(skill.skillName, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { title = skill.skillName },
                                    label = { Text(skill.skillName, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WestconYellow,
                                        selectedLabelColor = WestconDarkBlue,
                                        containerColor = Color(0xFFF1F5F9),
                                        labelColor = Color.Gray
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = Color.Transparent,
                                        selectedBorderColor = WestconYellow
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Skill Title", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WestconDarkBlue)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 40) title = it },
                        placeholder = { Text("e.g. UI/UX Design, Calculus, Guitar", color = Color.Gray.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WestconDarkBlue,
                            unfocusedTextColor = WestconDarkBlue,
                            focusedBorderColor = WestconDarkBlue,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )
                    Text(
                        "${title.length}/40",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Category", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WestconDarkBlue)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = category == cat
                            val icon = when(cat) {
                                "Technology" -> Icons.Default.Computer
                                "Academics" -> Icons.Default.School
                                "Arts" -> Icons.Default.Palette
                                "Language" -> Icons.Default.Language
                                "Sports" -> Icons.Default.SportsBasketball
                                else -> Icons.Default.AutoAwesome
                            }
                            
                            Surface(
                                onClick = { category = cat },
                                color = if (isSelected) WestconDarkBlue else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) White else Color.Gray
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        cat,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) White else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Description", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WestconDarkBlue)
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 200) description = it },
                        placeholder = { Text("Tell us a bit about what you can teach and how you can help others...", color = Color.Gray.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = WestconDarkBlue,
                            unfocusedTextColor = WestconDarkBlue,
                            focusedBorderColor = WestconDarkBlue,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                        )
                    )
                    Text(
                        "${description.length}/200",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                
                Button(
                    onClick = {
                        if (title.isNotBlank() && description.isNotBlank()) {
                            isLoading = true
                            scope.launch {
                                val currentUser = FirebaseManager.getCurrentUser()
                                val profile = userProfile ?: (currentUser?.let { FirebaseManager.getUserProfile(it.uid) })
                                
                                // SYNC LOGIC: Check if this skill exists on profile
                                val existingSkill = profile?.skillsToTeach?.find { it.skillName.equals(title.trim(), ignoreCase = true) }
                                val currentMastery = existingSkill?.level ?: 1
                                
                                // If new skill, add to profile automatically
                                if (existingSkill == null && profile != null) {
                                    val updatedSkills = profile.skillsToTeach.toMutableList().apply {
                                        add(com.example.westcon.data.SkillMastery(skillName = title.trim(), level = 1))
                                    }
                                    FirebaseManager.saveUserProfile(profile.copy(skillsToTeach = updatedSkills))
                                }
                                
                                val post = com.example.westcon.data.SkillPost(
                                    authorUid = currentUser?.uid ?: "",
                                    authorName = profile?.name ?: "User",
                                    authorIconName = profile?.profileIconName ?: "Person",
                                    authorMastery = currentMastery,
                                    department = profile?.department ?: "WVSU",
                                    category = category,
                                    title = title.trim(),
                                    description = description,
                                    anonymous = false
                                )
                                
                                FirebaseManager.postSkill(post)
                                isLoading = false
                                onDismiss()
                            }
                        }
                    },
                    enabled = !isLoading && title.isNotBlank() && description.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WestconDarkBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = White, strokeWidth = 2.dp)
                    } else {
                        Text("Post Skill Listing", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeFeed(
    onPostClick: () -> Unit = {},
    onExchangeClick: (com.example.westcon.data.SkillPost) -> Unit = {},
    onMessageClick: (String, String, String) -> Unit = { _, _, _ -> },
    onProfileClick: (String) -> Unit = {}
) {
    val posts by FirebaseManager.getSkillPosts().collectAsState(initial = emptyList())
    val currentUid = FirebaseManager.getCurrentUser()?.uid
    var selectedCategory by remember { mutableStateOf("All Skills") }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val filteredPosts = remember(posts, selectedCategory) {
        posts.filterNot { it.authorName.contains("Chris Daniel Apin", ignoreCase = true) }
            .filter { selectedCategory == "All Skills" || it.category == selectedCategory }
    }
    
    // Extract trending categories
    val trendingCategories = filteredPosts.groupBy { it.category }
        .map { it.key to it.value.size }
        .sortedByDescending { it.second }
        .take(3)
        .map { it.first }

    WestconPullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            scope.launch {
                kotlinx.coroutines.delay(1500) // Simulate refresh
                isRefreshing = false
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F2F5))
        ) {
            item { PostSkillCard(onClick = onPostClick) }
            
            if (trendingCategories.isNotEmpty()) {
                item {
                    TrendingSection(
                        categories = trendingCategories,
                        onCategoryClick = { selectedCategory = it }
                    )
                }
            }

            item { 
                CategoryChips(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                ) 
            }
            
            item {
                Text(
                    if (selectedCategory == "All Skills") "Skill Marketplace" else "$selectedCategory Skills",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WestconDarkBlue,
                    fontFamily = MomotrustFontFamily
                )
            }

            items(filteredPosts, key = { it.id }) { post ->
                SkillPostCard(
                    post = post,
                    isOwnPost = post.authorUid == currentUid,
                    onExchangeClick = { onExchangeClick(post) },
                    onMessageClick = {
                        val uid = currentUid ?: ""
                        if (uid != post.authorUid) {
                            val chatId = if (uid < post.authorUid) "${uid}_${post.authorUid}" else "${post.authorUid}_$uid"
                            onMessageClick(chatId, post.authorName, post.authorUid)
                        }
                    },
                    onProfileClick = { onProfileClick(post.authorUid) }
                )
            }
            
            if (filteredPosts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No posts found in $selectedCategory",
                            color = Color.Gray,
                            fontFamily = MomotrustFontFamily
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun TrendingSection(
    categories: List<String>,
    onCategoryClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.TrendingUp,
                contentDescription = null,
                tint = WestconYellow,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Trending Categories",
                color = WestconDarkBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MomotrustFontFamily
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            categories.forEach { category ->
                Surface(
                    onClick = { onCategoryClick(category) },
                    color = White,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(WestconYellow.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                when(category) {
                                    "Technology" -> Icons.Default.Computer
                                    "Academics" -> Icons.Default.School
                                    "Arts" -> Icons.Default.Palette
                                    else -> Icons.Default.AutoAwesome
                                },
                                contentDescription = null,
                                tint = WestconYellow,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray,
                            maxLines = 1,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardTopBar(
    title: String = "WestCon",
    showLogo: Boolean = true,
    onNotificationClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    hasNotifications: Boolean = false
) {
    Surface(
        color = White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showLogo) {
                    Icon(
                        painter = painterResource(id = com.example.westcon.R.drawable.icon),
                        contentDescription = null,
                        tint = WestconYellow,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    title,
                    color = WestconDarkBlue,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MomotrustFontFamily
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = WestconDarkBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onNotificationClick) {
                    Box {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = WestconDarkBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        if (hasNotifications) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .align(Alignment.TopEnd)
                                    .border(1.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostSkillCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F2F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = WestconDarkBlue, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Share a skill with your fellow Taga-West...",
                color = Color.Gray,
                fontSize = 14.sp,
                fontFamily = MomotrustFontFamily,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
fun CategoryChips(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("All Skills", "Technology", "Academics", "Arts", "Language", "Sports", "Others")

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category, fontFamily = MomotrustFontFamily, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WestconDarkBlue,
                    selectedLabelColor = White,
                    containerColor = White,
                    labelColor = Color.Gray
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedCategory == category,
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.shadow(if (selectedCategory == category) 4.dp else 0.dp, RoundedCornerShape(12.dp))
            )
        }
    }
}

@Composable
fun MasteryBadge(level: Int) {
    val (label, color) = when(level) {
        1 -> "Novice" to Color(0xFF94A3B8)
        2 -> "Intermediate" to Color(0xFF10B981)
        3 -> "Advanced" to Color(0xFF3B82F6)
        4 -> "Expert" to Color(0xFF8B5CF6)
        5 -> "Guru" to Color(0xFFF59E0B)
        else -> "Novice" to Color(0xFF94A3B8)
    }
    
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Verified,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                label,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun SkillPostCard(
    post: com.example.westcon.data.SkillPost,
    isOwnPost: Boolean = false,
    onExchangeClick: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = White,
            title = { Text("Delete Post?") },
            text = { Text("Are you sure you want to delete this skill post?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            FirebaseManager.deleteSkillPost(post.id)
                            showDeleteConfirm = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !post.isAnonymous) { onProfileClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (post.isAnonymous) WestconDarkBlue else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (post.isAnonymous) Icons.Default.VisibilityOff else UIUtils.getProfileIcon(post.authorIconName),
                            contentDescription = null,
                            tint = if (post.isAnonymous) White else WestconDarkBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            post.authorName, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 15.sp,
                            color = WestconDarkBlue
                        )
                        Text(
                            "${post.department} • ${UIUtils.formatTimestamp(post.timestamp)}", 
                            color = Color.Gray, 
                            fontSize = 11.sp
                        )
                    }
                }
                
                Surface(
                    color = WestconYellow.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        post.category,
                        color = Color(0xFF92400E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                post.title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = WestconDarkBlue,
                fontFamily = MomotrustFontFamily,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            MasteryBadge(post.authorMastery)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                post.description,
                fontSize = 14.sp,
                color = Color(0xFF475569),
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (!isOwnPost) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onExchangeClick,
                        modifier = Modifier.weight(1.2f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WestconDarkBlue),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Exchange", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedButton(
                        onClick = onMessageClick,
                        modifier = Modifier.weight(1f).height(48.dp),
                        border = BorderStroke(1.5.dp, WestconDarkBlue),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp), tint = WestconDarkBlue)
                        Spacer(Modifier.width(8.dp))
                        Text("Message", color = WestconDarkBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Your Listing",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic
                    )
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardBottomNav(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple("HOME", Icons.Default.Home, 0),
            Triple("FREEDOM", Icons.Default.EditNote, 1),
            Triple("MESSAGES", Icons.Default.Email, 2),
            Triple("PROFILE", Icons.Default.Person, 3)
        )
        
        items.forEach { (label, icon, index) ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = WestconDarkBlue,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = WestconDarkBlue,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
