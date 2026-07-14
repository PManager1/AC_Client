package com.example.birdy.ui.explore

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HandGesture
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

// MARK: - Color Constants

private val DarkGreen = Color(0xFF008000)
private val LightGreen = Color(0xFFD4EDDA)
private val LateOrange = Color(0xFFFF9800)
private val CardGray = Color(0xFFF5F5F5)
private val CardDarkGreen = Color(0xFF006400)
private val PrimaryGreen = Color(0xFF008000)

// MARK: - Data Models

enum class CardState { Available, Loading, Locked }

enum class ReleaseReason(val label: String) {
    TRAFFIC("Running late / Stuck in traffic"),
    EMERGENCY("Emergency / Car trouble"),
    BREAK("Taking a break")
}

data class RestaurantEntry(
    val id: String = UUID.randomUUID().toString(),
    val time: String,
    val name: String,
    val subtitle: String,
    val offer: String,
    val timeNdistance: String = "20 min (2.2 mi) total",
    var orderIndex: Int = 0
) {
    val payoutValue: Int get() = offer.replace("$", "").toIntOrNull() ?: 0
}

// MARK: - Main Screen

@Composable
fun NewDriverScreen(
    onBack: () -> Unit = {},
    onNavigateToDetail: (RestaurantEntry) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var restaurants by remember { mutableStateOf(listOf<RestaurantEntry>()) }
    var activeRuns by remember { mutableStateOf(listOf<RestaurantEntry>()) }
    var cardStates by remember { mutableStateOf(mapOf<String, CardState>()) }
    var cardDeadlines by remember { mutableStateOf(mapOf<String, Long>()) }
    var isLoading by remember { mutableStateOf(true) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var releasingEntry by remember { mutableStateOf<RestaurantEntry?>(null) }
    var showReleaseSheet by remember { mutableStateOf(false) }

    fun getDeadline(id: String): Long = cardDeadlines[id] ?: System.currentTimeMillis()

    fun isLate(id: String): Boolean {
        if (cardStates[id] != CardState.Locked) return false
        return (getDeadline(id) - now) < 600_000
    }

    fun isSafeRelease(id: String): Boolean {
        if (cardStates[id] != CardState.Locked) return true
        return (getDeadline(id) - now) > 1_200_000
    }

    fun releaseCardById(id: String) {
        cardStates = cardStates + (id to CardState.Available)
        cardDeadlines = cardDeadlines - id
        val entry = activeRuns.find { it.id == id } ?: return
        val idx = restaurants.indexOfFirst { it.orderIndex > entry.orderIndex }
        activeRuns = activeRuns.filter { it.id != id }
        restaurants = if (idx >= 0) restaurants.toMutableList().apply { add(idx, entry) }
        else restaurants + entry
    }

    // Timer tick
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
            // Auto-release expired entries
            val expired = activeRuns.filter { it.id in cardDeadlines && cardDeadlines[it.id]!! < now }
            for (entry in expired) {
                releaseCardById(entry.id)
            }
        }
    }

    // Load data
    LaunchedEffect(Unit) {
        loadRestaurants(context) { result ->
            restaurants = result
            isLoading = false
        }
    }

    val pipelineOrderCount = max(restaurants.size + activeRuns.size, 4)
    val pipelineTotalEst = (restaurants.take(4) + activeRuns).take(4).sumOf { it.payoutValue }
    val pipelineProgress = pipelineOrderCount.toFloat() / 10f

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }

                item {
                    Text(
                        "Available Matches",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                item { PipelineBanner(count = restaurants.size, est = pipelineTotalEst, progress = pipelineProgress) }

                if (activeRuns.isNotEmpty()) {
                    item {
                        Text(
                            "Active Runs (${activeRuns.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(activeRuns, key = { it.id }) { entry ->
                        ActiveRunCard(
                            entry = entry,
                            minutesLeft = max(0, ((getDeadline(entry.id) - now) / 60000).toInt()),
                            isLate = isLate(entry.id),
                            onRelease = {
                                releasingEntry = entry
                                showReleaseSheet = true
                            },
                            onClick = { onNavigateToDetail(entry) }
                        )
                    }
                }

                item {
                    Text(
                        "Matches in the pipeline: ${restaurants.size}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                itemsIndexed(restaurants, key = { _, e -> e.id }) { index, entry ->
                    val state = cardStates[entry.id] ?: CardState.Available
                    DriverJobCard(
                        entry = entry,
                        state = state,
                        isGreyed = index > 1,
                        onClaim = {
                            scope.launch {
                                cardStates = cardStates + (entry.id to CardState.Loading)
                                delay(800)
                                val deadline = System.currentTimeMillis() + 30 * 60 * 1000
                                cardStates = cardStates + (entry.id to CardState.Locked)
                                cardDeadlines = cardDeadlines + (entry.id to deadline)
                                val idx = restaurants.indexOfFirst { it.id == entry.id }
                                if (idx >= 0) {
                                    val claimed = restaurants[idx]
                                    activeRuns = activeRuns + claimed
                                    restaurants = restaurants.toMutableList().apply { removeAt(idx) }
                                }
                            }
                        },
                        onClick = { onNavigateToDetail(entry) }
                    )

                    if (index == 1) {
                        SecondPipelineBanner(progress = pipelineProgress)
                    }
                }

                item { Spacer(modifier = Modifier.height(30.dp)) }
            }
        }
    }

    if (showReleaseSheet && releasingEntry != null) {
        ReleaseSheetDialog(
            entry = releasingEntry!!,
            isSafeRelease = isSafeRelease(releasingEntry!!.id),
            onKeep = {
                releasingEntry = null
                showReleaseSheet = false
            },
            onConfirm = {
                releaseCardById(releasingEntry!!.id)
                releasingEntry = null
                showReleaseSheet = false
            }
        )
    }
}

