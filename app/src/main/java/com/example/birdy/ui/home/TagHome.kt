// Matches iOS Home/TagHome.swift — fetches real data from /brands/sectioned?tag={tag}

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
fun TagFilterChip(
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

// MARK: - Tag Home Screen (matches iOS TagHome.swift)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagHomeScreen(
    tag: String,
    title: String,
    filters: List<String>,
    onBack: () -> Unit = {},
    onRestaurantClick: (String) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var places by remember { mutableStateOf<List<NewFoodRestaurant>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(retryTrigger, selectedFilter) {
        fetchTagPlaces(tag, selectedFilter) { items, error ->
            places = items
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
                text = title,
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
                TagFilterChip(
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
        } else if (places.isEmpty()) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp)
            ) {
                places.forEach { item ->
                    NewFoodCard(
                        restaurant = item,
                        onClick = { onRestaurantClick(item.id) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

// MARK: - API Fetch — matches iOS TagHome.fetchPlaces()

private suspend fun fetchTagPlaces(
    tag: String,
    filter: String,
    onResult: (List<NewFoodRestaurant>, String?) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            var urlString = "${Config.API_BASE_URL}/brands/sectioned?tag=$tag"
            if (filter == "Restaurant") {
                urlString += "&type=restaurant"
            } else if (filter == "Grocery") {
                urlString += "&type=grocery"
            }

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

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

                val carouselArray = section.optJSONArray("carouselImages")
                val carouselImages = if (carouselArray != null) {
                    (0 until carouselArray.length()).mapNotNull { carouselArray.optString(it).takeIf { it.isNotEmpty() } }
                } else emptyList()

                val locationsArray = section.optJSONArray("locations")
                val nearestDistance = if (locationsArray != null && locationsArray.length() > 0) {
                    (0 until locationsArray.length()).mapNotNull {
                        locationsArray.optJSONObject(it)?.optDouble("distance", Double.MAX_VALUE)
                    }.minOrNull() ?: 0.0
                } else 0.0

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
                    for (taggedItem in availableItems) {
                        val itemName = taggedItem.optString("name", "")
                        val itemPrice = taggedItem.optDouble("price", 0.0)
                        val itemImageUrl = taggedItem.optString("imageUrl", "").takeIf { it.isNotEmpty() }

                        val images = if (itemImageUrl != null) listOf(itemImageUrl) else carouselImages

                        items.add(
                            NewFoodRestaurant(
                                id = sectionId,
                                restaurantName = itemName,
                                logoURL = logoUrl,
                                images = images,
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
                    val brandImages = carouselImages

                    items.add(
                        NewFoodRestaurant(
                            id = sectionId,
                            restaurantName = sectionName,
                            logoURL = logoUrl,
                            images = brandImages,
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
