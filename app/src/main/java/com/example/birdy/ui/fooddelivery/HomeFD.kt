package com.example.birdy.ui.fooddelivery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.birdy.data.GroceryStore
import com.example.birdy.data.HomeFDData
import com.example.birdy.data.HomeFeedData
import com.example.birdy.data.DeliveryAddressManager
import com.example.birdy.data.AddressService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log
import org.json.JSONObject

/**
 * Home FD Screen — replicates iOS HomeFD.swift
 *
 * Loads data from /homefeed API (live backend)
 * Sections (top to bottom):
 * 1. Header (address pin + notification bell + cart)
 * 2. Search bar (disabled, navigates to search on tap)
 * 3. Category horizontal scroll (20 emoji categories)
 * 4. Featured banners (from API)
 * 5. Dynamic sections with restaurant cards (from API)
 */
@Composable
fun HomeFDScreen(
    onNavigateToSearch: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onRestaurantClick: (restaurantId: String) -> Unit = {},
    onGroceryStoreClick: (storeId: String, storeName: String) -> Unit = { _, _ -> },
    onCategoryClick: (categoryName: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Address state (stub — will connect to AddressService later)
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var selectedAddressId by remember { mutableStateOf<String?>(null) }
    var isLoadingAddress by remember { mutableStateOf(false) }
    var showAddressSheet by remember { mutableStateOf(false) }

    // Network monitoring
    val context = LocalContext.current
    var isNetworkConnected by remember { mutableStateOf(true) }
    var showNoInternetAlert by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager

        // Check current state
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        isNetworkConnected = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        if (!isNetworkConnected) showNoInternetAlert = true

        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                isNetworkConnected = true
                showNoInternetAlert = false
            }

            override fun onLost(network: android.net.Network) {
                isNetworkConnected = false
                showNoInternetAlert = true
            }

            override fun onCapabilitiesChanged(network: android.net.Network, capabilities: android.net.NetworkCapabilities) {
                val connected = capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                isNetworkConnected = connected
                if (!connected) showNoInternetAlert = true
                else showNoInternetAlert = false
            }
        }

        val request = android.net.NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        onDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    // Main category tab state — matches iOS selectedMainCategory
    var selectedMainCategory by remember { mutableStateOf("All") }
    val currentSubcategories = remember(selectedMainCategory) {
        HomeFDData.mainCategories.firstOrNull { it.name == selectedMainCategory }?.subcategories ?: emptyList()
    }

    // Grocery stores (fetched from /grocery-stores API)
    var groceryStores by remember { mutableStateOf<List<GroceryStore>>(emptyList()) }
    var isLoadingGroceryStores by remember { mutableStateOf(false) }

    // Favorites — shared source of truth across all tabs
    var favoriteRestaurantIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun toggleFavorite(restaurantId: String, nowFavorited: Boolean) {
        favoriteRestaurantIds = if (nowFavorited) favoriteRestaurantIds + restaurantId
        else favoriteRestaurantIds - restaurantId

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val token = com.example.birdy.data.AuthManager.getToken() ?: return@launch
                val url = if (nowFavorited) {
                    URL("${com.example.birdy.data.Config.API_BASE_URL}/favorites")
                } else {
                    URL("${com.example.birdy.data.Config.API_BASE_URL}/favorites?restaurantId=$restaurantId")
                }
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = if (nowFavorited) "POST" else "DELETE"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                if (nowFavorited) {
                    conn.outputStream.write("""{"restaurantId":"$restaurantId"}""".toByteArray())
                }
                println("❤️ [Fav-AC] ${if (nowFavorited) "ADD" else "REMOVE"} $restaurantId → ${conn.responseCode}")
                conn.disconnect()
            } catch (e: Exception) {
                println("❌ [Fav-AC] Error: ${e.message}")
            }
        }
    }

    // Home feed per tab ("All" / "Food" / "Drinks"), all from /homefeed?category=... (matches iOS)
    val feedCategories = remember { setOf("All", "Food", "Drinks") }
    var feeds by remember { mutableStateOf<Map<String, HomeFeedData>>(emptyMap()) }
    var loadingCategories by remember { mutableStateOf(setOf("All")) } // "All" loads on appear
    var feedErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var feedRetryKey by remember { mutableIntStateOf(0) }

    // One endpoint for All / Food / Drinks; the backend filters by category and location.
    suspend fun loadHomeFeed(category: String) {
        if (category !in feedCategories) return
        loadingCategories = loadingCategories + category
        val addr = DeliveryAddressManager.selectedAddress
        val hasCoords = addr != null && (addr.latitude != 0.0 || addr.longitude != 0.0)
        val result = withContext(Dispatchers.IO) {
            HomeFDData.fetchHomeFeed(
                category = category,
                lat = if (hasCoords) addr!!.latitude else null,
                lng = if (hasCoords) addr!!.longitude else null
            )
        }
        when (result.status) {
            com.example.birdy.data.HomeFeedStatus.SUCCESS -> {
                val data = result.data ?: HomeFeedData(emptyList(), emptyList())
                feeds = feeds + (category to data)
                feedErrors = feedErrors - category
                data.sections.flatMap { it.restaurants }.filter { it.isFavorited }.forEach {
                    favoriteRestaurantIds = favoriteRestaurantIds + it.id
                }
                println("✅ [HomeFDScreen] Loaded $category feed: ${data.featuredBanners.size} banners, ${data.sections.firstOrNull()?.restaurants?.size ?: 0} cards")
            }
            com.example.birdy.data.HomeFeedStatus.AUTH_ERROR -> {
                feedErrors = feedErrors + (category to "Couldn't load — please sign in and try again.")
                println("⚠️ [HomeFDScreen] Auth error loading $category feed")
            }
            com.example.birdy.data.HomeFeedStatus.NETWORK_ERROR -> {
                feedErrors = feedErrors + (category to "Couldn't load. Check your connection and try again.")
                println("⚠️ [HomeFDScreen] Network error loading $category feed")
            }
        }
        loadingCategories = loadingCategories - category
    }

    // Load the selected tab's feed on appear, on tab change (if not cached) and on retry
    LaunchedEffect(selectedMainCategory, feedRetryKey) {
        if (selectedMainCategory in feedCategories && selectedMainCategory !in feeds) {
            loadHomeFeed(selectedMainCategory)
        }
    }

    // New location → cached feeds are stale (matches iOS onChange of selectedAddress)
    LaunchedEffect(selectedAddressId) {
        if (selectedAddressId != null) {
            feeds = emptyMap()
            loadHomeFeed(selectedMainCategory)
        }
    }

    // Load default address on startup (matches iOS loadDefaultAddress)
    LaunchedEffect(Unit) {
        isLoadingAddress = true
        val token = com.example.birdy.data.AuthManager.getToken()
        if (token != null) {
            val addresses = withContext(Dispatchers.IO) {
                AddressService.getAddresses(token)
            }
            val defaultAddr = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
            if (defaultAddr != null) {
                DeliveryAddressManager.selectAddress(defaultAddr)
                selectedAddress = defaultAddr.street
                selectedAddressId = defaultAddr.id
                // Force re-fetch of grocery stores with distance filter
                if (selectedMainCategory == "Grocery") {
                    groceryStores = emptyList()
                }
                println("✅ [HomeFDScreen] Loaded default address: ${defaultAddr.street}")
            } else {
                println("⚠️ [HomeFDScreen] No addresses found")
            }
        } else {
            println("⚠️ [HomeFDScreen] No auth token, skipping address load")
        }
        isLoadingAddress = false
    }

    // Fetch grocery stores when Grocery tab is selected (matches iOS: onChange of selectedMainCategory)
    LaunchedEffect(selectedMainCategory) {
        if (selectedMainCategory == "Grocery" && groceryStores.isEmpty()) {
            isLoadingGroceryStores = true
            val stores = withContext(Dispatchers.IO) {
                val addr = DeliveryAddressManager.selectedAddress
                if (addr != null && addr.latitude != 0.0 && addr.longitude != 0.0) {
                    HomeFDData.fetchGroceryStores(lat = addr.latitude, lng = addr.longitude, maxDistance = 10.0)
                } else {
                    HomeFDData.fetchGroceryStores()
                }
            }
            groceryStores = stores
            isLoadingGroceryStores = false
            println("✅ [HomeFDScreen] Loaded ${stores.size} grocery stores")
        }
    }

    // Re-fetch grocery stores when address changes (distance filter needs fresh coordinates)
    LaunchedEffect(selectedAddressId) {
        if (selectedMainCategory == "Grocery" && selectedAddressId != null) {
            isLoadingGroceryStores = true
            val stores = withContext(Dispatchers.IO) {
                val addr = DeliveryAddressManager.selectedAddress
                if (addr != null && addr.latitude != 0.0 && addr.longitude != 0.0) {
                    HomeFDData.fetchGroceryStores(lat = addr.latitude, lng = addr.longitude, maxDistance = 10.0)
                } else {
                    HomeFDData.fetchGroceryStores()
                }
            }
            groceryStores = stores
            isLoadingGroceryStores = false
            println("✅ [HomeFDScreen] Re-fetched ${stores.size} grocery stores (address changed)")
        }
    }

    // Auto-reload data when connectivity returns after being lost (matches iOS onReceive isConnected)
    var wasOffline by remember { mutableStateOf(false) }
    LaunchedEffect(isNetworkConnected) {
        if (isNetworkConnected && wasOffline) {
            feeds = emptyMap()
            loadHomeFeed(selectedMainCategory)
            wasOffline = false
        } else if (!isNetworkConnected) {
            wasOffline = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // MARK: - Header
            HomeFDHeader(
                selectedAddress = selectedAddress,
                isLoadingAddress = isLoadingAddress,
                onAddressClick = {
                    showAddressSheet = true
                },
                onCartClick = { onNavigateToCart() }
            )

            // MARK: - Search Bar
            HomeFDSearchBar(
                onClick = { onNavigateToSearch() },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // MARK: - Categories (tab strip + subcategory icons)
            // Matches iOS: Main Category Strip + Subcategory Icons
            Column {
                MainCategoryTabStrip(
                    mainCategories = HomeFDData.mainCategories,
                    selectedMainCategory = selectedMainCategory,
                    onMainCategorySelected = { selectedMainCategory = it }
                )
                HomeFDCategoryList(
                    categories = currentSubcategories,
                    onCategoryClick = { category ->
                        onCategoryClick(category.name)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // MARK: - "Fastest near you" heading for non-Food tabs
            if (selectedMainCategory != "Food" && selectedMainCategory != "All") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Fastest near you",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            // MARK: - Grocery Stores (when Grocery tab selected)
            if (selectedMainCategory == "Grocery") {
                if (isLoadingGroceryStores) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .heightIn(max = 150.dp),
                        userScrollEnabled = false
                    ) {
                        items(4) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SkeletonBlock(width = 80.dp, height = 80.dp, cornerRadius = 16.dp)
                                SkeletonBlock(width = 60.dp, height = 12.dp, cornerRadius = 6.dp)
                            }
                        }
                    }
                } else if (groceryStores.isEmpty()) {
                    Text(
                        text = "No grocery stores available",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    GroceryStoreList(
                        stores = groceryStores,
                        onStoreClick = { store ->
                            println("🛒 [HomeFDScreen] Tapped grocery store: ${store.name} (${store.id})")
                            onGroceryStoreClick(store.id, store.name)
                        }
                    )
                }
            }

            // MARK: - Feed content for All / Food / Drinks (one /homefeed call per tab)
            if (selectedMainCategory in feedCategories) {
                val feed = feeds[selectedMainCategory]
                val isLoadingFeed = feed == null && selectedMainCategory in loadingCategories
                val feedError = feedErrors[selectedMainCategory]

                // Featured Banners or Skeleton
                if (isLoadingFeed) {
                    SkeletonPromoBanner(modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    feed?.featuredBanners?.forEach { banner ->
                        DynamicPromoBannerView(
                            banner = banner,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dynamic Sections or Skeletons
                if (isLoadingFeed) {
                    SkeletonFeedSection(modifier = Modifier.padding(horizontal = 0.dp))
                    SkeletonFeedSection(modifier = Modifier.padding(horizontal = 0.dp))
                } else if (feed == null && feedError != null) {
                    FeedErrorCard(
                        message = feedError,
                        onRetry = { feedRetryKey++ }
                    )
                } else {
                    feed?.sections?.forEach { section ->
                        FeedRestaurantSection(
                            title = section.heading,
                            restaurants = section.restaurants,
                            favoriteIds = favoriteRestaurantIds,
                            onToggleFavorite = { id -> toggleFavorite(id, id !in favoriteRestaurantIds) },
                            // Every feed card is a brand: open its store page by brand id
                            // (iOS: AppRoute.BrandStore(brandId:)), same as search results.
                            onRestaurantClick = { restaurant -> onRestaurantClick(restaurant.id) }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }

        // MARK: - Address Selection Sheet
        if (showAddressSheet) {
            SelectAddressSheet(
                currentAddressId = selectedAddressId,
                onAddressSelected = { address ->
                    DeliveryAddressManager.selectAddress(address)
                    selectedAddress = address.street
                    selectedAddressId = address.id
                },
                onDismiss = {
                    showAddressSheet = false
                }
            )
        }

        // MARK: - No Internet Alert
        if (showNoInternetAlert) {
            AlertDialog(
                onDismissRequest = { showNoInternetAlert = false },
                title = { androidx.compose.material3.Text("No Internet Connection") },
                text = { androidx.compose.material3.Text("Please check your internet connection and try again.") },
                confirmButton = {
                    Button(onClick = { showNoInternetAlert = false }) {
                        androidx.compose.material3.Text("OK")
                    }
                }
            )
        }

        // MARK: - Not in our area yet (same screen as the web modal and iOS)
        DeliveryAddressManager.zoneResult.value?.let { result ->
            OutOfZoneSheet(
                result = result,
                onTryAnother = {
                    DeliveryAddressManager.dismissZone()
                    showAddressSheet = true
                },
                onDone = { DeliveryAddressManager.dismissZone() }
            )
        }
    }
}
