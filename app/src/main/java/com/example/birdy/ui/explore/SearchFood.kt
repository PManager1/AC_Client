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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.birdy.data.BrandSuggestion
import com.example.birdy.data.RecentSearchEntry
import com.example.birdy.data.SearchApi
import com.example.birdy.data.SearchStatus
import com.example.birdy.data.VisitedBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Matches iOS SearchFood.swift (udo3 search API via SearchApi)

private val suggestedSearches = listOf("Pizza", "Sushi", "Burgers", "Chicken", "Mexican", "Italian", "Chinese", "Thai", "Indian", "BBQ")

@Composable
fun SearchFoodScreen(
    onBack: () -> Unit = {},
    onBrandClick: (brandId: String, brandType: String) -> Unit = { _, _ -> },
    onSeeMore: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }
    var brandSuggestions by remember { mutableStateOf<List<BrandSuggestion>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val recentSearches = remember { mutableStateListOf<RecentSearchEntry>() }
    var visitedBrands by remember { mutableStateOf<List<VisitedBrand>>(emptyList()) }
    var isOpeningBrand by remember { mutableStateOf(false) }

    // Loads recent searches and recently visited stores once. The server is the
    // source of truth, so "Clear All" in See More sticks.
    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) { SearchApi.fetchHistory() }
        when (result.status) {
            SearchStatus.SUCCESS -> {
                recentSearches.clear()
                recentSearches.addAll(result.recentSearches)
                visitedBrands = result.visitedBrands
            }
            // Session expired / offline: keep whatever is shown locally.
            SearchStatus.AUTH_ERROR -> println("❌ [SearchFood] load history: not authenticated")
            SearchStatus.NETWORK_ERROR -> println("❌ [SearchFood] load history failed")
        }
    }

    // Debounced typeahead: every keystroke restarts this effect, cancelling the previous run.
    LaunchedEffect(searchText) {
        val query = SearchApi.cleanQuery(searchText)
        if (query.length < 2) {
            brandSuggestions = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        // Keep the current results on screen while the next ones load (no flicker).
        isSearching = true
        delay(250)
        val result = withContext(Dispatchers.IO) { SearchApi.fetchBrands(query) }
        // Drop responses for text the user has already changed.
        if (result != null && result.query != SearchApi.cleanQuery(searchText)) return@LaunchedEffect
        // On a failed request, keep the previous results.
        brandSuggestions = result?.brands ?: brandSuggestions
        isSearching = false
    }

    // Records a submitted search and/or a visited brand. Only on submit or tap, never while typing.
    // Local lists update immediately; the POST is fire-and-forget.
    fun saveHistory(query: String?, brand: BrandSuggestion?) {
        val cleaned = query?.let { SearchApi.cleanQuery(it) }?.takeIf { it.length >= 2 }
        if (cleaned != null) {
            recentSearches.removeAll { it.query.equals(cleaned, ignoreCase = true) }
            recentSearches.add(0, RecentSearchEntry(cleaned, 1))
            while (recentSearches.size > 10) recentSearches.removeAt(recentSearches.size - 1)
        }
        if (brand != null) {
            val entry = VisitedBrand(brand.id, brand.name, brand.logoUrl, brand.tags, brandType = brand.brandType)
            visitedBrands = (listOf(entry) + visitedBrands.filter { it.brandId != brand.id }).take(20)
        }
        if (cleaned == null && brand == null) return
        scope.launch(Dispatchers.IO) { SearchApi.recordHistory(cleaned, brand?.id) }
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
                    onSearch = { saveHistory(searchText, null) }
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
                onVisitedClick = { visited ->
                    if (!isOpeningBrand) {
                        isOpeningBrand = true
                        scope.launch {
                            val brandType = withContext(Dispatchers.IO) {
                                SearchApi.resolveBrandType(visited.brandId, visited.brandType)
                            }
                            isOpeningBrand = false
                            saveHistory(null, BrandSuggestion(visited.brandId, visited.brandName, visited.logoUrl, visited.tags, brandType))
                            onBrandClick(visited.brandId, brandType)
                        }
                    }
                },
                onSeeMore = onSeeMore
            )
        } else {
            ResultsList(
                searchText = searchText,
                brandSuggestions = brandSuggestions,
                isSearching = isSearching,
                onBrandClick = { brand ->
                    saveHistory(searchText, brand)
                    onBrandClick(brand.id, brand.brandType)
                }
            )
        }
    }
}

@Composable
private fun EmptyStateView(
    recentSearches: List<RecentSearchEntry>,
    visitedBrands: List<VisitedBrand> = emptyList(),
    onSearchClick: (String) -> Unit = {},
    onVisitedClick: (VisitedBrand) -> Unit = {},
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
                            onClick = { onVisitedClick(brand) }
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
private fun ResultsList(
    searchText: String,
    brandSuggestions: List<BrandSuggestion>,
    isSearching: Boolean,
    onBrandClick: (BrandSuggestion) -> Unit = {}
) {
    if (brandSuggestions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isSearching) {
                CircularProgressIndicator(color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Searching for \"$searchText\"...",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.Gray.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No results found for \"$searchText\"",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
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
            if (brand.tags.isNotEmpty()) {
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
                text = brand.tags.take(3).joinToString(" • "),
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
