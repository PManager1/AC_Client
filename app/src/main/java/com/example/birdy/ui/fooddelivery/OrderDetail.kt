package com.example.birdy.ui.fooddelivery

import android.location.Geocoder
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.birdy.data.Config
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale

// MARK: - Order Detail Screen — Matches iOS OrderDetail.swift
@Suppress("DEPRECATION")
@Composable
fun OrderDetailScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Map state
    var restaurantPoint by remember { mutableStateOf<Point?>(null) }
    var deliveryPoint by remember { mutableStateOf<Point?>(null) }
    var showPaymentDetails by remember { mutableStateOf(false) }
    var distanceText by remember { mutableStateOf("") }
    var routePoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var polylineAnnotationManager by remember { mutableStateOf<PolylineAnnotationManager?>(null) }

    // Sample food order data (static for now — will be wired to real order API)
    val orderNumber = "F-10412"
    val restaurantName = "Smash'd Burger"
    val orderDate = "May 6, 2026 • 1:32 PM"
    val orderTotal = 37.98
    val subtotal = 33.99
    val deliveryFee = 2.99
    val tax = 3.50
    val tip = 5.00
    val itemCount = 3
    val estimatedDelivery = "25-35 min"
    val restaurantAddress = "1234 M St NW, Washington, DC 20005"
    val deliveryAddress = "567 14th St SE, Washington, DC 20003"

    // Sample ordered items
    val orderedItems = remember {
        listOf(
            Triple("Smash Burger Double", 1, 14.99),
            Triple("Loaded Fries", 1, 8.99),
            Triple("Oreo Milkshake", 1, 7.99)
        )
    }

    // MARK: - Geocoding
    LaunchedEffect(Unit) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())

            val restResults = geocoder.getFromLocationName(restaurantAddress, 1)
            if (!restResults.isNullOrEmpty()) {
                restaurantPoint = Point.fromLngLat(restResults[0].longitude, restResults[0].latitude)
                Log.d("OrderDetail", "📍 Restaurant: ${restResults[0].latitude}, ${restResults[0].longitude}")
            }

            val delResults = geocoder.getFromLocationName(deliveryAddress, 1)
            if (!delResults.isNullOrEmpty()) {
                deliveryPoint = Point.fromLngLat(delResults[0].longitude, delResults[0].latitude)
                Log.d("OrderDetail", "📍 Delivery: ${delResults[0].latitude}, ${delResults[0].longitude}")
            }
        } catch (e: Exception) {
            Log.e("OrderDetail", "❌ Geocoding error: ${e.message}")
        }
    }

    // MARK: - Fetch Mapbox Directions route when both coordinates are ready
    LaunchedEffect(restaurantPoint, deliveryPoint) {
        val restPt = restaurantPoint ?: return@LaunchedEffect
        val delPt = deliveryPoint ?: return@LaunchedEffect

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val accessToken = Config.MAPBOX_ACCESS_TOKEN
                if (accessToken.isEmpty()) {
                    Log.w("OrderDetail", "⚠️ No Mapbox token — using straight line")
                    withContext(Dispatchers.Main) {
                        val dist = FloatArray(1)
                        android.location.Location.distanceBetween(
                            restPt.latitude(), restPt.longitude(),
                            delPt.latitude(), delPt.longitude(),
                            dist
                        )
                        distanceText = String.format("%.1f mi (straight line)", dist[0] / 1609.34)
                        routePoints = listOf(restPt, delPt)
                    }
                    return@launch
                }

                // Use Mapbox Directions API
                val url = "https://api.mapbox.com/directions/v5/mapbox/driving/" +
                        "${restPt.longitude()},${restPt.latitude()};" +
                        "${delPt.longitude()},${delPt.latitude()}?" +
                        "overview=full&geometries=geojson&access_token=$accessToken"

                val response = URL(url).readText()
                val json = JSONObject(response)

                if (json.getJSONArray("routes").length() > 0) {
                    val route = json.getJSONArray("routes").getJSONObject(0)
                    val distanceMeters = route.getDouble("distance")
                    val durationSeconds = route.getDouble("duration")
                    val geometry = route.getJSONObject("geometry")
                    val coords = geometry.getJSONArray("coordinates")

                    val distanceMiles = distanceMeters / 1609.34
                    val durationMinutes = (durationSeconds / 60).toInt()

                    val decoded = mutableListOf<Point>()
                    for (i in 0 until coords.length()) {
                        val coord = coords.getJSONArray(i)
                        decoded.add(Point.fromLngLat(coord.getDouble(0), coord.getDouble(1)))
                    }

                    withContext(Dispatchers.Main) {
                        distanceText = String.format("%.1f mi • ~%d min", distanceMiles, durationMinutes)
                        routePoints = decoded
                    }

                    Log.d("OrderDetail", "✅ Mapbox route: ${String.format("%.1f", distanceMiles)} mi, ~$durationMinutes min, ${decoded.size} points")
                } else {
                    Log.w("OrderDetail", "⚠️ No routes found")
                    val dist = FloatArray(1)
                    android.location.Location.distanceBetween(
                        restPt.latitude(), restPt.longitude(),
                        delPt.latitude(), delPt.longitude(),
                        dist
                    )
                    withContext(Dispatchers.Main) {
                        distanceText = String.format("%.1f mi (straight line)", dist[0] / 1609.34)
                        routePoints = listOf(restPt, delPt)
                    }
                }
            } catch (e: Exception) {
                Log.e("OrderDetail", "❌ Directions API error: ${e.message}")
            }
        }
    }

    // MARK: - Draw route & markers on map when data is ready
    LaunchedEffect(mapViewRef, restaurantPoint, deliveryPoint, routePoints) {
        val mapView = mapViewRef ?: return@LaunchedEffect
        val restPt = restaurantPoint ?: return@LaunchedEffect
        val delPt = deliveryPoint ?: return@LaunchedEffect
        val ptMgr = pointAnnotationManager ?: return@LaunchedEffect
        val lineMgr = polylineAnnotationManager ?: return@LaunchedEffect

        @Suppress("DEPRECATION")
        val mapboxMap = mapView.getMapboxMap()

        // Clear previous annotations
        ptMgr.deleteAll()
        lineMgr.deleteAll()

        // Restaurant pin (green) — simple point marker, same as StoreInfo.kt
        val pickupOptions = PointAnnotationOptions()
            .withPoint(restPt)
        ptMgr.create(pickupOptions)

        // Delivery pin (red) — simple point marker
        val deliveryOptions = PointAnnotationOptions()
            .withPoint(delPt)
        ptMgr.create(deliveryOptions)

        // Draw route line
        if (routePoints.size >= 2) {
            val lineString = LineString.fromLngLats(routePoints)
            val lineOptions = PolylineAnnotationOptions()
                .withGeometry(lineString)
                .withLineColor("#FF9800") // Orange — matches iOS
                .withLineWidth(6.0)
            lineMgr.create(lineOptions)
            Log.d("OrderDetail", "🟠 Route line drawn with ${routePoints.size} points")
        }

        // Camera: fit to show both pins
        val latList = listOf(restPt.latitude(), delPt.latitude())
        val lngList = listOf(restPt.longitude(), delPt.longitude())
        val minLat = latList.min()
        val maxLat = latList.max()
        val minLng = lngList.min()
        val maxLng = lngList.max()

        val padding = 0.02 // degrees (~2km padding)
        val centerLat = (minLat + maxLat) / 2
        val centerLng = (minLng + maxLng) / 2
        val latSpan = maxLat - minLat + padding * 2
        val zoom = when {
            latSpan > 0.5 -> 10.0
            latSpan > 0.1 -> 12.0
            latSpan > 0.05 -> 13.0
            else -> 14.0
        }

        mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(centerLng, centerLat))
                .zoom(zoom)
                .build()
        )
    }

    // MARK: - Layout — single scrollable Column matching StoreInfo.kt structure
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Black
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Order Details",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Black
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { /* Help */ }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = "Help",
                    tint = Black
                )
            }
        }

        HorizontalDivider()

        // Content (no nested weight column — directly in the scrollable column)
        Column {
            // MARK: - Order Info and Price
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Order $orderNumber • $restaurantName",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray
                )
                Text(
                    text = "$${String.format("%.2f", orderTotal)}",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
                Text(
                    text = orderDate,
                    fontSize = 15.sp,
                    color = Gray
                )
            }

            // MARK: - Status Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF5F5F5)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = Green,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Order placed — preparing your food!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // MARK: - Mapbox Map — INLINED exactly like StoreInfo.kt (no separate composable)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AndroidView(
                    factory = { factoryContext ->
                        MapView(factoryContext, MapInitOptions(
                            context = factoryContext
                        )).also { mapView ->
                            val mapboxMap = mapView.getMapboxMap()
                            mapboxMap.setCamera(
                                CameraOptions.Builder()
                                    .center(Point.fromLngLat(-77.0369, 38.9072))
                                    .zoom(12.0)
                                    .build()
                            )
                            mapboxMap.loadStyle("mapbox://styles/mapbox/streets-v12") { style ->
                                Log.d("OrderDetail", "🗺️ Mapbox style loaded")
                                val annotationPlugin = mapView.annotations
                                val ptMgr = annotationPlugin.createPointAnnotationManager()
                                val lineMgr = annotationPlugin.createPolylineAnnotationManager()
                                // Store refs so LaunchedEffect can draw markers/route later
                                mapViewRef = mapView
                                pointAnnotationManager = ptMgr
                                polylineAnnotationManager = lineMgr
                            }
                            mapView.gestures.updateSettings {
                                scrollEnabled = false
                                pinchToZoomEnabled = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // MARK: - Order Metrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    Text(
                        text = "Est. Delivery",
                        fontSize = 15.sp,
                        color = Gray
                    )
                    Text(
                        text = "$estimatedDelivery${if (distanceText.isNotEmpty()) " • $distanceText" else ""}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Black
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Items",
                        fontSize = 15.sp,
                        color = Gray
                    )
                    Text(
                        text = "$itemCount items",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MARK: - Pickup / Delivery Addresses
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pickup
                Row(verticalAlignment = Alignment.Top) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Green,
                            modifier = Modifier.size(12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(Gray.copy(alpha = 0.4f))
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pickup",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Gray
                        )
                        Text(
                            text = restaurantAddress,
                            fontSize = 15.sp,
                            color = Black
                        )
                    }
                }

                // Delivery
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Red,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Deliver to",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Gray
                        )
                        Text(
                            text = deliveryAddress,
                            fontSize = 15.sp,
                            color = Black
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // MARK: - Ordered Items
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Order Summary",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )

                orderedItems.forEach { (name, quantity, price) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${quantity}x",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Gray,
                            modifier = Modifier.width(30.dp)
                        )
                        Text(
                            text = name,
                            fontSize = 15.sp,
                            color = Black,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$${String.format("%.2f", price)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Black
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // MARK: - Payment Breakdown
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPaymentDetails = !showPaymentDetails },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Payment Breakdown",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (showPaymentDetails) "▲" else "▼",
                        fontSize = 12.sp,
                        color = Gray
                    )
                }

                PaymentRow("Subtotal", String.format("%.2f", subtotal))
                PaymentRow("Delivery Fee", String.format("%.2f", deliveryFee))
                PaymentRow("Tax", String.format("%.2f", tax))
                PaymentRow("Tip", String.format("%.2f", tip), textColor = Green)

                HorizontalDivider()

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Total",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$${String.format("%.2f", orderTotal)}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                }

                if (showPaymentDetails) {
                    HorizontalDivider()
                    Text(
                        text = "Payment Method",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = Color(0xFF2196F3)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Visa •••• 4242",
                            fontSize = 15.sp,
                            color = Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// MARK: - Payment Row Helper
@Composable
private fun PaymentRow(label: String, amount: String, textColor: androidx.compose.ui.graphics.Color = Black) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = if (textColor == Green) Green else Black,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$$amount",
            fontSize = 15.sp,
            color = textColor
        )
    }
}


