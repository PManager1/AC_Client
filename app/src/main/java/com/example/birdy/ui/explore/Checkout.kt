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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalConfiguration
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
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.example.birdy.ui.fooddelivery.SelectAddressSheet
import com.example.birdy.ui.fooddelivery.OutOfZoneSheet
import com.example.birdy.data.ServiceAreaException
import com.example.birdy.data.ServiceAreaService
import com.example.birdy.data.ZoneCheckResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.derivedStateOf
import com.example.birdy.data.LocationManager
import org.json.JSONArray
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import java.security.MessageDigest
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// MARK: - Mock Data Models (matches iOS Checkout.swift)

data class DeliveryAddress(
    val id: String,
    val title: String,
    val fullAddress: String,
    val instructions: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

// MARK: - Checkout Screen (matches iOS Checkout view)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onTrackOrder: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedAddress by remember { mutableStateOf<DeliveryAddress?>(null) }
    var isLoadingAddresses by remember { mutableStateOf(true) }
    var showSelectAddress by remember { mutableStateOf(false) }
    var tipAmount by remember { mutableStateOf(4.0) }
    var leaveAtDoor by remember { mutableStateOf(true) }
    var showOrderSuccess by remember { mutableStateOf(false) }
    var isPlacingOrder by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var zoneResult by remember { mutableStateOf<ZoneCheckResult?>(null) }
    var showTipPage by remember { mutableStateOf(false) }
    // The order waiting for its card hold to be confirmed in PaymentSheet.
    var pendingOrder by remember { mutableStateOf<JSONObject?>(null) }
    var holdNotice by remember { mutableStateOf<String?>(null) }
    // New per checkout screen, so the same basket ordered again later is a new hold.
    val idempotencySalt = remember { java.util.UUID.randomUUID().toString() }
    var selectedMode by remember { mutableStateOf("Delivery") }
    var userLat by remember { mutableStateOf(0.0) }
    var userLng by remember { mutableStateOf(0.0) }
    var hasUserLocation by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var competitorEstimate by remember { mutableStateOf<Double?>(null) }
    var isLoadingCompetitorEstimate by remember { mutableStateOf(true) }
    var showCompetitorDetails by remember { mutableStateOf(false) }

    // The server's total for this order (card hold response); shown once known.
    var serverTotal by remember { mutableStateOf<Double?>(null) }
    // A changed tip or mode is priced again on the next hold.
    LaunchedEffect(tipAmount, selectedMode) { serverTotal = null }

    val totalWithTip = serverTotal ?: (CartManager.total + tipAmount)

    val displayCompetitorEstimate = competitorEstimate ?: 0.0

    val isUserFarFromAddress by remember(selectedAddress, userLat, userLng, hasUserLocation) {
        derivedStateOf {
            val addr = selectedAddress ?: return@derivedStateOf false
            if (!hasUserLocation || addr.latitude == 0.0 || addr.longitude == 0.0) return@derivedStateOf false
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                userLat, userLng,
                addr.latitude, addr.longitude,
                results
            )
            val distanceMiles = results[0] / 1609.344f
            println("📍 [Checkout] Distance to address: ${String.format("%.4f", distanceMiles)} mi")
            distanceMiles > 0.2f
        }
    }

    // MARK: - Load Addresses from Backend (matches iOS loadAddresses)
    LaunchedEffect(Unit) {
        val token = AuthManager.getToken(context)
        if (token.isNullOrEmpty()) {
            isLoadingAddresses = false
            return@LaunchedEffect
        }
        try {
            val loadedAddresses = withContext(Dispatchers.IO) {
                AddressService.getAddresses(token)
            }
            if (loadedAddresses.isNotEmpty()) {
                val defaultAddr = loadedAddresses.firstOrNull { it.isDefault }
                    ?: loadedAddresses.first()
                selectedAddress = DeliveryAddress(
                    id = defaultAddr.id,
                    title = if (defaultAddr.isDefault) "Home" else defaultAddr.street,
                    fullAddress = "${defaultAddr.street}, ${defaultAddr.cityStateZip}",
                    instructions = defaultAddr.gateCode ?: "",
                    latitude = defaultAddr.latitude,
                    longitude = defaultAddr.longitude
                )
                println("✅ [Checkout] Auto-selected address: ${defaultAddr.street}")
            }
        } catch (e: Exception) {
            println("❌ [Checkout] Failed to load addresses: ${e.message}")
        }
        isLoadingAddresses = false
    }

    // MARK: - Fetch user location for distance validation
    LaunchedEffect(Unit) {
        try {
            val (lat, lng) = withContext(Dispatchers.IO) {
                LocationManager.fetchLocation(context)
            }
            if (lat != 0.0 && lng != 0.0) {
                userLat = lat
                userLng = lng
                hasUserLocation = true
                println("📍 [Checkout] User location: ($lat, $lng)")
            } else {
                println("⚠️ [Checkout] No user location available")
            }
        } catch (e: Exception) {
            println("❌ [Checkout] Failed to fetch location: ${e.message}")
        }
    }

    // MARK: - Fetch Competitor Estimate from Backend
    LaunchedEffect(Unit) {
        val token = AuthManager.getToken(context)
        if (!token.isNullOrEmpty()) {
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = URL("${Config.API_BASE_URL}/orders/competitor-estimate?subtotal=${CartManager.subtotal}")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        val body = connection.inputStream.bufferedReader().readText()
                        val json = JSONObject(body)
                        json.optDouble("competitorEstimate", -1.0)
                    } else -1.0
                }
                if (result > 0) {
                    competitorEstimate = result
                }
            } catch (e: Exception) {
                println("⚠️ [Checkout] Failed to fetch competitor estimate: ${e.message}")
            }
        }
        isLoadingCompetitorEstimate = false
    }

    // MARK: - Place Order (same flow as iOS handlePlaceOrder)
    //
    // 1. POST /payments/intents with the order: the server prices it and holds the card
    //    for total + buffer (manual capture). 2. PaymentSheet confirms the card (3-D Secure
    //    inside the sheet). 3. POST /orders with paymentIntentId. Only a 201 counts as
    //    placed; if the server refuses, it releases the hold right away.

    // Idempotency-Key for POST /payments/intents: the same order contents on this screen
    // give the same key, so a retry after a timeout gets the same card hold back. Any change
    // to items, tip, fees, address or total gives a new key (Stripe refuses a reused key
    // with different params).
    fun idempotencyKey(addressId: String): String {
        val items = CartManager.items.joinToString(";") { item ->
            listOf(
                item.menuItem?.id ?: item.dishName, String.format("%.2f", item.price), "${item.quantity}",
                item.selectedOptions.joinToString(","), item.specialInstructions
            ).joinToString("|")
        }
        val parts = listOf(
            idempotencySalt, CartManager.restaurantId, items, addressId,
            String.format("%.2f|%.2f|%.2f|%.2f", tipAmount, CartManager.deliveryFee, CartManager.serviceFee, totalWithTip)
        )
        val digest = MessageDigest.getInstance("SHA-256").digest(parts.joinToString("\n").toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    // POST with JSON; returns (HTTP status, body). Network errors throw.
    fun postJson(path: String, body: JSONObject, token: String, headers: Map<String, String> = emptyMap()): Pair<Int, String> {
        val connection = URL("${Config.API_BASE_URL}$path").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json")
        headers.forEach { (field, value) -> connection.setRequestProperty(field, value) }
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.doOutput = true
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val status = connection.responseCode
        val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.readText() ?: ""
        return status to text
    }

    fun serverMessage(body: String): String =
        try { JSONObject(body).optString("message", "") } catch (_: Exception) { "" }

    // 409 cart_changed / items_unavailable, 404 cart_not_found: the saved cart moved on
    // (another device, a price edit, an item the store stopped selling). Back to the
    // cart, refreshed, with the server's message.
    fun cartChanged(status: Int, body: String): Boolean {
        val code = try { JSONObject(body).optString("error", "") } catch (_: Exception) { "" }
        if ((status != 409 && status != 404) || code !in setOf("cart_changed", "items_unavailable", "cart_not_found")) return false
        serverTotal = null
        pendingOrder = null
        isPlacingOrder = false
        CartManager.refresh()
        CartManager.cartMessage = serverMessage(body).ifEmpty { "Your cart changed. Please review it and check out again." }
        onBack()
        return true
    }

    // Step 3: the card is held; place the order. A retry with the same intent returns the
    // same order, never a second one.
    suspend fun submitOrder(order: JSONObject, token: String) {
        try {
            var result: Pair<Int, String>? = null
            for (attempt in 1..3) {
                result = try {
                    withContext(Dispatchers.IO) { postJson("/orders", order, token) }
                } catch (e: java.io.IOException) {
                    if (attempt == 3) throw e
                    null
                }
                val status = result?.first ?: 0
                if (status in 200..499) break
                delay(1000L * attempt)
            }
            val (status, body) = result ?: (0 to "")
            println("📦 [Checkout] POST /orders → HTTP $status")

            if (status == 201 || status == 200) {
                val json = JSONObject(body)
                // udo3 returns {"id": "<uuid>", "orderNumber": "UDO-XXXXXX", ...}
                CartManager.orderId = json.optString("id", "").ifEmpty { json.optString("_id", "") }
                CartManager.orderNumber = json.optString("orderNumber", "")
                println("✅ [Checkout] Order created! ID: ${CartManager.orderId}")
                pendingOrder = null
                // Only a created order shows success and clears the cart.
                showOrderSuccess = true
            } else if (cartChanged(status, body)) {
                println("🛒 [Checkout] Cart changed before the order (HTTP $status)")
            } else {
                println("❌ [Checkout] Order creation failed (HTTP $status): $body")
                // Not served / service paused: no order was created, so never show success
                ServiceAreaException.parse(status, body)?.let { throw it }
                errorMessage = serverMessage(body)
                    .ifEmpty { "We couldn't place your order (HTTP $status). Your card hold was released." }
            }
        } catch (e: ServiceAreaException) {
            println("📍 [Checkout] Order blocked: ${e.code}")
            // Prefer the full "not here yet" sheet; fall back to the server message
            val zone = selectedAddress?.id?.let { id ->
                try {
                    withContext(Dispatchers.IO) { ServiceAreaService.checkSavedAddress(id, token) }
                } catch (_: Exception) {
                    null
                }
            }
            if (zone != null && !zone.inZone && !zone.couldNotVerify) {
                zoneResult = zone
            } else {
                errorMessage = e.message ?: "We don't deliver to this address yet."
            }
        } catch (e: Exception) {
            println("❌ [Checkout] Failed to create order: ${e.message}")
            // No confirmed order: keep the cart so the user can try again.
            errorMessage = "We couldn't reach the server, so your order wasn't placed. Please try again."
        } finally {
            isPlacingOrder = false
        }
    }

    val paymentSheet = rememberPaymentSheet { result ->
        val order = pendingOrder
        when (result) {
            is PaymentSheetResult.Completed -> {
                val token = AuthManager.getToken(context)
                if (order == null || token.isNullOrEmpty()) {
                    isPlacingOrder = false
                    errorMessage = "Your session expired, so the order wasn't placed. Please log in and try again."
                } else {
                    scope.launch { submitOrder(order, token) }
                }
            }
            // Nothing was placed; the cart stays as it is.
            is PaymentSheetResult.Canceled -> {
                isPlacingOrder = false
                serverTotal = null
            }
            is PaymentSheetResult.Failed -> {
                isPlacingOrder = false
                errorMessage = result.error.localizedMessage ?: "Your card couldn't be confirmed. Please try again."
            }
        }
    }

    suspend fun handlePlaceOrder() {
        val token = AuthManager.getToken(context)
        if (token.isNullOrEmpty()) {
            errorMessage = "Not authenticated — please log in again."
            return
        }

        val addr = selectedAddress
        if (addr == null) {
            errorMessage = "Please select a delivery address."
            return
        }

        isPlacingOrder = true
        errorMessage = ""

        // Build the order payload — matches iOS Checkout.swift orderPayload
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
        val addressId = if (addr.id.isNotEmpty() && addr.id != "current_location") addr.id else ""
        val orderPayload = JSONObject().apply {
            put("restaurantId", CartManager.restaurantId)
            put("restaurantName", CartManager.items.firstOrNull()?.restaurantName ?: "Unknown Restaurant")
            put("items", itemsArray)
            put("subtotal", CartManager.subtotal)
            put("deliveryFee", CartManager.deliveryFee)
            put("serviceFee", CartManager.serviceFee)
            put("tax", CartManager.tax)
            put("tip", tipAmount)
            put("total", totalWithTip)
            put("deliveryAddress", JSONObject().apply {
                put("street", addr.fullAddress)
                put("cityStateZip", "")
                put("isDefault", addr.id == "home")
            })
            put("leaveAtDoor", leaveAtDoor)
            // The server checks the service area against this saved address
            if (addressId.isNotEmpty()) put("addressId", addressId)
            // The server prices fees itself and its total is what's shown and charged.
            put("serverPricing", true)
            put("deliveryMode", if (selectedMode == "Pickup") "pickup" else "delivery")
            // A grocery store's saved cart: the server prices exactly these lines from it.
            val cartId = CartManager.serverCartId
            if (CartManager.isSavedCart && cartId != null) {
                put("cartId", cartId)
                put("cartItemIds", JSONArray(CartManager.items.mapNotNull { it.serverLineId }))
            }
        }

        try {
            // Step 1: price on the server and hold the card.
            val (status, body) = withContext(Dispatchers.IO) {
                postJson("/payments/intents", orderPayload, token, mapOf("Idempotency-Key" to idempotencyKey(addressId)))
            }
            val json = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
            val intentId = json.optString("paymentIntentId", "")
            val clientSecret = json.optString("clientSecret", "")
            val publishableKey = json.optString("publishableKey", "")
            if (cartChanged(status, body)) return
            if (status != 200 || intentId.isEmpty() || clientSecret.isEmpty() || publishableKey.isEmpty()) {
                isPlacingOrder = false
                errorMessage = serverMessage(body).ifEmpty { "Payments are unavailable right now. Please try again shortly." }
                return
            }

            // Step 2: confirm the card in Stripe's sheet; the result callback places the order.
            // The server's key, so the sheet always matches the account that made the hold.
            PaymentConfiguration.init(context, publishableKey)
            pendingOrder = JSONObject(orderPayload.toString()).put("paymentIntentId", intentId)
            json.optString("total").toDoubleOrNull()?.let { serverTotal = it }
            holdNotice = json.optString("message").ifEmpty { null }
            val config = PaymentSheet.Configuration.Builder("U-DO")
                .allowsDelayedPaymentMethods(false)
                .apply { json.optString("holdAmount").takeIf { it.isNotEmpty() }?.let { primaryButtonLabel("Hold $$it") } }
                .build()
            paymentSheet.presentWithPaymentIntent(clientSecret, config)
        } catch (e: Exception) {
            println("❌ [Checkout] Couldn't start payment: ${e.message}")
            isPlacingOrder = false
            errorMessage = "We couldn't reach the server, so your order wasn't placed. Please try again."
        }
    }

    // Auto-transition to driver tracking after 1 second (matches iOS handlePlaceOrder)
    LaunchedEffect(showOrderSuccess) {
        if (showOrderSuccess) {
            delay(1000)
            // 1. Trigger driver tracking (MainActivity observes CartManager.showDriverTracking)
            CartManager.showDriverTracking = true
            // 2. A saved cart already lost the ordered lines on the server: re-read it.
            //    The phone's cart is emptied (but NOT showDriverTracking!)
            CartManager.afterOrderPlaced()
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
                    if (isLoadingAddresses) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Loading address...",
                                fontSize = 15.sp,
                                color = Color.Gray
                            )
                        }
                    } else if (selectedAddress != null) {
                        DeliveryAddressSection(
                            selectedAddress = selectedAddress!!,
                            onAddressTap = { showSelectAddress = true },
                            leaveAtDoor = leaveAtDoor,
                            onLeaveAtDoorChanged = { leaveAtDoor = it },
                            isUserFarFromAddress = isUserFarFromAddress
                        )
                    } else {
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(16.dp))
                                    .clickable { showSelectAddress = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFCC5500),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Select a delivery address",
                                    fontSize = 15.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Address selection bottom sheet
                    if (showSelectAddress) {
                        SelectAddressSheet(
                            currentAddressId = selectedAddress?.id,
                            onAddressSelected = { address ->
                                selectedAddress = DeliveryAddress(
                                    id = address.id,
                                    title = if (address.isDefault) "Home" else address.street,
                                    fullAddress = "${address.street}, ${address.cityStateZip}",
                                    instructions = address.gateCode ?: "",
                                    latitude = address.latitude,
                                    longitude = address.longitude
                                )
                                showSelectAddress = false
                            },
                            onDismiss = { showSelectAddress = false }
                        )
                    }

                    // Delivery address isn't served: same sheet as the web modal and iOS
                    zoneResult?.let { result ->
                        OutOfZoneSheet(
                            result = result,
                            onTryAnother = {
                                zoneResult = null
                                showSelectAddress = true
                            },
                            onDone = { zoneResult = null }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Payment: Stripe's sheet collects and confirms the card on "Place Order"
                    PaymentMethodSection(holdNotice = holdNotice)

                    // Competitor details bottom sheet
                    if (showCompetitorDetails) {
                        val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
                            skipPartiallyExpanded = true
                        )
                        androidx.compose.material3.ModalBottomSheet(
                            onDismissRequest = { showCompetitorDetails = false },
                            sheetState = sheetState,
                            containerColor = Color.White
                        ) {
                            CompetitorDetailsView(
                                subtotal = CartManager.subtotal,
                                competitorEstimate = displayCompetitorEstimate,
                                onDismiss = { showCompetitorDetails = false }
                            )
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

                    // Competitor Estimate section
                    CompetitorSection(
                        competitorEstimate = displayCompetitorEstimate,
                        isLoading = isLoadingCompetitorEstimate,
                        cartTotal = CartManager.total,
                        onShowDetails = { showCompetitorDetails = true }
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
                                val check = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                                if (check != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    showLocationDialog = true
                                    return@clickable
                                }
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

                // Location permission dialog
                if (showLocationDialog) {
                    AlertDialog(
                        onDismissRequest = { showLocationDialog = false },
                        title = { Text("Location Access Required") },
                        text = { Text("Please provide access to your current location in Settings to ensure accurate delivery routing.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showLocationDialog = false
                                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }) {
                                Text("Settings")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLocationDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
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
    onLeaveAtDoorChanged: (Boolean) -> Unit,
    isUserFarFromAddress: Boolean = false
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

        if (selectedAddress.latitude != 0.0 && selectedAddress.longitude != 0.0) {
            Spacer(modifier = Modifier.height(8.dp))
            val mapHeight = LocalConfiguration.current.screenHeightDp.dp * 0.09f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mapHeight)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AndroidView(
                    factory = { factoryContext ->
                        MapView(factoryContext, MapInitOptions(factoryContext)).also { mapView ->
                            val mapboxMap = mapView.mapboxMap
                            val deliveryPoint = Point.fromLngLat(selectedAddress.longitude, selectedAddress.latitude)
                            mapboxMap.setCamera(
                                CameraOptions.Builder()
                                    .center(deliveryPoint)
                                    .zoom(14.0)
                                    .build()
                            )
                            mapboxMap.loadStyle("mapbox://styles/mapbox/streets-v12") { style ->
                                val annotationPlugin = mapView.annotations
                                val pointAnnotationManager = annotationPlugin.createPointAnnotationManager()
                                val annotationOptions = PointAnnotationOptions()
                                    .withPoint(deliveryPoint)
                                pointAnnotationManager.create(annotationOptions)
                            }
                            mapView.gestures.updateSettings {
                                scrollEnabled = false
                                pinchToZoomEnabled = false
                                rotateEnabled = false
                                pitchEnabled = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

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

        if (isUserFarFromAddress) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Yellow.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "You seem far away from this address. Please double-check your delivery location before ordering!",
                    fontSize = 13.sp,
                    color = Color.Black
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

// MARK: - Payment Method Section
//
// No card picker here: PaymentSheet collects and confirms the card when the user places
// the order, so what's shown is only how the hold works.

@Composable
private fun PaymentMethodSection(holdNotice: String?) {
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
                    text = "Card",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = holdNotice
                        ?: "You'll enter your card securely when you place the order. We hold a little over the total and only charge for what we deliver.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
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
        CheckoutPriceRow(title = "Delivery Fee (estimate)", amount = CartManager.deliveryFee)
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

// MARK: - Competitor Estimate Section (matches iOS competitorSection)

@Composable
private fun CompetitorSection(
    competitorEstimate: Double,
    isLoading: Boolean,
    cartTotal: Double,
    onShowDetails: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Competitor Estimate*",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color(0xFFCC5500),
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CheckoutPriceRow(title = "Competitor Estimate", amount = competitorEstimate)

                val savings = competitorEstimate - cartTotal
                Text(
                    text = "You saved $${String.format("%.2f", savings)} by using U-DO!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Row(
                    modifier = Modifier
                        .clickable { onShowDetails() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Show me details",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFCC5500)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFCC5500),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "*Estimates are calculated based on standard corporate platform markups (15% average item inflation + 10% service fee + 8% tax). Actual competitor checkout prices may vary based on location and promotion status.",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

// MARK: - Price Row (matches iOS PriceRow_CO)

@Composable
fun CheckoutPriceRow(
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