// MARK: - Data Loading

private suspend fun loadRestaurants(context: android.content.Context, onResult: (List<RestaurantEntry>) -> Unit) {
    try {
        val json = withContext(Dispatchers.IO) {
            try {
                context.resources.openRawResource(
                    context.resources.getIdentifier("driver_restaurants_mock", "raw", context.packageName)
                ).bufferedReader().use { it.readText() }
            } catch (e: Exception) { null }
        }

        val result = if (json != null) {
            try {
                val obj = JSONObject(json)
                val arr = obj.getJSONArray("restaurants")
                (0 until arr.length()).map { i ->
                    val item = arr.getJSONObject(i)
                    RestaurantEntry(
                        time = item.getString("time"),
                        name = item.getString("name"),
                        subtitle = item.getString("subtitle"),
                        offer = item.getString("offer"),
                        timeNdistance = item.optString("timeNdistance", "20 min (2.2 mi) total")
                    )
                }
            } catch (e: Exception) { fallbackRestaurants() }
        } else {
            fallbackRestaurants()
        }

        withContext(Dispatchers.Main) {
            onResult(result.mapIndexed { index, entry -> entry.copy(orderIndex = index) })
        }
    } catch (e: Exception) {
        Log.e("NewDriver", "Failed to load restaurants", e)
        withContext(Dispatchers.Main) {
            onResult(fallbackRestaurants().mapIndexed { index, entry -> entry.copy(orderIndex = index) })
        }
    }
}

private fun fallbackRestaurants() = listOf(
    RestaurantEntry(time = "3:00 PM", name = "Chic-fil-A", subtitle = "ChinaTown → GW University", offer = "$30"),
    RestaurantEntry(time = "3:40 PM", name = "Shake shack", subtitle = "ChinaTown → GW University", offer = "$31"),
    RestaurantEntry(time = "4:20 PM", name = "Chinese Food", subtitle = "ChinaTown → GW University", offer = "$32"),
    RestaurantEntry(time = "5:00 PM", name = "Taco Bell", subtitle = "ChinaTown → GW University", offer = "$30"),
    RestaurantEntry(time = "5:40 PM", name = "Tony Cheng's", subtitle = "ChinaTown → GW University", offer = "$35"),
    RestaurantEntry(time = "6:20 PM", name = "Subway", subtitle = "ChinaTown → Logan Circle", offer = "$52"),
    RestaurantEntry(time = "7:00 PM", name = "Pizza Hut", subtitle = "ChinaTown → Logan Circle", offer = "$40"),
    RestaurantEntry(time = "7:40 PM", name = "Raising Cane", subtitle = "ChinaTown → Logan Circle", offer = "$40"),
    RestaurantEntry(time = "8:20 PM", name = "Raising Cane", subtitle = "ChinaTown → Navy Yard", offer = "$40"),
    RestaurantEntry(time = "9:00 PM", name = "Daikaya", subtitle = "ChinaTown → Navy Yard", offer = "$40"),
    RestaurantEntry(time = "8:20 PM", name = "Chipotle", subtitle = "ChinaTown → GW University", offer = "$40"),
    RestaurantEntry(time = "9:00 PM", name = "Chipotle", subtitle = "ChinaTown → Navy Yard", offer = "$40"),
    RestaurantEntry(time = "9:40 PM", name = "KFC", subtitle = "ChinaTown → Navy Yard", offer = "$40"),
    RestaurantEntry(time = "10:00 PM", name = "Chipotle", subtitle = "ChinaTown → NOMA", offer = "$40"),
    RestaurantEntry(time = "10:20 PM", name = "Succotash Prime", subtitle = "ChinaTown → NOMA", offer = "$40"),
    RestaurantEntry(time = "11:00 PM", name = "KFC", subtitle = "ChinaTown → NOMA", offer = "$40"),
    RestaurantEntry(time = "11:30 PM", name = "Tonari", subtitle = "ChinaTown → NOMA", offer = "$40"),
    RestaurantEntry(time = "12:00 PM", name = "KFC", subtitle = "ChinaTown → NOMA", offer = "$40")
)

