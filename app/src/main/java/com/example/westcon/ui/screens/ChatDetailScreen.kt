package com.example.westcon.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.westcon.data.*
import com.example.westcon.ui.UIUtils
import com.example.westcon.ui.WestconPullToRefresh
import com.example.westcon.ui.theme.WestconDarkBlue
import com.example.westcon.ui.theme.WestconYellow
import com.example.westcon.ui.theme.MomotrustFontFamily
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    otherUserUid: String,
    otherUserName: String,
    onBackClick: () -> Unit,
    onProfileClick: (String) -> Unit = {}
) {
    val messagesFlow = remember(chatId) { FirebaseManager.getMessages(chatId) }
    val messages by messagesFlow.collectAsState(initial = emptyList())
    val currentUser = FirebaseManager.getCurrentUser()
    var otherUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    
    val exchangeFlow = remember(currentUser?.uid, otherUserUid) { 
        FirebaseManager.getRelevantExchangeFlow(currentUser?.uid ?: "", otherUserUid) 
    }
    val activeExchange by exchangeFlow.collectAsState(initial = null)
    
    val typingFlow = remember(otherUserUid) { FirebaseManager.getTypingStatus(otherUserUid) }
    val isOtherUserTyping by typingFlow.collectAsState(initial = false)
    
    var showRateDialog by remember { mutableStateOf(false) }
    var showConfirmRating by remember { mutableStateOf(false) }
    var isSubmittingRating by remember { mutableStateOf(false) }
    
    var ratingForTheirTeaching by remember { mutableDoubleStateOf(5.0) }
    var ratingForTheirLearning by remember { mutableDoubleStateOf(5.0) }
    var skillTheyTaughtName by remember { mutableStateOf("") }
    var skillTheyLearnedName by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Fetch data and mark as read
    LaunchedEffect(otherUserUid) {
        if (otherUserUid.isNotBlank()) {
            otherUserProfile = FirebaseManager.getUserProfile(otherUserUid)
            FirebaseManager.markChatAsRead(otherUserUid)
            FirebaseManager.markChatMessagesAsRead(chatId)
        }
    }
    
    // Typing status logic
    LaunchedEffect(messageText) {
        if (otherUserUid.isNotBlank()) {
            FirebaseManager.setUserTypingStatus(otherUserUid, messageText.isNotBlank())
            if (messageText.isNotBlank()) {
                delay(3000) // Clear typing after 3 seconds of inactivity
                if (messageText.isBlank()) {
                    FirebaseManager.setUserTypingStatus(otherUserUid, false)
                }
            }
        }
    }
    
    // Clear typing status when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                if (otherUserUid.isNotBlank()) {
                    FirebaseManager.setUserTypingStatus(otherUserUid, false)
                }
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmRating) {
        AlertDialog(
            onDismissRequest = { if (!isSubmittingRating) showConfirmRating = false },
            containerColor = Color.White,
            title = { Text("Confirm Feedback", fontWeight = FontWeight.Bold, color = WestconDarkBlue) },
            text = { 
                Column {
                    Text(
                        "Ready to submit your feedback for $otherUserName?",
                        fontWeight = FontWeight.Bold,
                        color = WestconDarkBlue
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Teaching proficiency ($skillTheyTaughtName):", fontSize = 11.sp, color = WestconDarkBlue)
                    Text("$ratingForTheirTeaching / 5.0 Stars", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WestconDarkBlue)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Learning progress ($skillTheyLearnedName):", fontSize = 11.sp, color = WestconDarkBlue)
                    Text("$ratingForTheirLearning / 5.0 Stars", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WestconDarkBlue)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("This action is final and cannot be changed.", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittingRating = true
                        scope.launch {
                            val result = FirebaseManager.submitExchangeRating(
                                exchangeId = activeExchange?.id ?: "",
                                targetUid = otherUserUid,
                                teachingRating = ratingForTheirTeaching,
                                learningRating = ratingForTheirLearning,
                                taughtSkillName = skillTheyTaughtName,
                                learnedSkillName = skillTheyLearnedName
                            )
                            isSubmittingRating = false
                            showConfirmRating = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show()
                                showRateDialog = false
                            } else {
                                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to submit feedback"
                                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                if (errorMsg.contains("Already rated")) {
                                    showRateDialog = false
                                }
                            }
                        }
                    },
                    enabled = !isSubmittingRating,
                    colors = ButtonDefaults.buttonColors(containerColor = WestconDarkBlue)
                ) {
                    if (isSubmittingRating) CircularProgressIndicator(color = WestconYellow, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Confirm & Submit", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRating = false }, enabled = !isSubmittingRating) {
                    Text("Go Back", color = WestconDarkBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Auto-scroll to bottom
    LaunchedEffect(messages, isOtherUserTyping) {
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
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { onProfileClick(otherUserUid) },
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
                    
                    Column(modifier = Modifier.weight(1f).clickable { onProfileClick(otherUserUid) }) {
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
                        if (isOtherUserTyping) {
                            Text(
                                "typing...",
                                fontSize = 10.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        } else if (activeExchange != null && activeExchange?.status != "DONE") {
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
                        val alreadyRated = if (currentUser?.uid == activeExchange?.requesterUid) activeExchange!!.requesterRated else activeExchange!!.responderRated
                        if (!alreadyRated) {
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
                                FirebaseManager.setUserTypingStatus(otherUserUid, false)
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
                    delay(1000)
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
                    
                    if (isOtherUserTyping) {
                        item {
                            TypingIndicatorBubble()
                        }
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
            onRateSubmitted = { tRating, lRating, tSkill, lSkill ->
                ratingForTheirTeaching = tRating
                ratingForTheirLearning = lRating
                skillTheyTaughtName = tSkill
                skillTheyLearnedName = lSkill
                showConfirmRating = true
            },
            onMarkDone = {
                scope.launch {
                    val res = FirebaseManager.markExchangeDone(activeExchange!!.id, currentUser?.uid ?: "")
                    if (res.isSuccess) {
                        Toast.makeText(context, "Session marked as complete!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun ChatBubble(message: Message, isCurrentUser: Boolean) {
    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(message.timestamp.toDate())
    val isRead = message.isRead || message.readCompat
    
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        timeStr,
                        color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = if (isRead) "Read" else "Sent",
                            tint = if (isRead) Color(0xFF34B7F1) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(600, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(600, delayMillis = 200, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(600, delayMillis = 400, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "dot3"
    )

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).background(Color.Gray.copy(alpha = alpha1), CircleShape))
            Box(modifier = Modifier.size(6.dp).background(Color.Gray.copy(alpha = alpha2), CircleShape))
            Box(modifier = Modifier.size(6.dp).background(Color.Gray.copy(alpha = alpha3), CircleShape))
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
                trailingIcon = {
                    IconButton(
                        onClick = onSendClick,
                        enabled = messageText.isNotBlank(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = if (messageText.isNotBlank()) WestconDarkBlue else Color.Gray.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            Icons.Default.Send, 
                            contentDescription = "Send", 
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                maxLines = 5,
                singleLine = false
            )
        }
    }
}
