package com.example.birdy.ui.explore

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.birdy.R
import com.example.birdy.data.CartManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

// Module-level route fetch debounce state (shared across recompositions)
private var lastRouteFetchTimeMs: Long = 0L
private var isFetchingRoute: Boolean = false

// MARK: - ShowDriverPositionScreen — Matches iOS ShowDriverPosition
// Listens to Firebase Realtime Database for driver location and shows it on the map.
// Camera auto-fits to show both the user's blue dot and the driver's orange dot.
// Draws a road-following route line between driver and user via Google Maps Directions API.

@Composable
fun DriverTrackingScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Driver location from Firebase
    var driverLat by remember { mutableStateOf(0.0) }
    var driverLng by remember { mutableStateOf(0.0) }
    var driverBearing by remember { mutableStateOf(-1.0) }
    var driverSpeed by remember { mutableStateOf(0.0) }
    var isDriverActive by remember { mutableStateOf(false) }
    var hasDriverLocation by remember { mutableStateOf(false) }

    // Firebase reference for cleanup
    var firebaseListener by remember { mutableStateOf<ValueEventListener?>(null) }

    // Options menu
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Google Map reference
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var driverMarker by remember { mutableStateOf<Marker?>(null) }
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }

    // User location
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasCenteredOnBoth by remember { mutableStateOf(false) }

    // Interpolation state for smooth driver movement
    var currentDisplayCoord by remember { mutableStateOf<LatLng?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var interpolationJob by remember { mutableStateOf<Job?>(null) }
    var lastFirebaseUpdateTime by remember { mutableStateOf(0L) }

    // MARK: - Firebase Realtime Database Listener
    DisposableEffect(Unit) {
        val ref = FirebaseDatabase.getInstance().reference
            .child("active_rides")
            .child("test-ride")
            .child("location")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.value as? Map<*, *> ?: run {
                    Log.w("ShowDriverPosition", "⚠️ No driver location data found")
                    return
                }

                val lat = value["lat"] as? Double ?: return
                val lng = value["lng"] as? Double ?: return

                driverLat = lat
                driverLng = lng
                hasDriverLocation = true
                driverBearing = value["bearing"] as? Double ?: -1.0
                driverSpeed = value["speed"] as? Double ?: 0.0
                isDriverActive = value["isActive"] as? Boolean ?: false

                Log.d("ShowDriverPosition", "📍 Driver location updated: $lat, $lng | bearing: $driverBearing | speed: $driverSpeed")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ShowDriverPosition", "❌ Firebase listener cancelled: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)
        firebaseListener = listener
        Log.d("ShowDriverPosition", "🔥 Started listening to Firebase: active_rides/test-ride/location")

        onDispose {
            ref.removeEventListener(listener)
            firebaseListener = null
            interpolationJob?.cancel()
            Log.d("ShowDriverPosition", "🔥 Stopped listening to Firebase driver location")
        }
    }

    // MARK: - Handle driver location updates on the map
    LaunchedEffect(driverLat, driverLng, hasDriverLocation, googleMap) {
        if (!hasDriverLocation || driverLat == 0.0 || driverLng == 0.0 || googleMap == null) return@LaunchedEffect
        val map = googleMap ?: return@LaunchedEffect
        val newCoord = LatLng(driverLat, driverLng)

        if (currentDisplayCoord == null) {
            // First location — snap immediately (no interpolation)
            currentDisplayCoord = newCoord
            lastFirebaseUpdateTime = System.currentTimeMillis()

            // Place marker immediately
            driverMarker?.remove()
            driverMarker = map.addMarker(
                MarkerOptions()
                    .position(newCoord)
                    .icon(BitmapDescriptorFactory.fromBitmap(createDriverMarkerBitmap(context)))
                    .anchor(0.5f, 1.0f)
            )

            // Fetch route and fit camera
            handleRouteAndCamera(map, newCoord, userLocation, hasCenteredOnBoth, { hasCenteredOnBoth = true }, context) { newPolyline ->
                routePolyline?.remove()
                routePolyline = newPolyline
            }
        } else {
            // Subsequent updates — smooth interpolation
            val now = System.currentTimeMillis()
            val timeSinceLastUpdate = (now - lastFirebaseUpdateTime) / 1000.0
            lastFirebaseUpdateTime = now

            // Dynamic animation duration based on update cadence
            var animationDuration = 2500L // default 2.5s
            if (timeSinceLastUpdate in 0.5..10.0) {
                animationDuration = (min(max(timeSinceLastUpdate + 0.3, 1.0), 5.0) * 1000).toLong()
            }

            val startCoord = currentDisplayCoord!!
            interpolationJob?.cancel()
            interpolationJob = coroutineScope.launch {
                animateDriverMarker(
                    map = map,
                    from = startCoord,
                    to = newCoord,
                    durationMs = animationDuration,
                    onFrame = { coord ->
                        currentDisplayCoord = coord
                    },
                    markerRef = { driverMarker }
                )
            }

            // Fetch route and fit camera using target position
            handleRouteAndCamera(map, newCoord, userLocation, hasCenteredOnBoth, { hasCenteredOnBoth = true }, context) { newPolyline ->
                routePolyline?.remove()
                routePolyline = newPolyline
            }
        }
    }

    // MARK: - Back handler
    BackHandler {
        CartManager.showDriverTracking = false
        onBack()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Full-screen Google Map
        DriverPositionGoogleMap(
            onMapReady = { map ->
                googleMap = map
            },
            onUserLocationUpdated = { location ->
                userLocation = location
            }
        )

        // Floating back button overlay (top-left) + driver status indicator
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(4.dp, CircleShape)
                        .background(Color.White, CircleShape)
                        .clickable {
                            CartManager.showDriverTracking = false
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF191970), // orangeSecNavyBlue
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Driver status indicator pill
                if (hasDriverLocation) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isDriverActive) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (isDriverActive) "Driver Active" else "Tracking Driver",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // MARK: - Bottom Ride Details Card (matches iOS bottom card)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp), // matches iOS .padding(.bottom, 8)
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dropoff time header
            Text(
                text = "Dropoff at 5:19 PM",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )

            // Main ride info card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Ride details",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.Gray
                            )
                            Text(
                                text = "Heading to 1310 28th St NW",
                                fontSize = 18.sp, // matches iOS latest: 18 (not 20)
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Three dots options button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFFF2F2F2), RoundedCornerShape(8.dp))
                                .clickable { showOptionsMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // U-DO order badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(
                                Color(0xFFE65100).copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "U-DO Order",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }
        }
    }

    // MARK: - Ride Options Bottom Sheet (matches iOS RideOptionsSheet)
    if (showOptionsMenu) {
        RideOptionsBottomSheet(
            onDismiss = { showOptionsMenu = false }
        )
    }
}

