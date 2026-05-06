package com.example.birdy.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

// Matches iOS Inbox.swift data models

data class InboxMessage(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val timeAgo: String,
    val isUnread: Boolean = false
)

data class ScheduledRequest(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String,
    val time: String
)

data class SupportTicket(
    val id: String = UUID.randomUUID().toString(),
    val ticketID: String,
    val status: String,
    val subject: String
)

/**
 * Notification model matching backend BK/models/Notification.go
 * Used to display real order notifications from the /notifications API
 */
data class Notification(
    val id: String = "",
    val userId: String = "",
    val service: String = "",
    val type: String = "",
    val title: String = "",
    val subtitle: String = "",
    val orderNumber: String = "",
    val orderId: String = "",
    val isRead: Boolean = false,
    val createdAt: String = "",  // ISO 8601 from backend
) {
    /** Convert ISO createdAt to human-readable "time ago" string */
    val timeAgo: String
        get() = run {
            if (createdAt.isEmpty()) return ""
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val notifTime = sdf.parse(createdAt.substring(0, 19)) ?: return ""
                val diffMs = Date().time - notifTime.time
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
                val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
                val days = TimeUnit.MILLISECONDS.toDays(diffMs)
                when {
                    minutes < 1 -> "Just now"
                    minutes < 60 -> "${minutes}m ago"
                    hours < 24 -> "${hours}h ago"
                    days < 7 -> "${days}d ago"
                    else -> "${days / 7}w ago"
                }
            } catch (e: Exception) {
                ""
            }
        }
}

object InboxData {

    val scheduledRequests = listOf(
        ScheduledRequest(title = "Babysitting for Sarah's kids", date = "Friday, Nov 22", time = "5:00 PM - 8:00 PM"),
        ScheduledRequest(title = "Dog walking for Buster", date = "Saturday, Nov 23", time = "10:00 AM - 11:00 AM"),
        ScheduledRequest(title = "Home cleaning service", date = "Monday, Nov 25", time = "1:00 PM - 3:00 PM")
    )

    val supportTickets = listOf(
        SupportTicket(ticketID = "#53215", status = "Open", subject = "Dispute about a recent trip"),
        SupportTicket(ticketID = "#53214", status = "Closed", subject = "Refund for a cancelled order"),
        SupportTicket(ticketID = "#53213", status = "Open", subject = "Problem with the app's GPS")
    )

    /**
     * Fetch real notifications from GET /notifications (matches iOS Inbox.swift fetchNotifications)
     */
    suspend fun fetchNotifications(context: Context): List<Notification> {
        return withContext(Dispatchers.IO) {
            try {
                val token = AuthManager.getToken(context)
                if (token.isNullOrEmpty()) {
                    Log.w("InboxData", "No auth token, skipping notification fetch")
                    return@withContext emptyList()
                }

                val url = URL("${Config.API_BASE_URL}/notifications")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val responseCode = conn.responseCode
                Log.d("InboxData", "GET /notifications → HTTP $responseCode")

                if (responseCode == 200) {
                    val jsonStr = conn.inputStream.bufferedReader().readText()
                    val jsonArray = JSONArray(jsonStr)
                    val notifications = mutableListOf<Notification>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        notifications.add(
                            Notification(
                                id = obj.optString("_id", ""),
                                userId = obj.optString("userId", ""),
                                service = obj.optString("service", ""),
                                type = obj.optString("type", ""),
                                title = obj.optString("title", ""),
                                subtitle = obj.optString("subtitle", ""),
                                orderNumber = obj.optString("orderNumber", ""),
                                orderId = obj.optString("orderId", ""),
                                isRead = obj.optBoolean("isRead", false),
                                createdAt = obj.optString("createdAt", ""),
                            )
                        )
                    }
                    Log.d("InboxData", "Fetched ${notifications.size} notifications")
                    notifications
                } else {
                    Log.w("InboxData", "Failed to fetch notifications: HTTP $responseCode")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("InboxData", "Error fetching notifications", e)
                emptyList()
            }
        }
    }
}