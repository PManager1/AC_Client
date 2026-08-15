package com.example.birdy.ui.store

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.birdy.data.AuthManager
import com.example.birdy.data.CartManager
import com.example.birdy.data.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

// MARK: - Grocery Store Screen (brands of type "grocery")
// Mirrors IC GStore.swift — loads /brands/{id} + /brands/{id}/aisles and displays aisle items.

data class GroceryAisleItem(
    val name: String,
    val price: Double,
    val description: String,
    val rawImageUrl: String,
    val available: Boolean,
    val tags: List<String>
)

data class GroceryAisle(
    val category: String,
    val items: List<GroceryAisleItem>
)

@Composable
fun GStoreScreen(
    onBack: () -> Unit,
    onViewCart: () -> Unit,
    restaurantId: String,
    onSearch: (StoreData) -> Unit = {}
) {
    var storeData by remember { mutableStateOf<StoreData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(restaurantId) {
        isLoading = true
        errorMessage = null
        storeData = fetchGroceryStore(restaurantId)
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                GrocerySkeletonLoading()
            }

            errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Failed to load store", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage ?: "", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Go Back",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(Color.Gray, RoundedCornerShape(12.dp))
                                .clickable { onBack() }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                        Text(
                            text = "Retry",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(colors = listOf(Color(0xFF4CAF50), Color(0xFF388E3C))),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    isLoading = true
                                    errorMessage = null
                                    scope.launch {
                                        storeData = fetchGroceryStore(restaurantId)
                                        isLoading = false
                                    }
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }
            }

            else -> {
                val data = storeData
                if (data != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 1. BANNER with header buttons
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        ) {
                            if (data.brand_info.banner_image_url.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .background(Color(0xFFE0E0E0))
                                )
                            } else {
                                SubcomposeAsyncImage(
                                    model = data.brand_info.banner_image_url,
                                    contentDescription = "Banner",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp),
                                    loading = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(240.dp)
                                                .background(Color.Gray.copy(alpha = 0.15f))
                                        )
                                    },
                                    error = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(240.dp)
                                                .background(Color.Gray.copy(alpha = 0.15f))
                                        )
                                    }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent)
                                        )
                                    )
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 50.dp, start = 16.dp, end = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HeaderCircleButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HeaderCircleButton(icon = Icons.Default.Search, onClick = { onSearch(data) })
                                    HeaderCircleButton(icon = Icons.Default.MoreVert) {}
                                }
                            }
                        }

                        // 2. MAIN CONTENT
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(horizontal = 16.dp)
                        ) {
                            // Logo overlapping banner
                            Box(modifier = Modifier.offset(y = (-40).dp)) {
                                if (data.brand_info.logo_url.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(84.dp)
                                            .background(Color.Gray.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = data.brand_info.name.take(1).uppercase(),
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )
                                    }
                                } else {
                                    SubcomposeAsyncImage(
                                        model = data.brand_info.logo_url,
                                        contentDescription = "Logo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(84.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .shadow(8.dp, CircleShape),
                                        loading = {
                                            Box(
                                                modifier = Modifier
                                                    .size(84.dp)
                                                    .background(Color.Gray.copy(alpha = 0.1f), CircleShape)
                                            )
                                        },
                                        error = {
                                            Box(
                                                modifier = Modifier
                                                    .size(84.dp)
                                                    .background(Color.Gray.copy(alpha = 0.15f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = data.brand_info.name.take(1).uppercase(),
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height((-30).dp))

                            // Title & rating
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = data.brand_info.name,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = String.format("%.1f", data.brand_info.rating),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "(${data.brand_info.review_count}) • Grocery",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // 3. MENU (aisles)
                            if (data.menu.isEmpty()) {
                                Text(
                                    text = "No menu available yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                )
                            } else {
                                data.menu.forEachIndexed { index, category ->
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = category.category_name,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(
                                                top = if (index == 0) 0.dp else 30.dp,
                                                bottom = 16.dp
                                            )
                                        )
                                        category.items.chunked(2).forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                rowItems.forEach { item ->
                                                    StoreFoodCard(
                                                        menuItem = item,
                                                        restaurantName = data.brand_info.name,
                                                        onItemTap = { },
                                                        modifier = Modifier.weight(1f),
                                                        descriptionColor = Color.Black
                                                    )
                                                }
                                                if (rowItems.size == 1) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(if (CartManager.items.isEmpty()) 20.dp else 100.dp))
                        }
                    }
                }
            }
        }

        // Floating cart bar
        if (CartManager.items.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .shadow(20.dp, RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(colors = listOf(Color(0xFF4CAF50), Color(0xFF388E3C))),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .background(Color.Red, CircleShape)
                                .size(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${CartManager.itemCount}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "${CartManager.itemCount} item${if (CartManager.itemCount == 1) "" else "s"}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$${String.format(java.util.Locale.US, "%.2f", CartManager.total)}",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "View Cart",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF388E3C),
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(50))
                            .clickable { onViewCart() }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

// MARK: - Data loading (mirrors IC GStore.swift fetchGroceryStore)
private suspend fun fetchGroceryStore(restaurantId: String): StoreData? {
    return withContext(Dispatchers.IO) {
        try {
            // 1. Fetch brand info
            val brandJson = fetchJson("${Config.API_BASE_URL}/brands/$restaurantId")
            val brandName = brandJson?.optString("name", "") ?: ""
            val logoUrl = brandJson?.optString("logoUrl", "") ?: ""
            val bannerUrl = brandJson?.optString("bannerUrl", "") ?: ""

            // 2. Fetch aisles
            var aisles: List<GroceryAisle> = emptyList()
            try {
                val aislesJson = fetchJson("${Config.API_BASE_URL}/brands/$restaurantId/aisles")
                aisles = parseAisles(aislesJson)
            } catch (e: Exception) {
                println("⚠️ [GStore] Failed to fetch aisles for store $restaurantId: ${e.message}")
            }

            val menu = aisles.map { aisle ->
                StoreMenuCategory(
                    category_name = aisle.category,
                    items = aisle.items.mapIndexed { i, item ->
                        StoreMenuItem(
                            id = "g-$i",
                            name = item.name,
                            description = item.description,
                            price = item.price,
                            image_url = item.rawImageUrl,
                            is_available = item.available,
                            modifier_groups = emptyList()
                        )
                    }
                )
            }

            val data = StoreData(
                restaurant_id = restaurantId,
                brand_info = StoreBrandInfo(
                    name = brandName.ifEmpty { "Grocery Store" },
                    logo_url = logoUrl,
                    banner_image_url = bannerUrl,
                    rating = 4.5,
                    review_count = "Grocery",
                    cuisine = "grocery",
                    tags = emptyList()
                ),
                location_info = StoreLocationInfo(
                    distance = "",
                    delivery_fee = 0.0,
                    delivery_time_est = "20-35 min",
                    address = "",
                    phone = null,
                    operating_hours = null,
                    location_id = ""
                ),
                menu = menu
            )
            println("✅ [GStore] Loaded grocery store ${data.brand_info.name}: ${menu.size} aisles")
            data
        } catch (e: Exception) {
            println("❌ [GStore] Failed to load store $restaurantId: ${e.message}")
            null
        }
    }
}

private suspend fun fetchJson(urlString: String): JSONObject? {
    return withContext(Dispatchers.IO) {
        try {
            val conn = URL(urlString).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            AuthManager.getToken()?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            JSONObject(body)
        } catch (e: Exception) {
            println("⚠️ [GStore] Fetch failed for $urlString: ${e.message}")
            null
        }
    }
}

private fun parseAisles(root: JSONObject?): List<GroceryAisle> {
    if (root == null) return emptyList()
    val aislesArr = root.optJSONArray("aisles") ?: return emptyList()
    val result = mutableListOf<GroceryAisle>()
    for (i in 0 until aislesArr.length()) {
        val aisleObj = aislesArr.optJSONObject(i) ?: continue
        val category = aisleObj.optString("category", "Items")
        val itemsArr = aisleObj.optJSONArray("items") ?: org.json.JSONArray()
        val items = mutableListOf<GroceryAisleItem>()
        for (j in 0 until itemsArr.length()) {
            val itemObj = itemsArr.optJSONObject(j) ?: continue
            val tagsArr = itemObj.optJSONArray("tags")
            val tags = if (tagsArr != null) {
                (0 until tagsArr.length()).map { tagsArr.getString(it) }
            } else emptyList()
            items.add(
                GroceryAisleItem(
                    name = itemObj.optString("name", "Item"),
                    price = itemObj.optDouble("price", 0.0),
                    description = itemObj.optString("description", ""),
                    rawImageUrl = itemObj.optString("raw_image_url", ""),
                    available = itemObj.optBoolean("available", true),
                    tags = tags
                )
            )
        }
        result.add(GroceryAisle(category = category, items = items))
    }
    return result
}

// MARK: - Fancy Skeleton Loading Waves for Grocery Stores
@Composable
fun GrocerySkeletonLoading() {
    val transition = rememberInfiniteTransition(label = "skeletonWaves")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Banner skeleton with wave animation
        val bannerWave1 by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(animation = tween(1500, delayMillis = 0)),
            label = "bannerWave1"
        )
        val bannerWave2 by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(animation = tween(1500, delayMillis = 300)),
            label = "bannerWave2"
        )
        val bannerWave3 by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(animation = tween(1500, delayMillis = 600)),
            label = "bannerWave3"
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF5F5F5),
                            Color(0xFFE8E8E8),
                            Color(0xFFF5F5F5)
                        ),
                        start = Offset(bannerWave1 - 200f, bannerWave1 - 200f),
                        end = Offset(bannerWave1, bannerWave1)
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Main content area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp)
        ) {
            // Logo circle with wave animation
            val logoWave by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1000f,
                animationSpec = infiniteRepeatable(animation = tween(1500, delayMillis = 200)),
                label = "logoWave"
            )
            
            Box(modifier = Modifier.offset(y = (-40).dp)) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFF0F0F0),
                                    Color(0xFFE0E0E0),
                                    Color(0xFFF0F0F0)
                                ),
                                start = Offset(logoWave - 200f, logoWave - 200f),
                                end = Offset(logoWave, logoWave)
                            )
                        )
                )
            }
            
            Spacer(modifier = Modifier.height((-30).dp))
            
            // Title and subtitle with wave animations
            val titleWave by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1000f,
                animationSpec = infiniteRepeatable(animation = tween(1500, delayMillis = 400)),
                label = "titleWave"
            )
            val subtitleWave by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1000f,
                animationSpec = infiniteRepeatable(animation = tween(1500, delayMillis = 700)),
                label = "subtitleWave"
            )
            
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .fillMaxWidth(0.65f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFF5F5F5),
                                    Color(0xFFE5E5E5),
                                    Color(0xFFF5F5F5)
                                ),
                                start = Offset(titleWave - 200f, titleWave - 200f),
                                end = Offset(titleWave, titleWave)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .height(16.dp)
                        .fillMaxWidth(0.45f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFF5F5F5),
                                    Color(0xFFE8E8E8),
                                    Color(0xFFF5F5F5)
                                ),
                                start = Offset(subtitleWave - 200f, subtitleWave - 200f),
                                end = Offset(subtitleWave, subtitleWave)
                            )
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Menu categories with staggered wave animations
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(4) { index ->
                    val categoryWave by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1000f,
                        animationSpec = infiniteRepeatable(animation = tween(1500, delayMillis = (900 + index * 200).toInt())),
                        label = "categoryWave$index"
                    )
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Category title
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .width(140.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFF5F5F5),
                                            Color(0xFFE8E8E8),
                                            Color(0xFFF5F5F5)
                                        ),
                                        start = Offset(categoryWave - 200f, categoryWave - 200f),
                                        end = Offset(categoryWave, categoryWave)
                                    )
                                )
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Food card placeholders in grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            repeat(2) { cardIndex ->
                                val cardWave by transition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 1000f,
                                    animationSpec = infiniteRepeatable(animation = tween(1500, delayMillis = (1100 + index * 200 + cardIndex * 100).toInt())),
                                    label = "cardWave${index}_${cardIndex}"
                                )
                                
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Card image
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color(0xFFF5F5F5),
                                                        Color(0xFFE8E8E8),
                                                        Color(0xFFF5F5F5)
                                                    ),
                                                    start = Offset(cardWave - 200f, cardWave - 200f),
                                                    end = Offset(cardWave, cardWave)
                                                )
                                            )
                                    )
                                    // Card title
                                    Box(
                                        modifier = Modifier
                                            .height(14.dp)
                                            .fillMaxWidth(0.7f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color(0xFFF5F5F5),
                                                        Color(0xFFE8E8E8),
                                                        Color(0xFFF5F5F5)
                                                    ),
                                                    start = Offset(cardWave - 200f, cardWave - 200f),
                                                    end = Offset(cardWave, cardWave)
                                                )
                                            )
                                    )
                                    // Card price
                                    Box(
                                        modifier = Modifier
                                            .height(12.dp)
                                            .width(40.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color(0xFFF5F5F5),
                                                        Color(0xFFE8E8E8),
                                                        Color(0xFFF5F5F5)
                                                    ),
                                                    start = Offset(cardWave - 200f, cardWave - 200f),
                                                    end = Offset(cardWave, cardWave)
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
