package com.example.birdy.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

// MARK: - Data Models (matching iOS StoreData structs)

data class StoreBrandInfo(
    val name: String,
    val logo_url: String,
    val banner_image_url: String,
    val rating: Double,
    val review_count: String,
    val cuisine: String,
    val tags: List<String>
)

data class StoreLocationInfo(
    val distance: String,
    val delivery_fee: Double,
    val delivery_time_est: String,
    val address: String
)

data class StoreMenuCategory(
    val category_name: String,
    val items: List<StoreMenuItem>
)

data class StoreMenuItem(
    val name: String,
    val price: Double,
    val image_url: String
)

data class StoreData(
    val restaurant_id: String,
    val brand_info: StoreBrandInfo,
    val location_info: StoreLocationInfo,
    val menu: List<StoreMenuCategory>
)

// MARK: - JSON Loader

fun loadStoreData(inputStream: InputStream): StoreData? {
    return try {
        val json = JSONObject(inputStream.bufferedReader().use { it.readText() })
        val brandObj = json.getJSONObject("brand_info")
        val locObj = json.getJSONObject("location_info")
        val menuArr = json.getJSONArray("menu")

        val brandInfo = StoreBrandInfo(
            name = brandObj.getString("name"),
            logo_url = brandObj.getString("logo_url"),
            banner_image_url = brandObj.getString("banner_image_url"),
            rating = brandObj.getDouble("rating"),
            review_count = brandObj.getString("review_count"),
            cuisine = brandObj.getString("cuisine"),
            tags = parseStringArray(brandObj.getJSONArray("tags"))
        )

        val locationInfo = StoreLocationInfo(
            distance = locObj.getString("distance"),
            delivery_fee = locObj.getDouble("delivery_fee"),
            delivery_time_est = locObj.getString("delivery_time_est"),
            address = locObj.getString("address")
        )

        val menu = (0 until menuArr.length()).map { i ->
            val catObj = menuArr.getJSONObject(i)
            val itemsArr = catObj.getJSONArray("items")
            StoreMenuCategory(
                category_name = catObj.getString("category_name"),
                items = (0 until itemsArr.length()).map { j ->
                    val itemObj = itemsArr.getJSONObject(j)
                    StoreMenuItem(
                        name = itemObj.getString("name"),
                        price = itemObj.getDouble("price"),
                        image_url = itemObj.getString("image_url")
                    )
                }
            )
        }

        StoreData(
            restaurant_id = json.getString("restaurant_id"),
            brand_info = brandInfo,
            location_info = locationInfo,
            menu = menu
        )
    } catch (e: Exception) {
        null
    }
}

private fun parseStringArray(arr: JSONArray): List<String> {
    return (0 until arr.length()).map { arr.getString(it) }
}

// MARK: - Store Screen (matches iOS Store.swift)