// MARK: - Google Map Composable (matches iOS DriverPositionMapView)
// Wraps MapView in try-catch so a missing/invalid Google Maps API key
// shows a fallback UI instead of crashing the app (iOS Mapbox handles this gracefully).
@Composable
fun DriverPositionGoogleMap(
    onMapReady: (GoogleMap) -> Unit,
    onUserLocationUpdated: (LatLng) -> Unit
) {
    val context = LocalContext.current
    var mapInitError by remember { mutableStateOf(false) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    if (mapInitError) {
        // Fallback UI when Google Maps fails to initialize (bad/missing API key)
        // Matches iOS behavior where Mapbox gracefully shows blank map
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE8E8E8)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Map unavailable",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Driver tracking is active",
                    fontSize = 13.sp,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }
        }
        return
    }

    val mapView = remember {
        try {
            MapView(context)
        } catch (e: Exception) {
            Log.e("ShowDriverPosition", "❌ MapView creation failed: ${e.message}")
            null
        }
    }

    if (mapView == null) {
        mapInitError = true
        return
    }

    AndroidView(
        factory = { factoryContext ->
            try {
                mapView.apply {
                    onCreate(null)
                    getMapAsync { map ->
                        // Enable user location blue dot
                        if (ContextCompat.checkSelfPermission(factoryContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            map.isMyLocationEnabled = true
                        }

                        // Camera: center on DC area initially (matches iOS)
                        val dcCenter = LatLng(38.9072, -77.0369)
                        map.moveCamera(CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(dcCenter, 12f)
                        ))

                        // Map styling — light/clean look
                        map.uiSettings.isMyLocationButtonEnabled = false
                        map.uiSettings.isZoomControlsEnabled = false
                        map.uiSettings.isMapToolbarEnabled = false

                        onMapReady(map)
                    }
                }
            } catch (e: Exception) {
                Log.e("ShowDriverPosition", "❌ MapView factory failed: ${e.message}")
                mapInitError = true
                mapView
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    // Track user location via FusedLocationProviderClient (matches iOS CLLocationManager)
    DisposableEffect(Unit) {
        if (!mapInitError) {
            mapView.onResume()
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    onUserLocationUpdated(latLng)
                }
            }
        }

        if (!mapInitError && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            if (!mapInitError) {
                mapView.onPause()
                mapView.onDestroy()
            }
        }
    }
}

