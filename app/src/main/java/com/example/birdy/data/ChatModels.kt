package com.example.birdy.data

import java.util.Date

/**
 * Chat models — mirrors iOS BirdyKit/Models.swift (ChatMessage, ConnectionState)
 */

/// WebSocket connection state for real-time chat.
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

/// Lightweight UI model for displaying chat messages in ChatView.
data class ChatMessage(
    val id: String,
    val text: String,
    val senderId: String,
    val timestamp: Date
) {
    /// Temporary IDs (prefixed with "temp_") indicate messages still being sent.
    val isPending: Boolean
        get() = id.startsWith("temp_")

    fun isFromMe(currentUserId: String): Boolean = senderId == currentUserId
}