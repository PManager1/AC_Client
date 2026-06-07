package com.example.birdy.ui.explore

import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.birdy.data.CartManager
import com.example.birdy.data.Config
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
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
import kotlin.math.max
import kotlin.math.min

// Module-level route fetch debounce state (shared across recompositions)
private var lastRouteFetchTimeMs: Long = 0L
private var isFetchingRoute: Boolean = false

// MARK: - DriverTrackingScreen — Matches iOS DriverTracking.swift
// Uses Mapbox Maps (same as iOS) to avoid Google Play Services issues.
// Listens to Firebase Realtime Database for driver location and shows it on the map.

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

    // Mapbox Map references
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var pointAnnotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var polylineAnnotationManager by remember { mutableStateOf<PolylineAnnotationManager?>(null) }

    // User location
    var userLocation by remember { mutableStateOf<Point?>(null) }
    var hasCenteredOnBoth by remember { mutableStateOf(false) }

    // Interpolation state for smooth driver movement
    var currentDisplayPoint by remember { mutableStateOf<Point?>(null) }
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
                    Log.w("DriverTracking", "⚠️ No driver location data found")
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

                Log.d("DriverTracking", "📍 Driver location updated: $lat, $lng | bearing: $driverBearing | speed: $driverSpeed")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DriverTracking", "❌ Firebase listener cancelled: ${error.message}")
            }
        }

        ref.addValueEventListener(listener)
        firebaseListener = listener
        Log.d("DriverTracking", "🔥 Started listening to Firebase: active_rides/test-ride/location")

        onDispose {
            ref.removeEventListener(listener)
            firebaseListener = null
            interpolationJob?.cancel()
            Log.d("DriverTracking", "🔥 Stopped listening to Firebase driver location")
        }
    }

    // MARK: - Handle driver location updates on the map
    LaunchedEffect(driverLat, driverLng, hasDriverLocation, pointAnnotationManager) {
        if (!hasDriverLocation || driverLat == 0.0 || driverLng == 0.0) return@LaunchedEffect
        val ptMgr = pointAnnotationManager ?: return@LaunchedEffect
        val mapView = mapViewRef ?: return@LaunchedEffect
        val newPoint = Point.fromLngLat(driverLng, driverLat)

        if (currentDisplayPoint == null) {
            // First location — snap immediately (no interpolation)
            currentDisplayPoint = newPoint
            lastFirebaseUpdateTime = System.currentTimeMillis()

            // Place marker immediately
            placeDriverMarker(ptMgr, newPoint)

            // Fetch route and fit camera
            handleRouteAndCamera(
                mapView = mapView,
                driver = newPoint,
                userLoc = userLocation,
                hasCentered = hasCenteredOnBoth,
                onCentered = { hasCenteredOnBoth = true },
                lineMgr = polylineAnnotationManager
            )
        } else {
            // Subsequent updates — smooth interpolation
            val now = System.currentTimeMillis()
            val timeSinceLastUpdate = (now - lastFirebaseUpdateTime) / 1000.0
            lastFirebaseUpdateTime = now

            var animationDuration = 2500L
            if (timeSinceLastUpdate in 0.5..10.0) {
                animationDuration = (min(max(timeSinceLastUpdate + 0.3, 1.0), 5.0) * 1000).toLong()
            }

            val startPoint = currentDisplayPoint!!
            interpolationJob?.cancel()
            interpolationJob = coroutineScope.launch {
                animateDriverMarker(
                    ptMgr = ptMgr,
                    from = startPoint,
                    to = newPoint,
                    durationMs = animationDuration,
                    onFrame = { point -> currentDisplayPoint = point }
                )
            }

            // Fetch route and fit camera
            handleRouteAndCamera(
                mapView = mapView,
                driver = newPoint,
                userLoc = userLocation,
                hasCentered = hasCenteredOnBoth,
                onCentered = { hasCenteredOnBoth = true },
                lineMgr = polylineAnnotationManager
            )
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
        // Full-screen Mapbox Map (same as iOS DriverPositionMapView)
        AndroidView(
            factory = { factoryContext ->
                MapView(factoryContext, MapInitOptions(factoryContext)).also { mapView ->
                    val mapboxMap = mapView.mapboxMap

                    // Camera: center on DC area initially (matches iOS)
                    mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(-77.0369, 38.9072))
                            .zoom(12.0)
                            .build()
                    )

                    // Load style and set up annotation managers
                    mapboxMap.loadStyle("mapbox://styles/mapbox/streets-v12") { style ->
                        Log.d("DriverTracking", "🗺️ Mapbox style loaded")

                        val annotationPlugin = mapView.annotations
                        pointAnnotationManager = annotationPlugin.createPointAnnotationManager()
                        polylineAnnotationManager = annotationPlugin.createPolylineAnnotationManager()
                    }

                    // Show user location puck (blue dot) — matches iOS
                    try {
                        val locationPlugin = mapView.location
                        locationPlugin.updateSettings {
                            enabled = true
                            puckBearingEnabled = true
                        }
                    } catch (e: Exception) {
                        Log.w("DriverTracking", "⚠️ Location puck setup: ${e.message}")
                    }

                    mapViewRef = mapView
                }
            },
            modifier = Modifier.fillMaxSize()
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
                        tint = Color(0xFF191970),
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
                .padding(bottom = 8.dp),
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
                                fontSize = 18.sp,
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

// MARK: - Place driver marker on Mapbox map
private fun placeDriverMarker(
    ptMgr: PointAnnotationManager,
    point: Point
) {
    ptMgr.deleteAll()
    val options = PointAnnotationOptions()
        .withPoint(point)
    ptMgr.create(options)
}

// MARK: - Route & Camera handling (matches iOS handleRouteAndCamera)
private fun handleRouteAndCamera(
    mapView: MapView,
    driver: Point,
    userLoc: Point?,
    hasCentered: Boolean,
    onCentered: () -> Unit,
    lineMgr: PolylineAnnotationManager?
) {
    val mapboxMap = mapView.mapboxMap

    if (userLoc != null) {
        // Fetch route and draw line
        CoroutineScope(Dispatchers.Main).launch {
            fetchRouteAndDrawLine(lineMgr, driver, userLoc)
        }

        if (!hasCentered) {
            fitCameraToShowBoth(mapboxMap, userLoc, driver)
            onCentered()
        }
    } else {
        if (!hasCentered) {
            onCentered()
            mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(driver)
                    .zoom(15.0)
                    .build()
            )
            Log.d("DriverTracking", "🎯 Camera centered on driver (no user location yet)")
        }
    }
}

