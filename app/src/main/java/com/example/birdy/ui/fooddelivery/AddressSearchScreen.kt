package com.example.birdy.ui.fooddelivery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.birdy.data.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AddressSuggestion(
    val id: String,
    val description: String
)

data class AddressSearchResult(
    val street: String,
    val cityStateZip: String,
    val latitude: Double,
    val longitude: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSearchScreen(
    onDismiss: () -> Unit,
    onAddressSelected: (street: String, cityStateZip: String, latitude: Double, longitude: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchText by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Address",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search bar
            OutlinedTextField(
                value = searchText,
                onValueChange = { text ->
                    searchText = text
                    searchJob?.cancel()
                    if (text.length >= 3) {
                        searchJob = scope.launch {
                            delay(300)
                            isLoading = true
                            val results = withContext(Dispatchers.IO) {
                                fetchSuggestions(text)
                            }
                            suggestions = results
                            isLoading = false
                        }
                    } else {
                        suggestions = emptyList()
                    }
                },
                placeholder = { Text("Enter street address...", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = {
                            searchText = ""
                            suggestions = emptyList()
                            searchJob?.cancel()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedBorderColor = Color(0xFFD95F02),
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = Color(0xFFD95F02)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Results
            if (isLoading) {
                Text(
                    text = "Searching...",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else if (suggestions.isNotEmpty()) {
                LazyColumn {
                    items(suggestions) { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            geocodePlace(suggestion)
                                        }
                                        if (result != null) {
                                            onAddressSelected(result.street, result.cityStateZip, result.latitude, result.longitude)
                                            onDismiss()
                                        }
                                    }
                                }
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = suggestion.description,
                                    fontSize = 15.sp,
                                    color = Color.Black
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else if (searchText.length >= 3) {
                Text(
                    text = "No results found",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                Text(
                    text = "Start typing to find an address",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

private fun fetchSuggestions(text: String): List<AddressSuggestion> {
    return try {
        val url = URL("https://maps.googleapis.com/maps/api/place/autocomplete/json?input=${java.net.URLEncoder.encode(text, "UTF-8")}&key=${Config.GOOGLE_API_KEY}&language=en&components=country:us")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        val json = connection.inputStream.bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        if (obj.optString("status") == "OK") {
            val predictions = obj.optJSONArray("predictions") ?: JSONArray()
            (0 until predictions.length()).map { i ->
                val pred = predictions.getJSONObject(i)
                AddressSuggestion(
                    id = pred.optString("place_id", ""),
                    description = pred.optString("description", "")
                )
            }
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        println("❌ [AddressSearch] Fetch error: ${e.message}")
        emptyList()
    }
}

private fun geocodePlace(suggestion: AddressSuggestion): AddressSearchResult? {
    return try {
        val url = URL("https://maps.googleapis.com/maps/api/geocode/json?place_id=${suggestion.id}&key=${Config.GOOGLE_API_KEY}")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        val json = connection.inputStream.bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        if (obj.optString("status") == "OK") {
            val results = obj.optJSONArray("results")
            if (results != null && results.length() > 0) {
                val first = results.getJSONObject(0)
                val geometry = first.getJSONObject("geometry")
                val location = geometry.getJSONObject("location")
                val lat = location.optDouble("lat", 0.0)
                val lng = location.optDouble("lng", 0.0)
                val parts = suggestion.description.split(", ")
                val street = parts.firstOrNull() ?: suggestion.description
                val cityStateZip = parts.drop(1).joinToString(", ")
                AddressSearchResult(street, cityStateZip, lat, lng)
            } else null
        } else null
    } catch (e: Exception) {
        println("❌ [AddressSearch] Geocode error: ${e.message}")
        null
    }
}