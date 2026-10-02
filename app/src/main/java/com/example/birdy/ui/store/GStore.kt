package com.example.birdy.ui.store

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.birdy.data.AuthManager
import com.example.birdy.data.CartItem
import com.example.birdy.data.CartManager
import com.example.birdy.data.Config
import com.example.birdy.ui.components.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale

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

private val SystemGray5 = Color(0xFFE5E5EA)
private val SystemGray6 = Color(0xFFF2F2F7)
private val SecondaryText = Color(0xFF8A8A8E)
private val BurntOrange = Color(0xFFD95F02)
private val FreeFeeGreen = Color(0xFF0D8040)
private val CartGreen = Color(0xFF34C759)

// TODO: replace with the nearest store location once the backend returns it
private const val MOCK_LOCATION_LINE = "2.5 mi • 6400 Allentown Road"

// Fixed rows in the LazyColumn before the first aisle (banner, header, search, categories)
private const val ROWS_BEFORE_AISLES = 4

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
    var selectedItem by remember { mutableStateOf<StoreMenuItem?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        isLoading = true
        errorMessage = null
        storeData = fetchGroceryStore(restaurantId)
        if (storeData == null) errorMessage = "Failed to fetch store info"
        isLoading = false
    }

    LaunchedEffect(restaurantId) { load() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val data = storeData
        when {
            isLoading -> GroceryStoreSkeleton()
            errorMessage != null -> GroceryErrorView(
                error = errorMessage ?: "",
                onRetry = { scope.launch { load() } },
                onBack = onBack
            )
            data != null -> GroceryStoreContent(
                data = data,
                storeId = restaurantId,
                onBack = onBack,
                onSearch = { onSearch(data) },
                onItemTap = { selectedItem = it }
            )
        }

        AnimatedVisibility(
            visible = CartManager.items.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            FloatingCartBar(onViewCart = onViewCart)
        }
    }

    selectedItem?.let { item ->
        ItemDetailSheet(
            item = item,
            restaurantName = storeData?.brand_info?.name ?: "",
            onDismiss = { selectedItem = null },
            onAddToCart = { cartItem ->
                CartManager.restaurantId = restaurantId
                CartManager.addItem(cartItem)
                selectedItem = null
            }
        )
    }
}

// MARK: - Store Content

@Composable
private fun GroceryStoreContent(
    data: StoreData,
    storeId: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onItemTap: (StoreMenuItem) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val expandedAisles = remember { mutableStateListOf<Int>() }
    val storeName = data.brand_info.name

    // Grocery items have no options, so + adds one straight to the cart.
    fun quickAdd(item: StoreMenuItem) {
        CartManager.restaurantId = storeId
        CartManager.restaurantName = storeName
        CartManager.addItem(
            CartItem(
                dishName = item.name,
                restaurantName = storeName,
                price = item.price,
                quantity = 1,
                imageURL = item.image_url,
                menuItem = item
            )
        )
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun quantityInCart(item: StoreMenuItem): Int =
        CartManager.items
            .filter { it.dishName == item.name && it.restaurantName == storeName }
            .sumOf { it.quantity }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Banner with back button
        item(key = "banner") { StoreBanner(data, onBack) }

        // 2. Logo + name, location line, ETA and fee chips
        item(key = "header") { StoreHeader(data) }

        // 3. Search bar (opens the existing store search)
        item(key = "search") { StoreSearchBar(storeName, onSearch) }

        if (data.menu.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = "This store hasn't added any products yet.",
                    fontSize = 16.sp,
                    color = SecondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp)
                )
            }
        } else {
            // 4. Emoji category row: tapping one scrolls to that aisle
            item(key = "categories") {
                CategoryRow(data.menu) { index ->
                    scope.launch { listState.animateScrollToItem(ROWS_BEFORE_AISLES + index) }
                }
            }

            // 5. Aisles
            itemsIndexed(data.menu, key = { index, _ -> "aisle-$index" }) { index, aisle ->
                AisleSection(
                    aisle = aisle,
                    isExpanded = index in expandedAisles,
                    onToggle = {
                        if (index in expandedAisles) expandedAisles.remove(index) else expandedAisles.add(index)
                    },
                    quantityInCart = ::quantityInCart,
                    onAdd = ::quickAdd,
                    onItemTap = onItemTap
                )
            }
        }

        item(key = "bottom-spacer") {
            Spacer(modifier = Modifier.height(if (CartManager.items.isEmpty()) 24.dp else 110.dp))
        }
    }
}

// Stores without a banner get a slim top bar instead of an empty grey block.
@Composable
private fun StoreBanner(data: StoreData, onBack: () -> Unit) {
    val hasBanner = data.brand_info.banner_image_url.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (hasBanner) 190.dp else 72.dp)
            .background(Color.White)
    ) {
        if (hasBanner) {
            AsyncImage(
                model = data.brand_info.banner_image_url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent))
                    )
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 16.dp, start = 16.dp)
                .size(40.dp)
                .shadow(if (hasBanner) 6.dp else 0.dp, CircleShape)
                .background(if (hasBanner) Color.White else SystemGray6, CircleShape)
                .clip(CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun StoreHeader(data: StoreData) {
    val fee = data.location_info.delivery_fee

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StoreLogo(data.brand_info.logo_url)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = data.brand_info.name,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    Text(
                        text = "$MOCK_LOCATION_LINE • ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "Change",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        textDecoration = TextDecoration.Underline,
                        maxLines = 1
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip(Icons.Filled.Schedule, data.location_info.delivery_time_est, highlighted = false)
            InfoChip(Icons.Filled.DirectionsBike, deliveryFeeText(fee), highlighted = fee <= 0)
        }
    }
}

