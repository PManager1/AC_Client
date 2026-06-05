package com.example.birdy.ui.store

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import android.content.Intent
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.core.net.toUri
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// MARK: - Color Constants
private val BurntOrange = Color(0xFFCC5500)

// MARK: - Store Info Full-Screen Sheet (matches iOS StoreInfo)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoreInfo(
    data: StoreData,
    onDismiss: () -> Unit
) {
    var isHoursExpanded by remember { mutableStateOf(false) }
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val today = remember {
        SimpleDateFormat("EEEE", Locale.getDefault()).format(Calendar.getInstance().time)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. CLOSE BUTTON + RESTAURANT NAME — centered name with close button overlaid (matches iOS ZStack)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 26.dp, bottom = 12.dp)
        ) {
            // Restaurant Name — perfectly centered on screen (matches iOS)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = data.brand_info.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    maxLines = 1
                )
                Text(
                    text = data.brand_info.cuisine,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Gray
                )
            }

            // Back Button — pinned to left, aligned with restaurant name
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFF2F2F7), CircleShape)
                    .align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. MAP VIEW — static Mapbox map with restaurant pin
        var mapLat by remember { mutableStateOf(data.location_info.latitude) }
        var mapLng by remember { mutableStateOf(data.location_info.longitude) }
        val context = LocalContext.current

        // Geocode address if lat/lng are missing
        LaunchedEffect(data.location_info.address) {
            if (mapLat == 0.0 || mapLng == 0.0) {
                try {
                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                    val results = geocoder.getFromLocationName(data.location_info.address, 1)
                    if (!results.isNullOrEmpty()) {
                        mapLat = results[0].latitude
                        mapLng = results[0].longitude
                        android.util.Log.d("StoreInfo", "📍 Geocoded: ${data.location_info.address} → lat=$mapLat, lng=$mapLng")
                    } else {
                        android.util.Log.w("StoreInfo", "⚠️ Geocoder returned no results for: ${data.location_info.address}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("StoreInfo", "❌ Geocoding error: ${e.message}")
                }
            }
        }

        android.util.Log.d("StoreInfo", "📍 Map check: lat=$mapLat, lng=$mapLng")
        if (mapLat != 0.0 && mapLng != 0.0) {
            android.util.Log.d("StoreInfo", "✅ Creating map with lat=$mapLat, lng=$mapLng")
            val restaurantPoint = Point.fromLngLat(mapLng, mapLat)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AndroidView(
                    factory = { factoryContext ->
                        MapView(factoryContext, MapInitOptions(
                            context = factoryContext
                        )).also { mapView ->
                            val mapboxMap = mapView.getMapboxMap()
                            mapboxMap.setCamera(
                                CameraOptions.Builder()
                                    .center(restaurantPoint)
                                    .zoom(15.0)
                                    .build()
                            )
                            android.util.Log.d("StoreInfo", "🗺️ About to load Mapbox style...")
                            mapboxMap.loadStyle("mapbox://styles/mapbox/streets-v12") { style ->
                                android.util.Log.d("StoreInfo", "✅ Mapbox style loaded successfully!")
                                val annotationPlugin = mapView.annotations
                                val pointAnnotationManager = annotationPlugin.createPointAnnotationManager()
                                val annotationOptions = PointAnnotationOptions()
                                    .withPoint(restaurantPoint)
                                pointAnnotationManager.create(annotationOptions)
                            }
                            android.util.Log.d("StoreInfo", "⚠️ Style load callback not yet called — map view created")
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

            Spacer(modifier = Modifier.height(20.dp))
        }

        // 3. ADDRESS SECTION (matches iOS ModernSection)
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Address",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = BurntOrange
                )
                Text(
                    text = data.location_info.address,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // PHONE SECTION (matches iOS — shown between Address and Hours)
        if (!data.location_info.phone.isNullOrEmpty()) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Phone",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = data.location_info.phone,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black,
                        modifier = Modifier
                            .weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL, "tel:${data.location_info.phone}".toUri())
                                context.startActivity(intent)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Call",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // 4. HOURS SECTION (matches iOS — delivery time card + expandable full schedule)
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Hours",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            // Always visible delivery time card (matches iOS .ultraThinMaterial style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F2F7), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.Gray
                )
                Text(
                    text = data.location_info.delivery_time_est,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // Expandable "Full Schedule" toggle (matches iOS)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isHoursExpanded = !isHoursExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Full Schedule",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = if (isHoursExpanded) "▲" else "▼",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }

            // Expandable hours list (matches iOS spring animation + HourRow)
            AnimatedVisibility(
                visible = isHoursExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .background(Color(0xFFF2F2F7), RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    val hours = data.location_info.operating_hours
                    if (hours != null && hours.isNotEmpty()) {
                        daysOfWeek.forEachIndexed { index, day ->
                            val time = hours[day] ?: "Closed"
                            val isToday = day == today
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = day,
                                    fontSize = 15.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isToday) BurntOrange else Color.Black
                                )
                                Text(
                                    text = time,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (isToday) BurntOrange else Color.Gray
                                )
                            }
                            if (index < daysOfWeek.lastIndex) {
                                // Thin divider between days
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(Color.LightGray.copy(alpha = 0.5f))
                                )
                            }
                        }
                    } else {
                        // Placeholder hours
                        daysOfWeek.forEachIndexed { index, day ->
                            val isToday = day == today
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = day,
                                    fontSize = 15.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isToday) BurntOrange else Color.Black
                                )
                                Text(
                                    text = "10:00 AM - 10:00 PM",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (isToday) BurntOrange else Color.Gray
                                )
                            }
                            if (index < daysOfWeek.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(Color.LightGray.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 5. RATING SECTION (matches iOS ModernSection)
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Rating",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stars
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (i <= data.brand_info.rating.roundToInt()) Color(0xFFFFA000) else Color.LightGray
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = String.format("%.1f", data.brand_info.rating),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                    Text(
                        text = "(${data.brand_info.review_count} reviews)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 6. DELIVERY INFO — ModernInfoCard style (matches iOS)
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Delivery Fee Card — contextual based on delivery_enabled
            val deliveryEnabled = data.location_info.delivery_enabled
            val deliveryCardTitle = when {
                deliveryEnabled == true && data.location_info.delivery_fee == 0.0 -> "Free Delivery"
                deliveryEnabled == true -> "$${String.format("%.2f", data.location_info.delivery_fee)}"
                deliveryEnabled == false -> "Unavailable"
                else -> if (data.location_info.delivery_fee == 0.0) "Free Delivery"
                        else "$${String.format("%.2f", data.location_info.delivery_fee)}"
            }
            val deliveryCardSubtitle = when {
                deliveryEnabled == true -> "delivery available"
                deliveryEnabled == false -> "delivery disabled"
                else -> "delivery fee"
            }
            ModernInfoCard(
                title = deliveryCardTitle,
                subtitle = deliveryCardSubtitle,
                icon = Icons.Default.DirectionsBike,
                color = if (deliveryEnabled == false) Color.Gray else BurntOrange,
                modifier = Modifier.weight(1f)
            )

            // Delivery Time Card — shows pickup badge when both enabled
            val pickupEnabled = data.location_info.pickup_enabled
            val timeCardSubtitle = when {
                pickupEnabled == true && deliveryEnabled == true -> "pickup & delivery"
                pickupEnabled == true -> "pickup available"
                else -> "est. time"
            }
            ModernInfoCard(
                title = data.location_info.delivery_time_est,
                subtitle = timeCardSubtitle,
                icon = Icons.Default.Schedule,
                color = BurntOrange,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 7. TAGS — capsule chips (matches iOS FlowLayout + Capsule)
        if (data.brand_info.tags.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Tags",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    data.brand_info.tags.forEach { tag ->
                        Text(
                            text = tag,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            modifier = Modifier
                                .background(Color(0xFFF2F2F7), RoundedCornerShape(50))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // 8. MENU OVERVIEW (matches iOS — 16dp rounded cards)
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Menu",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            val totalItems = data.menu.sumOf { it.items.size }
            Text(
                text = "${data.menu.size} categories • $totalItems dishes",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(0.dp))

            data.menu.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF2F2F7), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.category_name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Text(
                        text = "${category.items.size} items",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

// MARK: - Modern Info Card (matches iOS ModernInfoCard)
@Composable
private fun ModernInfoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFFF2F2F7), RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = color
        )
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Gray
        )
    }
}

// Helper extension to round Double to Int
private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()