// MARK: - Mapbox Directions API — Fetch Route (matches iOS fetchRouteAndDrawLine)
private suspend fun fetchRouteAndDrawLine(
    lineMgr: PolylineAnnotationManager?,
    driver: Point,
    user: Point
) {
    val manager = lineMgr ?: return

    // Debounce
    val now = System.currentTimeMillis()
    if (now - lastRouteFetchTimeMs < 15000) return
    if (isFetchingRoute) return

    isFetchingRoute = true
    lastRouteFetchTimeMs = now

    try {
        val accessToken = Config.MAPBOX_ACCESS_TOKEN

        if (accessToken.isEmpty()) {
            Log.w("DriverTracking", "⚠️ No Mapbox token — drawing straight line")
            withContext(Dispatchers.Main) {
                drawStraightLine(manager, driver, user)
            }
            return
        }

        // Mapbox Directions API (same as OrderDetail.kt)
        val url = "https://api.mapbox.com/directions/v5/mapbox/driving/" +
                "${driver.longitude()},${driver.latitude()};" +
                "${user.longitude()},${user.latitude()}?" +
                "overview=full&geometries=geojson&access_token=$accessToken"

        val response = withContext(Dispatchers.IO) { URL(url).readText() }
        val json = JSONObject(response)

        if (json.getJSONArray("routes").length() > 0) {
            val route = json.getJSONArray("routes").getJSONObject(0)
            val distanceMeters = route.getDouble("distance")
            val durationSeconds = route.getDouble("duration")
            val geometry = route.getJSONObject("geometry")
            val coords = geometry.getJSONArray("coordinates")

            val distanceMiles = distanceMeters / 1609.34
            val durationMinutes = durationSeconds / 60

            val decoded = mutableListOf<Point>()
            for (i in 0 until coords.length()) {
                val coord = coords.getJSONArray(i)
                decoded.add(Point.fromLngLat(coord.getDouble(0), coord.getDouble(1)))
            }

            Log.d("DriverTracking", "✅ Route fetched: ${String.format("%.1f", distanceMiles)} mi, ~${String.format("%.0f", durationMinutes)} min")

            withContext(Dispatchers.Main) {
                drawRouteLine(manager, decoded)
            }
        } else {
            Log.w("DriverTracking", "⚠️ No routes found — drawing straight line")
            withContext(Dispatchers.Main) {
                drawStraightLine(manager, driver, user)
            }
        }
    } catch (e: Exception) {
        Log.e("DriverTracking", "❌ Directions API error: ${e.message}")
        withContext(Dispatchers.Main) {
            drawStraightLine(manager, driver, user)
        }
    } finally {
        isFetchingRoute = false
    }
}

