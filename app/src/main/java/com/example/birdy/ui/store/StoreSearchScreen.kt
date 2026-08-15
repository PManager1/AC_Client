package com.example.birdy.ui.store

import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// MARK: - Store Search Screen (mirrors iOS StoreSearch.swift redesign)

private val BurntOrange = Color(0xFFCC5500)

@Composable
fun StoreSearchScreen(
    storeId: String,
    storeName: String,
    categories: List<StoreMenuCategory>,
    onBack: () -> Unit,
    onItemTap: (StoreMenuItem) -> Unit = {},
    onCategoryClick: (StoreMenuCategory) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val allItems = remember(categories) {
        categories.flatMap { it.items }
    }
    val trimmed = searchQuery.trim()
    val hasQuery = trimmed.length >= 2

    val exactMatches = remember(trimmed, allItems) {
        if (!hasQuery) emptyList()
        else allItems.filter { it.name.contains(trimmed, ignoreCase = true) }
    }
    val relatedMatches = remember(trimmed, exactMatches, allItems) {
        if (!hasQuery) emptyList()
        else allItems.filter { item ->
            !exactMatches.contains(item) && item.description.contains(trimmed, ignoreCase = true)
        }
    }

    // Recent searches (store-scoped, local persistence)
    val prefs = remember { context.getSharedPreferences("store_search_prefs", Context.MODE_PRIVATE) }
    var recentQueries by remember { mutableStateOf(loadRecent(prefs, storeId)) }

    val commitQuery: (String) -> Unit = { term ->
        val t = term.trim()
        if (t.isNotEmpty()) {
            val updated = (listOf(t) + recentQueries).distinct().take(5)
            recentQueries = updated
            saveRecent(prefs, storeId, updated)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Search bar with back button + scope chip
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
                    Text("Search $storeName...", color = Color.Gray, fontSize = 15.sp)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        if (storeName.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .background(BurntOrange.copy(alpha = 0.12f), RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "In $storeName",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BurntOrange
                                )
                            }
                        }
                    }
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { commitQuery(searchQuery) }),
                singleLine = true
            )
        }

        if (hasQuery) {
            ResultsBody(
                query = trimmed,
                storeName = storeName,
                exactMatches = exactMatches,
                relatedMatches = relatedMatches,
                onItemTap = onItemTap
            )
        } else {
            EmptyStateBody(
                categories = categories,
                recentQueries = recentQueries,
                onRecentSelect = { term ->
                    searchQuery = term
                },
                onClearRecent = {
                    recentQueries = clearRecent(prefs, storeId)
                },
                onCategoryClick = onCategoryClick,
                onTrendingSelect = { term ->
                    searchQuery = term
                    commitQuery(term)
                }
            )
        }
    }
}

// MARK: - Results body

@Composable
private fun ResultsBody(
    query: String,
    storeName: String,
    exactMatches: List<StoreMenuItem>,
    relatedMatches: List<StoreMenuItem>,
    onItemTap: (StoreMenuItem) -> Unit
) {
    if (exactMatches.isEmpty() && relatedMatches.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
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
                text = "No results for \"$query\" in $storeName",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        if (exactMatches.isNotEmpty()) {
            item("header-exact") {
                SectionHeader("Matches for \"$query\"")
            }
            items(chunked2(exactMatches)) { row ->
                GridRow(row, storeName, onItemTap)
            }
        }

        if (relatedMatches.isNotEmpty()) {
            item("header-related") {
                SectionHeader("You might also like")
            }
            items(chunked2(relatedMatches)) { row ->
                GridRow(row, storeName, onItemTap)
            }
        }
        item("bottom-space") {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
    )
}

private fun chunked2(items: List<StoreMenuItem>): List<List<StoreMenuItem>> {
    return items.chunked(2)
}

@Composable
private fun GridRow(
    row: List<StoreMenuItem>,
    storeName: String,
    onItemTap: (StoreMenuItem) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        row.forEach { item ->
            StoreFoodCard(
                menuItem = item,
                restaurantName = storeName,
                onItemTap = { onItemTap(item) },
                modifier = Modifier.weight(1f),
                descriptionColor = Color.Black
            )
        }
        if (row.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

// MARK: - Empty state body

@Composable
private fun EmptyStateBody(
    categories: List<StoreMenuCategory>,
    recentQueries: List<String>,
    onRecentSelect: (String) -> Unit,
    onClearRecent: () -> Unit,
    onCategoryClick: (StoreMenuCategory) -> Unit,
    onTrendingSelect: (String) -> Unit
) {
    val trending = remember(categories) { deriveTrending(categories) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(vertical = 16.dp)
    ) {
        if (recentQueries.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Searches",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Clear",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.clickable { onClearRecent() }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ChipRow(
                items = recentQueries,
                withIcon = true,
                onSelect = onRecentSelect,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (categories.isNotEmpty()) {
            Text(
                text = "Shop by category",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categories.chunked(2).forEach { rowCats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowCats.forEach { category ->
                            CategoryChip(
                                category = category,
                                onClick = { onCategoryClick(category) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowCats.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (trending.isNotEmpty()) {
            Text(
                text = "Trending this week",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            ChipRow(
                items = trending,
                withIcon = false,
                onSelect = onTrendingSelect,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun CategoryChip(
    category: StoreMenuCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(BurntOrange.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon(category.category_name),
                contentDescription = null,
                tint = BurntOrange,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = category.category_name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ChipRow(
    items: List<String>,
    withIcon: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { term ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFF5F5F5))
                    .clickable { onSelect(term) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (withIcon) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = term,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }
        }
    }
}

// MARK: - Recent searches persistence (store-scoped, local)
private fun recentKey(storeId: String) = "storeSearch.recent.$storeId"

private fun loadRecent(prefs: SharedPreferences, storeId: String): List<String> {
    val set = prefs.getStringSet(recentKey(storeId), null) ?: return emptyList()
    return set.toList()
}

private fun saveRecent(prefs: SharedPreferences, storeId: String, list: List<String>) {
    val capped = list.take(5)
    prefs.edit().putStringSet(recentKey(storeId), LinkedHashSet(capped)).apply()
}

private fun clearRecent(prefs: SharedPreferences, storeId: String): List<String> {
    prefs.edit().remove(recentKey(storeId)).apply()
    return emptyList()
}

// MARK: - Trending (derived from menu keywords, mock fallback)
private fun deriveTrending(categories: List<StoreMenuCategory>): List<String> {
    val freq = HashMap<String, Int>()
    categories.forEach { cat ->
        cat.items.forEach { item ->
            item.name.split(Regex("[^A-Za-z]")).forEach { word ->
                if (word.length >= 4) {
                    val lower = word.lowercase()
                    freq[lower] = (freq[lower] ?: 0) + 1
                }
            }
        }
    }
    val derived = freq.entries
        .sortedByDescending { it.value }
        .take(5)
        .map { it.key.replaceFirstChar { c -> c.uppercase() } }
    return if (derived.isNotEmpty()) derived else listOf("Bananas", "Oat Milk", "Eggs", "Bread", "Chicken")
}
