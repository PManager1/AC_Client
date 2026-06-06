package com.example.birdy.ui.explore

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.birdy.data.AuthManager
import com.example.birdy.data.CartManager
import com.example.birdy.data.Config
import com.example.birdy.data.AddressService
import com.example.birdy.ui.fooddelivery.Address
import com.example.birdy.ui.account.Wallet
import com.example.birdy.ui.fooddelivery.SelectAddressSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// MARK: - Mock Data Models (matches iOS Checkout.swift)

data class DeliveryAddress(
    val id: String,
    val title: String,
    val fullAddress: String,
    val instructions: String
)

data class PaymentMethod(
    val id: String,
    val type: String,
    val last4: String?,
    val brandIcon: String
)

// MARK: - Checkout Screen (matches iOS Checkout view)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onTrackOrder: () -> Unit = {}
) {
    // Mock data — matches iOS addresses/paymentMethods
    val addresses = remember {
        listOf(
            DeliveryAddress(
                id = "home",
                title = "Home",
                fullAddress = "123 Main Street, Apt 4B, New York, NY 10001",
                instructions = "Ring bell 3 times"
            ),
            DeliveryAddress(
                id = "work",
                title = "Work",
                fullAddress = "456 Broadway, Floor 12, New York, NY 10003",
                instructions = "Security will buzz you in"
            )
        )
    }

    val paymentMethods = remember {
        listOf(
            PaymentMethod(id = "gpay", type = "Google Pay", last4 = null, brandIcon = "gpay"),
            PaymentMethod(id = "visa", type = "Visa", last4 = "4242", brandIcon = "visa")
        )
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedAddress by remember { mutableStateOf(addresses.first()) }
    var showSelectAddress by remember { mutableStateOf(false) }
    var selectedPayment by remember { mutableStateOf(paymentMethods.first()) }
    var tipAmount by remember { mutableStateOf(4.0) }
    var leaveAtDoor by remember { mutableStateOf(true) }
    var showOrderSuccess by remember { mutableStateOf(false) }
    var isPlacingOrder by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showTipPage by remember { mutableStateOf(false) }
    var showWallet by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("Delivery") }

    val totalWithTip = CartManager.total + tipAmount

    // MARK: - Place Order — calls POST /orders (matches iOS handlePlaceOrder)
    suspend fun handlePlaceOrder() {
        val token = AuthManager.getToken(context)
        if (token.isNullOrEmpty()) {
            errorMessage = "Not authenticated — please log in again."
            return
        }

        isPlacingOrder = true

        try {
            // Build the order payload — matches iOS Checkout.swift orderPayload exactly
            val itemsArray = JSONArray().apply {
                CartManager.items.forEach { item ->
                    put(JSONObject().apply {
                        put("itemId", item.menuItem?.id ?: java.util.UUID.randomUUID().toString())
                        put("itemName", item.dishName)
                        put("price", item.price)
                        put("quantity", item.quantity)
                        put("selectedOptions", JSONArray(item.selectedOptions))
                        put("specialInstructions", item.specialInstructions)
                        put("imageURL", item.imageURL)
                    })
                }
            }

            val restaurantName = CartManager.items.firstOrNull()?.restaurantName ?: "Unknown Restaurant"

            val addressDict = JSONObject().apply {
                put("street", selectedAddress.fullAddress)
                put("cityStateZip", "")
                put("isDefault", selectedAddress.id == "home")
            }

            val orderPayload = JSONObject().apply {
                put("restaurantId", CartManager.restaurantId)
                put("restaurantName", restaurantName)
                put("items", itemsArray)
                put("subtotal", CartManager.subtotal)
                put("deliveryFee", CartManager.deliveryFee)
                put("serviceFee", CartManager.serviceFee)
                put("tax", CartManager.tax)
                put("tip", tipAmount)
                put("total", totalWithTip)
                put("deliveryAddress", addressDict)
                put("leaveAtDoor", leaveAtDoor)
                put("paymentMethodId", selectedPayment.id)
                put("paymentType", if (selectedPayment.id == "gpay") "google_pay" else "saved_card")
            }

            println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            println("📦 [Checkout] ORDER PAYLOAD:")
            println("   Restaurant: $restaurantName (${CartManager.restaurantId})")
            println("   Items: ${CartManager.items.size}")
            println("   Total: $${String.format("%.2f", totalWithTip)}")
            println("   Payment: ${selectedPayment.type}")
            println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            val result = withContext(Dispatchers.IO) {
                val url = URL("${Config.API_BASE_URL}/orders")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                connection.outputStream.use { os ->
                    os.write(orderPayload.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                println("📦 [Checkout] POST /orders → HTTP $responseCode")

                if (responseCode == 201 || responseCode == 200) {
                    val responseBody = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(responseBody)
                    val orderId = json.optString("_id", "")
                    val orderNumber = json.optInt("orderNumber", 0)
                    // Save to CartManager — matches iOS CartManager.shared.orderId / orderNumber
                    CartManager.orderId = orderId
                    CartManager.orderNumber = orderNumber
                    println("✅ [Checkout] Order created! ID: $orderId, Number: $orderNumber")
                    "success"
                } else {
                    val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                    println("❌ [Checkout] Order creation failed (HTTP $responseCode): $errorBody")
                    "error: HTTP $responseCode"
                }
            }

            if (result != "success") {
                // Still proceed — don't block the user if order creation fails (matches iOS behavior)
                println("⚠️ [Checkout] Order creation had issues but proceeding anyway")
            }

            // Show success animation
            showOrderSuccess = true

        } catch (e: Exception) {
            println("❌ [Checkout] Failed to create order: ${e.message}")
            // Still proceed — don't block the user (matches iOS behavior)
            showOrderSuccess = true
        } finally {
            isPlacingOrder = false
        }
    }

    // Auto-transition to driver tracking after 1 second (matches iOS handlePlaceOrder)
    LaunchedEffect(showOrderSuccess) {
        if (showOrderSuccess) {
            delay(1000)
            // 1. Trigger driver tracking (MainActivity observes CartManager.showDriverTracking)
            CartManager.showDriverTracking = true
            // 2. Clear the cart (but NOT showDriverTracking!)
            CartManager.clear()
            // 3. Navigate back from Checkout
            onTrackOrder()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Checkout",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF2F2F7) // systemGroupedBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showTipPage) {
                // Show TipPage as full-screen overlay (matches iOS .fullScreenCover)
                TipPage(
                    onBack = { showTipPage = false },
                    onTipSelected = { tipAmount = it },
                    subtotal = CartManager.subtotal
                )
            } else {
                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 100.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Delivery / Pickup Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(50))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        listOf("Delivery", "Pickup").forEach { mode ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (selectedMode == mode) Color.Black else Color.Transparent,
                                        RoundedCornerShape(50)
                                    )
                                    .clickable { selectedMode = mode }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedMode == mode) Color.White else Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Delivery Address section
                    DeliveryAddressSection(
                        selectedAddress = selectedAddress,
                        onAddressTap = { showSelectAddress = true },
                        leaveAtDoor = leaveAtDoor,
                        onLeaveAtDoorChanged = { leaveAtDoor = it }
                    )

                    // Address selection bottom sheet (uses proper SelectAddressSheet from fooddelivery/)
                    if (showSelectAddress) {
                        SelectAddressSheet(
                            currentAddressId = selectedAddress.id,
                            onAddressSelected = { address ->
                                selectedAddress = DeliveryAddress(
                                    id = address.id,
                                    title = if (address.isDefault) "Home" else address.street,
                                    fullAddress = "${address.street}, ${address.cityStateZip}",
                                    instructions = address.gateCode ?: ""
                                )
                                showSelectAddress = false
                            },
                            onDismiss = { showSelectAddress = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Payment Method section
                    PaymentMethodSection(
                        paymentMethods = paymentMethods,
                        selectedPayment = selectedPayment,
                        onPaymentSelected = { selectedPayment = it },
                        onAddPayment = { showWallet = true }
                    )

                    // Wallet bottom sheet (full height)
                    if (showWallet) {
                        val walletSheetState = androidx.compose.material3.rememberModalBottomSheetState(
                            skipPartiallyExpanded = true
                        )
                        androidx.compose.material3.ModalBottomSheet(
                            onDismissRequest = { showWallet = false },
                            sheetState = walletSheetState,
                            containerColor = Color.White
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Wallet(onBack = { showWallet = false })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Order Items section
                    OrderItemsSection()

                    Spacer(modifier = Modifier.height(20.dp))

                    // Summary section
                    SummarySection(
                        tipAmount = tipAmount,
                        totalWithTip = totalWithTip
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tip section
                    TipSection(
                        tipAmount = tipAmount,
                        onTipSelected = { tipAmount = it },
                        onShowTipPage = { showTipPage = true }
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Floating Place Order button at bottom
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // Error message
                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            fontSize = 13.sp,
                            color = Color.Red,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }

                    Text(
                        text = if (isPlacingOrder) "Placing Order..." else "Place Order • $${String.format("%.2f", totalWithTip)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                when {
                                    showOrderSuccess -> Color(0xFF4CAF50)
                                    isPlacingOrder -> Color.Gray
                                    else -> Color(0xFFCC5500)
                                },
                                RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = !isPlacingOrder && !showOrderSuccess) {
                                scope.launch {
                                    handlePlaceOrder()
                                }
                            }
                            .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Success overlay — matches iOS OrderSuccessOverlay (shown for 1 second before driver tracking)
                if (showOrderSuccess) {
                    OrderSuccessOverlay()
                }
            }
        }
    }
}

// MARK: - Delivery Address Section (matches iOS deliverySection — shows only selected address)

@Composable
private fun DeliveryAddressSection(
    selectedAddress: DeliveryAddress,
    onAddressTap: () -> Unit,
    leaveAtDoor: Boolean,
    onLeaveAtDoorChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Delivery Address",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Show only the selected address card (matches iOS)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .clickable { onAddressTap() }
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFFCC5500),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = selectedAddress.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = selectedAddress.fullAddress,
                    fontSize = 15.sp,
                    color = Color.Gray
                )
                if (selectedAddress.instructions.isNotEmpty()) {
                    Text(
                        text = "Note: ${selectedAddress.instructions}",
                        fontSize = 12.sp,
                        color = Color(0xFF2196F3)
                    )
                }
            }

            // Blue "Change" button — matches iOS Button("Change").foregroundColor(.blue)
            TextButton(onClick = onAddressTap) {
                Text(
                    text = "Change",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF007AFF) // iOS system blue
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Leave at door toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Leave at door",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Switch(
                checked = leaveAtDoor,
                onCheckedChange = onLeaveAtDoorChanged,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Color(0xFFCC5500),
                    checkedThumbColor = Color.White
                )
            )
        }
    }
}

