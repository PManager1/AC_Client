package com.example.birdy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.birdy.data.AuthManager
import com.example.birdy.data.CartManager
import com.example.birdy.data.Config
import com.example.birdy.data.LocationManager
import com.stripe.android.PaymentConfiguration
import com.example.birdy.data.ForceUpdateChecker
import com.example.birdy.ui.account.AccountScreen
import com.example.birdy.ui.components.BirdyBottomNavBar
import com.example.birdy.ui.components.ForceUpdateDialog
import com.example.birdy.data.ExploreCategory
import com.example.birdy.ui.explore.ExploreScreen
import com.example.birdy.ui.explore.NewFoodPlacesScreen
import com.example.birdy.ui.explore.SearchFoodScreen
import com.example.birdy.ui.explore.SeaMoreScreen
import com.example.birdy.ui.store.StoreScreen
import com.example.birdy.ui.explore.CartScreen
import com.example.birdy.ui.explore.CheckoutScreen
import com.example.birdy.ui.explore.DriverTrackingScreen
import com.example.birdy.ui.fooddelivery.HomeFDScreen
import com.example.birdy.ui.fooddelivery.OrderDetailScreen
import com.example.birdy.ui.home.TagHomeScreen
import com.example.birdy.ui.inbox.InboxScreen
import com.example.birdy.ui.inbox.RequestDetailScreen
import com.example.birdy.ui.theme.BirdyTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.json.JSONObject

// Tab indices — matches iOS NavigationFlow.swift selectedTab
private const val TAB_HOME = 0
private const val TAB_EXPLORE = 1
private const val TAB_INBOX = 2
private const val TAB_ACCOUNT = 3

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d("MainActivity", "Notification permission granted: $granted")
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val fineGranted = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            Log.d("MainActivity", "📍 Location permission granted — capturing download location")
            captureDownloadLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AuthManager.init(applicationContext)
        // Initialize Stripe SDK — matches iOS AppDelegate stripeInit()
        PaymentConfiguration.init(applicationContext, Config.STRIPE_PUBLISHABLE_KEY)
        enableEdgeToEdge()

        // Request notification permission (Android 13+)
        requestNotificationPermission()

        // Clear all notifications (and badge) when app is opened from launcher
        clearNotifications()

        // Request location permission and capture download location
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            captureDownloadLocation()
        } else {
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }

        setContent {
            BirdyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    BirdyAppContent()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Clear badge whenever app comes to foreground
        clearNotifications()
    }

    private fun clearNotifications() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        Log.d("MainActivity", "✅ All notifications cleared (badge removed)")
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun captureDownloadLocation() {
        MainScope().launch {
            try {
                val (lat, lng) = LocationManager.fetchLocation(this@MainActivity)
                if (lat == 0.0 && lng == 0.0) {
                    Log.w("MainActivity", "⚠️ Could not get location for download tracking")
                    return@launch
                }
                val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                val osVersion = Build.VERSION.RELEASE
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: ""

                val json = JSONObject().apply {
                    put("latitude", lat)
                    put("longitude", lng)
                    put("deviceModel", deviceModel)
                    put("osVersion", osVersion)
                    put("deviceId", deviceId)
                }

                val url = java.net.URL("${Config.API_BASE_URL}/download-location")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.write(json.toString().toByteArray())
                val responseCode = conn.responseCode
                conn.disconnect()
                Log.d("MainActivity", "📍 Download location sent — response $responseCode")
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Failed to send download location: ${e.message}")
            }
        }
    }
}

