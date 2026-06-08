package com.example.birdy.ui.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.birdy.data.AuthManager
import com.example.birdy.data.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun SeaMoreScreen(
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var recentSearches by remember { mutableStateOf<List<RecentSearchEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        recentSearches = loadRecentSearches()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with back button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        HorizontalDivider()

        if (recentSearches.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color.Gray.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.size(20.dp))
                Text(
                    text = "No recent searches",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Your recent searches will appear here",
                    fontSize = 14.sp,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Header with Clear All
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        TextButton(onClick = {
                            scope.launch { clearAllSearches() }
                            recentSearches = emptyList()
                        }) {
                            Text(
                                text = "Clear All",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                        }
                    }
                }

                items(recentSearches) { entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.size(20.dp))
                        Text(
                            text = entry.query,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 60.dp))
                }
            }
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
        android.util.Log.e("SeaMore", "Failed to load recent searches", e)
        emptyList()
    }
}

private suspend fun clearAllSearches() = withContext(Dispatchers.IO) {
    try {
        val url = URL("${Config.API_BASE_URL}/users/search-food-history")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "DELETE"
        conn.setRequestProperty("Content-Type", "application/json")
        AuthManager.getToken()?.let {
            conn.setRequestProperty("Authorization", "Bearer $it")
        }
        val code = conn.responseCode
        android.util.Log.d("SeaMore", "Clear All returned $code")
    } catch (e: Exception) {
        android.util.Log.e("SeaMore", "Failed to clear searches", e)
    }
}
