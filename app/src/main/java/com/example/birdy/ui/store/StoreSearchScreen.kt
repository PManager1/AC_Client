package com.example.birdy.ui.store

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// MARK: - Store Search Screen (matches iOS StoreSearchScreen)

@Composable
fun StoreSearchScreen(
    data: StoreData,
    onBack: () -> Unit,
    onItemTap: (StoreMenuItem) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    val allItems = remember(data) {
        data.menu.flatMap { category ->
            category.items.map { item -> category.category_name to item }
        }
    }

    val filteredItems = remember(searchQuery, allItems) {
        if (searchQuery.isBlank()) emptyList()
        else allItems.filter { (_, item) ->
            item.name.contains(searchQuery, ignoreCase = true) ||
            item.description.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Search ${data.brand_info.name} menu", color = Color.Gray, fontSize = 15.sp)
                },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true
            )
        }

        // Results
        if (searchQuery.isBlank()) {
            // Show popular categories as suggestions
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Menu Categories",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(12.dp))
                data.menu.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { searchQuery = category.category_name }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.category_name,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "${category.items.size}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else if (filteredItems.isEmpty()) {
            // No results
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.LightGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No results for \"$searchQuery\"",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Try searching for something else",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        } else {
            // Show filtered results
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(filteredItems) { (categoryName, item) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemTap(item) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Item image
                        if (item.image_url.isNotEmpty()) {
                            AsyncImage(
                                model = item.image_url,
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Item info
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = item.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = categoryName,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            if (item.description.isNotEmpty()) {
                                Text(
                                    text = item.description,
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Price
                        Text(
                            text = "$${String.format("%.2f", item.price)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCC5500)
                        )
                    }
                }
            }
        }
    }
}