// MARK: - Payment Method Section (matches iOS paymentSection)

@Composable
private fun PaymentMethodSection(
    paymentMethods: List<PaymentMethod>,
    selectedPayment: PaymentMethod,
    onPaymentSelected: (PaymentMethod) -> Unit,
    onAddPayment: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Payment Method",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Show only the default/selected payment method
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedPayment.type,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                if (selectedPayment.last4 != null) {
                    Text(
                        text = "•••• ${selectedPayment.last4}",
                        fontSize = 15.sp,
                        color = Color.Gray
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFFCC5500),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // "Add another payment" → navigates to Wallet
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                .clickable { onAddPayment() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = null,
                tint = Color(0xFFCC5500),
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Add another payment",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCC5500),
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// MARK: - Order Items Section (matches iOS orderItemsSection)

@Composable
private fun OrderItemsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Your Order",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        CartManager.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Food image
                AsyncImage(
                    model = item.imageURL,
                    contentDescription = item.dishName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Gray.copy(alpha = 0.2f))
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Item info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.dishName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "${item.quantity} × $${String.format("%.2f", item.price)}",
                        fontSize = 15.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// MARK: - Tip Section (matches iOS tipSection — $4, $5, $10, Other)

private val PRESET_TIPS = listOf(4.0, 5.0, 10.0)

@Composable
private fun TipSection(
    tipAmount: Double,
    onTipSelected: (Double) -> Unit,
    onShowTipPage: () -> Unit = {}
) {
    val isPresetTip = tipAmount in PRESET_TIPS

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Tip Your Driver",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            (PRESET_TIPS + listOf(-1.0)).forEach { tip ->
                val isSelected = if (tip == -1.0) !isPresetTip else tipAmount == tip
                val label = if (tip == -1.0) "Other" else "$${tip.toInt()}"

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) Color(0xFFCC5500) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            if (tip == -1.0) {
                                onShowTipPage()
                            } else {
                                onTipSelected(tip)
                            }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color.Black
                    )
                }
            }
        }
    }
}

