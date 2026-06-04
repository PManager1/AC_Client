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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.birdy.data.Config
import com.example.birdy.ui.components.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Matches iOS NewFoodPlaces.swift

// MARK: - Data Models

data class NewFoodRestaurant(
    val id: String,
    val restaurantName: String,
    val logoURL: String?,
    val images: List<String>,
    val rating: Double,
    val reviewCount: Int,
    val distance: Double,
    val deliveryTime: Int,
    val deliveryFee: Double,
    val promoText: String?,
    val isSponsored: Boolean,
    val itemName: String?,
    val itemPrice: Double?
) {
    val formattedDistance: String get() = String.format("%.1f mi", distance)
    val formattedTime: String get() = "$deliveryTime min"
    val formattedFee: String get() = if (deliveryFee == 0.0) "$0 delivery fee" else "$${"%.2f".format(deliveryFee)} delivery fee"
}

// MARK: - Main Screen

@Composable
fun NewFoodPlacesScreen(
    category: String,
    onBack: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onRestaurantClick: (String) -> Unit = {}
) {
    var foodItems by remember { mutableStateOf<List<NewFoodRestaurant>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }

    // Fetch restaurants on appear (and on retry)
    LaunchedEffect(category, retryTrigger) {
        fetchRestaurants(category) { items, error ->
            foodItems = items
            errorMessage = error
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // MARK: - Top Bar
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
                text = category.replaceFirstChar { it.uppercase() },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.Black
                )
            }
        }

        HorizontalDivider(color = Color(0xFFE0E0E0))

        // MARK: - Content
        if (isLoading) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                items(3) {
                    ShimmerCard()
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
        } else if (foodItems.isEmpty()) {
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                items(foodItems) { item ->
                    NewFoodCard(
                        restaurant = item,
                        onClick = onRestaurantClick
                    )
                }
            }
        }
    }
}

// MARK: - Restaurant Card (matches iOS NewFoodCardView)

@Composable
fun NewFoodCard(
    restaurant: NewFoodRestaurant,
    onClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable { onClick(restaurant.id) }
    ) {
        // 1. Image Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        ) {
            // Main image
            val imageUrl = restaurant.images.firstOrNull() ?: ""
            AsyncImage(
                model = imageUrl,
                contentDescription = restaurant.restaurantName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Image count indicator
            if (restaurant.images.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "1/${restaurant.images.size}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Price tag (top left)
            if (restaurant.itemName != null && restaurant.itemPrice != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = restaurant.itemName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "$${String.format("%.2f", restaurant.itemPrice)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    }
                }
            }

            // Sponsored tag (top right)
            if (restaurant.isSponsored) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Sponsored",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Info Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Logo
                if (restaurant.logoURL != null) {
                    AsyncImage(
                        model = restaurant.logoURL,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.1f))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.1f))
                    )
                }

                // Name + rating
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = restaurant.restaurantName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = Color(0xFFFF9500)
                        )
                        Text(
                            text = String.format("%.1f", restaurant.rating),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9500)
                        )
                        Text(
                            text = "(${restaurant.reviewCount}+)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                        Text("•", fontSize = 13.sp, color = Color.Gray.copy(alpha = 0.3f))
                        Text(
                            text = restaurant.formattedTime,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = Color.Gray.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Promo & Fee Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                restaurant.promoText?.let { promo ->
                    Box(
                        modifier = Modifier
                            .background(Color.Red, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = promo,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = restaurant.formattedFee,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Green
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = restaurant.formattedDistance,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }

    // Card shadow via spacing
    Spacer(modifier = Modifier.height(4.dp))
}

// MARK: - Shimmer Loading Card

@Composable
fun ShimmerCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
    ) {
        // Image placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .shimmer()
        )

        // Info section
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(34.dp).clip(CircleShape).shimmer())
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.width(180.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                    Box(modifier = Modifier.width(140.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.width(80.dp).height(24.dp).clip(RoundedCornerShape(6.dp)).shimmer())
                Box(modifier = Modifier.width(100.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmer())
            }
        }
    }
}

// MARK: - API Fetch