@Composable
private fun StoreLogo(url: String) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .shadow(3.dp, CircleShape)
            .background(Color.White, CircleShape)
            .border(1.dp, SystemGray5, CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (url.isEmpty()) {
            Icon(
                imageVector = Icons.Filled.ShoppingCart,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = "Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            )
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String, highlighted: Boolean) {
    val color = if (highlighted) FreeFeeGreen else Color.Black
    Row(
        modifier = Modifier
            .background(if (highlighted) FreeFeeGreen.copy(alpha = 0.1f) else SystemGray6, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun StoreSearchBar(storeName: String, onSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 22.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(SystemGray6)
            .clickable { onSearch() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "Search $storeName",
            fontSize = 16.sp,
            color = SecondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CategoryRow(aisles: List<StoreMenuCategory>, onSelect: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        itemsIndexed(aisles) { index, aisle ->
            Column(
                modifier = Modifier
                    .width(78.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.size(width = 64.dp, height = 56.dp), contentAlignment = Alignment.Center) {
                    Text(text = AisleEmoji.emoji(aisle.category_name), fontSize = 40.sp)
                }
                Text(
                    text = aisle.category_name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// One aisle: header + horizontal row, or a full grid when expanded
@Composable
private fun AisleSection(
    aisle: StoreMenuCategory,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    quantityInCart: (StoreMenuItem) -> Int,
    onAdd: (StoreMenuItem) -> Unit,
    onItemTap: (StoreMenuItem) -> Unit
) {
    val count = aisle.items.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = aisle.category_name,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
                Text(
                    text = "$count item${if (count == 1) "" else "s"}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SecondaryText
                )
            }
            if (count > 2) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SystemGray6)
                        .clickable { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = if (isExpanded) "Show fewer ${aisle.category_name}" else "See all ${aisle.category_name}",
                        tint = Color.Black,
                        modifier = Modifier.size(if (isExpanded) 22.dp else 16.dp)
                    )
                }
            }
        }

        when {
            aisle.items.isEmpty() -> Text(
                text = "No items in this aisle yet.",
                fontSize = 14.sp,
                color = SecondaryText,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            isExpanded -> Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                aisle.items.chunked(2).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEach { item ->
                            GroceryProductCard(
                                item = item,
                                quantityInCart = quantityInCart(item),
                                onAdd = { onAdd(item) },
                                onTap = { onItemTap(item) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(aisle.items, key = { _, item -> item.id }) { _, item ->
                    GroceryProductCard(
                        item = item,
                        quantityInCart = quantityInCart(item),
                        onAdd = { onAdd(item) },
                        onTap = { onItemTap(item) },
                        modifier = Modifier.width(136.dp)
                    )
                }
            }
        }
    }
}

// MARK: - Product Card

@Composable
private fun GroceryProductCard(
    item: StoreMenuItem,
    quantityInCart: Int,
    onAdd: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAvailable = item.is_available

    Column(
        modifier = modifier
            .alpha(if (isAvailable) 1f else 0.55f)
            .clickable(onClick = onTap),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Product photos come on white, so the tile is white with a hairline border.
        Box(
            modifier = Modifier
                .padding(bottom = 6.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(1.dp, SystemGray5, RoundedCornerShape(14.dp))
        ) {
            if (item.image_url.isEmpty()) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingCart,
                    contentDescription = null,
                    tint = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center)
                )
            } else {
                AsyncImage(
                    model = item.image_url,
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                )
            }

            if (isAvailable) {
                AddButton(
                    quantityInCart = quantityInCart,
                    itemName = item.name,
                    onAdd = onAdd,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                )
            }
        }

        if (isAvailable) {
            PriceText(item.price)
        } else {
            Text("Out of stock", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SecondaryText)
        }

        if (item.description.isNotEmpty()) {
            Text(
                text = item.description,
                fontSize = 13.sp,
                color = SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = item.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AddButton(quantityInCart: Int, itemName: String, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val inCart = quantityInCart > 0
    Box(
        modifier = modifier
            .size(34.dp)
            .shadow(3.dp, CircleShape)
            .background(if (inCart) BurntOrange else Color.White, CircleShape)
            .then(if (inCart) Modifier else Modifier.border(BorderStroke(1.dp, SystemGray5), CircleShape))
            .clip(CircleShape)
            .clickable(onClick = onAdd),
        contentAlignment = Alignment.Center
    ) {
        if (inCart) {
            Text(text = "$quantityInCart", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        } else {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add $itemName to cart",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// "$4.39" drawn as big dollars with small raised cents.
@Composable
private fun PriceText(price: Double) {
    val totalCents = Math.round(price * 100)
    Row(verticalAlignment = Alignment.Top) {
        Text(text = "$${totalCents / 100}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
        Text(
            text = String.format(Locale.US, "%02d", totalCents % 100),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black,
            modifier = Modifier.padding(start = 1.dp, top = 2.dp)
        )
    }
}

private fun deliveryFeeText(fee: Double): String =
    if (fee <= 0) "$0 delivery fee" else "$${String.format(Locale.US, "%.2f", fee)} delivery fee"

// MARK: - Loading / Error / Cart bar

@Composable
private fun GroceryErrorView(error: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Text("Could not load store", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SecondaryText)
        Text(
            text = error,
            fontSize = 14.sp,
            color = SecondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
        Text(
            text = "Try Again",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF007AFF))
                .clickable { onRetry() }
                .padding(horizontal = 24.dp, vertical = 10.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "Go Back",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = SecondaryText,
            modifier = Modifier
                .clickable { onBack() }
                .padding(8.dp)
        )
    }
}

@Composable
private fun FloatingCartBar(onViewCart: () -> Unit) {
    val count = CartManager.itemCount
    Row(
        modifier = Modifier
            .padding(start = 36.dp, end = 36.dp, bottom = 24.dp)
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(50), ambientColor = CartGreen, spotColor = CartGreen)
            .background(
                Brush.verticalGradient(listOf(CartGreen, Color(0xFF2DB04F))),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box {
            Icon(
                imageVector = Icons.Filled.ShoppingBag,
                contentDescription = "Cart",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "$count",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-5).dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$${String.format(Locale.US, "%.2f", CartManager.total)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "$count item${if (count == 1) "" else "s"}",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .clickable { onViewCart() }
                .padding(horizontal = 18.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text("View Cart", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CartGreen)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = CartGreen,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun GroceryStoreSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .shimmer()
        )
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .shimmer()
                )
                Box(
                    modifier = Modifier
                        .size(width = 160.dp, height = 26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmer()
                )
            }
            Box(
                modifier = Modifier
                    .size(width = 250.dp, height = 14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer()
            )
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .shimmer()
            )
        }
    }
}

// MARK: - Aisle emoji (mirrors IC AisleEmoji)

private object AisleEmoji {
    // Checked in order, so more specific words come first.
    private val rules: List<Pair<List<String>, String>> = listOf(
        listOf("deal", "sale", "special") to "🏷️",
        listOf("baby", "infant") to "🍼",
        listOf("pet", "dog", "cat") to "🐾",
        listOf("seafood", "fish", "shrimp") to "🦐",
        listOf("meat", "beef", "chicken", "pork", "poultry") to "🥩",
        listOf("deli", "prepared", "sandwich") to "🥪",
        listOf("produce", "fruit", "vegetable", "veggie", "fresh") to "🥦",
        listOf("bakery", "bread", "bagel") to "🍞",
        listOf("egg") to "🥚",
        listOf("dairy", "cheese", "milk", "yogurt") to "🧀",
        listOf("frozen", "ice") to "🧊",
        listOf("snack", "chip", "cracker") to "🍿",
        listOf("candy", "sweet", "chocolate", "dessert") to "🍬",
        listOf("coffee", "tea") to "☕️",
        listOf("wine", "beer", "alcohol", "spirit", "liquor") to "🍷",
        listOf("beverage", "drink", "juice", "soda", "water") to "🥤",
        listOf("breakfast", "cereal") to "🥣",
        listOf("pasta", "rice", "grain", "noodle") to "🍝",
        listOf("condiment", "sauce", "spice", "seasoning", "oil") to "🧂",
        listOf("pantry", "canned", "can ", "soup") to "🥫",
        listOf("household", "cleaning", "paper", "laundry") to "🧴",
        listOf("health", "pharmacy", "vitamin", "medicine") to "💊",
        listOf("personal", "beauty", "bath", "body") to "🧼",
        listOf("find", "new", "trending", "popular") to "✨"
    )

    fun emoji(aisleName: String): String {
        val name = aisleName.lowercase()
        return rules.firstOrNull { (keywords, _) -> keywords.any { name.contains(it) } }?.second ?: "🛒"
    }
}

// MARK: - Data loading (mirrors IC GStore.swift fetchGroceryStore)
private suspend fun fetchGroceryStore(restaurantId: String): StoreData? {
    return withContext(Dispatchers.IO) {
        try {
            // 1. Fetch brand info
            val brandJson = fetchJson("${Config.API_BASE_URL}/brands/$restaurantId")
            val brandName = brandJson?.optNonNullString("name") ?: ""
            val logoUrl = brandJson?.optNonNullString("logoUrl") ?: ""
            val bannerUrl = brandJson?.optNonNullString("bannerUrl") ?: ""

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
                    items = aisle.items.map { item ->
                        StoreMenuItem(
                            id = java.util.UUID.randomUUID().toString(),
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

// optString returns the literal "null" for JSON null (e.g. "bannerUrl": null), so treat that as empty.
private fun JSONObject.optNonNullString(key: String): String =
    if (isNull(key)) "" else optString(key, "")