// MARK: - Summary Section (matches iOS summarySection)

@Composable
private fun SummarySection(
    tipAmount: Double,
    totalWithTip: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Order Summary",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        CheckoutPriceRow(title = "Subtotal", amount = CartManager.subtotal)
        Spacer(modifier = Modifier.height(8.dp))
        CheckoutPriceRow(title = "Delivery Fee", amount = CartManager.deliveryFee)
        Spacer(modifier = Modifier.height(8.dp))
        CheckoutPriceRow(title = "Service Fee", amount = CartManager.serviceFee)
        Spacer(modifier = Modifier.height(8.dp))
        CheckoutPriceRow(title = "Tax", amount = CartManager.tax)
        Spacer(modifier = Modifier.height(8.dp))
        CheckoutPriceRow(title = "Tip", amount = tipAmount)

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = Color.Gray.copy(alpha = 0.3f)
        )

        CheckoutPriceRow(title = "Total", amount = totalWithTip, isBold = true)
    }
}

// MARK: - Price Row (matches iOS PriceRow_CO)

@Composable
private fun CheckoutPriceRow(
    title: String,
    amount: Double,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = if (isBold) 20.sp else 17.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) Color.Black else Color.Gray
        )
        Text(
            text = "$${String.format("%.2f", amount)}",
            fontSize = if (isBold) 20.sp else 17.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) Color.Black else Color.Gray
        )
    }
}

// MARK: - Order Success Overlay (matches iOS OrderSuccessOverlay)
// Shown inline in Checkout for 1 second before auto-transitioning to driver tracking

@Composable
private fun OrderSuccessOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .background(
                    Color.Black.copy(alpha = 0.7f),
                    RoundedCornerShape(24.dp)
                )
                .padding(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50), // Green checkmark — matches iOS
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = "Order Placed!",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Finding your driver...",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f)
            )

            androidx.compose.material3.CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(32.dp),
                strokeWidth = 3.dp
            )
        }
    }
}

