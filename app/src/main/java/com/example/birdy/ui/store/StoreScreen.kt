package com.example.birdy.ui.store

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BreakfastDining
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.birdy.data.AuthManager
import com.example.birdy.data.CartItem
import com.example.birdy.data.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import com.example.birdy.data.CartManager
import java.io.InputStream

// MARK: - Store Screen (matches iOS StoreView)

@Composable
fun StoreScreen(
    onBack: () -> Unit = {},
    onViewCart: () -> Unit = {},
    onViewRestaurantInfo: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    restaurantId: String = "",
    storeName: String = "",
    jsonInputStream: InputStream? = null,
    isGrocery: Boolean = false
) {
    var storeData by remember { mutableStateOf<StoreData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("Delivery") }
    var selectedItem by remember { mutableStateOf<StoreMenuItem?>(null) }
    var showRestaurantInfo by remember { mutableStateOf(false) }
    var showAislesSheet by remember { mutableStateOf(false) }
    var activeCategory by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    var anchorInitialY by remember { mutableStateOf(Float.MAX_VALUE) }
    val categoryHeaderPositions = remember { mutableStateMapOf<String, Float>() }
    val showPinnedHeader by remember {
        derivedStateOf { scrollState.value >= anchorInitialY.toInt() }
    }
    val scope = rememberCoroutineScope()
    var isFavorited by remember { mutableStateOf(false) }
    var favoriteRestaurantIds by remember { mutableStateOf<List<String>>(emptyList()) }

    // Load data: three-phase — quick brand → full menu (no GPS) → location (GPS background)
    val context = LocalContext.current
    LaunchedEffect(restaurantId) {
        isLoading = true
        loadError = false
        try {
            if (restaurantId.isNotEmpty()) {
                // Phase 1: Quick brand fetch (banner + name only) — dismisses skeleton
                val quickData = fetchBrandQuick(restaurantId)
                if (quickData != null) {
                    storeData = quickData
                    CartManager.restaurantId = restaurantId
                    CartManager.restaurantName = quickData.brand_info.name
                }

                // Phase 2: Full menu + brand info (no GPS, fast via /brands/{id} + /menu)
                val fastData = fetchBrandWithMenu(restaurantId)
                if (fastData != null) {
                    storeData = fastData
                    CartManager.restaurantId = restaurantId
                    CartManager.restaurantName = fastData.brand_info.name
                } else if (quickData == null) {
                    loadError = true
                }

                // Phase 3: Background location fetch (GPS + nearest-store)
                scope.launch(Dispatchers.IO) {
                    val location = fetchStoreLocation(restaurantId, context)
                    withContext(Dispatchers.Main) {
                        storeData = storeData?.copy(location_info = location)
                    }
                }
            } else if (jsonInputStream != null) {
                storeData = loadStoreData(jsonInputStream)
                if (storeData == null) loadError = true
                if (storeData != null) {
                    CartManager.restaurantId = restaurantId
                    CartManager.restaurantName = storeData!!.brand_info.name
                }
            }
            if (storeData == null && restaurantId.isEmpty()) loadError = true
        } catch (e: Exception) {
            loadError = true
        }
        isLoading = false
    }

    // Fetch favorite status on load
    LaunchedEffect(restaurantId) {
        try {
            val token = AuthManager.getToken(context) ?: return@LaunchedEffect
            val url = URL("${Config.API_BASE_URL}/favorites")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                val arr = org.json.JSONArray(body)
                val ids = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    arr.getJSONObject(i).optString("restaurantId", "")?.let { if (it.isNotEmpty()) ids.add(it) }
                }
                favoriteRestaurantIds = ids
                isFavorited = ids.contains(restaurantId)
            }
            conn.disconnect()
        } catch (e: Exception) {
            println("❌ [Fav] GET error: ${e.message}")
        }
    }

    fun toggleFavorite(restaurantId: String, nowFavorited: Boolean, context: android.content.Context) {
        scope.launch(Dispatchers.IO) {
            try {
                val token = AuthManager.getToken(context) ?: return@launch
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
                println("❤️ [Fav-AC-Store] ${if (nowFavorited) "ADD" else "REMOVE"} $restaurantId → ${conn.responseCode}")
                conn.disconnect()
            } catch (e: Exception) {
                println("❌ [Fav-AC-Store] Error: ${e.message}")
            }
        }
    }

    // Loading skeleton — clean white shimmer (matches iOS)
    if (isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Banner placeholder
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )

            // 2. Main content area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp)
            ) {
                // Logo circle overlapping
                Box(modifier = Modifier.offset(y = (-40).dp)) {
                    ShimmerBox(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(-30.dp))

                // Restaurant name + info
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .height(22.dp)
                            .fillMaxWidth(0.7f)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    ShimmerBox(
                        modifier = Modifier
                            .height(14.dp)
                            .fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }

                // Delivery / Pickup toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(200.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    ShimmerBox(
                        modifier = Modifier
                            .width(110.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                }

                // Delivery info box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(120.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        ShimmerBox(
                            modifier = Modifier
                                .width(80.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier
                            .width(1.dp)
                            .height(50.dp)
                            .align(Alignment.CenterVertically),
                        color = Color.Gray.copy(alpha = 0.2f)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(100.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        ShimmerBox(
                            modifier = Modifier
                                .width(80.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Menu section title
                ShimmerBox(
                    modifier = Modifier
                        .width(160.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Food card placeholders
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(4) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.width(180.dp)
                        ) {
                            ShimmerBox(
                                modifier = Modifier
                                    .size(172.dp, 170.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            ShimmerBox(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            ShimmerBox(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
        return
    }

    // Error state
    if (loadError || storeData == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Failed to load restaurant", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Please try again", fontSize = 14.sp, color = Color.Gray)
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
                            loadError = false
                        }
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }
        return
    }

    val data = storeData!!

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(scrollState)
        ) {
            // 1. BANNER with header buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                if (data.brand_info.banner_image_url.isEmpty()) {
                    // Neutral background when no banner
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
                        HeaderCircleButton(icon = Icons.Default.Search) { onSearchClick?.invoke() }
                        HeaderCircleButton(
                            icon = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            onClick = {
                                val newState = !isFavorited
                                isFavorited = newState
                                toggleFavorite(restaurantId, newState, context)
                            },
                            tint = if (isFavorited) Color.Red else Color.Black
                        )
                        HeaderCircleButton(icon = Icons.Default.MoreVert) { /* TODO */ }
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

                Spacer(modifier = Modifier.height(-30.dp))

                // Title & Info (tappable for Restaurant Info)
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onViewRestaurantInfo?.invoke()
                                showRestaurantInfo = true
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = data.brand_info.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Restaurant Info",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable {
                                    onViewRestaurantInfo?.invoke()
                                    showRestaurantInfo = true
                                }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format("%.1f", data.brand_info.rating),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = Color.Black
                        )
                        Text(
                            text = "(${data.brand_info.review_count}) • ${data.brand_info.tags.joinToString(" • ")} • ${data.brand_info.cuisine} • ${data.location_info.distance}",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 3. DELIVERY / PICKUP TOGGLE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(50))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        ToggleBtn("Delivery", selectedMode == "Delivery") { selectedMode = "Delivery" }
                        ToggleBtn("Pickup", selectedMode == "Pickup") { selectedMode = "Pickup" }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Group Order",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier
                            .background(Color.Gray.copy(alpha = 0.08f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                // 4. DELIVERY INFO BOX
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF0F0))
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (data.location_info.delivery_fee == 0.0) "Free delivery"
                            else "$${String.format("%.2f", data.location_info.delivery_fee)} delivery fee",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCC1111)
                        )
                        Text(text = "pricing & fees", fontSize = 13.sp, color = Color.Gray)
                    }
                    HorizontalDivider(
                        modifier = Modifier
                            .width(1.dp)
                            .height(50.dp)
                            .align(Alignment.CenterVertically),
                        color = Color.Gray.copy(alpha = 0.3f)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.Gray.copy(alpha = 0.03f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = data.location_info.delivery_time_est,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(text = "delivery time", fontSize = 13.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // 5. MENU CATEGORIES
                // Anchor marker for pinned header visibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .onGloballyPositioned { coords ->
                            if (anchorInitialY == Float.MAX_VALUE) {
                                anchorInitialY = coords.positionInRoot().y
                            }
                        }
                )

                data.menu.forEachIndexed { index, category ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = category.category_name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            modifier = Modifier
                                .padding(
                                    top = if (index == 0) 0.dp else 30.dp,
                                    bottom = 16.dp
                                )
                                .onGloballyPositioned { coords ->
                                    categoryHeaderPositions[category.category_name] =
                                        coords.positionInRoot().y + scrollState.value
                                }
                        )
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val cardW = if (isGrocery) 172.dp else 190.dp
                            category.items.forEach { item ->
                                StoreFoodCard(
                                    menuItem = item,
                                    restaurantName = data.brand_info.name,
                                    onItemTap = { selectedItem = item },
                                    cardWidth = cardW
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (CartManager.items.isEmpty()) 20.dp else 100.dp))
            }
        }

        // Active category tracking based on scroll position
        LaunchedEffect(scrollState.value, categoryHeaderPositions.size, storeData) {
            if (categoryHeaderPositions.isEmpty() || data.menu.isEmpty()) return@LaunchedEffect
            var best = data.menu.first().category_name
            var bestY = Float.MAX_VALUE
            for (cat in data.menu) {
                val contentY = categoryHeaderPositions[cat.category_name] ?: continue
                val currentY = contentY - scrollState.value
                if (currentY >= 0 && currentY < bestY) {
                    bestY = currentY
                    best = cat.category_name
                }
            }
            activeCategory = best
        }

        // 6. PINNED HEADER — appears on scroll past anchor, matches iOS
        AnimatedVisibility(
            visible = showPinnedHeader,
            enter = slideInVertically { -it } + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically { -it } + fadeOut(animationSpec = tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                Spacer(modifier = Modifier.height(0.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                    Text(
                        text = data.brand_info.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    IconButton(
                        onClick = { onSearchClick?.invoke() },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.Gray
                        )
                    }
                }

                PinnedCategoryStrip(
                    menu = data.menu,
                    activeCategory = activeCategory,
                    onCategoryClick = { catName ->
                        val targetY = categoryHeaderPositions[catName]
                        if (targetY != null) {
                            scope.launch {
                                scrollState.animateScrollTo(
                                    targetY.toInt().coerceAtLeast(0)
                                )
                            }
                        }
                    },
                    onAislesClick = { showAislesSheet = true }
                )

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            }
        }

        // 7. FLOATING CART BAR (matches iOS floating cart bar)
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
                            text = "$${String.format("%.2f", CartManager.total)}",
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

    // 8. RESTAURANT INFO SHEET
    if (showRestaurantInfo) {
        StoreInfo(
            data = data,
            onDismiss = { showRestaurantInfo = false }
        )
    }

    // 8b. AISLE CATEGORIES OVERLAY (matches iOS AisleCategories)
    if (showAislesSheet) {
        AisleCategoriesOverlay(
            menu = data.menu,
            onDismiss = { showAislesSheet = false }
        )
    }

    // 9. ITEM DETAIL SHEET (matches iOS ItemDetailSheet)
    selectedItem?.let { item ->
        ItemDetailSheet(
            item = item,
            restaurantName = data.brand_info.name,
            onDismiss = { selectedItem = null },
            onAddToCart = { cartItem ->
                CartManager.addItem(cartItem)
                selectedItem = null
            }
        )
    }
}

// MARK: - Aisle Categories Overlay (matches iOS AisleCategories)
@Composable
fun AisleCategoriesOverlay(
    menu: List<StoreMenuCategory>,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header with close button and title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 26.dp, bottom = 12.dp)
        ) {
            Text(
                text = "Aisles",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
        // Category list
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp)
        ) {
            menu.forEachIndexed { index, category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: navigate to aisle detail */ }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFF57C00).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon(category.category_name),
                            contentDescription = null,
                            tint = Color(0xFFF57C00),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = category.category_name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (index < menu.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 76.dp),
                        color = Color.Gray.copy(alpha = 0.1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// MARK: - Category Pill (matches iOS CategoryPill)
@Composable
fun CategoryPill(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Color(0xFFF57C00).copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.06f)
    val textColor = if (isSelected) Color(0xFFF57C00) else Color.Black
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 0) "$title ($count)" else title,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

fun categoryIcon(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    val lower = name.lowercase()
    return when {
        lower.contains("beverage") || lower.contains("drink") || lower.contains("soda") || lower.contains("juice") || lower.contains("water") ->
            Icons.Default.LocalCafe
        lower.contains("snack") || lower.contains("chip") || lower.contains("cracker") ->
            Icons.Default.Fastfood
        lower.contains("dairy") || lower.contains("milk") || lower.contains("cheese") || lower.contains("yogurt") ->
            Icons.Default.WaterDrop
        lower.contains("produce") || lower.contains("fruit") || lower.contains("vegetable") || lower.contains("fresh") ->
            Icons.Default.Eco
        lower.contains("meat") || lower.contains("poultry") || lower.contains("chicken") || lower.contains("beef") ->
            Icons.Default.LocalFireDepartment
        lower.contains("bakery") || lower.contains("bread") || lower.contains("pastry") || lower.contains("cake") ->
            Icons.Default.Cake
        lower.contains("frozen") || lower.contains("ice cream") ->
            Icons.Default.AcUnit
        lower.contains("personal") || lower.contains("health") || lower.contains("beauty") || lower.contains("pharmacy") ->
            Icons.Default.Favorite
        lower.contains("household") || lower.contains("clean") || lower.contains("home") ->
            Icons.Default.Home
        lower.contains("baby") || lower.contains("diaper") || lower.contains("infant") ->
            Icons.Default.ChildCare
        lower.contains("pet") || lower.contains("dog") || lower.contains("cat") ->
            Icons.Default.Pets
        lower.contains("candy") || lower.contains("sweet") || lower.contains("chocolate") ->
            Icons.Default.CardGiftcard
        lower.contains("cereal") || lower.contains("breakfast") || lower.contains("oat") ->
            Icons.Default.BreakfastDining
        lower.contains("canned") || lower.contains("soup") || lower.contains("sauce") ->
            Icons.Default.Inventory2
        lower.contains("deli") || lower.contains("prepared") || lower.contains("hot food") ->
            Icons.Default.Restaurant
        lower.contains("alcohol") || lower.contains("beer") || lower.contains("wine") || lower.contains("liquor") ->
            Icons.Default.LocalBar
        lower.contains("tobacco") || lower.contains("cigarette") ->
            Icons.Default.SmokingRooms
        lower.contains("electronics") || lower.contains("phone") || lower.contains("tech") ->
            Icons.Default.Devices
        lower.contains("toys") || lower.contains("games") ->
            Icons.Default.SportsEsports
        else -> Icons.Default.GridView
    }
}

// MARK: - Shimmer Loading Component (clean white style, matches iOS)
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200)
        ),
        label = "shimmerSlide"
    )

    val shimmerColor = Color(0xFFE8E8E8)
    val shimmerHighlight = Color.White.copy(alpha = 0.7f)

    Box(
        modifier = modifier
            .background(shimmerColor)
            .then(
                Modifier.background(
                    Brush.linearGradient(
                        colors = listOf(
                            shimmerColor,
                            shimmerHighlight,
                            shimmerColor,
                        ),
                        start = Offset(translateAnim - 300f, translateAnim - 300f),
                        end = Offset(translateAnim, translateAnim)
                    )
                )
            )
    )
}

// MARK: - Pinned Category Strip (matches iOS storeCategoryStrip)
@Composable
private fun PinnedCategoryStrip(
    menu: List<StoreMenuCategory>,
    activeCategory: String?,
    onCategoryClick: (String) -> Unit,
    onAislesClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAislesClick) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "All Aisles",
                tint = Color.Black
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Bottom
        ) {
            menu.forEach { cat ->
                CategoryTabButton(
                    title = cat.category_name,
                    isSelected = activeCategory == cat.category_name,
                    onClick = { onCategoryClick(cat.category_name) }
                )
            }
        }
    }
}

// MARK: - Category Tab Button (matches iOS CategoryTabButton — underline style)
@Composable
private fun CategoryTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.Black else Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(if (isSelected) Color.Black else Color.Transparent)
        )
    }
}

