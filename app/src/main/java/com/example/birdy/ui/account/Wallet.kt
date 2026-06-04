package com.example.birdy.ui.account

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.birdy.data.AuthManager
import com.example.birdy.data.Config
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
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
    var isEditing by remember { mutableStateOf(false) }

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
        // MARK: - Back Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 16.dp, end = 16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // MARK: - Header (matches iOS headerSection)
        Text(
            text = "Payment Methods",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 40.dp)
        )

        Text(
            text = "Manage your payment methods for faster checkout and seamless transactions.",
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Gray,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider(color = CardGrey)

        Spacer(modifier = Modifier.height(12.dp))

        // MARK: - Saved Payment Methods Section (matches iOS savedPaymentMethodsSection)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Saved Payment Methods",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = { isEditing = !isEditing }
            ) {
                Text(
                    text = if (isEditing) "Done" else "Edit",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEditing) Color.Red else BurntOrange
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Saved card content (matches iOS savedCardContent)
        Surface(
            shape = RoundedCornerShape(15.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
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

                // Card Row (matches iOS cardRow)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Card icon (matches iOS creditcard.fill — 25pt frame)
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = brand,
                        modifier = Modifier.size(25.dp),
                        tint = Color.Gray
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Card info column
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${brandDisplayName(brand)} •••• $last4",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BurntOrange
                        )
                        // Expiry
                        if (card.expMonth != null && card.expYear != null) {
                            val shortYear = card.expYear!! % 100
                            Text(
                                text = "Expires ${String.format(Locale.US, "%02d", card.expMonth!!)}/${String.format(Locale.US, "%02d", shortYear)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // "Default" Badge (matches iOS checkmark.circle.fill + "Default")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(BurntOrange.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = BurntOrange
                        )
                        Text(
                            text = "Default",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BurntOrange
                        )
                    }

                    // Delete button — only shows when editing (matches iOS isEditing check)
                    if (isEditing) {
                        Spacer(modifier = Modifier.width(8.dp))
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
                }

                // Expiring Soon Warning (matches iOS isExpiringSoon)
                if (isExpiringSoon(card)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF9500).copy(alpha = 0.08f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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

        Spacer(modifier = Modifier.height(12.dp))

        // MARK: - Security Note (matches iOS securityNote)
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

        Spacer(modifier = Modifier.height(20.dp))

        // MARK: - Add Payment Methods Section (matches iOS accountLinksSection)
        Text(
            text = "Add payment Methods",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(15.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column {
                // Credit / Debit Card Row (matches iOS first button — entire row clickable)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddSheet = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(25.dp),
                        tint = Color.Gray
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Credit / Debit Card",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BurntOrange
                        )
                        Text(
                            text = "Visa, Mastercard, Amex, Discover",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Add Card",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                HorizontalDivider(color = CardGrey, modifier = Modifier.padding(start = 50.dp))

                // PayPal Row (matches iOS PayPal button — entire row clickable)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddSheet = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(25.dp),
                        tint = Color.Gray
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "PayPal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BurntOrange
                        )
                        Text(
                            text = "Pay with your PayPal account",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "PayPal",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
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

// MARK: - Add Payment Method Sheet (matches iOS AddPaymentMethodSheet — custom card fields)
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

    // Custom card fields (matches iOS individual TextFields)
    var cardNumber by remember { mutableStateOf("") }
    var expDate by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }
    var zipCode by remember { mutableStateOf("") }

    // Form validation (matches iOS isFormValid)
    val isFormValid = remember(cardNumber, expDate, cvc, zipCode) {
        val cleanNumber = cardNumber.replace(" ", "")
        cleanNumber.length >= 15 &&
        expDate.length == 5 && // MM/YY
        cvc.length >= 3 &&
        zipCode.length >= 5
    }

    // Card brand detection from number prefix (matches iOS detectedBrand)
    val detectedBrand = remember(cardNumber) {
        val clean = cardNumber.replace(" ", "")
        when {
            clean.startsWith("4") -> "Visa"
            clean.startsWith("5") || clean.startsWith("2") -> "Mastercard"
            clean.startsWith("3") -> "Amex"
            clean.startsWith("6") -> "Discover"
            else -> ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Drag Handle (matches iOS RoundedRectangle drag handle)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(5.dp)
                    .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
            )

            // Title + Close Button (matches iOS HStack with title + xmark button)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Text(
                    text = "Saved Payment Methods",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
                    modifier = Modifier
                        .size(30.dp)
                        .background(SoftGrey, RoundedCornerShape(50))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            HorizontalDivider(color = CardGrey)

            Spacer(modifier = Modifier.height(24.dp))

            // Card Form Fields (matches iOS VStack with 4 rows)
            Text(
                text = "Enter your card details",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Row 1 — Card Number (matches iOS card number field)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Card Number",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { newValue ->
                        val formatted = formatCardNumber(newValue)
                        if (formatted != newValue) {
                            cardNumber = formatted
                        } else {
                            cardNumber = newValue
                        }
                    },
                    placeholder = { Text("1234 5678 9012 3456", color = Color.Gray) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (cardNumber.isEmpty()) Color.Gray else BurntOrange
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = SoftGrey,
                        focusedContainerColor = SoftGrey,
                        unfocusedBorderColor = CardGrey,
                        focusedBorderColor = BurntOrange
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2 — Expiration & CVC side by side (matches iOS HStack)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Expiration
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Expiration",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    OutlinedTextField(
                        value = expDate,
                        onValueChange = { newValue ->
                            val formatted = formatExpDate(newValue)
                            expDate = formatted
                        },
                        placeholder = { Text("MM/YY", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SoftGrey,
                            focusedContainerColor = SoftGrey,
                            unfocusedBorderColor = CardGrey,
                            focusedBorderColor = BurntOrange
                        )
                    )
                }

                // CVC
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CVC",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    OutlinedTextField(
                        value = cvc,
                        onValueChange = { newValue ->
                            if (newValue.length <= 4) cvc = newValue.filter { it.isDigit() }
                        },
                        placeholder = { Text("123", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SoftGrey,
                            focusedContainerColor = SoftGrey,
                            unfocusedBorderColor = CardGrey,
                            focusedBorderColor = BurntOrange
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 3 — ZIP Code (matches iOS ZIP field)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ZIP Code",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                OutlinedTextField(
                    value = zipCode,
                    onValueChange = { newValue ->
                        if (newValue.length <= 5) zipCode = newValue.filter { it.isDigit() }
                    },
                    placeholder = { Text("10001", color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = SoftGrey,
                        focusedContainerColor = SoftGrey,
                        unfocusedBorderColor = CardGrey,
                        focusedBorderColor = BurntOrange
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Security badge (matches iOS lock.shield.fill row)
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

            // Error Message (matches iOS errorMessage conditional)
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

            Spacer(modifier = Modifier.weight(1f))

            // Add Card Button (matches iOS "Add Card" button)
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null

                    scope.launch {
                        try {
                            val card = saveCardFromFields(context, cardNumber, expDate, cvc, zipCode)
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
                enabled = isFormValid && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFormValid && !isLoading) BurntOrange else Color.Gray
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

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// MARK: - Formatting Helpers (matches iOS formatCardNumber, formatExpDate)

private fun formatCardNumber(value: String): String {
    val digits = value.filter { it.isDigit() }
    val limited = digits.take(16)
    val result = StringBuilder()
    for ((i, char) in limited.withIndex()) {
        if (i > 0 && i % 4 == 0) result.append(' ')
        result.append(char)
    }
    return result.toString()
}

private fun formatExpDate(value: String): String {
    val digits = value.filter { it.isDigit() }.take(4)
    return if (digits.length > 2) {
        digits.substring(0, 2) + "/" + digits.substring(2)
    } else {
        digits
    }
}

// MARK: - API: Save Card from individual fields (matches iOS saveCard — builds PaymentMethodCreateParams manually)
private suspend fun saveCardFromFields(
    context: android.content.Context,
    cardNumber: String,
    expDate: String,
    cvc: String,
    zipCode: String
): SavedCard? {
    return withContext(Dispatchers.IO) {
        try {
            // Parse expiration (matches iOS expParts parsing)
            val expParts = expDate.split("/")
            if (expParts.size != 2) {
                Log.e("Wallet", "❌ Invalid expiration date")
                return@withContext null
            }

            val expMonth = expParts[0].toIntOrNull()
            val expYearShort = expParts[1].toIntOrNull()
            if (expMonth == null || expYearShort == null) {
                Log.e("Wallet", "❌ Invalid expiration date format")
                return@withContext null
            }

            // Convert 2-digit year to 4-digit (matches iOS fullYear logic)
            val fullYear = if (expYearShort < 100) 2000 + expYearShort else expYearShort

            val cleanNumber = cardNumber.replace(" ", "")

            // Build PaymentMethodCreateParams from individual fields (matches iOS STPPaymentMethodCardParams)
            val cardParams = PaymentMethodCreateParams.Card(
                number = cleanNumber,
                expiryMonth = expMonth,
                expiryYear = fullYear,
                cvc = cvc
            )

            val billingDetails = PaymentMethod.BillingDetails(
                address = Address(
                    postalCode = zipCode
                )
            )

            val paymentMethodParams = PaymentMethodCreateParams.create(
                card = cardParams,
                billingDetails = billingDetails
            )

            // Create PaymentMethod via Stripe SDK
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

            // Send pm_ID to Go backend (matches iOS attachPaymentMethod)
            attachPaymentMethod(context, pmId)
        } catch (e: Exception) {
            Log.e("Wallet", "❌ Error saving card: ${e.message}")
            null
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