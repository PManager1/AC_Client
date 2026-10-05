package com.example.birdy.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.birdy.kit.chat.OrderChatScreen
import com.example.birdy.data.AuthManager
import com.example.birdy.data.CartManager
import com.example.birdy.data.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Accent = Color(0xFFD95F02)

/**
 * "Message Driver": opens the chat for the order just placed (Checkout saves its udo3
 * id/number on CartManager). After a relaunch the cart is empty, so it loads the customer's
 * latest order from GET /orders (newest first) and keeps it on the cart for next time.
 */
@Composable
fun OrderChatSheet(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var noOrders by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    fun loadLatestOrder() {
        if (isLoading) return
        isLoading = true
        loadError = null
        scope.launch {
            when (val result = fetchLatestOrder()) {
                is LatestOrder.Found -> {
                    CartManager.orderNumber = result.orderNumber
                    CartManager.orderId = result.id
                }
                LatestOrder.None -> noOrders = true
                is LatestOrder.Failed -> loadError = result.message
            }
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding(),
        ) {
            val orderId = CartManager.orderId
            if (orderId.isNotEmpty()) {
                OrderChatScreen(
                    orderId = orderId,
                    fallbackTitle = CartManager.orderNumber.ifEmpty { "Your order" },
                    placeholder = "Message your provider...",
                    accent = Accent,
                    onBack = onClose,
                )
            } else {
                IconButton(onClick = onClose, modifier = Modifier.padding(4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when {
                        noOrders -> EmptyMessage("No orders yet")
                        loadError != null -> {
                            EmptyMessage("Couldn't load your order")
                            Text(loadError ?: "", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            TextButton(onClick = ::loadLatestOrder) {
                                Text("Retry", color = Accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        else -> {
                            CircularProgressIndicator(color = Accent)
                            Text("Loading your order…", color = Color.Gray, modifier = Modifier.padding(top = 12.dp))
                            LaunchedEffect(Unit) { loadLatestOrder() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Text("💬", fontSize = 50.sp)
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF003366), modifier = Modifier.padding(8.dp))
}

private sealed class LatestOrder {
    data class Found(val id: String, val orderNumber: String) : LatestOrder()
    object None : LatestOrder()
    data class Failed(val message: String) : LatestOrder()
}

/** GET /orders → {"data": [{"id", "order_number", …}]}, newest first. */
private suspend fun fetchLatestOrder(): LatestOrder = withContext(Dispatchers.IO) {
    val token = AuthManager.getToken() ?: return@withContext LatestOrder.Failed("Please sign in again.")
    try {
        val conn = URL("${Config.API_BASE_URL}/orders").openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        try {
            if (conn.responseCode != 200) return@withContext LatestOrder.Failed("Please try again.")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val latest = JSONObject(body).optJSONArray("data")?.optJSONObject(0)
                ?: return@withContext LatestOrder.None
            LatestOrder.Found(latest.getString("id"), latest.optString("order_number", ""))
        } finally {
            conn.disconnect()
        }
    } catch (e: Exception) {
        LatestOrder.Failed(e.message ?: "Please try again.")
    }
}