private suspend fun fetchRestaurants(
    category: String,
    onResult: (List<NewFoodRestaurant>, String?) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            // Convert title like "Fast Food" to slug like "fast-food"
            val slug = category
                .trim()
                .lowercase()
                .replace("\\s+".toRegex(), "-")
                .replace("&".toRegex(), "and")
            val encodedCategory = URLEncoder.encode(slug, "UTF-8")
            val url = URL("${Config.API_BASE_URL}/restaurants?category=$encodedCategory")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                withContext(Dispatchers.Main) { onResult(emptyList(), "Server error: $responseCode") }
                return@withContext
            }

            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val restaurantsArray = json.getJSONArray("restaurants")
            val items = mutableListOf<NewFoodRestaurant>()

            for (i in 0 until restaurantsArray.length()) {
                val r = restaurantsArray.getJSONObject(i)
                val imagesArray = r.optJSONArray("images")
                val images = if (imagesArray != null) {
                    (0 until imagesArray.length()).mapNotNull { imagesArray.optString(it).takeIf { it.isNotEmpty() } }
                } else emptyList()

                val restaurantPromoText = r.optString("promoText", "").takeIf { it.isNotEmpty() }
                val logoURL = r.optString("logoURL", "").takeIf { it.isNotEmpty() }

                // Parse foodItems (API returns "foodItems" not "items") and extract image URLs
                val itemsArray = r.optJSONArray("foodItems")
                val itemName = itemsArray?.optJSONObject(0)?.optString("name")
                val itemPrice = itemsArray?.optJSONObject(0)?.optDouble("basePrice")
                val itemPromoText = itemsArray?.optJSONObject(0)?.optString("promoText", "")?.takeIf { it.isNotEmpty() }

                // Match iClient: use food item images for carousel (not restaurant-level branding images)
                // iClient: let carouselImages = restaurant.foodItems.flatMap { parseImageUrls($0.imageUrl) }
                val foodItemImages = mutableListOf<String>()
                itemsArray?.let { arr ->
                    for (j in 0 until arr.length()) {
                        val rawUrl = arr.optJSONObject(j)?.optString("imageUrl", "") ?: ""
                        foodItemImages.addAll(parseImageUrls(rawUrl))
                    }
                }
                // Filter out logoURL from food item images just in case
                val carouselImages = foodItemImages.filter { img -> logoURL == null || img != logoURL }
                    .ifEmpty {
                        // Fallback: use restaurant-level images only if no food item images at all
                        images.filter { img -> logoURL == null || img != logoURL }
                    }

                items.add(
                    NewFoodRestaurant(
                        id = r.optString("id"),
                        restaurantName = r.optString("restaurantName"),
                        logoURL = logoURL,
                        images = carouselImages.ifEmpty { listOf("https://storage.googleapis.com/birdyimages/__App/placeholder-restaurant.jpg") },
                        rating = r.optDouble("rating", 0.0),
                        reviewCount = r.optInt("reviewCount", 0),
                        distance = r.optDouble("distance", 0.0),
                        deliveryTime = r.optInt("deliveryTime", 30),
                        deliveryFee = r.optDouble("deliveryFee", 0.0),
                        promoText = itemPromoText ?: restaurantPromoText,
                        isSponsored = r.optBoolean("isSponsored", false),
                        itemName = itemName,
                        itemPrice = itemPrice
                    )
                )
            }

            withContext(Dispatchers.Main) { onResult(items, null) }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onResult(emptyList(), e.message) }
        }
    }
}

// MARK: - Parse Image URLs
// Handles imageUrl which may be a plain URL string OR a JSON-encoded array of URLs
// e.g. "[\"https://...\",\"https://...\"]" → ["https://...", "https://..."]
// e.g. "https://..." → ["https://..."]
private fun parseImageUrls(raw: String): List<String> {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return emptyList()
    if (trimmed.startsWith("[")) {
        try {
            val arr = org.json.JSONArray(trimmed)
            return (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { it.isNotEmpty() } }
        } catch (_: Exception) { /* fall through */ }
    }
    return listOf(trimmed)
}
