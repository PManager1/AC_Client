// Matches iOS Home/PizzaHome.swift

package com.example.birdy.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.birdy.ui.explore.NewFoodCard
import com.example.birdy.ui.explore.NewFoodRestaurant

// MARK: - Mock Pizza Data (matches iOS PizzaHome.swift)

private val mockPizzaPlaces = listOf(
    NewFoodRestaurant(
        id = "pizza-0",
        restaurantName = "Pizza Hut",
        logoURL = null,
        images = listOf(
            "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=600",
            "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=600",
            "https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=600"
        ),
        rating = 4.3,
        reviewCount = 520,
        distance = 0.5,
        deliveryTime = 18,
        deliveryFee = 0.0,
        promoText = "Free Delivery",
        isSponsored = true,
        itemName = "Large Pepperoni",
        itemPrice = 11.99
    ),
    NewFoodRestaurant(
        id = "pizza-2",
        restaurantName = "Papa John's",
        logoURL = null,
        images = listOf(
            "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=600",
            "https://images.unsplash.com/photo-1571407970349-bc81e7e96d47?w=600",
            "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=600"
        ),
        rating = 4.3,
        reviewCount = 380,
        distance = 0.7,
        deliveryTime = 22,
        deliveryFee = 0.0,
        promoText = "20% Off First Order",
        isSponsored = false,
        itemName = "The Works",
        itemPrice = 14.99
    ),
    NewFoodRestaurant(
        id = "pizza-3",
        restaurantName = "Little Caesars",
        logoURL = null,
        images = listOf(
            "https://images.unsplash.com/photo-1588315029754-2dd089d39a1a?w=600",
            "https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=600",
            "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=600"
        ),
        rating = 4.0,
        reviewCount = 290,
        distance = 1.0,
        deliveryTime = 25,
        deliveryFee = 1.99,
        promoText = "Hot-N-Ready \$5.99",
        isSponsored = false,
        itemName = "Crazy Bread Combo",
        itemPrice = 7.99
    ),
    NewFoodRestaurant(
        id = "pizza-1",
        restaurantName = "Mario's Pizzeria",
        logoURL = null,
        images = listOf(
            "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=600",
            "https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=600",
            "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=600"
        ),
        rating = 4.4,
        reviewCount = 230,
        distance = 0.9,
        deliveryTime = 23,
        deliveryFee = 0.0,
        promoText = "Free Delivery",
        isSponsored = false,
        itemName = "Margherita",
        itemPrice = 12.99
    ),
    NewFoodRestaurant(
        id = "pizza-4",
        restaurantName = "Luigi's Wood Fire",
        logoURL = null,
        images = listOf(
            "https://images.unsplash.com/photo-1604382355076-af4b0eb60143?w=600",
            "https://images.unsplash.com/photo-1593560708920-61dd98c46a4e?w=600",
            "https://images.unsplash.com/photo-1528137871618-79d2761e3fd5?w=600",
            "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=600"
        ),
        rating = 4.7,
        reviewCount = 567,
        distance = 1.5,
        deliveryTime = 35,
        deliveryFee = 2.99,
        promoText = "Buy 1 Get 1 Free",
        isSponsored = false,
        itemName = "Truffle Mushroom",
        itemPrice = 18.99
    )
)

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
    val filters = listOf("All", "Nearest", "Top Rated", "\$0 Delivery", "Deals")

    val filteredPlaces = remember(selectedFilter) {
        when (selectedFilter) {
            "Nearest" -> mockPizzaPlaces.sortedBy { it.distance }
            "Top Rated" -> mockPizzaPlaces.sortedByDescending { it.rating }
            "\$0 Delivery" -> mockPizzaPlaces.filter { it.deliveryFee == 0.0 }
            "Deals" -> mockPizzaPlaces.filter { !it.promoText.isNullOrEmpty() }
            else -> mockPizzaPlaces
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
                    imageVector = Icons.Default.ArrowBack,
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

        // Quick Filters (horizontal scroll)
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
                    action = { selectedFilter = filter }
                )
            }
        }

        // Restaurant Cards
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp)
        ) {
            filteredPlaces.forEach { item ->
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