// MARK: - Route & Camera handling (shared logic, matches iOS handleRouteAndCamera)
private fun handleRouteAndCamera(
    map: GoogleMap,
    driver: LatLng,
    userLoc: LatLng?,
    hasCentered: Boolean,
    onCentered: () -> Unit,
    context: android.content.Context,
    onPolyline: (Polyline?) -> Unit
) {
    val userCoord = userLoc
    if (userCoord != null) {
        // Fetch route and draw line
        CoroutineScope(Dispatchers.Main).launch {
            fetchRouteAndDrawLine(map, driver, userCoord, context, onPolyline)
        }

        if (!hasCentered) {
            fitCameraToShowBoth(map, userCoord, driver)
            onCentered()
        }
    } else {
        if (!hasCentered) {
            onCentered()
            map.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(driver, 15f)
            ))
            Log.d("ShowDriverPosition", "🎯 Camera centered on driver (no user location yet)")
        }
    }
}

// MARK: - Google Maps Directions API — Fetch Route (matches iOS fetchRouteAndDrawLine)
private suspend fun fetchRouteAndDrawLine(
    map: GoogleMap,
    driver: LatLng,
    user: LatLng,
    context: android.content.Context,
    onPolyline: (Polyline?) -> Unit
) {
    // Debounce: don't fetch more often than every 15 seconds (module-level state)
    val now = System.currentTimeMillis()
    if (now - lastRouteFetchTimeMs < 15000) return
    if (isFetchingRoute) return

    isFetchingRoute = true
    lastRouteFetchTimeMs = now

    try {
        // Read API key from manifest
        val apiKey = try {
            val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
            applicationInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: Exception) { "" }

        if (apiKey.isEmpty() || apiKey == "YOUR_GOOGLE_MAPS_API_KEY_HERE") {
            Log.w("ShowDriverPosition", "⚠️ No Google Maps API key — drawing straight line")
            withContext(Dispatchers.Main) {
                drawStraightLine(map, driver, user, onPolyline)
            }
            return
        }

        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${driver.latitude},${driver.longitude}" +
                "&destination=${user.latitude},${user.longitude}" +
                "&mode=driving" +
                "&key=$apiKey"

        val response = withContext(Dispatchers.IO) { URL(url).readText() }
        val json = JSONObject(response)

        if (json.getJSONArray("routes").length() > 0) {
            val route = json.getJSONArray("routes").getJSONObject(0)
            val overviewPolyline = route.getJSONObject("overview_polyline").getString("points")
            val legs = route.getJSONArray("legs")
            val leg = legs.getJSONObject(0)
            val distanceMeters = leg.getJSONObject("distance").getDouble("value")
            val durationSeconds = leg.getJSONObject("duration").getDouble("value")

            val distanceMiles = distanceMeters / 1609.34
            val durationMinutes = durationSeconds / 60

            Log.d("ShowDriverPosition", "✅ Route fetched: ${String.format("%.1f", distanceMiles)} mi, ${String.format("%.0f", durationMinutes)} min")

            val decodedPoints = decodePolyline(overviewPolyline)

            withContext(Dispatchers.Main) {
                drawRouteLine(map, decodedPoints, onPolyline)
            }
        } else {
            Log.w("ShowDriverPosition", "⚠️ No routes found — drawing straight line")
            withContext(Dispatchers.Main) {
                drawStraightLine(map, driver, user, onPolyline)
            }
        }
    } catch (e: Exception) {
        Log.e("ShowDriverPosition", "❌ Directions API error: ${e.message}")
        withContext(Dispatchers.Main) {
            drawStraightLine(map, driver, user, onPolyline)
        }
    } finally {
        isFetchingRoute = false
    }
}

