package com.example.westcon.ui.screens

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
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.westcon.ui.theme.*
import com.example.westcon.data.FirebaseManager
import com.example.westcon.data.SkillPost
import com.example.westcon.data.UserProfile
import com.example.westcon.ui.UIUtils
import com.example.westcon.ui.WestconPullToRefresh
import kotlinx.coroutines.launch

enum class SearchFilter(val label: String) {
    ALL("All"),
    SKILLS("Skills"),
    USERS("Users"),
    CATEGORIES("Categories")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onProfileClick: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(SearchFilter.ALL) }
    
    val posts by FirebaseManager.getSkillPosts().collectAsState(initial = emptyList())
    val users by FirebaseManager.getAllUserProfiles().collectAsState(initial = emptyList())
    
    val filteredResults = remember(searchQuery, posts, users, selectedFilter) {
        if (searchQuery.isBlank()) {
            Triple(emptyList<SkillPost>(), emptyList<UserProfile>(), emptyList<String>())
        } else {
            // 1. Identify matched users (by name, dept, course)
            val matchedUsers = users.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.department.contains(searchQuery, ignoreCase = true) ||
                it.course.contains(searchQuery, ignoreCase = true)
            }
            
            // 2. Identify matched categories
            val matchedCategories = posts.map { it.category }
                .distinct()
                .filter { it.contains(searchQuery, ignoreCase = true) }
            
            // 3. Associative Skill Search:
            // Match posts where: 
            // - The title/desc matches the query OR 
            // - The author matches the query OR
            // - The category matches the query
            val matchedUserUids = matchedUsers.map { it.uid }.toSet()
            
            val matchedPosts = posts.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.authorName.contains(searchQuery, ignoreCase = true) ||
                matchedUserUids.contains(it.authorUid) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
            
            Triple(matchedPosts, matchedUsers, matchedCategories)
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = White,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WestconDarkBlue)
                        }
                        
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Find users, skills, categories...", color = Color.Gray, fontSize = 15.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9)),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = WestconDarkBlue,
                                unfocusedTextColor = WestconDarkBlue,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = WestconDarkBlue
                            ),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = null, tint = WestconDarkBlue)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Cancel, contentDescription = "Clear", tint = Color.Gray)
                                    }
                                }
                            }
                        )
                    }
                    
                    if (searchQuery.isNotEmpty()) {
                        SearchFilterRow(selectedFilter) { selectedFilter = it }
                    }
                }
            }
        }
    ) { paddingValues ->
        var isRefreshing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        WestconPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch {
                    kotlinx.coroutines.delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC))
            ) {
                if (searchQuery.isEmpty()) {
                    InitialSearchState(onCategoryClick = { 
                        searchQuery = it 
                        selectedFilter = SearchFilter.CATEGORIES
                    })
                } else {
                    SearchResultsContent(
                        query = searchQuery,
                        filter = selectedFilter,
                        posts = filteredResults.first,
                        users = filteredResults.second,
                        categories = filteredResults.third,
                        onProfileClick = onProfileClick,
                        onCategorySelect = { 
                            searchQuery = it
                            selectedFilter = SearchFilter.SKILLS // Show skills for this category
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchFilterRow(selected: SearchFilter, onFilterSelect: (SearchFilter) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(SearchFilter.values()) { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onFilterSelect(filter) },
                label = { Text(filter.label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WestconDarkBlue,
                    selectedLabelColor = White,
                    containerColor = Color(0xFFF1F5F9),
                    labelColor = Color.Gray
                ),
                border = null,
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@Composable
fun InitialSearchState(onCategoryClick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "Popular Categories",
                    color = WestconDarkBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MomotrustFontFamily
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                val categories = listOf("Technology", "Academics", "Arts", "Language", "Sports", "Others")
                val categoryIcons = listOf(
                    Icons.Default.Computer, 
                    Icons.Default.School, 
                    Icons.Default.Palette, 
                    Icons.Default.Language, 
                    Icons.Default.SportsBasketball,
                    Icons.Default.AutoAwesome
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    categories.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { category ->
                                CategorySearchCard(
                                    label = category,
                                    icon = categoryIcons[categories.indexOf(category)],
                                    onClick = { onCategoryClick(category) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
        
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Searches",
                        color = WestconDarkBlue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MomotrustFontFamily
                    )
                    TextButton(onClick = { /* Clear History */ }) {
                        Text("Clear", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                val recentItems = listOf("UI/UX Design", "Kotlin Coroutines", "Digital Arts", "Calculus 1")
                recentItems.forEach { item ->
                    RecentSearchItem(item, onClick = { onCategoryClick(item) })
                }
            }
        }
    }
}

@Composable
fun CategorySearchCard(
    label: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(WestconYellow.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = WestconYellow, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = WestconDarkBlue
            )
        }
    }
}

@Composable
fun RecentSearchItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.History, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = Color.DarkGray, fontSize = 15.sp)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
    }
}

@Composable
fun SearchResultsContent(
    query: String,
    filter: SearchFilter,
    posts: List<SkillPost>,
    users: List<UserProfile>,
    categories: List<String>,
    onProfileClick: (String) -> Unit,
    onCategorySelect: (String) -> Unit
) {
    val showSkills = filter == SearchFilter.ALL || filter == SearchFilter.SKILLS
    val showUsers = filter == SearchFilter.ALL || filter == SearchFilter.USERS
    val showCategories = filter == SearchFilter.ALL || filter == SearchFilter.CATEGORIES

    val noResults = (showSkills && posts.isEmpty()) && 
                  (showUsers && users.isEmpty()) && 
                  (showCategories && categories.isEmpty())

    if (noResults) {
        EmptySearchState(query)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Categories Section
            if (showCategories && categories.isNotEmpty()) {
                item { SectionHeader("Matched Categories") }
                items(categories) { category ->
                    ListItem(
                        headlineContent = { Text(category, fontWeight = FontWeight.Bold) },
                        leadingContent = { Icon(Icons.Default.Category, contentDescription = null, tint = WestconDarkBlue) },
                        modifier = Modifier.clickable { onCategorySelect(category) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF1F5F9))
                }
            }

            // Users Section
            if (showUsers && users.isNotEmpty()) {
                item { SectionHeader("Taga-West Users") }
                items(users) { user ->
                    UserSearchItem(user, onProfileClick)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF1F5F9))
                }
            }

            // Skills Section
            if (showSkills && posts.isNotEmpty()) {
                item { SectionHeader("Skill Swaps") }
                items(posts, key = { it.id }) { post ->
                    SkillPostCard(
                        post = post,
                        isOwnPost = post.authorUid == FirebaseManager.getCurrentUser()?.uid,
                        onExchangeClick = { /* Handled elsewhere */ },
                        onMessageClick = { /* Handled elsewhere */ },
                        onProfileClick = { onProfileClick(post.authorUid) }
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.padding(16.dp),
        fontSize = 14.sp,
        fontWeight = FontWeight.Black,
        color = Color.Gray.copy(alpha = 0.8f),
        fontFamily = MomotrustFontFamily
    )
}

@Composable
fun UserSearchItem(user: UserProfile, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(user.uid) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                UIUtils.getProfileIcon(user.profileIconName),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = WestconDarkBlue
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.name, fontWeight = FontWeight.Bold, color = WestconDarkBlue, fontSize = 15.sp)
            Text("${user.department} • ${user.course}", color = Color.Gray, fontSize = 12.sp)
        }
        if (user.rating > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = WestconYellow, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(String.format("%.1f", user.rating), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun EmptySearchState(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SearchOff, 
                    contentDescription = null, 
                    modifier = Modifier.size(60.dp), 
                    tint = Color.LightGray
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "No results found",
                color = WestconDarkBlue,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MomotrustFontFamily
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "We couldn't find anything matching \"$query\". Try different filters or keywords.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}
