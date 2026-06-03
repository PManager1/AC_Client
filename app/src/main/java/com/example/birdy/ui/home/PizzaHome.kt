// Matches iOS Home/PizzaHome.swift — fetches real data from /chains/brands/sectioned?tag=pizza

package com.example.birdy.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.birdy.data.AuthManager
import com.example.birdy.data.Config
import com.example.birdy.ui.explore.NewFoodCard
import com.example.birdy.ui.explore.NewFoodRestaurant
import com.example.birdy.ui.explore.ShimmerCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// MARK: - Filter Chip (matches iOS FilterChip)

@Composable
fun PizzaFilterChip(
    title: String,
    isSelected: Boolean,
    action: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) Color(0xFFFF9500) else Color(0xFFF2F2F7))
            .clickable { action() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else Color.Black
        )
    }
}

// MARK: - Pizza Home Screen (matches iOS PizzaHome.swift)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PizzaHomeScreen(
    onBack: () -> Unit = {},
    onRestaurantClick: (String) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Restaurant")

    var pizzaPlaces by remember { mutableStateOf<List<NewFoodRestaurant>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }

    // Fetch on first appear and on retry or filter change
    LaunchedEffect(retryTrigger, selectedFilter) {
        fetchPizzaPlaces(selectedFilter) { items, error ->
            pizzaPlaces = items
            errorMessage = error
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }
            Text(
                text = "Pizza",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Filters (horizontal scroll) — matches iOS filters
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            filters.forEach { filter ->
                PizzaFilterChip(
                    title = filter,
                    isSelected = selectedFilter == filter,
                    action = {
                        if (selectedFilter != filter) {
                            selectedFilter = filter
                            isLoading = true
                            errorMessage = null
                        }
                    }
                )
            }
        }

        // Content
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp)
            ) {
                repeat(3) {
                    ShimmerCard()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else if (errorMessage != null) {
            // Error state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("⚠️", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Failed to load restaurants", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(errorMessage ?: "", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .background(Color.Blue, RoundedCornerShape(10.dp))
                        .clickable {
                            isLoading = true
                            errorMessage = null
                            retryTrigger++
                        }
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text("Try Again", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else if (pizzaPlaces.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🍽️", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("No restaurants found", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        } else {
            // Restaurant list
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp)
            ) {
                pizzaPlaces.forEach { item ->
                    NewFoodCard(
                        restaurant = item,
                        onClick = { onRestaurantClick(item.id) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Bottom spacing
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

// MARK: - API Fetch — matches iOS fetchPizzaPlaces()

private suspend fun fetchPizzaPlaces(
    filter: String,
    onResult: (List<NewFoodRestaurant>, String?) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            // Build URL — matches iOS: /chains/brands/sectioned?tag=pizza[&type=restaurant]
            var urlString = "${Config.API_BASE_URL}/chains/brands/sectioned?tag=pizza"
            if (filter == "Restaurant") {
                urlString += "&type=restaurant"
            }

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            // Add auth token if available — matches iOS AuthManager.shared.getToken()
            AuthManager.getToken()?.let { token ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                withContext(Dispatchers.Main) { onResult(emptyList(), "Server error: $responseCode") }
                return@withContext
            }

            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val sectionsArray = json.getJSONArray("sections")
            val items = mutableListOf<NewFoodRestaurant>()

            for (i in 0 until sectionsArray.length()) {
                val section = sectionsArray.getJSONObject(i)

                val sectionId = section.optString("id", "")
                val sectionName = section.optString("name", "")
                val logoUrl = section.optString("logoUrl", "").takeIf { it.isNotEmpty() }
                val bannerUrl = section.optString("bannerUrl", "").takeIf { it.isNotEmpty() }

                // Parse carousel images
                val carouselArray = section.optJSONArray("carouselImages")
                val sectionCarouselImages = if (carouselArray != null) {
                    (0 until carouselArray.length()).mapNotNull { carouselArray.optString(it).takeIf { it.isNotEmpty() } }
                } else emptyList()

                // Find nearest location distance — matches iOS: section.locations.map(\.distance).min()
                val locationsArray = section.optJSONArray("locations")
                val nearestDistance = if (locationsArray != null && locationsArray.length() > 0) {
                    (0 until locationsArray.length()).mapNotNull {
                        locationsArray.optJSONObject(it)?.optDouble("distance", Double.MAX_VALUE)
                    }.minOrNull() ?: 0.0
                } else 0.0

                // Parse tagged items — matches iOS: section.taggedItems?.filter { $0.available }
                val taggedArray = section.optJSONArray("taggedItems")
                val availableItems = mutableListOf<JSONObject>()
                if (taggedArray != null) {
                    for (j in 0 until taggedArray.length()) {
                        val taggedItem = taggedArray.getJSONObject(j)
                        if (taggedItem.optBoolean("available", false)) {
                            availableItems.add(taggedItem)
                        }
                    }
                }

                if (availableItems.isNotEmpty()) {
                    // Map each tagged item to a card — matches iOS taggedItems.map logic
                    for (taggedItem in availableItems) {
                        val itemName = taggedItem.optString("name", "")
                        val itemPrice = taggedItem.optDouble("price", 0.0)
                        val itemImageUrl = taggedItem.optString("imageUrl", "").takeIf { it.isNotEmpty() }

                        // Image fallback: item image → carousel → banner → logo
                        val images = mutableListOf<String>()
                        if (itemImageUrl != null) images.add(itemImageUrl)
                        if (images.isEmpty()) images.addAll(sectionCarouselImages)
                        if (images.isEmpty() && bannerUrl != null) images.add(bannerUrl)
                        if (images.isEmpty() && logoUrl != null) images.add(logoUrl)

                        items.add(
                            NewFoodRestaurant(
                                id = sectionId,
                                restaurantName = itemName,
                                logoURL = logoUrl,
                                images = images.ifEmpty { listOf("https://storage.googleapis.com/birdyimages/__App/placeholder-restaurant.jpg") },
                                rating = 4.5,
                                reviewCount = 100,
                                distance = nearestDistance,
                                deliveryTime = 30,
                                deliveryFee = 0.0,
                                promoText = null,
                                isSponsored = false,
                                itemName = itemName,
                                itemPrice = itemPrice
                            )
                        )
                    }
                } else {
                    // No tagged items — show brand as a card — matches iOS fallback
                    val brandImages = mutableListOf<String>()
                    brandImages.addAll(sectionCarouselImages)
                    if (brandImages.isEmpty() && bannerUrl != null) brandImages.add(bannerUrl)
                    if (brandImages.isEmpty() && logoUrl != null) brandImages.add(logoUrl)

                    items.add(
                        NewFoodRestaurant(
                            id = sectionId,
                            restaurantName = sectionName,
                            logoURL = logoUrl,
                            images = brandImages.ifEmpty { listOf("https://storage.googleapis.com/birdyimages/__App/placeholder-restaurant.jpg") },
                            rating = 4.5,
                            reviewCount = 100,
                            distance = nearestDistance,
                            deliveryTime = 30,
                            deliveryFee = 0.0,
                            promoText = null,
                            isSponsored = false,
                            itemName = null,
                            itemPrice = null
                        )
                    )
                }
            }

            withContext(Dispatchers.Main) { onResult(items, null) }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onResult(emptyList(), e.message) }
        }
    }
}