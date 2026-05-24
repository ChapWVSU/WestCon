package com.example.westcon.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.westcon.ui.theme.*
import com.example.westcon.data.*
import com.example.westcon.data.FirebaseManager
import com.example.westcon.data.Notification
import com.example.westcon.ui.UIUtils
import com.example.westcon.ui.WestconPullToRefresh
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(onBackClick: () -> Unit) {
    val notifications by FirebaseManager.getNotifications().collectAsState(initial = emptyList())
    var selectedFilter by remember { mutableStateOf("All") }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(notifications) {
        if (isLoading) isLoading = false
    }

    val filteredNotifications = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "Requests" -> notifications.filter { it.type == "SKILL_EXCHANGE" }
            "Unread" -> notifications.filter { !it.isActuallyRead }
            else -> notifications
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = White,
                shadowElevation = 4.dp
            ) {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "Notifications", 
                            color = WestconDarkBlue, 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Bold,
                            fontFamily = MomotrustFontFamily 
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WestconDarkBlue)
                        }
                    },
                    actions = {
                        if (notifications.any { !it.isActuallyRead }) {
                            TextButton(onClick = { scope.launch { FirebaseManager.markAllNotificationsAsRead() } }) {
                                Text("Mark all as read", color = WestconDarkBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = White)
                )
            }
        }
    ) { paddingValues ->
        var isRefreshing by remember { mutableStateOf(false) }

        WestconPullToRefresh(
            isRefreshing = isRefreshing,
            modifier = Modifier.padding(paddingValues),
            onRefresh = {
                isRefreshing = true
                scope.launch {
                    kotlinx.coroutines.delay(1200)
                    isRefreshing = false
                }
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC)),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item { NotificationFilters(selectedFilter) { selectedFilter = it } }

                if (isLoading && notifications.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = WestconDarkBlue)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Loading notifications...",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    if (isLoading) {
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                color = WestconDarkBlue,
                                trackColor = Color(0xFFEAF4FF)
                            )
                        }
                    }

                    if (filteredNotifications.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillParentMaxHeight(0.8f),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyNotifications(selectedFilter)
                            }
                        }
                    } else {
                        items(filteredNotifications, key = { it.id }) { notification ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                NotificationItem(notification)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotifications(filter: String) {
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
                    when(filter) {
                        "Requests" -> Icons.Default.SwapHoriz
                        "Unread" -> Icons.Outlined.CheckCircle
                        else -> Icons.Outlined.NotificationsActive
                    },
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color.LightGray
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                when(filter) {
                    "Requests" -> "No Exchange Requests"
                    "Unread" -> "You're All Caught Up!"
                    else -> "No Notifications"
                },
                color = WestconDarkBlue,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MomotrustFontFamily
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                when(filter) {
                    "Requests" -> "You haven't received any skill exchange requests yet. Try sharing more of your skills to get noticed!"
                    "Unread" -> "You've read all your notifications. Great job staying on top of things!"
                    else -> "When you get skill requests, achievement alerts, or messages, they'll show up here."
                },
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun NotificationItem(notification: Notification) {
    val timeAgo = UIUtils.formatTimestamp(notification.timestamp)
    val scope = rememberCoroutineScope()
    var showAcceptConfirm by remember { mutableStateOf(false) }
    var showDeclineConfirm by remember { mutableStateOf(false) }
    var showAcceptedDialog by remember { mutableStateOf(false) }
    var isAccepting by remember { mutableStateOf(false) }
    
    if (showAcceptConfirm) {
        AlertDialog(
            onDismissRequest = { showAcceptConfirm = false },
            containerColor = White,
            title = { Text("Accept Exchange?", fontWeight = FontWeight.Bold, color = WestconDarkBlue) },
            text = { 
                Text("Are you sure you want to accept this exchange? A new chat will be started with ${notification.senderName}.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isAccepting = true
                            val result = FirebaseManager.acceptExchangeRequest(notification)
                            isAccepting = false
                            if (result.isSuccess) {
                                showAcceptConfirm = false
                                showAcceptedDialog = true
                            }
                        }
                    },
                    enabled = !isAccepting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WestconDarkBlue,
                        contentColor = Color.White
                    )
                ) {
                    if (isAccepting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Accept", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAcceptConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAcceptedDialog) {
        AlertDialog(
            onDismissRequest = { showAcceptedDialog = false },
            containerColor = White,
            title = { Text("Exchange Accepted", fontWeight = FontWeight.Bold, color = WestconDarkBlue) },
            text = {
                Text(
                    "You accepted ${notification.senderName ?: "the exchange request"}. A chat has been started so you can continue the conversation.",
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = { showAcceptedDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WestconDarkBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAcceptedDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showDeclineConfirm) {
        AlertDialog(
            onDismissRequest = { showDeclineConfirm = false },
            containerColor = White,
            title = { Text("Decline Request?", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = { Text("Are you sure you want to decline this exchange request? This will remove the notification.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            FirebaseManager.deleteNotification(notification.id)
                            showDeclineConfirm = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    )
                ) {
                    Text("Decline")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeclineConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (!notification.isActuallyRead) 4.dp else 0.dp, RoundedCornerShape(16.dp))
            .clickable { 
                if (!notification.isActuallyRead) {
                    scope.launch { FirebaseManager.markNotificationAsRead(notification.id) }
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isActuallyRead) White else Color(0xFFEFF6FF)
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (notification.isActuallyRead) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Unread Indicator
            if (!notification.isActuallyRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(WestconDarkBlue)
                        .offset(x = (-8).dp, y = 4.dp)
                )
            }

            // Notification Icon/Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (notification.type) {
                            "SKILL_EXCHANGE" -> Color(0xFFF1F5F9)
                            "ACHIEVEMENT" -> WestconYellow.copy(alpha = 0.1f)
                            "MESSAGE" -> Color(0xFFF0FDF4)
                            else -> Color(0xFFF1F5F9)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (notification.type) {
                    "SKILL_EXCHANGE" -> {
                        Icon(
                            UIUtils.getProfileIcon(notification.senderIconName), 
                            contentDescription = null, 
                            tint = WestconDarkBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    "ACHIEVEMENT" -> {
                        Icon(Icons.Default.Stars, contentDescription = null, tint = WestconYellow, modifier = Modifier.size(26.dp))
                    }
                    "MESSAGE" -> {
                        Icon(Icons.Default.ChatBubble, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                    }
                    else -> {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(22.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when (notification.type) {
                            "SKILL_EXCHANGE" -> "Exchange Request"
                            "ACHIEVEMENT" -> "New Achievement"
                            "MESSAGE" -> "New Message"
                            else -> notification.title
                        },
                        color = WestconDarkBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(timeAgo, color = Color.Gray, fontSize = 10.sp)
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                NotificationContent(notification)
                
                if (notification.type == "SKILL_EXCHANGE") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showAcceptConfirm = true },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WestconDarkBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Accept", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { showDeclineConfirm = true },
                            modifier = Modifier.weight(1f).height(40.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Decline", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationContent(notification: Notification) {
    when (notification.type) {
        "SKILL_EXCHANGE" -> {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = WestconDarkBlue)) {
                        append("${notification.senderName ?: "Someone"} ")
                    }
                    append("wants to exchange ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = WestconDarkBlue)) {
                        append("'${notification.skillOffered ?: "a skill"}' ")
                    }
                    append("for your ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = WestconDarkBlue)) {
                        append("'${notification.skillWanted ?: "a skill"}'")
                    }
                },
                fontSize = 14.sp,
                color = Color.DarkGray,
                lineHeight = 20.sp
            )
        }
        "MESSAGE" -> {
            Column {
                Text(
                    buildAnnotatedString {
                        append("New message from ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = WestconDarkBlue)) {
                            append(notification.senderName ?: "User")
                        }
                    },
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "\"${notification.content}\"",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
        else -> {
            Text(
                notification.content,
                fontSize = 14.sp,
                color = Color.DarkGray,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun NotificationFilters(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    val filters = listOf("All", "Unread", "Requests")
    
    LazyRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(filters) { filter ->
            val isSelected = selectedFilter == filter
            Surface(
                onClick = { onFilterSelected(filter) },
                color = if (isSelected) WestconDarkBlue else White,
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.shadow(if (isSelected) 4.dp else 0.dp, RoundedCornerShape(12.dp))
            ) {
                Text(
                    filter,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) White else Color.Gray
                )
            }
        }
    }
}
