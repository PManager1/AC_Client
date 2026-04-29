package com.example.birdy.ui.account

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.birdy.data.AuthManager
import com.example.birdy.data.Config
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.CardInputWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale

// MARK: - Color Constants
private val BurntOrange = Color(0xFFCC5500)
private val SoftGrey = Color(0xFFF2F2F7)
private val CardGrey = Color(0xFFE5E5EA)

// MARK: - Saved Card Model (matches backend SavedPaymentMethod + iOS SavedCard)
data class SavedCard(
    val id: String? = null,
    val brand: String? = null,
    val last4: String? = null,
    val expMonth: Int? = null,
    val expYear: Int? = null
)

// MARK: - Wallet View (matches iOS Wallet.swift)
@Composable
fun Wallet(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var savedCard by remember { mutableStateOf<SavedCard?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isDeletingCard by remember { mutableStateOf(false) }

    // Fetch saved card on appear — matches iOS .task { await fetchSavedCard() }
    LaunchedEffect(Unit) {
        isLoading = true
        savedCard = fetchSavedCard(context)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // MARK: - Header
        Text(
            text = "Payment Methods",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 40.dp)
        )

        // Instructions
        Text(
            text = "Manage your payment methods for faster checkout and seamless transactions.",
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Gray,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider(color = CardGrey)

        Spacer(modifier = Modifier.height(12.dp))

        // MARK: - Payment Methods Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = SoftGrey,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // Add Payment Method Button
                Button(
                    onClick = { showAddSheet = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = null
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = BurntOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Payment Method",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Navigate",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Divider between add button and saved card
                if (savedCard != null) {
                    HorizontalDivider(color = CardGrey, modifier = Modifier.padding(vertical = 8.dp))
                }

                // Loading State
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                } else if (savedCard != null) {
                    val card = savedCard!!
                    val brand = card.brand ?: ""
                    val last4 = card.last4 ?: "????"

                    // --- Card Display ---
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Brand Icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(brandColor(brand).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = brand,
                                tint = brandColor(brand),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Card Info
                        Column {
                            Text(
                                text = "${brandDisplayName(brand)} •••• $last4",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                            // Expiry
                            if (card.expMonth != null && card.expYear != null) {
                                val shortYear = card.expYear!! % 100
                                Text(
                                    text = "Expires ${String.format(Locale.US, "%02d", card.expMonth!!)}/${String.format(Locale.US, "%02d", shortYear)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // "Default" Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BurntOrange.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "Default",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BurntOrange,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Delete Button (matches iOS trash button)
                        IconButton(
                            onClick = { showDeleteConfirmation = true },
                            enabled = !isDeletingCard,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Red.copy(alpha = 0.1f))
                        ) {
                            if (isDeletingCard) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.Red
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Card",
                                    tint = Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // --- "Expiring Soon" Warning ---
                    if (isExpiringSoon(card)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFF9500).copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = Color(0xFFFF9500),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Card expiring soon — update to avoid failed orders.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFF9500)
                                )
                            }
                        }
                    }
                }
            }
        }

        // MARK: - Security Note
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Secure",
                tint = Color(0xFF34C759),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Your card details are securely stored with Stripe.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray
            )
        }
    }

    // MARK: - Delete Confirmation Dialog (matches iOS .alert("Remove Card?"))
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = "Remove Card?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val card = savedCard
                if (card != null) {
                    val brand = card.brand ?: ""
                    val last4 = card.last4 ?: "????"
                    Text("This will remove your ${brandDisplayName(brand)} card ending in $last4. You can always add it again later.")
                } else {
                    Text("This will remove your saved payment method. You can always add it again later.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        scope.launch {
                            isDeletingCard = true
                            val success = deleteCard(context)
                            if (success) {
                                savedCard = null
                                Log.d("Wallet", "✅ Card deleted successfully")
                            } else {
                                Log.e("Wallet", "❌ Failed to delete card")
                            }
                            isDeletingCard = false
                        }
                    }
                ) {
                    Text("Remove", color = Color.Red, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // MARK: - Add Payment Method Bottom Sheet
    if (showAddSheet) {
        AddPaymentMethodSheet(
            onDismiss = { showAddSheet = false },
            onCardSaved = { card ->
                savedCard = card
                showAddSheet = false
            }
        )
    }
}

// MARK: - Add Payment Method Sheet (matches iOS AddPaymentMethodSheet)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentMethodSheet(
    onDismiss: () -> Unit,
    onCardSaved: (SavedCard) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cardInputWidget by remember { mutableStateOf<CardInputWidget?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Close button row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Add Payment Method",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = CardGrey)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Enter your card details",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stripe's secure multi-line card input — shows all fields at once:
            // Card Number, Expiry, CVC, and Postal/ZIP code (postalCodeEnabled = true)
            // Matches iOS STPPaymentCardTextField behavior
            AndroidView(
                factory = { ctx ->
                    CardInputWidget(ctx).apply {
                        postalCodeEnabled = true
                        cardInputWidget = this
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Security badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Secure",
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Secured by Stripe — we never see your card number",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Gray
                )
            }

            // Error Message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = errorMessage!!,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Red
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Add Card Button
            Button(
                onClick = {
                    val widget = cardInputWidget ?: return@Button
                    val paymentMethodParams = widget.paymentMethodCreateParams
                    if (paymentMethodParams == null) {
                        errorMessage = "Please enter valid card details"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    scope.launch {
                        try {
                            val card = saveCardViaStripe(context, paymentMethodParams)
                            if (card != null) {
                                Log.d("Wallet", "✅ Card saved: ${card.brand} •••• ${card.last4}")
                                onCardSaved(card)
                            } else {
                                errorMessage = "Failed to save card"
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Unknown error"
                            Log.e("Wallet", "❌ Error saving card: ${e.message}")
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLoading) Color.Gray else BurntOrange
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Add Card",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Bottom safe area padding — matches iOS sheet safe area
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// MARK: - API: Fetch Saved Card (matches iOS fetchSavedCard)
private suspend fun fetchSavedCard(context: android.content.Context): SavedCard? {
    return withContext(Dispatchers.IO) {
        try {
            val token = AuthManager.getToken(context)
            if (token.isNullOrEmpty()) {
                Log.w("Wallet", "⚠️ No auth token found")
                return@withContext null
            }

            val url = URL("${Config.API_BASE_URL}/stripe/payment-method")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
            }

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                Log.w("Wallet", "⚠️ Failed to fetch payment method (HTTP $responseCode)")
                return@withContext null
            }

            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)

            if (json.optBoolean("hasPaymentMethod", false)) {
                val pmJson = json.optJSONObject("paymentMethod")
                if (pmJson != null) {
                    val card = SavedCard(
                        id = pmJson.optString("id").takeIf { it.isNotEmpty() },
                        brand = pmJson.optString("brand").takeIf { it.isNotEmpty() },
                        last4 = pmJson.optString("last4").takeIf { it.isNotEmpty() },
                        expMonth = pmJson.optInt("expMonth", 0).let { if (it == 0) null else it },
                        expYear = pmJson.optInt("expYear", 0).let { if (it == 0) null else it }
                    )
                    Log.d("Wallet", "✅ Loaded saved card: ${card.brand} •••• ${card.last4}")
                    return@withContext card
                }
            }

            Log.i("Wallet", "ℹ️ No saved payment method")
            null
        } catch (e: Exception) {
            Log.e("Wallet", "❌ Error fetching card: ${e.message}")
            null
        }
    }
}

// MARK: - API: Delete Card (matches iOS deleteCard)
private suspend fun deleteCard(context: android.content.Context): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val token = AuthManager.getToken(context)
            if (token.isNullOrEmpty()) {
                Log.w("Wallet", "⚠️ No auth token found")
                return@withContext false
            }

            val url = URL("${Config.API_BASE_URL}/stripe/payment-method")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
            }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                Log.d("Wallet", "✅ Card deleted successfully")
                true
            } else {
                Log.w("Wallet", "⚠️ Failed to delete card (HTTP $responseCode)")
                false
            }
        } catch (e: Exception) {
            Log.e("Wallet", "❌ Error deleting card: ${e.message}")
            false
        }
    }
}

