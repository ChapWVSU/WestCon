package com.example.westcon.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.westcon.ui.theme.*
import kotlinx.coroutines.launch
import com.example.westcon.data.*
import com.example.westcon.data.FirebaseManager
import com.example.westcon.data.FreedomPost
import com.example.westcon.ui.UIUtils
import com.example.westcon.ui.WestconPullToRefresh

@Composable
fun FreedomWallScreen(onProfileClick: (String) -> Unit = {}) {
    val posts by FirebaseManager.getFreedomPosts().collectAsState(initial = emptyList())
    var selectedFilter by remember { mutableStateOf("Recent") }
    
    val filteredPosts = remember(posts, selectedFilter) {
        val baseList = posts.filter { it.content.isNotBlank() }
        when (selectedFilter) {
            "Recent" -> baseList.sortedByDescending { it.timestamp }
            "Most Liked" -> baseList.sortedByDescending { it.likes }
            "Known Users" -> baseList.filter { !it.isAnonymous }.sortedByDescending { it.timestamp }
            "Anonymous" -> baseList.filter { it.isAnonymous }.sortedByDescending { it.timestamp }
            else -> baseList.sortedByDescending { it.timestamp }
        }
    }
    val currentUid = FirebaseManager.getCurrentUser()?.uid
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    var selectedPostIdForComments by remember { mutableStateOf<String?>(null) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F2F5))
        ) {
            FreedomWallFilters(selectedFilter) { selectedFilter = it }
            
            if (filteredPosts.isEmpty()) {
                EmptyFreedomWall()
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp
                ) {
                    items(filteredPosts, key = { it.id }) { post ->
                        FreedomPostCard(
                            post = post, 
                            isOwnPost = post.authorUid == currentUid,
                            currentUid = currentUid ?: "",
                            onProfileClick = { onProfileClick(post.authorUid) },
                            onCommentClick = { selectedPostIdForComments = post.id }
                        )
                    }
                }
            }
        }
    }

    if (selectedPostIdForComments != null) {
        CommentDialog(
            postId = selectedPostIdForComments!!,
            onDismiss = { selectedPostIdForComments = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentDialog(postId: String, onDismiss: () -> Unit) {
    var content by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val comments by FirebaseManager.getComments(postId).collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            color = White,
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Comments",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WestconDarkBlue,
                        fontFamily = MomotrustFontFamily
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (comments.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No comments yet.", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(comments) { comment ->
                                CommentItem(comment)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isAnonymous,
                        onCheckedChange = { isAnonymous = it },
                        modifier = Modifier.scale(0.8f),
                        colors = SwitchDefaults.colors(checkedThumbColor = WestconDarkBlue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Anonymous", fontSize = 12.sp, color = WestconDarkBlue, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { if (it.length <= 150) content = it },
                        placeholder = { Text("Add a comment...", fontSize = 14.sp, color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = WestconDarkBlue,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                        ),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (content.isNotBlank()) {
                                isLoading = true
                                scope.launch {
                                    val comment = FreedomComment(
                                        postId = postId,
                                        content = content,
                                        anonymous = isAnonymous
                                    )
                                    FirebaseManager.postComment(comment)
                                    content = ""
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading && content.isNotBlank(),
                        modifier = Modifier
                            .size(44.dp)
                            .background(if (content.isNotBlank()) WestconDarkBlue else Color.LightGray.copy(alpha = 0.3f), CircleShape)
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = White, strokeWidth = 2.dp)
                        else Icon(Icons.Default.Send, contentDescription = "Send", tint = White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: FreedomComment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (comment.anonymous) WestconDarkBlue else Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (comment.anonymous) Icons.Default.VisibilityOff else UIUtils.getProfileIcon(comment.authorIconName),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (comment.anonymous) White else WestconDarkBlue
            )
        }
        
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                .background(Color(0xFFF1F5F9))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    comment.authorName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = WestconDarkBlue
                )
                Text(
                    UIUtils.formatTimestamp(comment.timestamp),
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                comment.content,
                fontSize = 13.sp,
                color = Color.DarkGray,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun EmptyFreedomWall() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.LightGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "The wall is empty...",
            fontFamily = MomotrustFontFamily,
            fontSize = 18.sp,
            color = Color.Gray
        )
        Text(
            "Be the first to share something!",
            fontSize = 14.sp,
            color = Color.LightGray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostFreedomDialog(onDismiss: () -> Unit) {
    var content by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(UIUtils.freedomWallColors.first()) }
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.clip(RoundedCornerShape(28.dp)),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = White),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Post to Freedom Wall",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WestconDarkBlue,
                    fontFamily = MomotrustFontFamily
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(android.graphics.Color.parseColor(selectedColor))),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                ) {
                    TextField(
                        value = content,
                        onValueChange = { if (it.length <= 250) content = it },
                        placeholder = { Text("What's on your mind? (Keep it respectful!)", color = Color.Gray.copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxSize(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.DarkGray,
                            unfocusedTextColor = Color.DarkGray,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.DarkGray,
                            fontStyle = FontStyle.Italic
                        )
                    )
                }
                
                Text(
                    "${content.length}/250",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    fontSize = 12.sp,
                    color = if (content.length > 230) Color.Red else Color.Gray
                )
                
                Text("Pick a background color:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UIUtils.freedomWallColors.take(5).forEach { colorHex ->
                        ColorCircle(
                            colorHex = colorHex,
                            isSelected = selectedColor == colorHex,
                            onClick = { selectedColor = colorHex }
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isAnonymous,
                        onCheckedChange = { isAnonymous = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = WestconDarkBlue)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Post Anonymously", fontSize = 14.sp)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (content.isNotBlank()) {
                                isLoading = true
                                scope.launch {
                                    val post = FreedomPost(
                                        content = content,
                                        anonymous = isAnonymous,
                                        colorHex = selectedColor,
                                        timestamp = com.google.firebase.Timestamp.now()
                                    )
                                    FirebaseManager.postToFreedomWall(post)
                                    isLoading = false
                                    onDismiss()
                                }
                            }
                        },
                        enabled = !isLoading && content.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = WestconDarkBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        else Text("Post", color = Color.White)
                    }                }
            }
        }
    }
}

