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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.birdy.data.AuthManager
import com.example.birdy.data.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

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

private data class BrandSuggestion(
    val id: String,
    val name: String,
    val logoUrl: String,
    val tags: List<String>?
)

data class RecentSearchEntry(
    val query: String,
    val count: Int
)

private data class VisitedBrand(
    val brandId: String,
    val brandName: String,
    val logoUrl: String,
    val tags: List<String>?
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

private val suggestedSearches = listOf("Chicken nuggets", "Candy", "Blueberry muffin")

@Composable
fun SearchFoodScreen(
    onBack: () -> Unit = {},
    onRestaurantClick: (String) -> Unit = {},
    onBrandClick: (String) -> Unit = {},
    onSeeMore: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }
    var brandSuggestions by remember { mutableStateOf<List<BrandSuggestion>>(emptyList()) }
    val recentSearches = remember { mutableStateListOf<RecentSearchEntry>() }
    var visitedBrands by remember { mutableStateOf<List<VisitedBrand>>(emptyList()) }

    val results = remember(searchText) {
        if (searchText.isEmpty()) {
            emptyList()
        } else {
            mockRestaurants.filter { it.name.lowercase().contains(searchText.lowercase()) }
        }
    }

    LaunchedEffect(Unit) {
        val loaded = loadRecentSearches()
        recentSearches.clear()
        recentSearches.addAll(loaded)
        visitedBrands = loadRecentlyVisitedBrands()
    }

    LaunchedEffect(searchText) {
        if (searchText.length >= 2) {
            brandSuggestions = fetchBrandSuggestions(searchText)
        } else {
            brandSuggestions = emptyList()
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        val trimmed = searchText.trim()
                        if (trimmed.isNotEmpty()) {
                            recentSearches.removeAll { it.query == trimmed }
                            recentSearches.add(0, RecentSearchEntry(trimmed, 1))
                            if (recentSearches.size > 10) {
                                recentSearches.removeAt(recentSearches.size - 1)
                            }
                            scope.launch { saveSearchQuery(trimmed) }
                        }
                    }
                ),
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
                recentSearches = recentSearches,
                visitedBrands = visitedBrands,
                onSearchClick = { searchText = it },
                onBrandClick = { brandId -> onBrandClick(brandId) },
                onSeeMore = onSeeMore
            )
        } else {
            ResultsList(
                brandSuggestions = brandSuggestions,
                results = results,
                onBrandClick = { brand ->
                    val trimmed = searchText.trim()
                    if (trimmed.isNotEmpty()) {
                        recentSearches.removeAll { it.query == trimmed }
                        recentSearches.add(0, RecentSearchEntry(trimmed, 1))
                        if (recentSearches.size > 10) {
                            recentSearches.removeAt(recentSearches.size - 1)
                        }
                        scope.launch { saveSearchQuery(trimmed) }
                    }
                    visitedBrands = visitedBrands.filter { it.brandId != brand.id } + VisitedBrand(
                        brandId = brand.id, brandName = brand.name, logoUrl = brand.logoUrl, tags = brand.tags
                    )
                    scope.launch { saveRecentlyVisitedBrand(brand) }
                    onBrandClick(brand.id)
                },
                onResultClick = { restaurant -> onRestaurantClick(restaurant.id.toString()) }
            )
        }
    }
}

private suspend fun loadRecentSearches(): List<RecentSearchEntry> = withContext(Dispatchers.IO) {
    try {
        val url = URL("${Config.API_BASE_URL}/users/search-food-history")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Content-Type", "application/json")
        AuthManager.getToken()?.let {
            conn.setRequestProperty("Authorization", "Bearer $it")
        }
        val json = conn.inputStream.bufferedReader().readText()
        val obj = JSONObject(json)
        val arr = obj.getJSONArray("searches")
        (0 until arr.length()).map { i ->
            val item = arr.getJSONObject(i)
            RecentSearchEntry(
                query = item.getString("query"),
                count = item.optInt("count", 0)
            )
        }
    } catch (e: Exception) {
        android.util.Log.e("SearchFood", "Failed to load recent searches", e)
        emptyList()
    }
}

private suspend fun saveSearchQuery(query: String) = withContext(Dispatchers.IO) {
    try {
        val url = URL("${Config.API_BASE_URL}/users/search-food-history")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        AuthManager.getToken()?.let {
            conn.setRequestProperty("Authorization", "Bearer $it")
        }
        conn.doOutput = true
        val body = JSONObject().apply { put("query", query) }
        conn.outputStream.write(body.toString().toByteArray())
        conn.responseCode
    } catch (e: Exception) {
        android.util.Log.e("SearchFood", "Failed to save search query", e)
    }
}

