package com.example.birdy.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.birdy.data.AuthManager
import com.example.birdy.data.ChatMessage
import com.example.birdy.data.ConnectionState
import com.example.birdy.data.WebSocketManager
import java.text.SimpleDateFormat
import java.util.Locale

// iOS color constants
private val OffWhite = Color(0xFFF5F0EB)
private val BurntOrange = Color(0xFFF27836)
private val OrangeSec7 = Color(0xFF1C1C1E)

// Test user IDs — matches iOS ChatView.swift
private const val TEST_USER_202 = "693f83ac326f1d48499deae8"  // IP app user
private const val TEST_USER_650 = "694d8d3a37070a1a20678d63"  // IC app user

/**
 * ChatScreen — mirrors iOS IC/Birdy/Booking/ChatView.swift
 * Real-time chat using WebSocket with message bubbles, connection status, and auto-scroll.
 */
@Composable
fun ChatScreen(
    recipientId: String = TEST_USER_202,
    recipientName: String = "202 User",
    chatId: String = "test_chat_room",
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val wsManager = remember { WebSocketManager.shared }
    val connectionState by wsManager.connectionState.collectAsState()
    val conversation by wsManager.currentConversation.collectAsState()
    var newMessage by remember { mutableStateOf("") }
    var currentUserId by remember { mutableStateOf("") }

    // Auto-detect the correct recipient based on who's logged in — matches iOS effectiveRecipientId
    val effectiveRecipientId = remember(currentUserId) {
        when (currentUserId) {
            TEST_USER_202 -> TEST_USER_650   // I'm 202 user, send to 650 user
            TEST_USER_650 -> TEST_USER_202   // I'm 650 user, send to 202 user
            else -> recipientId              // Fallback: use whatever was passed in
        }
    }

    // Setup connection on appear — matches iOS onAppear { setupConnection() }
    LaunchedEffect(Unit) {
        currentUserId = AuthManager.getUserID()

        // Connect WebSocket if not already connected
        if (connectionState == ConnectionState.DISCONNECTED) {
            val token = AuthManager.getToken(context)
            if (!token.isNullOrEmpty()) {
                wsManager.connect(token, currentUserId)
            }
        }

        println("💬 ChatScreen: Opened chat with $recipientName (id: $recipientId, chatId: $chatId)")
    }

    // Auto-scroll state
    val listState = rememberLazyListState()

    // Auto-scroll when new messages arrive — matches iOS .onChange(of: wsManager.currentConversation.count)
    LaunchedEffect(conversation.size) {
        if (conversation.isNotEmpty()) {
            listState.animateScrollToItem(conversation.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .imePadding()
    ) {
        // MARK: - Back Button Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 8.dp, end = 16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OrangeSec7
                )
            }
        }

        // MARK: - Connection Status Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        when (connectionState) {
                            ConnectionState.CONNECTED -> Color.Green
                            ConnectionState.CONNECTING -> Color(0xFFFF9500)
                            ConnectionState.DISCONNECTED -> Color.Red
                        },
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when (connectionState) {
                    ConnectionState.CONNECTED -> "Connected"
                    ConnectionState.CONNECTING -> "Connecting..."
                    ConnectionState.DISCONNECTED -> "Disconnected"
                },
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        // MARK: - Header
        Text(
            text = "Chat with $recipientName",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = OrangeSec7,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        // MARK: - Messages List
        if (conversation.isEmpty()) {
            // Empty state — matches iOS emptyStateView
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 60.dp)
            ) {
                Text(text = "💬", fontSize = 50.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Start a live conversation with $recipientName...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
                if (connectionState != ConnectionState.CONNECTED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Messages will appear here once connected",
                        fontSize = 13.sp,
                        color = Color.Gray.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(conversation, key = { it.id }) { message ->
                    ChatMessageBubble(
                        message = message,
                        isFromMe = message.isFromMe(currentUserId)
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }

        // MARK: - Message Input Bar
        val canSend = newMessage.trim().isNotEmpty() && connectionState == ConnectionState.CONNECTED

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            TextField(
                value = newMessage,
                onValueChange = { newMessage = it },
                placeholder = { Text("Type a message...", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 17.sp,
                    color = OrangeSec7
                ),
                singleLine = false,
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val text = newMessage.trim()
                    if (text.isNotEmpty() && canSend) {
                        println("💬 ChatScreen: Sending message — I am $currentUserId, sending to $effectiveRecipientId")
                        wsManager.send(
                            text = text,
                            recipientId = effectiveRecipientId,
                            chatId = chatId,
                            senderId = currentUserId
                        )
                        newMessage = ""
                    }
                },
                enabled = canSend,
                modifier = Modifier
                    .background(
                        if (canSend) BurntOrange else Color.Gray,
                        CircleShape
                    )
                    .size(44.dp)
            ) {
                Text("➤", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

// MARK: - Chat Message Bubble — mirrors iOS ChatMessageBubble

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    isFromMe: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.75f)
        ) {
            // Message bubble
            Box(
                modifier = Modifier
                    .background(
                        if (isFromMe) BurntOrange else Color.White,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 17.sp,
                    color = if (isFromMe) Color.White else OrangeSec7
                )
            }

            // Timestamp + status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = SimpleDateFormat("h:mm a", Locale.US).format(message.timestamp),
                    fontSize = 11.sp,
                    color = OrangeSec7.copy(alpha = 0.6f)
                )

                if (isFromMe && message.isPending) {
                    Text(
                        text = "Sending...",
                        fontSize = 10.sp,
                        color = Color(0xFFFF9500)
                    )
                } else if (isFromMe) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Sent",
                        tint = Color.Green,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}