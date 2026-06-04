package com.example.birdy.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * WebSocket Manager — mirrors iOS IC/Birdy/Booking/WebSocketManager.swift
 * Manages a single WebSocket connection for real-time chat using OkHttp.
 */
class WebSocketManager private constructor() {

    companion object {
        val shared = WebSocketManager()
        private const val TAG = "WebSocketManager"
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }

    // MARK: - State Flows (replaces iOS @Published)
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _currentConversation = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentConversation: StateFlow<List<ChatMessage>> = _currentConversation.asStateFlow()

    // MARK: - Private Properties
    private var webSocket: WebSocket? = null
    private var okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private var currentUserId: String = ""
    private var reconnectAttempts: Int = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // MARK: - Connect
    fun connect(token: String, userId: String) {
        currentUserId = userId
        _connectionState.value = ConnectionState.CONNECTING

        val url = "${Config.WS_API_BASE_URL}?token=$token&userId=$userId"
        val request = Request.Builder().url(url).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "✅ WebSocket connected")
                scope.launch {
                    _connectionState.value = ConnectionState.CONNECTED
                    reconnectAttempts = 0
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "📩 Received: $text")
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "🔌 WebSocket closed")
                scope.launch {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "❌ WebSocket failure: ${t.message}")
                scope.launch {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    handleDisconnect()
                }
            }
        })

        Log.d(TAG, "🔗 Connecting to $url")
    }

    // MARK: - Disconnect
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _currentConversation.value = emptyList()
        Log.d(TAG, "🔌 Disconnected")
    }

    // MARK: - Send Message
    fun send(text: String, recipientId: String, chatId: String, senderId: String) {
        // 1. Add optimistic local message
        val tempId = "temp_${UUID.randomUUID()}"
        val localMessage = ChatMessage(
            id = tempId,
            text = text,
            senderId = senderId,
            timestamp = Date()
        )
        _currentConversation.value = _currentConversation.value + localMessage

        // 2. Send via WebSocket
        val json = JSONObject().apply {
            put("action", "sendMessage")
            put("chatId", chatId)
            put("recipientId", recipientId)
            put("senderId", senderId)
            put("content", text)
            put("messageType", "text")
        }

        val success = webSocket?.send(json.toString()) ?: false
        if (success) {
            Log.d(TAG, "✅ Message sent to chat $chatId")
        } else {
            Log.e(TAG, "❌ Failed to send message")
        }
    }

    // MARK: - Handle Incoming Messages
    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type", "")

            when (type) {
                "message", "new_message", "newMessage" -> {
                    val data = json.optJSONObject("data") ?: json
                    val id = data.optString("messageId", data.optString("_id", data.optString("id", UUID.randomUUID().toString())))
                    val senderId = data.optString("senderId", "")
                    val content = data.optString("content", "")
                    val timestamp = parseDate(data.optString("createdAt", "")) ?: Date()

                    // Replace optimistic temp message if this is our own echoed message
                    if (senderId == currentUserId) {
                        val messages = _currentConversation.value.toMutableList()
                        val tempIndex = messages.indexOfLast { it.id.startsWith("temp_") }
                        if (tempIndex >= 0) {
                            val confirmedMessage = ChatMessage(
                                id = id,
                                text = content,
                                senderId = senderId,
                                timestamp = timestamp
                            )
                            messages[tempIndex] = confirmedMessage
                            scope.launch { _currentConversation.value = messages }
                            return
                        }
                    }

                    // Otherwise append incoming message
                    val incomingMessage = ChatMessage(
                        id = id,
                        text = content,
                        senderId = senderId,
                        timestamp = timestamp
                    )
                    scope.launch {
                        _currentConversation.value = _currentConversation.value + incomingMessage
                    }
                }

                "message_sent", "messageSent" -> {
                    val data = json.optJSONObject("data")
                    if (data != null) {
                        val messageId = data.optString("messageId", "")
                        if (messageId.isNotEmpty()) {
                            val messages = _currentConversation.value.toMutableList()
                            val tempIndex = messages.indexOfLast { it.id.startsWith("temp_") }
                            if (tempIndex >= 0) {
                                val confirmed = ChatMessage(
                                    id = messageId,
                                    text = messages[tempIndex].text,
                                    senderId = messages[tempIndex].senderId,
                                    timestamp = parseDate(data.optString("createdAt", "")) ?: Date()
                                )
                                messages[tempIndex] = confirmed
                                scope.launch { _currentConversation.value = messages }
                            }
                        }
                    }
                    Log.d(TAG, "✅ Server confirmed message saved")
                }

                "error" -> {
                    val errorMsg = json.optString("message", "Unknown error")
                    Log.e(TAG, "❌ Server error: $errorMsg")
                }

                "connected" -> {
                    Log.d(TAG, "✅ Server acknowledged connection")
                }

                else -> {
                    Log.d(TAG, "ℹ️ Unhandled message type '$type'")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to parse incoming message: ${e.message}")
        }
    }

    // MARK: - Reconnection
    private fun handleDisconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "❌ Max reconnection attempts reached")
            return
        }

        reconnectAttempts++
        val delay = minOf(reconnectAttempts * 2000L, 10_000L) // exponential back-off, max 10s

        Log.d(TAG, "🔄 Reconnecting in ${delay}ms (attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)")

        scope.launch {
            kotlinx.coroutines.delay(delay)
            val token = AuthManager.getToken()
            if (!token.isNullOrEmpty()) {
                connect(token, currentUserId)
            }
        }
    }

    // MARK: - Helpers
    private fun parseDate(value: String): Date? {
        if (value.isEmpty()) return null
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            formatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
            formatter.parse(value)
        } catch (e: Exception) {
            try {
                val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                formatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
                formatter.parse(value)
            } catch (e2: Exception) {
                null
            }
        }
    }
}