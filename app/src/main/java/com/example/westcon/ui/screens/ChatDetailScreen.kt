package com.example.westcon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.westcon.ui.theme.*
import com.example.westcon.data.*
import com.example.westcon.ui.UIUtils
import com.example.westcon.ui.WestconPullToRefresh
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    otherUserUid: String,
    otherUserName: String,
    onBackClick: () -> Unit
) {
    val messages by FirebaseManager.getMessages(chatId).collectAsState(initial = emptyList())
    val currentUser = FirebaseManager.getCurrentUser()
    var otherUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var activeExchange by remember { mutableStateOf<SkillExchange?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    var showRateDialog by remember { mutableStateOf(false) }

    // Fetch data and mark as read
    LaunchedEffect(otherUserUid) {
        if (otherUserUid.isNotBlank()) {
            otherUserProfile = FirebaseManager.getUserProfile(otherUserUid)
            FirebaseManager.markChatAsRead(otherUserUid)
            FirebaseManager.markChatMessagesAsRead(chatId)
            
            val currentUid = currentUser?.uid ?: ""
            activeExchange = FirebaseManager.getActiveExchange(currentUid, otherUserUid)
        }
    }

    // Auto-scroll to bottom
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(WestconDarkBlue, Color(0xFF002244))
                        )
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            UIUtils.getProfileIcon(otherUserProfile?.profileIconName ?: "Person"), 
                            contentDescription = null, 
                            tint = WestconYellow, 
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                otherUserName, 
                                fontSize = 18.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = Color.White, 
                                fontFamily = MomotrustFontFamily,
                                maxLines = 1
                            )
                            if (otherUserProfile != null && otherUserProfile!!.rating > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Star, 
                                        contentDescription = null, 
                                        tint = WestconYellow, 
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        String.format("%.1f", otherUserProfile!!.rating),
                                        fontSize = 12.sp,
                                        color = WestconYellow,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = MomotrustFontFamily
                                    )
                                }
                            }
                        }
                        if (activeExchange != null) {
                            Text(
                                "Exchange: ${activeExchange?.skillWanted} ↔ ${activeExchange?.skillOffered}",
                                fontSize = 10.sp,
                                color = WestconYellow.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                    
                    if (activeExchange != null) {
                        IconButton(
                            onClick = { showRateDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Stars, 
                                contentDescription = "Rate Session", 
                                tint = WestconYellow,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            ChatInputBar(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSendClick = {
                    if (messageText.isNotBlank() && currentUser != null) {
                        val currentUid = currentUser.uid
                        val newMessage = Message(
                            senderUid = currentUid,
                            receiverUid = otherUserUid,
                            text = messageText,
                            timestamp = Timestamp.now()
                        )
                        scope.launch {
                            val result = FirebaseManager.sendMessage(newMessage, chatId)
                            if (result.isSuccess) {
                                messageText = ""
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        var isRefreshing by remember { mutableStateOf(false) }

        WestconPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch {
                    activeExchange = FirebaseManager.getActiveExchange(currentUser?.uid ?: "", otherUserUid)
                    kotlinx.coroutines.delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF1F3F5))
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message ->
                        ChatBubble(message, isCurrentUser = message.senderUid == currentUser?.uid)
                    }
                }
            }
        }
    }

    if (showRateDialog && activeExchange != null) {
        RateUserDialog(
            otherUserName = otherUserName,
            exchange = activeExchange!!,
            currentUid = currentUser?.uid ?: "",
            onDismiss = { showRateDialog = false },
            onRate = { rating, skillName ->
                scope.launch {
                    FirebaseManager.rateUserLearning(otherUserUid, skillName, rating, activeExchange!!.id)
                    showRateDialog = false
                    // Refresh exchange info
                    activeExchange = FirebaseManager.getActiveExchange(currentUser?.uid ?: "", otherUserUid)
                }
            }
        )
    }
}

@Composable
fun ChatBubble(message: Message, isCurrentUser: Boolean) {
    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(message.timestamp.toDate())
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isCurrentUser) WestconDarkBlue else Color.White,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isCurrentUser) 20.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 20.dp
            ),
            tonalElevation = if (isCurrentUser) 2.dp else 1.dp,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    message.text,
                    color = if (isCurrentUser) Color.White else Color.Black,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    timeStr,
                    color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 12.dp,
        modifier = Modifier.padding(WindowInsets.ime.asPaddingValues())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { }) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Add", tint = Color.Gray)
            }
            
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageChange,
                placeholder = { Text("Write a message...", fontSize = 15.sp, color = Color.Gray) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF1F3F5),
                    focusedContainerColor = Color(0xFFF1F3F5),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = WestconYellow.copy(alpha = 0.3f),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                maxLines = 5,
                singleLine = false
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = onSendClick,
                enabled = messageText.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (messageText.isNotBlank()) WestconDarkBlue else Color.LightGray,
                    contentColor = Color.White
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Send, 
                    contentDescription = "Send", 
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun RateUserDialog(
    otherUserName: String,
    exchange: SkillExchange,
    currentUid: String,
    onDismiss: () -> Unit,
    onRate: (Double, String) -> Unit
) {
    var rating by remember { mutableDoubleStateOf(5.0) }
    
    // Determine which skill I am teaching the other user
    val skillITaught = if (exchange.requesterUid == currentUid) exchange.skillOffered else exchange.skillWanted
    
    // Check if other user has marked it as done
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
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(WestconYellow.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Stars,
                        contentDescription = null,
                        tint = WestconYellow,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Rate $otherUserName's Learning Progress",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WestconDarkBlue,
                    fontFamily = MomotrustFontFamily,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Skill: $skillITaught",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = WestconYellow,
                    fontFamily = MomotrustFontFamily
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (!otherUserMarkedDone) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            "Waiting for $otherUserName to mark as done...",
                            fontSize = 12.sp,
                            color = Color.Red,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { index ->
                        val isSelected = index < rating.toInt()
                        IconButton(
                            onClick = { if (otherUserMarkedDone) rating = (index + 1).toDouble() },
                            modifier = Modifier.size(48.dp),
                            enabled = otherUserMarkedDone
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (isSelected) WestconYellow else Color.LightGray.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
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
                        onClick = { onRate(rating, skillITaught) },
                        enabled = otherUserMarkedDone,
                        modifier = Modifier.weight(1.5f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WestconDarkBlue,
                            disabledContainerColor = Color.LightGray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text("Submit Rating", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
