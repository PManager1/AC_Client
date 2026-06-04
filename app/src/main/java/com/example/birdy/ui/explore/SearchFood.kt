package com.example.birdy.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Matches iOS SearchFood.swift

private data class SearchRestaurant(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val itemName: String,
    val itemPrice: Double,
    val rating: Double,
    val distance: String,
    val deliveryTime: String
)

private val mockRestaurants = listOf(
    SearchRestaurant(
        id = 1,
        name = "American Best Wings and Pizza",
        imageUrl = "https://storage.googleapis.com/birdyimages/__App/Chicken-Wings.webp",
        itemName = "10 Pcs wings",
        itemPrice = 14.99,
        rating = 4.3,
        distance = "2.7 mi",
        deliveryTime = "34 min"
    ),
    SearchRestaurant(
        id = 2,
        name = "Burger Palace",
        imageUrl = "https://storage.googleapis.com/birdyimages/__App/Burger2.jpg",
        itemName = "Double Cheeseburger",
        itemPrice = 12.49,
        rating = 4.7,
        distance = "1.2 mi",
        deliveryTime = "22 min"
    ),
    SearchRestaurant(
        id = 3,
        name = "Popeyes Louisiana Kitchen",
        imageUrl = "https://storage.googleapis.com/birdyimages/__App/Popeyes-B1.webp",
        itemName = "Classic Chicken Sandwich",
        itemPrice = 7.99,
        rating = 3.8,
        distance = "1.8 mi",
        deliveryTime = "35 min"
    ),
    SearchRestaurant(
        id = 4,
        name = "Tacos El Paso",
        imageUrl = "https://img.taste.com.au/R_dRdL7V/taste/2022/09/healthy-tacos-recipe-181113-1.jpg",
        itemName = "Burrito Bowl",
        itemPrice = 11.99,
        rating = 4.1,
        distance = "3.5 mi",
        deliveryTime = "28 min"
    ),
    SearchRestaurant(
        id = 5,
        name = "Chinese Wok Express",
        imageUrl = "https://tb-static.uber.com/prod/image-proc/processed_images/4c6c06fde277821132a9868113564d7b/c67fc65e9b4e16a553eb7574fba090f1.jpeg",
        itemName = "Spicy Chicken Tenders",
        itemPrice = 8.99,
        rating = 4.5,
        distance = "2.1 mi",
        deliveryTime = "30 min"
    )
)

private val recentSearches = listOf("Popeyes Louisiana Kitchen", "Fast Food", "Chinese", "Argentine")
private val suggestedSearches = listOf("Chicken nuggets", "Candy", "Blueberry muffin")

@Composable
fun SearchFoodScreen(
    onBack: () -> Unit = {},
    onRestaurantClick: (String) -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }
    val results = remember(searchText) {
        if (searchText.isEmpty()) {
            emptyList()
        } else {
            mockRestaurants.filter { it.name.lowercase().contains(searchText.lowercase()) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // MARK: - Search Bar with Back Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .background(Color(0xFFF2F2F7), RoundedCornerShape(25.dp))
                .padding(horizontal = 8.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(18.dp)
                )
            }
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = {
                    Text("Search U-Do", color = Color.Gray, fontSize = 17.sp)
                },
                singleLine = true,
                textStyle = TextStyle(fontSize = 17.sp, color = Color.Black),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = Color.Black
                ),
                modifier = Modifier.weight(1f)
            )
            if (searchText.isNotEmpty()) {
                IconButton(
                    onClick = { searchText = "" },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        modifier = Modifier.size(18.dp),
                        tint = Color.Gray
                    )
                }
            }
        }

        HorizontalDivider()

        // MARK: - Content
        if (searchText.isEmpty()) {
            EmptyStateView(
                onSearchClick = { searchText = it }
            )
        } else {
            ResultsList(
                results = results,
                onResultClick = { restaurant -> onRestaurantClick(restaurant.id.toString()) }
            )
        }
    }
}

@Composable
private fun EmptyStateView(
    onSearchClick: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(25.dp)
    ) {
        // Recent Searches
        item {
            Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Searches",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "See More",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                recentSearches.forEach { item ->
                    SearchRow(
                        icon = Icons.Default.AccessTime,
                        text = item,
                        onClick = { onSearchClick(item) }
                    )
                }
            }
        }

        // Suggested Searches
        item {
            Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Text(
                    text = "Suggested Searches",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                suggestedSearches.forEach { item ->
                    SearchRow(
                        icon = Icons.Default.Search,
                        text = item,
                        onClick = { onSearchClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.Black
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color.Black
        )
    }
}

@Composable
private fun ResultsList(
    results: List<SearchRestaurant>,
    onResultClick: (SearchRestaurant) -> Unit = {}
) {
    if (results.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No results found",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Try a different search term",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(results) { item ->
                SearchResultRow(
                    restaurant = item,
                    onClick = { onResultClick(item) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 85.dp))
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    restaurant: SearchRestaurant,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Restaurant image
        AsyncImage(
            model = restaurant.imageUrl,
            contentDescription = restaurant.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF2F2F7))
        )

        Spacer(modifier = Modifier.width(15.dp))

        // Restaurant info
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = restaurant.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Text(
                text = restaurant.itemName,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = Color(0xFFFF9500)
                )
                Text(
                    text = String.format("%.1f", restaurant.rating),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(text = "•", fontSize = 12.sp, color = Color.Gray)
                Text(
                    text = restaurant.distance,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}