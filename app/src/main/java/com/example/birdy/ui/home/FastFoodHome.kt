// Matches iOS Home/FastFoodHome.swift

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
import com.example.birdy.ui.explore.NewFoodCard
import com.example.birdy.ui.explore.NewFoodRestaurant

// MARK: - Mock Fast Food Data (matches iOS FastFoodHome.swift)

private val mockFastFoodPlaces = listOf(
    NewFoodRestaurant(
        id = "fastfood-0",
        restaurantName = "McDonald's",
        logoURL = null,
        images = listOf(
            "https://images.unsplash.com/photo-1561758033-d89a9ad46330?w=600",
            "https://images.unsplash.com/photo-1550547660-d9450f859349?w=600",
            "https://images.unsplash.com/photo-1587314168485-3236d6710814?w=600"
        ),
        rating = 4.1,
        reviewCount = 1200,
        distance = 0.3,
        deliveryTime = 15,
        deliveryFee = 0.0,
        promoText = "Free Delivery",
        isSponsored = true,
        itemName = "Big Mac Meal",
        itemPrice = 9.99
    ),
    NewFoodRestaurant(
        id = "fastfood-1",
        restaurantName = "Burger King",
        logoURL = null,
        images = listOf(
            "https://images.unsplash.com/photo-1550547660-d9450f859349?w=600",
            "https://images.unsplash.com/photo-1561758033-d89a9ad46330?w=600",
            "https://images.unsplash.com/photo-1587314168485-3236d6710814?w=600"
        ),
        rating = 4.0,
        reviewCount = 890,
        distance = 0.6,
        deliveryTime = 18,
        deliveryFee = 0.0,
        promoText = "2 Whoppers for \$10",
        isSponsored = false,
        itemName = "Whopper Meal",
        itemPrice = 8.49
    ),
    NewFoodRestaurant(
        id = "fastfood-2",
        restaurantName = "Wendy's",
        logoURL = null,
        images = listOf(
            "https://images.unsplash.com/photo-1587314168485-3236d6710814?w=600",
            "https://images.unsplash.com/photo-1561758033-d89a9ad46330?w=600",
            "https://images.unsplash.com/photo-1550547660-d9450f859349?w=600"
        ),
        rating = 4.2,
        reviewCount = 650,
        distance = 0.8,
        deliveryTime = 20,
        deliveryFee = 1.49,
        promoText = "Free Frosty w/ Order",
        isSponsored = false,
        itemName = "Dave's Single",
        itemPrice = 7.99
    ),
    NewFoodRestaurant(
        id = "fastfood-3",
        restaurantName = "Chick-fil-A",
        logoURL = null,
        images = listOf(
            "https://images.unsplash.com/photo-1626645738196-c2a7c87a8f58?w=600",
            "https://images.unsplash.com/photo-1561758033-d89a9ad46330?w=600",
            "https://images.unsplash.com/photo-1587314168485-3236d6710814?w=600"
        ),
        rating = 4.5,
        reviewCount = 980,
        distance = 1.2,
        deliveryTime = 22,
        deliveryFee = 0.0,
        promoText = "Free Delivery",
        isSponsored = false,
        itemName = "Chicken Sandwich",
        itemPrice = 6.89
    ),
    NewFoodRestaurant(
        id = "fastfood-4",
        restaurantName = "Taco Bell",
        logoURL = null,
        images = listOf(
            "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=600",
            "https://images.unsplash.com/photo-1550547660-d9450f859349?w=600",
            "https://images.unsplash.com/photo-1561758033-d89a9ad46330?w=600"
        ),
        rating = 3.9,
        reviewCount = 750,
        distance = 0.9,
        deliveryTime = 17,
        deliveryFee = 0.99,
        promoText = "Cravings Box \$5.99",
        isSponsored = false,
        itemName = "Crunchwrap Supreme",
        itemPrice = 5.49
    )
)

// MARK: - Fast Food Home Screen (matches iOS FastFoodHome.swift)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastFoodHomeScreen(
    onBack: () -> Unit = {},
    onRestaurantClick: (String) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Nearest", "Top Rated", "\$0 Delivery", "Deals")

    val filteredPlaces = remember(selectedFilter) {
        when (selectedFilter) {
            "Nearest" -> mockFastFoodPlaces.sortedBy { it.distance }
            "Top Rated" -> mockFastFoodPlaces.sortedByDescending { it.rating }
            "\$0 Delivery" -> mockFastFoodPlaces.filter { it.deliveryFee == 0.0 }
            "Deals" -> mockFastFoodPlaces.filter { !it.promoText.isNullOrEmpty() }
            else -> mockFastFoodPlaces
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
                text = "Fast Food",
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