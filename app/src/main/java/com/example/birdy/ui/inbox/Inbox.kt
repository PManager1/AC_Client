package com.example.birdy.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.example.birdy.data.InboxData
import com.example.birdy.data.InboxMessage
import com.example.birdy.data.Notification
import com.example.birdy.data.ScheduledRequest
import com.example.birdy.data.SupportTicket

// Matches iOS Inbox.swift
// - Header: "Inbox" title + download icon
// - Segmented picker: Notifications | Scheduled Requests | Support
// - Tab content below

@Composable
fun InboxScreen(
    modifier: Modifier = Modifier,
    onNavigateToRequestDetail: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // MARK: - Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Inbox",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { /* TODO: download/export */ }) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // MARK: - Segmented Tab Picker (matches iOS .segmented picker)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            val tabs = listOf("Notifications", "Scheduled Requests", "Support")
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color.Black else Color.Gray,
                    modifier = Modifier
                        .clickable { selectedTab = index }
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                )
            }
        }

        // Active tab indicator underline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE0E0E0))
        )

        // MARK: - Tab Content (takes remaining space, scrollable)
        when (selectedTab) {
            0 -> NotificationsTabView(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                onMessageClick = onNavigateToRequestDetail
            )
            1 -> ScheduledRequestsTabView(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            )
            2 -> SupportTabView(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

// MARK: - Notifications Tab (fetches real notifications from API — matches iOS Inbox.swift)

@Composable
fun NotificationsTabView(
    modifier: Modifier = Modifier,
    onMessageClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch notifications on first composition — matches iOS .onAppear { fetchNotifications() }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        notifications = InboxData.fetchNotifications(context)
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        if (isLoading) {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", color = Color.Gray, fontSize = 15.sp)
            }
        } else if (notifications.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No notifications yet", color = Color.Gray, fontSize = 15.sp)
            }
        } else {
            notifications.forEachIndexed { index, notification ->
                NotificationRowView(
                    notification = notification,
                    onClick = onMessageClick
                )
                if (index < notifications.size - 1) {
                    HorizontalDivider(
                        color = Color(0xFFE0E0E0),
                        thickness = 1.dp,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

/** Displays a single notification row — matches iOS notification row in Inbox.swift */
@Composable
fun NotificationRowView(notification: Notification, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Icon based on notification type
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFA500).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = "Order",
                tint = Color(0xFFFFA500),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = notification.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            if (notification.subtitle.isNotEmpty()) {
                Text(
                    text = notification.subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = notification.timeAgo,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray
            )
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCC5500))
                )
            }
        }
    }
}

@Composable
fun MessageRowView(message: InboxMessage, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = message.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = message.timeAgo,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray
            )
            if (message.isUnread) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Blue)
                )
            }
        }
    }
}

// MARK: - Scheduled Requests Tab

@Composable
fun ScheduledRequestsTabView(
    modifier: Modifier = Modifier
) {
    val requests = remember { InboxData.scheduledRequests }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        requests.forEach { request ->
            ScheduledRequestCard(request = request)
        }
    }
}

@Composable
fun ScheduledRequestCard(request: ScheduledRequest) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF2F2F7),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = request.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Text(
                text = "${request.date} at ${request.time}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray
            )
        }
    }
}

// MARK: - Support Tab

@Composable
fun SupportTabView(
    modifier: Modifier = Modifier
) {
    val tickets = remember { InboxData.supportTickets }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        tickets.forEach { ticket ->
            SupportTicketCard(ticket = ticket)
        }
    }
}

@Composable
fun SupportTicketCard(ticket: SupportTicket) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF2F2F7),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Ticket ${ticket.ticketID}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = ticket.status,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (ticket.status == "Open") Color(0xFF4CAF50) else Color.Red
                )
            }
            Text(
                text = ticket.subject,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray
            )
        }
    }
}