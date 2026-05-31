package com.example.birdy.ui.store

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.birdy.data.CartItem
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

    // Load data: API if restaurantId provided, else local JSON
    LaunchedEffect(restaurantId) {
        isLoading = true
        loadError = false
        try {
            if (restaurantId.isNotEmpty()) {
                storeData = fetchStoreDetail(restaurantId, storeName = storeName)
            } else if (jsonInputStream != null) {
                storeData = loadStoreData(jsonInputStream)
            }
            if (storeData == null) loadError = true
            // Set cart restaurant context — matches iOS setting cart.restaurantId
            if (storeData != null) {
                CartManager.restaurantId = restaurantId
                CartManager.restaurantName = storeData!!.brand_info.name
            }
        } catch (e: Exception) {
            loadError = true
        }
        isLoading = false
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
                .verticalScroll(rememberScrollState())
        ) {
            // 1. BANNER with header buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                if (data.brand_info.banner_image_url.isEmpty()) {
                    // Grey placeholder when no banner (matches iOS)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(Color.Gray.copy(alpha = 0.15f))
                    )
                } else {
                    AsyncImage(
                        model = data.brand_info.banner_image_url,
                        contentDescription = "Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
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
                        HeaderCircleButton(icon = Icons.Default.FavoriteBorder) { /* TODO */ }
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
                    AsyncImage(
                        model = data.brand_info.logo_url,
                        contentDescription = "Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .shadow(8.dp, CircleShape)
                    )
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

        // 6. FLOATING CART BAR
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

    // 7. RESTAURANT INFO SHEET
    if (showRestaurantInfo) {
        StoreInfo(
            data = data,
            onDismiss = { showRestaurantInfo = false }
        )
    }

    // 8. ITEM DETAIL SHEET (matches iOS ItemDetailSheet)
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

    val shimmerColor = Color(0xFFF5F5F5)  // Very light gray (systemGray6 equivalent)
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