@Composable
fun StoreScreen(
    onBack: () -> Unit = {},
    onViewCart: () -> Unit = {},
    jsonInputStream: InputStream? = null
) {
    val storeData = remember(jsonInputStream) {
        jsonInputStream?.let { loadStoreData(it) }
    } ?: run {
        // Fallback placeholder data
        StoreData(
            restaurant_id = "unknown",
            brand_info = StoreBrandInfo("Restaurant", "", "", 0.0, "0", "", emptyList()),
            location_info = StoreLocationInfo("", 0.0, "", ""),
            menu = emptyList()
        )
    }

    var selectedMode by remember { mutableStateOf("Delivery") }

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
                // Banner image
                AsyncImage(
                    model = storeData.brand_info.banner_image_url,
                    contentDescription = "Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )

                // Gradient overlay at top for button visibility
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

                // Header buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close button
                    HeaderCircleButton(icon = Icons.Default.Close, onClick = onBack)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HeaderCircleButton(icon = Icons.Default.Search) { /* TODO */ }
                        HeaderCircleButton(icon = Icons.Default.FavoriteBorder) { /* TODO */ }
                        HeaderCircleButton(icon = Icons.Default.MoreVert) { /* TODO */ }
                    }
                }
            }

            // 2. MAIN CONTENT (overlaps banner)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp)
            ) {
                // Logo overlapping banner
                Box(
                    modifier = Modifier.offset(y = (-40).dp)
                ) {
                    AsyncImage(
                        model = storeData.brand_info.logo_url,
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

                // Title & Info
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = storeData.brand_info.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.Search, // chevron right placeholder
                            contentDescription = "More",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format("%.1f", storeData.brand_info.rating),
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
                            text = "(${storeData.brand_info.review_count}) • ${storeData.brand_info.tags.joinToString(" • ")} • ${storeData.brand_info.cuisine} • ${storeData.location_info.distance}",
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
                        ToggleBtn(
                            title = "Delivery",
                            isSelected = selectedMode == "Delivery",
                            onClick = { selectedMode = "Delivery" }
                        )
                        ToggleBtn(
                            title = "Pickup",
                            isSelected = selectedMode == "Pickup",
                            onClick = { selectedMode = "Pickup" }
                        )
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
                    // Delivery fee
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (storeData.location_info.delivery_fee == 0.0) "Free delivery"
                            else "$${String.format("%.2f", storeData.location_info.delivery_fee)} delivery fee",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCC1111)
                        )
                        Text(
                            text = "pricing & fees",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier
                            .width(1.dp)
                            .height(50.dp)
                            .align(Alignment.CenterVertically),
                        color = Color.Gray.copy(alpha = 0.3f)
                    )

                    // Delivery time
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.Gray.copy(alpha = 0.03f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = storeData.location_info.delivery_time_est,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "delivery time",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // 5. MENU CATEGORIES
                storeData.menu.forEachIndexed { index, category ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
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

                        // Horizontal scrolling items
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            category.items.forEach { item ->
                                StoreFoodCard(
                                    menuItem = item,
                                    restaurantName = storeData.brand_info.name
                                )
                            }
                        }
                    }
                }

                // Bottom padding for floating cart bar
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
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF4CAF50), Color(0xFF388E3C))
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cart icon with badge
                    Box {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        // Count badge
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

                    // Item count + total
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

                    // View Cart button
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

// MARK: - Store Food Card (matches iOS StoreFoodCardView)

@Composable
fun StoreFoodCard(
    menuItem: StoreMenuItem,
    restaurantName: String
) {
    val cartQuantity = CartManager.items
        .filter { it.dishName == menuItem.name }
        .sumOf { it.quantity }

    Column(
        modifier = Modifier.width(180.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Image with add/stepper button
        Box(
            modifier = Modifier
                .size(172.dp, 170.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = menuItem.image_url,
                contentDescription = menuItem.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            )

            if (cartQuantity == 0) {
                // Initial + button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .shadow(2.dp, CircleShape)
                        .background(Color.White, CircleShape)
                        .clickable {
                            CartManager.addItem(
                                CartItem(
                                    dishName = menuItem.name,
                                    restaurantName = restaurantName,
                                    price = menuItem.price,
                                    imageURL = menuItem.image_url
                                )
                            )
                        }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "+",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            } else {
                // Quantity stepper: [ − ] count [ + ]
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .shadow(3.dp, RoundedCornerShape(50))
                        .background(Color.White, RoundedCornerShape(50))
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Minus
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color(0xFFCC5500), CircleShape)
                            .clickable { CartManager.decrementItem(menuItem.name) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("−", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Count
                    Text(
                        text = "$cartQuantity",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.width(22.dp)
                    )

                    // Plus
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color(0xFFCC5500), CircleShape)
                            .clickable {
                                CartManager.addItem(
                                    CartItem(
                                        dishName = menuItem.name,
                                        restaurantName = restaurantName,
                                        price = menuItem.price,
                                        imageURL = menuItem.image_url
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Text(
            text = menuItem.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "$${String.format("%.2f", menuItem.price)}",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

// MARK: - Toggle Button (matches iOS ToggleBtn)

@Composable
fun ToggleBtn(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = if (isSelected) Color.White else Color.Gray,
        modifier = Modifier
            .background(if (isSelected) Color.Black else Color.Transparent, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

// MARK: - Header Circle Button (matches iOS HeaderCircleButton)

@Composable
fun HeaderCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .shadow(4.dp, CircleShape)
            .background(Color.White, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.Black
        )
    }
}