@Composable
fun BirdyAppContent() {
    var selectedTab by remember { mutableIntStateOf(TAB_HOME) }
    var showRequestDetail by remember { mutableStateOf(false) }
    var showSearchFood by remember { mutableStateOf(false) }
    var showFoodPlaces by remember { mutableStateOf(false) }
    var showStore by remember { mutableStateOf(false) }
    var selectedRestaurantId by remember { mutableStateOf("") }
    var selectedStoreName by remember { mutableStateOf("") }
    var selectedIsGrocery by remember { mutableStateOf(false) }
    var showCart by remember { mutableStateOf(false) }
    var showCheckout by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<ExploreCategory?>(null) }
    var showTagHome by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf("") }
    var selectedTagTitle by remember { mutableStateOf("") }
    var selectedTagFilters by remember { mutableStateOf(listOf<String>()) }
    var showSeaMore by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Force update check
    var showForceUpdate by remember { mutableStateOf(false) }
    var forceUpdateMinVersion by remember { mutableStateOf("0.0.0") }
    LaunchedEffect(Unit) {
        val result = ForceUpdateChecker.check(context)
        if (result.needsUpdate) {
            forceUpdateMinVersion = result.minVersion
            showForceUpdate = true
        }
    }
    if (showForceUpdate) {
        ForceUpdateDialog(minVersion = forceUpdateMinVersion)
        return
    }

    // Wrap Scaffold + overlays in a Box so DriverTrackingScreen renders as a
    // true full-screen overlay — matches iOS .fullScreenCover behavior
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.White,
            bottomBar = {
                BirdyBottomNavBar(
                    selectedIndex = selectedTab,
                    onTabSelected = { index ->
                        selectedTab = index
                        showRequestDetail = false
                        showSearchFood = false
                        showFoodPlaces = false
                        showStore = false
                        selectedCategory = null
                        showTagHome = false
                        showSeaMore = false
                        selectedStoreName = ""
                        selectedIsGrocery = false
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                when (selectedTab) {
                    TAB_HOME -> {
                        when {
                            showSearchFood -> {
                                SearchFoodScreen(
                                    onBack = { showSearchFood = false },
                                    onRestaurantClick = { restaurantId ->
                                        selectedRestaurantId = restaurantId
                                        showStore = true
                                    },
                                    onBrandClick = { brandId ->
                                        selectedRestaurantId = brandId
                                        showSearchFood = false
                                        showStore = true
                                    },
                                    onSeeMore = { showSeaMore = true }
                                )
                            }
                            showTagHome -> {
                                TagHomeScreen(
                                    tag = selectedTag,
                                    title = selectedTagTitle,
                                    filters = selectedTagFilters,
                                    onBack = { showTagHome = false },
                                    onRestaurantClick = { restaurantId ->
                                        selectedRestaurantId = restaurantId
                                        selectedStoreName = ""
                                        showTagHome = false
                                        showStore = true
                                    }
                                )
                            }
                            showCheckout -> {
                                CheckoutScreen(
                                    onBack = { showCheckout = false },
                                    onTrackOrder = {
                                        showCheckout = false
                                        showCart = false
                                    }
                                )
                            }
                            showCart -> {
                                CartScreen(
                                    onBack = { showCart = false },
                                    onCheckout = {
                                        showCart = false
                                        showCheckout = true
                                    }
                                )
                            }
                            showStore -> {
                                StoreScreen(
                                    onBack = { showStore = false },
                                    onViewCart = { showCart = true },
                                    restaurantId = selectedRestaurantId,
                                    storeName = selectedStoreName,
                                    isGrocery = selectedIsGrocery,
                                    jsonInputStream = if (selectedRestaurantId.isEmpty()) context.assets.open("storejson.json") else null
                                )
                            }
                            else -> {
                                HomeFDScreen(
                                    onNavigateToSearch = {
                                        showSearchFood = true
                                    },
                                    onNavigateToCart = {
                                        showCart = true
                                    },
                                    onRestaurantClick = { restaurantId ->
                                        selectedRestaurantId = restaurantId
                                        selectedStoreName = ""
                                        selectedIsGrocery = false
                                        showStore = true
                                    },
                                    onGroceryStoreClick = { storeId, storeName ->
                                        selectedRestaurantId = storeId
                                        selectedStoreName = storeName
                                        selectedIsGrocery = true
                                        showStore = true
                                    },
                                     onCategoryClick = { categoryName ->
                                        when (categoryName) {
                                            "Pizza" -> {
                                                selectedTag = "pizza"
                                                selectedTagTitle = "Pizza"
                                                selectedTagFilters = listOf("All", "Restaurant")
                                                showTagHome = true
                                            }
                                            "Fast Food" -> {
                                                selectedTag = "fast_food"
                                                selectedTagTitle = "Fast Food"
                                                selectedTagFilters = listOf("All", "Nearest", "Top Rated", "\$0 Delivery", "Deals")
                                                showTagHome = true
                                            }
                                            else -> {
                                                var tag = categoryName.lowercase().replace(" ", "_")
                                                if (tag.endsWith("s")) tag = tag.dropLast(1)
                                                selectedTag = tag
                                                selectedTagTitle = categoryName
                                                selectedTagFilters = listOf("All", "Restaurant")
                                                showTagHome = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    TAB_EXPLORE -> {
                        when {
                            showCheckout -> {
                                CheckoutScreen(
                                    onBack = { showCheckout = false },
                                    onTrackOrder = {
                                        showCheckout = false
                                        showCart = false
                                        // CartManager.showDriverTracking is already set to true
                                    }
                                )
                            }
                            showCart -> {
                                CartScreen(
                                    onBack = { showCart = false },
                                    onCheckout = {
                                        showCart = false
                                        showCheckout = true
                                    }
                                )
                            }
                            showStore -> {
                                StoreScreen(
                                    onBack = { showStore = false },
                                    onViewCart = { showCart = true },
                                    restaurantId = selectedRestaurantId,
                                    isGrocery = selectedIsGrocery,
                                    jsonInputStream = if (selectedRestaurantId.isEmpty()) context.assets.open("storejson.json") else null
                                )
                            }
                            showSeaMore -> {
                                SeaMoreScreen(
                                    onBack = { showSeaMore = false }
                                )
                            }
                            showSearchFood -> {
                                SearchFoodScreen(
                                    onBack = { showSearchFood = false },
                                    onRestaurantClick = { restaurantId ->
                                        selectedRestaurantId = restaurantId
                                        showStore = true
                                    },
                                    onBrandClick = { brandId ->
                                        selectedRestaurantId = brandId
                                        showSearchFood = false
                                        showStore = true
                                    },
                                    onSeeMore = { showSeaMore = true }
                                )
                            }
                            showFoodPlaces && selectedCategory != null -> {
                                NewFoodPlacesScreen(
                                    category = selectedCategory!!.title,
                                    onBack = {
                                        showFoodPlaces = false
                                        selectedCategory = null
                                    },
                                    onSearchClick = { showSearchFood = true },
                                    onRestaurantClick = { restaurantId ->
                                        selectedRestaurantId = restaurantId
                                        showStore = true
                                    }
                                )
                            }
                            else -> {
                                ExploreScreen(
                                    onNavigateToSearch = { showSearchFood = true },
                                    onCategoryClick = { category ->
                                        var tag = category.title.lowercase().replace(" ", "_")
                                        if (tag.endsWith("s")) tag = tag.dropLast(1)
                                        selectedTag = tag
                                        selectedTagTitle = category.title
                                        selectedTagFilters = listOf("All", "Restaurant")
                                        showTagHome = true
                                    }
                                )
                            }
                        }
                    }

                    TAB_INBOX -> {
                        if (showRequestDetail) {
                            RequestDetailScreen(
                                onBack = { showRequestDetail = false }
                            )
                        } else {
                            InboxScreen(
                                onNavigateToRequestDetail = { showRequestDetail = true }
                            )
                        }
                    }

                    TAB_ACCOUNT -> AccountScreen()
                }
            }
        }

        // Full-screen driver tracking overlay — matches iOS .fullScreenCover for DriverTracking
        // Rendered OUTSIDE Scaffold so it covers the entire screen including bottom nav
        if (CartManager.showDriverTracking) {
            DriverTrackingScreen(
                onBack = {
                    CartManager.showDriverTracking = false
                }
            )
        }

        // Full-screen order detail overlay — matches iOS .fullScreenCover for OrderDetail
        if (CartManager.showOrderDetail) {
            OrderDetailScreen(
                onBack = {
                    CartManager.showOrderDetail = false
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BirdyAppPreview() {
    BirdyTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            BirdyAppContent()
        }
    }
}