// MARK: - API: Save Card via Stripe SDK → Backend (matches iOS saveCard + attachPaymentMethod)
// Accepts PaymentMethodCreateParams directly from CardInputWidget — never touches raw card fields
private suspend fun saveCardViaStripe(
    context: android.content.Context,
    paymentMethodParams: PaymentMethodCreateParams
): SavedCard? {
    return withContext(Dispatchers.IO) {
        // 1. Create PaymentMethod via Stripe SDK (card data goes directly to Stripe — same as iOS)
        val stripe = Stripe(context, PaymentConfiguration.getInstance(context).publishableKey)

        val paymentMethod: PaymentMethod? = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            stripe.createPaymentMethod(
                paymentMethodParams,
                callback = object : ApiResultCallback<PaymentMethod> {
                    override fun onSuccess(result: PaymentMethod) {
                        cont.resume(result) {}
                    }

                    override fun onError(e: Exception) {
                        cont.cancel(e)
                    }
                }
            )
        }

        if (paymentMethod == null) {
            Log.e("Wallet", "❌ Failed to create PaymentMethod")
            return@withContext null
        }

        val pmId = paymentMethod.id ?: ""
        Log.d("Wallet", "✅ Created PaymentMethod: $pmId")

        // 2. Send pm_ID to our Go backend (which attaches it to Stripe Customer + saves to MongoDB)
        attachPaymentMethod(context, pmId)
    }
}