@Composable
fun ColorCircle(colorHex: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(android.graphics.Color.parseColor(colorHex)))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) WestconDarkBlue else Color.LightGray.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
fun FreedomWallFilters(selectedFilter: String, onFilterChange: (String) -> Unit) {
    val filters = listOf("Recent", "Most Liked", "Known Users", "Anonymous")

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter, fontSize = 12.sp, fontFamily = MomotrustFontFamily) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WestconDarkBlue,
                    selectedLabelColor = White,
                    containerColor = White,
                    labelColor = Color.Gray
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedFilter == filter,
                    borderColor = Color.Transparent,
                    selectedBorderColor = WestconDarkBlue
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.shadow(if (selectedFilter == filter) 4.dp else 0.dp, RoundedCornerShape(12.dp))
            )
        }
    }
}

@Composable
fun FreedomPostCard(
    post: FreedomPost, 
    isOwnPost: Boolean, 
    currentUid: String,
    onProfileClick: () -> Unit = {},
    onCommentClick: () -> Unit = {}
) {
    val cardColor = try { Color(android.graphics.Color.parseColor(post.colorHex)) } catch (e: Exception) { Color(0xFFE3F2FD) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isLiked = post.likedBy.contains(currentUid)
    val scope = rememberCoroutineScope()

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = White,
            title = { Text("Delete Post?") },
            text = { Text("Are you sure you want to delete this post? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            FirebaseManager.deleteFreedomPost(post.id)
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
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!post.isAnonymous) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onProfileClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            UIUtils.getProfileIcon(post.authorIconName),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = WestconDarkBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        post.authorName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    post.content,
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2D3436),
                    lineHeight = 22.sp,
                    fontStyle = FontStyle.Italic
                )
                
                if (isOwnPost) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(24.dp).offset(x = 8.dp, y = (-8).dp)
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color.Black.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            if (post.topComment != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.4f))
                        .padding(8.dp)
                        .clickable { onCommentClick() }
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.FormatQuote,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = WestconDarkBlue.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            post.topComment ?: "",
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider(color = Color.Black.copy(alpha = 0.05f), thickness = 1.dp)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    UIUtils.formatTimestamp(post.timestamp), 
                    fontSize = 10.sp, 
                    color = Color.Black.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onCommentClick() }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline, 
                            contentDescription = null, 
                            modifier = Modifier.size(16.dp), 
                            tint = Color.Black.copy(alpha = 0.6f)
                        )
                        if (post.commentCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${post.commentCount}", 
                                fontSize = 11.sp, 
                                color = Color.Black.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { 
                                scope.launch {
                                    FirebaseManager.toggleLikeFreedomPost(post.id)
                                }
                            }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isLiked) Color.Red else Color.Black.copy(alpha = 0.6f)
                        )
                        if (post.likes > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${post.likes}", 
                                fontSize = 11.sp, 
                                color = if (isLiked) Color.Red else Color.Black.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