private suspend fun loadRecentlyVisitedBrands(): List<VisitedBrand> = withContext(Dispatchers.IO) {
    try {
        val url = URL("${Config.API_BASE_URL}/users/recently-visited-brands")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Content-Type", "application/json")
        AuthManager.getToken()?.let {
            conn.setRequestProperty("Authorization", "Bearer $it")
        }
        val json = conn.inputStream.bufferedReader().readText()
        val obj = JSONObject(json)
        val arr = obj.getJSONArray("brands")
        (0 until arr.length()).map { i ->
            val item = arr.getJSONObject(i)
            val tags = mutableListOf<String>()
            val tagsArr = item.optJSONArray("tags")
            if (tagsArr != null) {
                for (j in 0 until tagsArr.length()) {
                    tags.add(tagsArr.getString(j))
                }
            }
            VisitedBrand(
                brandId = item.getString("brandId"),
                brandName = item.getString("brandName"),
                logoUrl = item.optString("logoUrl", ""),
                tags = tags
            )
        }
    } catch (e: Exception) {
        android.util.Log.e("SearchFood", "Failed to load recently visited brands", e)
        emptyList()
    }
}

private suspend fun saveRecentlyVisitedBrand(brand: BrandSuggestion) = withContext(Dispatchers.IO) {
    try {
        val url = URL("${Config.API_BASE_URL}/users/recently-visited-brands")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        AuthManager.getToken()?.let {
            conn.setRequestProperty("Authorization", "Bearer $it")
        }
        conn.doOutput = true
        val tagsArr = JSONArray()
        brand.tags?.forEach { tagsArr.put(it) }
        val body = JSONObject().apply {
            put("brandId", brand.id)
            put("brandName", brand.name)
            put("logoUrl", brand.logoUrl)
            put("tags", tagsArr)
        }
        conn.outputStream.write(body.toString().toByteArray())
        conn.responseCode
    } catch (e: Exception) {
        android.util.Log.e("SearchFood", "Failed to save recently visited brand", e)
    }
}

private suspend fun fetchBrandSuggestions(query: String): List<BrandSuggestion> = withContext(Dispatchers.IO) {
    try {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("${Config.API_BASE_URL}/brands/search-by-tag?q=$encoded")
        val conn = url.openConnection() as HttpURLConnection
        val json = conn.inputStream.bufferedReader().readText()
        val obj = JSONObject(json)
        val arr = obj.getJSONArray("brands")
        (0 until arr.length()).map { i ->
            val item = arr.getJSONObject(i)
            val tags = mutableListOf<String>()
            val tagsArr = item.optJSONArray("tags")
            if (tagsArr != null) {
                for (j in 0 until tagsArr.length()) {
                    tags.add(tagsArr.getString(j))
                }
            }
            BrandSuggestion(
                id = item.getString("id"),
                name = item.getString("name"),
                logoUrl = item.optString("logoUrl", ""),
                tags = tags
            )
        }
    } catch (e: Exception) {
        android.util.Log.e("SearchFood", "Failed to fetch brand suggestions", e)
        emptyList()
    }
}

@Composable
private fun EmptyStateView(
    recentSearches: List<RecentSearchEntry>,
    visitedBrands: List<VisitedBrand> = emptyList(),
    onSearchClick: (String) -> Unit = {},
    onBrandClick: (String) -> Unit = {},
    onSeeMore: () -> Unit = {}
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
                        color = Color.Black,
                        modifier = Modifier.clickable { onSeeMore() }
                    )
                }
                recentSearches.forEach { entry ->
                    SearchRow(
                        icon = Icons.Default.AccessTime,
                        text = entry.query,
                        onClick = { onSearchClick(entry.query) }
                    )
                }
            }
        }

        // Recently Visited Stores
        if (visitedBrands.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    Text(
                        text = "Recently Visited Stores",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    visitedBrands.forEach { brand ->
                        VisitedBrandRow(
                            brand = brand,
                            onClick = { onBrandClick(brand.brandId) }
                        )
                    }
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
private fun VisitedBrandRow(
    brand: VisitedBrand,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (brand.logoUrl.isNotEmpty()) {
            AsyncImage(
                model = brand.logoUrl,
                contentDescription = brand.brandName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(15.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = brand.brandName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            if (!brand.tags.isNullOrEmpty()) {
                Text(
                    text = brand.tags.take(3).joinToString(" • "),
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.5f)
        )
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
    brandSuggestions: List<BrandSuggestion> = emptyList(),
    results: List<SearchRestaurant>,
    onBrandClick: (BrandSuggestion) -> Unit = {},
    onResultClick: (SearchRestaurant) -> Unit = {}
) {
    if (brandSuggestions.isEmpty() && results.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.Gray.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Searching...",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(brandSuggestions) { brand ->
                BrandSearchResultRow(
                    brand = brand,
                    onClick = { onBrandClick(brand) }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 85.dp))
            }
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
private fun BrandSearchResultRow(
    brand: BrandSuggestion,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (brand.logoUrl.isNotEmpty()) {
            AsyncImage(
                model = brand.logoUrl,
                contentDescription = brand.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(15.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = brand.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = brand.tags?.take(3)?.joinToString(" • ") ?: "",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.5f)
        )
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