// MARK: - API: Attach PaymentMethod to Backend (matches iOS attachPaymentMethod)
private suspend fun attachPaymentMethod(
    context: android.content.Context,
    pmId: String
): SavedCard? {
    return withContext(Dispatchers.IO) {
        try {
            val token = AuthManager.getToken(context)
            if (token.isNullOrEmpty()) {
                Log.w("Wallet", "⚠️ Not authenticated")
                return@withContext null
            }

            val url = URL("${Config.API_BASE_URL}/stripe/attach-payment-method")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val body = JSONObject().apply {
                put("paymentMethodId", pmId)
            }
            conn.outputStream.write(body.toString().toByteArray())

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val card = SavedCard(
                    id = json.optString("id").takeIf { it.isNotEmpty() },
                    brand = json.optString("brand").takeIf { it.isNotEmpty() },
                    last4 = json.optString("last4").takeIf { it.isNotEmpty() },
                    expMonth = json.optInt("expMonth", 0).let { if (it == 0) null else it },
                    expYear = json.optInt("expYear", 0).let { if (it == 0) null else it }
                )
                Log.d("Wallet", "✅ Card attached: ${card.brand} •••• ${card.last4}")
                card
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.readText()
                Log.e("Wallet", "❌ Failed to save card (HTTP $responseCode): $errorBody")
                null
            }
        } catch (e: Exception) {
            Log.e("Wallet", "❌ Error attaching card: ${e.message}")
            null
        }
    }
}

// MARK: - Expiring Soon Check (within 30 days) — matches iOS isExpiringSoon
private fun isExpiringSoon(card: SavedCard): Boolean {
    val expMonth = card.expMonth ?: return false
    val expYear = card.expYear ?: return false

    val calendar = Calendar.getInstance()
    val now = calendar.time

    // Expiry date is last day of the month
    calendar.set(Calendar.YEAR, expYear)
    calendar.set(Calendar.MONTH, expMonth - 1 + 1) // +1 to get next month
    calendar.set(Calendar.DAY_OF_MONTH, 0) // last day of expiry month
    val expiryDate = calendar.time

    calendar.time = now
    calendar.add(Calendar.DAY_OF_YEAR, 30)
    val thirtyDaysFromNow = calendar.time

    return expiryDate <= thirtyDaysFromNow
}

// MARK: - Brand Helpers (matches iOS brandColor, brandDisplayName)
private fun brandColor(brand: String): Color {
    return when (brand.lowercase()) {
        "visa" -> Color(0xFF1A1F71)
        "mastercard" -> Color(0xFFFF5F00)
        "amex" -> Color(0xFF2E77BC)
        "discover" -> Color(0xFFFF6000)
        else -> BurntOrange
    }
}

private fun brandDisplayName(brand: String): String {
    return when (brand.lowercase()) {
        "visa" -> "Visa"
        "mastercard" -> "Mastercard"
        "amex" -> "American Express"
        "discover" -> "Discover"
        else -> brand.replaceFirstChar { it.uppercase() }
    }
}