// MARK: - Decode Google Maps polyline (encoded polyline algorithm)
private fun decodePolyline(encoded: String): List<LatLng> {
    val points = mutableListOf<LatLng>()
    var index = 0
    var lat = 0
    var lng = 0

    while (index < encoded.length) {
        var result = 0
        var shift = 0
        var b: Int
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        result = 0
        shift = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        points.add(LatLng(lat / 1E6.toDouble(), lng / 1E6.toDouble()))
    }
    return points
}

// MARK: - Draw Route Line (road-following) (matches iOS drawRouteLine)
private fun drawRouteLine(
    map: GoogleMap,
    coordinates: List<LatLng>,
    onPolyline: (Polyline?) -> Unit
) {
    val polylineOptions = PolylineOptions()
        .addAll(coordinates)
        .width(7f)
        .color(android.graphics.Color.argb(204, 230, 81, 0)) // #E65100 at 80% opacity — matches iOS
        .geodesic(true)

    val polyline = map.addPolyline(polylineOptions)
    onPolyline(polyline)
    Log.d("ShowDriverPosition", "🟠 Route line drawn with ${coordinates.size} points")
}

// Fallback: draw straight line if Directions API fails (matches iOS drawStraightLine)
private fun drawStraightLine(
    map: GoogleMap,
    from: LatLng,
    to: LatLng,
    onPolyline: (Polyline?) -> Unit
) {
    val polylineOptions = PolylineOptions()
        .add(from, to)
        .width(7f)
        .color(android.graphics.Color.argb(204, 230, 81, 0))
        .pattern(listOf(com.google.android.gms.maps.model.Dot(), com.google.android.gms.maps.model.Gap(20f)))

    val polyline = map.addPolyline(polylineOptions)
    onPolyline(polyline)
    Log.d("ShowDriverPosition", "🔵 Straight fallback line drawn (Directions API failed)")
}

// Fit camera so both user blue dot and driver orange dot are visible (matches iOS fitCameraToShowBoth)
private fun fitCameraToShowBoth(map: GoogleMap, user: LatLng, driver: LatLng) {
    val bounds = LatLngBounds.Builder()
        .include(user)
        .include(driver)
        .build()

    val padding = 100 // matches iOS UIEdgeInsets(top: 100, left: 60, bottom: 100, right: 60)
    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))

    val results = FloatArray(1)
    android.location.Location.distanceBetween(
        user.latitude, user.longitude,
        driver.latitude, driver.longitude,
        results
    )
    Log.d("ShowDriverPosition", "🎯 Camera fitted to show both dots — distance: ${String.format("%.1f", results[0])}m")
}

// MARK: - Smooth driver marker interpolation using coroutine (matches iOS CADisplayLink animation)
private suspend fun animateDriverMarker(
    map: GoogleMap,
    from: LatLng,
    to: LatLng,
    durationMs: Long,
    onFrame: (LatLng) -> Unit,
    markerRef: () -> Marker?
) {
    val startTime = System.currentTimeMillis()
    val frameDuration = 16L // ~60fps

    while (true) {
        val elapsed = System.currentTimeMillis() - startTime
        var progress = if (durationMs > 0) elapsed.toDouble() / durationMs else 1.0
        progress = progress.coerceIn(0.0, 1.0)

        // Ease-in-out cubic — matches iOS easeInOut
        val easedProgress = easeInOut(progress)

        // Interpolate lat/lng
        val interpLat = from.latitude + (to.latitude - from.latitude) * easedProgress
        val interpLng = from.longitude + (to.longitude - from.longitude) * easedProgress
        val interpCoord = LatLng(interpLat, interpLng)

        onFrame(interpCoord)

        // Update marker on main thread
        withContext(Dispatchers.Main) {
            markerRef()?.position = interpCoord
        }

        if (progress >= 1.0) {
            onFrame(to)
            withContext(Dispatchers.Main) {
                markerRef()?.position = to
            }
            break
        }

        delay(frameDuration)
    }
}