// MARK: - Draw Route Line (road-following) on Mapbox (matches iOS drawRouteLine)
private fun drawRouteLine(
    manager: PolylineAnnotationManager,
    coordinates: List<Point>
) {
    manager.deleteAll()
    if (coordinates.size < 2) return

    val lineString = LineString.fromLngLats(coordinates)
    val lineOptions = PolylineAnnotationOptions()
        .withGeometry(lineString)
        .withLineColor("#E65100")
        .withLineWidth(6.0)
        .withLineOpacity(0.8)
    manager.create(lineOptions)
    Log.d("DriverTracking", "🟠 Route line drawn with ${coordinates.size} points")
}

// Fallback: draw straight line if Directions API fails
private fun drawStraightLine(
    manager: PolylineAnnotationManager,
    from: Point,
    to: Point
) {
    manager.deleteAll()
    val lineString = LineString.fromLngLats(listOf(from, to))
    val lineOptions = PolylineAnnotationOptions()
        .withGeometry(lineString)
        .withLineColor("#E65100")
        .withLineWidth(6.0)
        .withLineOpacity(0.8)
    manager.create(lineOptions)
    Log.d("DriverTracking", "🔵 Straight fallback line drawn (Directions API failed)")
}

// Fit camera so both user blue dot and driver orange dot are visible (matches iOS fitCameraToShowBoth)
private fun fitCameraToShowBoth(
    mapboxMap: com.mapbox.maps.MapboxMap,
    user: Point,
    driver: Point
) {
    val southWest = Point.fromLngLat(
        min(user.longitude(), driver.longitude()),
        min(user.latitude(), driver.latitude())
    )
    val northEast = Point.fromLngLat(
        max(user.longitude(), driver.longitude()),
        max(user.latitude(), driver.latitude())
    )

    val bounds = CoordinateBounds(southWest, northEast)

    val camera = mapboxMap.cameraForCoordinateBounds(
        bounds,
        EdgeInsets(100.0, 60.0, 100.0, 60.0)  // top, left, bottom, right padding
    )
    mapboxMap.setCamera(camera)

    val results = FloatArray(1)
    android.location.Location.distanceBetween(
        user.latitude(), user.longitude(),
        driver.latitude(), driver.longitude(),
        results
    )
    Log.d("DriverTracking", "🎯 Camera fitted to show both dots — distance: ${String.format("%.1f", results[0])}m")
}

// MARK: - Smooth driver marker interpolation using coroutine (matches iOS CADisplayLink animation)
private suspend fun animateDriverMarker(
    ptMgr: PointAnnotationManager,
    from: Point,
    to: Point,
    durationMs: Long,
    onFrame: (Point) -> Unit
) {
    val startTime = System.currentTimeMillis()
    val frameDuration = 16L // ~60fps

    while (true) {
        val elapsed = System.currentTimeMillis() - startTime
        var progress = if (durationMs > 0) elapsed.toDouble() / durationMs else 1.0
        progress = progress.coerceIn(0.0, 1.0)

        val easedProgress = easeInOut(progress)

        val interpLng = from.longitude() + (to.longitude() - from.longitude()) * easedProgress
        val interpLat = from.latitude() + (to.latitude() - from.latitude()) * easedProgress
        val interpPoint = Point.fromLngLat(interpLng, interpLat)

        onFrame(interpPoint)

        withContext(Dispatchers.Main) {
            placeDriverMarker(ptMgr, interpPoint)
        }

        if (progress >= 1.0) {
            onFrame(to)
            withContext(Dispatchers.Main) {
                placeDriverMarker(ptMgr, to)
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

// MARK: - Ride Options Bottom Sheet (matches iOS RideOptionsSheet)
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
            Text(
                text = "Ride Options",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

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