// MARK: - Pipeline Banner

@Composable
private fun PipelineBanner(count: Int, est: Int, progress: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .background(Color.Gray.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            "$count matches remaining · Est. \$$est available",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(Color.Gray.copy(alpha = 0.2f), cornerRadius = CornerRadius(3f, 3f))
                drawRoundRect(
                    DarkGreen,
                    cornerRadius = CornerRadius(3f, 3f),
                    size = Size(size.width * progress, size.height)
                )
            }
        }
    }
}

@Composable
private fun SecondPipelineBanner(progress: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .background(Color.Gray.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            "20 meals are on the way 🍳 - will be ready 🔥 in just a few minutes!",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(Color.Gray.copy(alpha = 0.2f), cornerRadius = CornerRadius(3f, 3f))
                drawRoundRect(
                    DarkGreen,
                    cornerRadius = CornerRadius(3f, 3f),
                    size = Size(size.width * progress, size.height)
                )
            }
        }
    }
}

// MARK: - Driver Job Card

@Composable
private fun DriverJobCard(
    entry: RestaurantEntry,
    state: CardState,
    isGreyed: Boolean,
    onClaim: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.time, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(entry.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(entry.timeNdistance, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(entry.subtitle, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "High Demand",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF006600),
                        modifier = Modifier
                            .background(LightGreen, shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    "$${entry.payoutValue}",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (state) {
                CardState.Available -> {
                    if (isGreyed) {
                        Text(
                            "Claim \$${entry.payoutValue}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(vertical = 14.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Button(
                            onClick = onClaim,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreen)
                        ) {
                            Text("Claim \$${entry.payoutValue}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                CardState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Claim \$${entry.payoutValue}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
                CardState.Locked -> { /* Hidden in available list when locked */ }
            }
        }
    }
}

// MARK: - Active Run Card

@Composable
private fun ActiveRunCard(
    entry: RestaurantEntry,
    minutesLeft: Int,
    isLate: Boolean,
    onRelease: () -> Unit,
    onClick: () -> Unit
) {
    val deadlineFmt = remember(entry) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, 30)
        SimpleDateFormat("h:mm a", Locale.US).format(cal.time)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.time, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(entry.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(entry.subtitle, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Matched",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF006600),
                        modifier = Modifier
                            .background(Color(0xFFD9F5DC), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    "$${entry.payoutValue}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Reserved (Arrive by $deadlineFmt)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF404040), RoundedCornerShape(12.dp))
                    .padding(vertical = 14.dp),
                textAlign = TextAlign.Center
            )

            if (isLate) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Traffic Delay Detected: You must arrive in $minutesLeft min to keep this batch.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onRelease,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Release Match", fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

// MARK: - Release Sheet Dialog

@Composable
private fun ReleaseSheetDialog(
    entry: RestaurantEntry,
    isSafeRelease: Boolean,
    onKeep: () -> Unit,
    onConfirm: (ReleaseReason) -> Unit
) {
    var selectedReason by remember { mutableStateOf<ReleaseReason?>(null) }

    Dialog(
        onDismissRequest = onKeep,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    "Are you sure you want to release?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isSafeRelease) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Green.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.HandGesture, contentDescription = null, tint = Color.Green)
                        Text(
                            "No problem! Releasing this early gives other drivers plenty of time to match.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Green.copy(alpha = 0.8f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LateOrange.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = LateOrange)
                        Text(
                            "Warning: This train leaves Chinatown soon. Releasing late may temporarily lower your priority for future high-paying pipeline drops.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LateOrange.copy(alpha = 0.9f)
                        )
                    }
                }

                Text("Select a reason:", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                ReleaseReason.entries.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(reason.label, fontSize = 17.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onKeep,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Keep Route", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { selectedReason?.let { onConfirm(it) } },
                        enabled = selectedReason != null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedReason != null) Color.Red else Color.Gray
                        )
                    ) {
                        Text("Confirm Release", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
