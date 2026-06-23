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
import com.example.birdy.data.FeedRestaurant
import com.example.birdy.data.FeedSection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.birdy.data.GroceryStore
import com.example.birdy.data.HomeFDData
import com.example.birdy.data.HomeFeedData
import com.example.birdy.data.DeliveryAddressManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

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

    // Main category tab state — matches iOS selectedMainCategory
    var selectedMainCategory by remember { mutableStateOf("All") }
    val currentSubcategories = remember(selectedMainCategory) {
        HomeFDData.mainCategories.firstOrNull { it.name == selectedMainCategory }?.subcategories ?: emptyList()
    }

    // Grocery stores (fetched from /grocery-stores API)
    var groceryStores by remember { mutableStateOf<List<GroceryStore>>(emptyList()) }
    var isLoadingGroceryStores by remember { mutableStateOf(false) }

    // Drink brands (tag-filtered from /brands)
    var drinkBrands by remember { mutableStateOf<List<FeedRestaurant>>(emptyList()) }
    var isLoadingDrinkBrands by remember { mutableStateOf(false) }

    // Food brands (tag-filtered from /brands)
    var foodBrands by remember { mutableStateOf<List<FeedRestaurant>>(emptyList()) }
    var isLoadingFoodBrands by remember { mutableStateOf(false) }

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

    // API-driven data
    var homeFeed by remember { mutableStateOf<HomeFeedData?>(null) }
    var isLoadingFeed by remember { mutableStateOf(true) }

    // Fetch home feed from API on appear
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            homeFeed = HomeFDData.fetchHomeFeed()
        }
        isLoadingFeed = false
        if (homeFeed != null) {
            println("✅ [HomeFDScreen] Loaded home feed: ${homeFeed!!.featuredBanners.size} banners, ${homeFeed!!.sections.size} sections")
        } else {
            println("⚠️ [HomeFDScreen] Home feed is null — API fetch may have failed")
        }
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

    // Fetch drink brands when Drinks tab is selected
    LaunchedEffect(selectedMainCategory) {
        if (selectedMainCategory == "Drinks" && drinkBrands.isEmpty()) {
            isLoadingDrinkBrands = true
            val brands = withContext(Dispatchers.IO) {
                HomeFDData.fetchTaggedFeedRestaurants(HomeFDData.drinkTags)
            }
            drinkBrands = brands
            isLoadingDrinkBrands = false
            brands.forEach { if (it.isFavorited) favoriteRestaurantIds = favoriteRestaurantIds + it.id }
            println("✅ [HomeFDScreen] Loaded ${brands.size} drink-tagged brands")
        }
    }

    // Fetch food brands when Food tab is selected
    LaunchedEffect(selectedMainCategory) {
        if (selectedMainCategory == "Food" && foodBrands.isEmpty()) {
            isLoadingFoodBrands = true
            val brands = withContext(Dispatchers.IO) {
                HomeFDData.fetchTaggedFeedRestaurants(HomeFDData.foodTags)
            }
            foodBrands = brands
            isLoadingFoodBrands = false
            brands.forEach { if (it.isFavorited) favoriteRestaurantIds = favoriteRestaurantIds + it.id }
            println("✅ [HomeFDScreen] Loaded ${brands.size} food-tagged brands")
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

            // MARK: - Food Feed Content (only when Food tab selected)
            if (selectedMainCategory == "All" || selectedMainCategory == "Food" || selectedMainCategory == "Drinks") {
                // Featured Banners or Skeleton
                if (isLoadingFeed) {
                    SkeletonPromoBanner(modifier = Modifier.padding(horizontal = 16.dp))
                } else {
                    homeFeed?.featuredBanners?.forEach { banner ->
                        DynamicPromoBannerView(
                            banner = banner,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dynamic Sections or Skeletons
                if (isLoadingFeed || (selectedMainCategory == "Drinks" && isLoadingDrinkBrands) || (selectedMainCategory == "Food" && isLoadingFoodBrands)) {
                    SkeletonFeedSection(modifier = Modifier.padding(horizontal = 0.dp))
                    SkeletonFeedSection(modifier = Modifier.padding(horizontal = 0.dp))
                } else {
                    val sections = when (selectedMainCategory) {
                        "Drinks" -> listOf(
                            FeedSection("Fastest near you", drinkBrands),
                            FeedSection("Most loved", drinkBrands),
                            FeedSection("Most Popular local", drinkBrands)
                        )
                        "Food" -> listOf(
                            FeedSection("Fastest near you", foodBrands),
                            FeedSection("Most loved", foodBrands),
                            FeedSection("Most Popular local", foodBrands)
                        )
                        else -> homeFeed?.sections ?: emptyList()
                    }
                    sections.forEach { section ->
                        FeedRestaurantSection(
                            title = section.heading,
                            restaurants = section.restaurants,
                            favoriteIds = favoriteRestaurantIds,
                            onToggleFavorite = { id -> toggleFavorite(id, id !in favoriteRestaurantIds) },
                            onRestaurantClick = { restaurant ->
                                if (restaurant.isBrandItem) {
                                    onGroceryStoreClick(restaurant.id, restaurant.restaurantName)
                                } else {
                                    onRestaurantClick(restaurant.id)
                                }
                            }
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
                    selectedAddress = address.street
                    selectedAddressId = address.id
                },
                onDismiss = {
                    showAddressSheet = false
                }
            )
        }
    }
}