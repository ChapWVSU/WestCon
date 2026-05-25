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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("westcon_prefs", android.content.Context.MODE_PRIVATE) }
    var lastFreedomWallVisit by remember { mutableLongStateOf(prefs.getLong("last_freedom_visit", 0L)) }

    val notificationsFlow = remember { FirebaseManager.getNotifications() }
    val notifications by notificationsFlow.collectAsState(initial = emptyList())
    val hasUnread = notifications.any { !it.isActuallyRead }
    
    // Bottom nav badges logic
    val chatSummariesFlow = remember { FirebaseManager.getChatSummaries() }
    val chatSummaries by chatSummariesFlow.collectAsState(initial = emptyList())
    val hasUnreadMessages = chatSummaries.any { !it.isActuallyRead }
    
    val freedomPostsFlow = remember { FirebaseManager.getFreedomPosts() }
    val freedomPosts by freedomPostsFlow.collectAsState(initial = emptyList())
    
    val hasNewFreedom = remember(freedomPosts, lastFreedomWallVisit) {
        freedomPosts.any { it.timestamp.seconds > lastFreedomWallVisit }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            val now = System.currentTimeMillis() / 1000
            lastFreedomWallVisit = now
            prefs.edit().putLong("last_freedom_visit", now).apply()
        }
    }

    // Update online status
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                FirebaseManager.updateOnlineStatus(true)
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                FirebaseManager.updateOnlineStatus(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
        bottomBar = { 
            DashboardBottomNav(
                selectedTab = selectedTab, 
                onTabSelected = { 
                    selectedTab = it
                    if (it == 1) {
                        val now = System.currentTimeMillis() / 1000
                        lastFreedomWallVisit = now
                        prefs.edit().putLong("last_freedom_visit", now).apply()
                    }
                },
                hasUnreadMessages = hasUnreadMessages,
                hasNewFreedom = hasNewFreedom
            )
        },
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
                2 -> MessageScreen(onMessageClick = onMessageClick, onProfileClick = onProfileClick)
                3 -> ProfileScreen(onLogoutClick = onLogoutClick, onMessageClick = { uid, name ->
                    val currentUid = FirebaseManager.getCurrentUser()?.uid ?: ""
                    val chatId = if (currentUid < uid) "${currentUid}_$uid" else "${uid}_$currentUid"
                    onMessageClick(chatId, name, uid)
                }, onExchangeClick = { targetUid, targetName ->
                    // Exchange initiation handled via marketplace
                })
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
                    text = { Text("You already have an active exchange with ${skillToExchange!!.authorName}. Please complete or rate it before starting a new one.") },
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostSkillDialog(onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Technology") }
    var postType by remember { mutableStateOf("SHARE") } // "SHARE" or "FIND"
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
        val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = screenHeight * 0.85f)
                .wrapContentHeight(),
            color = White,
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (postType == "SHARE") "Share a Skill" else "Find a Skill",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = WestconDarkBlue,
                        fontFamily = MomotrustFontFamily
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                // Post Type Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    listOf("SHARE" to "Share a Skill", "FIND" to "Find a Skill").forEach { (type, label) ->
                        val isSelected = postType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) White else Color.Transparent)
                                .clickable { 
                                    postType = type
                                    title = "" 
                                }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) WestconDarkBlue else Color.Gray
                            )
                        }
                    }
                }
                
                // Profile Skills Section (Only for "SHARE")
                if (postType == "SHARE" && userProfile != null && userProfile!!.skillsToTeach.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Choose from your skills:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WestconDarkBlue)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(userProfile!!.skillsToTeach) { skill ->
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
                    Text(if (postType == "SHARE") "Skill to Teach" else "Skill You're Looking For", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WestconDarkBlue)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 40) title = it },
                        placeholder = { 
                            Text(
                                if (postType == "SHARE") "e.g. UI/UX Design, Calculus, Guitar" else "e.g. React, Next.js, Academic Writing", 
                                color = Color.Gray.copy(alpha = 0.5f)
                            ) 
                        },
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
                        placeholder = { 
                            Text(
                                if (postType == "SHARE") "Tell us a bit about what you can teach and how you can help others..." 
                                else "Tell us a bit about what you want to learn and what help you need...", 
                                color = Color.Gray.copy(alpha = 0.5f)
                            ) 
                        },
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
                                
                                val existingSkill = if (postType == "SHARE") profile?.skillsToTeach?.find { it.skillName.equals(title.trim(), ignoreCase = true) } else null
                                val currentMastery = existingSkill?.level ?: 1
                                
                                if (postType == "SHARE" && existingSkill == null && profile != null) {
                                    val updatedSkills = profile.skillsToTeach.toMutableList().apply {
                                        add(com.example.westcon.data.SkillMastery(skillName = title.trim(), level = 1))
                                    }
                                    FirebaseManager.saveUserProfile(profile.copy(skillsToTeach = updatedSkills))
                                }
                                
                                val post = com.example.westcon.data.SkillPost(
                                    authorUid = currentUser?.uid ?: "",
                                    authorName = profile?.name ?: "User",
                                    authorIconName = profile?.profileIconName ?: "Person",
                                    authorMastery = if (postType == "SHARE") currentMastery else 1,
                                    department = profile?.department ?: "WVSU",
                                    category = category,
                                    title = title.trim(),
                                    description = description,
                                    postType = postType,
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
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = WestconYellow, strokeWidth = 2.dp)
                    } else {
                        Text(if (postType == "SHARE") "Post to Share" else "Post to Find", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
    val postsFlow = remember { FirebaseManager.getSkillPosts() }
    val posts by postsFlow.collectAsState(initial = emptyList())
    val currentUid = FirebaseManager.getCurrentUser()?.uid
    var selectedCategory by remember { mutableStateOf("All Skills") }
    var filterType by remember { mutableStateOf("ALL") } // "ALL", "SHARE", "FIND"
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val filteredPosts = remember(posts, selectedCategory, filterType) {
        posts.filterNot { it.authorName.contains("Chris Daniel Apin", ignoreCase = true) }
            .filter { selectedCategory == "All Skills" || it.category == selectedCategory }
            .filter { filterType == "ALL" || it.postType == filterType }
    }
    
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
                kotlinx.coroutines.delay(1500)
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
                        selectedFilterType = filterType,
                        onFilterTypeChange = { filterType = it },
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
                    if (selectedCategory == "All Skills") "Skill Domain" else "$selectedCategory Skills",
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
    selectedFilterType: String,
    onFilterTypeChange: (String) -> Unit,
    onCategoryClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            
            // Share/Find Filter
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(2.dp)
            ) {
                listOf("ALL" to "All", "SHARE" to "Share", "FIND" to "Find").forEach { (type, label) ->
                    val isSelected = selectedFilterType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) WestconDarkBlue else Color.Transparent)
                            .clickable { onFilterTypeChange(type) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) White else Color.Gray
                        )
                    }
                }
            }
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
        color = WestconDarkBlue,
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
                    color = White,
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
                        tint = White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onNotificationClick) {
                    Box {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = White,
                            modifier = Modifier.size(24.dp)
                        )
                        if (hasNotifications) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .align(Alignment.TopEnd)
                                    .border(1.dp, WestconDarkBlue, CircleShape)
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
                    Box(contentAlignment = Alignment.BottomEnd) {
                        val isOnline by FirebaseManager.getUserOnlineStatus(post.authorUid).collectAsState(initial = false)
                        
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
                        
                        if (!post.isAnonymous && isOnline) {
                            Surface(
                                color = Color(0xFF4CAF50),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(12.dp)
                                    .border(1.5.dp, Color.White, CircleShape)
                                    .shadow(4.dp, CircleShape)
                            ) {}
                        }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (post.postType == "FIND") "FINDING" else "SHARING",
                            color = WestconDarkBlue,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 10.dp, top = 5.dp, bottom = 5.dp)
                        )
                        Text(
                            " • ${post.category}",
                            color = Color(0xFF92400E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(end = 10.dp, top = 5.dp, bottom = 5.dp)
                        )
                    }
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
            
            if (post.postType == "SHARE") {
                Spacer(modifier = Modifier.height(8.dp))
                MasteryBadge(post.authorMastery)
            }
            
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
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(20.dp), tint = White)
                        Spacer(Modifier.width(8.dp))
                        Text("Exchange", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
fun DashboardBottomNav(
    selectedTab: Int, 
    onTabSelected: (Int) -> Unit,
    hasUnreadMessages: Boolean = false,
    hasNewFreedom: Boolean = false
) {
    NavigationBar(
        containerColor = Color(0xFFF0F2F5), // Match the grey background color
        tonalElevation = 0.dp // Remove elevation for a flatter look
    ) {
        val items = listOf(
            Triple("HOME", Icons.Default.Home, 0),
            Triple("FREEDOM", if (hasNewFreedom) Icons.Default.EditNote else Icons.Default.SpeakerNotes, 1),
            Triple("MESSAGES", Icons.Default.Email, 2),
            Triple("PROFILE", Icons.Default.Person, 3)
        )
        
        items.forEach { (label, icon, index) ->
            val hasBadge = (index == 1 && hasNewFreedom) || (index == 2 && hasUnreadMessages)
            
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = { 
                    BadgedBox(
                        badge = {
                            if (hasBadge) {
                                Badge(
                                    containerColor = if (index == 2) Color.Red else WestconYellow,
                                    modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                                )
                            }
                        }
                    ) {
                        Icon(icon, contentDescription = label)
                    }
                },
                label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = WestconYellow,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = WestconDarkBlue,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = WestconDarkBlue // This creates the circular blue background
                )
            )
        }
    }
}