// MARK: - Easing function — ease-in-out cubic (matches iOS easeInOut)
private fun easeInOut(t: Double): Double {
    return if (t < 0.5) {
        4 * t * t * t
    } else {
        1 - (-2 * t + 2).let { x -> x * x * x } / 2
    }
}

// MARK: - Create driver marker bitmap (matches iOS createDriverDotImage)
// Composite marker: profile picture circle above the orange location dot
private fun createDriverMarkerBitmap(context: android.content.Context): Bitmap {
    val density = context.resources.displayMetrics.density

    val profileSize = (44 * density).toInt()
    val dotSize = (20 * density).toInt()
    val overlap = (6 * density).toInt()
    val totalWidth = profileSize
    val totalHeight = profileSize + dotSize - overlap

    val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // === ORANGE LOCATION DOT (bottom center) ===
    val dotX = (totalWidth - dotSize) / 2f
    val dotY = (totalHeight - dotSize).toFloat()

    // White outer ring
    val whitePaint = Paint().apply { color = android.graphics.Color.WHITE; isAntiAlias = true }
    canvas.drawCircle(dotX + dotSize / 2f, dotY + dotSize / 2f, (dotSize / 2f + 2 * density), whitePaint)

    // Orange inner circle
    val orangePaint = Paint().apply {
        color = android.graphics.Color.rgb(217, 95, 2)
        isAntiAlias = true
    }
    canvas.drawCircle(dotX + dotSize / 2f, dotY + dotSize / 2f, dotSize / 2f, orangePaint)

    // === PROFILE PICTURE CIRCLE (top, centered) ===
    val profileCx = totalWidth / 2f
    val profileCy = profileSize / 2f
    val profileRadius = (profileSize / 2f - 3 * density)

    // White border ring
    val borderPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3 * density
        isAntiAlias = true
    }
    canvas.drawCircle(profileCx, profileCy, profileRadius, borderPaint)

    // Clip to circle for profile background
    val clipPath = Path().apply {
        addCircle(profileCx, profileCy, profileRadius - 1.5f * density, Path.Direction.CW)
    }
    canvas.save()
    canvas.clipPath(clipPath)

    // Profile background gradient (orange to navy — matches iOS)
    val gradient = LinearGradient(
        profileCx, profileCy - profileRadius,
        profileCx, profileCy + profileRadius,
        android.graphics.Color.rgb(217, 95, 2),  // orange
        android.graphics.Color.rgb(0, 51, 102),  // navy
        Shader.TileMode.CLAMP
    )
    val gradientPaint = Paint().apply {
        shader = gradient
        isAntiAlias = true
    }
    canvas.drawCircle(profileCx, profileCy, profileRadius, gradientPaint)

    // Draw initials "JD" (mock driver name: John Driver)
    val initialsPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 18 * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    val textBounds = android.graphics.Rect()
    initialsPaint.getTextBounds("JD", 0, 2, textBounds)
    canvas.drawText("JD", profileCx, profileCy + textBounds.height() / 2f, initialsPaint)

    canvas.restore()

    return bitmap
}

// MARK: - Ride Options Bottom Sheet (matches iOS RideOptionsSheet)
// White background styling — matches iOS .presentationBackground(Color.white) + onAppear appearance fixes
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RideOptionsBottomSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Text(
                text = "Ride Options",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Contact Section
            Text(
                text = "Contact",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            RideOptionItem(
                icon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF2196F3)) },
                title = "Message Driver",
                onClick = onDismiss
            )
            RideOptionItem(
                icon = { Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF4CAF50)) },
                title = "Call Driver",
                onClick = onDismiss
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Trip Section
            Text(
                text = "Trip",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            RideOptionItem(
                icon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF2196F3)) },
                title = "Share Trip Status",
                onClick = onDismiss
            )
            RideOptionItem(
                icon = { Icon(Icons.Default.ReportProblem, contentDescription = null, tint = Color(0xFFFF9800)) },
                title = "Report Issue",
                onClick = onDismiss
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Cancel Ride
            RideOptionItem(
                icon = { Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFF44336)) },
                title = "Cancel Ride",
                titleColor = Color(0xFFF44336),
                onClick = onDismiss
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RideOptionItem(
    icon: @Composable () -> Unit,
    title: String,
    titleColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            color = titleColor
        )
    }
}