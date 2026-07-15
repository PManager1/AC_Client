package com.example.birdy.ui.explore

import android.content.res.Resources
import android.util.Log
import com.example.birdy.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// MARK: - Color Constants

private val BurntOrange = Color(0xFFF27836)
private val SystemGray4 = Color(0xFFC7C7CC)
private val SystemGray6 = Color(0xFFF2F2F7)
private val SpotRed = Color(0xFFF44336)
private val SpotAmber = Color(0xFFD98F00)
private val SpotBlue = Color(0xFF2196F3)

// MARK: - Data Models

data class BatchZoneData(
    val zone: String,
    val activityEvents: List<ActivityEvent>,
    var deliveryTrains: MutableList<DeliveryTrain>
)

data class ActivityEvent(
    val id: String,
    val message: String,
    val timeAgo: String
)

data class DeliveryTrain(
    val id: String,
    var targetDeliveryTime: String,
    val orderDeadline: String,
    val statusType: String?,
    var batches: MutableList<BatchItem>
)

data class BatchItem(
    val id: String,
    val restaurantName: String,
    val itemName: String,
    val originalPrice: Double,
    val discountedPrice: Double,
    var currentOrdersCount: Int,
    var targetOrdersRequired: Int,
    var imageUrl: String,
    val restaurantLogo: String,
    var brandId: String?
) {
    val spotsRemaining: Int get() = targetOrdersRequired - currentOrdersCount
    val discountPercent: Int get() = ((1.0 - discountedPrice / originalPrice) * 100).roundToInt()
    val progress: Double get() = if (targetOrdersRequired > 0) currentOrdersCount.toDouble() / targetOrdersRequired else 0.0
}

// MARK: - Main Screen

@Composable
fun NewHomeBatchScreen(onBack: () -> Unit = {}, onNavigateToStore: (String) -> Unit = {}) {
    val context = LocalContext.current
    var zoneData by remember { mutableStateOf<BatchZoneData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf("Now") }
    var scrollTarget by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val allTimes: List<String> by remember(zoneData) {
        derivedStateOf {
            val data = zoneData ?: return@derivedStateOf listOf("Now")
            listOf("Now") + data.deliveryTrains.map { it.targetDeliveryTime }
        }
    }

    LaunchedEffect(Unit) {
        loadMockData(context) { data ->
            zoneData = data
            isLoading = false
        }
    }

    LaunchedEffect(scrollTarget) {
        if (scrollTarget.isEmpty()) return@LaunchedEffect
        val idx = zoneData?.deliveryTrains?.indexOfFirst { it.targetDeliveryTime == scrollTarget } ?: -1
        if (idx >= 0) {
            listState.animateScrollToItem(idx)
        }
        scrollTarget = ""
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        if (zoneData != null) {
            val data = zoneData!!
            Column(modifier = Modifier.fillMaxSize()) {
                ZoneHeader(zone = data.zone, onBack = onBack)

                LiveInfoTicker(text = tickerText(selectedTab, data))

                TimeSlider(
                    times = allTimes,
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        selectedTab = tab
                        scrollTarget = tab
                    }
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(data.deliveryTrains, key = { it.id }) { train ->
                        TrainSectionHeader(train = train)
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            train.batches.forEach { item ->
                                BatchCard(
                                    item = item,
                                    onClick = { onNavigateToStore(item.brandId!!) }
                                )
                            }
                        }
                    }
                }
            }
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BurntOrange)
            }
        }
    }
}

// MARK: - Data Loading (coroutine-based)

private suspend fun loadMockData(context: android.content.Context, onResult: (BatchZoneData) -> Unit) {
    try {
        val json = withContext(Dispatchers.IO) {
            context.resources.openRawResource(R.raw.mock_batch_data)
                .bufferedReader().use { it.readText() }
        }
        val root = JSONObject(json)
        val zone = root.getString("zone")

        val events = root.getJSONArray("activityEvents")
        val activityEvents = (0 until events.length()).map { i ->
            val e = events.getJSONObject(i)
            ActivityEvent(e.getString("id"), e.getString("message"), e.getString("timeAgo"))
        }

        val trainsArr = root.getJSONArray("deliveryTrains")
        val trains = mutableListOf<DeliveryTrain>()
        for (ti in 0 until trainsArr.length()) {
            val t = trainsArr.getJSONObject(ti)
            val trainId = t.getString("trainId")
            val targetTime = t.getString("targetDeliveryTime")
            val deadline = t.getString("orderDeadline")
            val statusType = if (t.has("statusType")) t.getString("statusType") else null
            val batchesArr = t.getJSONArray("batches")
            val batches = mutableListOf<BatchItem>()
            for (bi in 0 until batchesArr.length()) {
                val b = batchesArr.getJSONObject(bi)
                batches.add(
                    BatchItem(
                        id = b.getString("batchId"),
                        restaurantName = b.getString("restaurantName"),
                        itemName = b.getString("itemName"),
                        originalPrice = b.getDouble("originalPrice"),
                        discountedPrice = b.getDouble("discountedPrice"),
                        currentOrdersCount = b.getInt("currentOrdersCount"),
                        targetOrdersRequired = b.getInt("targetOrdersRequired"),
                        imageUrl = b.optString("imageUrl", ""),
                        restaurantLogo = b.optString("restaurantLogo", ""),
                        brandId = if (b.has("brandId")) b.getString("brandId") else null
                    )
                )
            }
            trains.add(DeliveryTrain(id = trainId, targetDeliveryTime = targetTime, orderDeadline = deadline, statusType = statusType, batches = batches))
        }

        var data = BatchZoneData(zone = zone, activityEvents = activityEvents, deliveryTrains = trains)

        // Time round logic (same as iOS)
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, 1)
        val minute = cal.get(Calendar.MINUTE)
        if (minute <= 30) cal.set(Calendar.MINUTE, 30) else {
            cal.set(Calendar.MINUTE, 0)
            cal.add(Calendar.HOUR_OF_DAY, 1)
        }
        val baseTime = cal.time
        val timeFmt = SimpleDateFormat("h:mm a", Locale.US)

        data.deliveryTrains = data.deliveryTrains.mapIndexed { index, train ->
            val trainCal = Calendar.getInstance().apply { time = baseTime; add(Calendar.MINUTE, index * 30) }
            train.copy(targetDeliveryTime = timeFmt.format(trainCal.time))
        }.toMutableList()

        // Fetch all brands to build a name→ID mapping for navigation
        val brandNameToId = mutableMapOf<String, String>()
        try {
            val brandsUrl = URL("${Config.API_BASE_URL}/brands")
            val brandsConn = brandsUrl.openConnection() as HttpURLConnection
            brandsConn.requestMethod = "GET"
            brandsConn.connectTimeout = 5000
            brandsConn.readTimeout = 5000
            val brandsResp = brandsConn.inputStream.bufferedReader().readText()
            val brandsArr = JSONArray(brandsResp)
            for (i in 0 until brandsArr.length()) {
                val b = brandsArr.getJSONObject(i)
                val id = b.optString("id", "")
                val name = b.optString("name", "")
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    brandNameToId[name.trim().lowercase()] = id
                }
            }
        } catch (e: Exception) {
            Log.e("NewHomeBatch", "Failed to fetch brands list", e)
        }

        // Populate brandId for any batch items missing it
        for (ti in data.deliveryTrains.indices) {
            for (bi in data.deliveryTrains[ti].batches.indices) {
                val lookupKey = data.deliveryTrains[ti].batches[bi].restaurantName.trim().lowercase()
                if (data.deliveryTrains[ti].batches[bi].brandId == null && brandNameToId.containsKey(lookupKey)) {
                    data.deliveryTrains[ti].batches[bi] =
                        data.deliveryTrains[ti].batches[bi].copy(brandId = brandNameToId[lookupKey])
                }
            }
        }

        // Fetch brand minBatchOrder + images for batch items with brandId
        val uniqueBrandIds = data.deliveryTrains.flatMap { t -> t.batches.mapNotNull { it.brandId } }.distinct()
        if (uniqueBrandIds.isNotEmpty()) {
            val brandResult = withContext(Dispatchers.IO) {
                val map = mutableMapOf<String, Int>()
                val mapImages = mutableMapOf<String, String>()  // NEW
                for (brandId in uniqueBrandIds) {
                    try {
                        val url = URL("${Config.API_BASE_URL}/brands/$brandId")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "GET"
                        conn.connectTimeout = 5000
                        conn.readTimeout = 5000
                        val resp = conn.inputStream.bufferedReader().readText()
                        val obj = JSONObject(resp)
                        val minStr = obj.optString("minBatchOrder", "")
                        val minVal = minStr.toIntOrNull()
                        if (minVal != null && minVal > 0) {
                            map[brandId] = minVal
                        }
                        // Extract first carousel image or logoUrl
                        val carouselArr = obj.optJSONArray("carouselImages")
                        if (carouselArr != null && carouselArr.length() > 0) {
                            val firstImage = carouselArr.optString(0, "")
                            if (firstImage.isNotEmpty()) mapImages[brandId] = firstImage
                        } else {
                            val logo = obj.optString("logoUrl", "")
                            if (logo.isNotEmpty()) mapImages[brandId] = logo
                        }
                    } catch (e: Exception) {
                        Log.e("NewHomeBatch", "Failed to fetch brand $brandId", e)
                    }
                }
                // Return both maps
                Pair(map, mapImages)
            }
            val brandMinOrders = brandResult.first
            val brandImages = brandResult.second
            for (ti in data.deliveryTrains.indices) {
                for (bi in data.deliveryTrains[ti].batches.indices) {
                    val brandId = data.deliveryTrains[ti].batches[bi].brandId
                    if (brandId != null && brandMinOrders.containsKey(brandId)) {
                        data.deliveryTrains[ti].batches[bi] =
                            data.deliveryTrains[ti].batches[bi].copy(targetOrdersRequired = brandMinOrders[brandId]!!)
                    }
                }
            }
            // Patch imageUrl with brand's first carousel image or logo
            for (ti in data.deliveryTrains.indices) {
                for (bi in data.deliveryTrains[ti].batches.indices) {
                    val brandId = data.deliveryTrains[ti].batches[bi].brandId
                    if (brandId != null && brandImages.containsKey(brandId)) {
                        data.deliveryTrains[ti].batches[bi] =
                            data.deliveryTrains[ti].batches[bi].copy(imageUrl = brandImages[brandId]!!)
                    }
                }
            }
        }

        withContext(Dispatchers.Main) { onResult(data) }
    } catch (e: Exception) {
        Log.e("NewHomeBatch", "Failed to load mock data", e)
        withContext(Dispatchers.Main) { onResult(BatchZoneData("Error", emptyList(), mutableListOf())) }
    }
}

// MARK: - Config API Base URL (matches BirdyKit)
// Standalone copy so no BirdyKit dependency needed on AC
private object Config {
    const val API_BASE_URL = "http://10.0.2.2:8090/api/v1"
}

// MARK: - Ticker Text

private fun tickerText(tab: String, data: BatchZoneData): String {
    if (tab == "Now") {
        val first = data.deliveryTrains.firstOrNull()
        if (first != null) {
            val urgent = first.batches.count { it.spotsRemaining <= 2 }
            return "${first.targetDeliveryTime} Delivery Train · $urgent items filling fast 😍"
        }
        return "Hot batches available now 🔥"
    }
    val train = data.deliveryTrains.firstOrNull { it.targetDeliveryTime == tab }
    if (train != null) {
        val urgent = train.batches.count { it.spotsRemaining == 1 }
        if (urgent > 0) {
            return "$tab Delivery Train · $urgent batch${if (urgent == 1) "" else "es"} need just 1 more! 😍"
        }
        return "$tab Delivery Train · Join a batch & save"
    }
    return "Select a delivery time"
}

// MARK: - Zone Header

@Composable
private fun ZoneHeader(zone: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Delivering to:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
            Text("\uD83D\uDCCD $zone", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray, modifier = Modifier.size(22.dp))
    }
}

// MARK: - Live Info Ticker

@Composable
private fun LiveInfoTicker(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(SystemGray6)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(Icons.Default.Fireplace, contentDescription = null, tint = BurntOrange, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// MARK: - Time Slider Bar

@Composable
private fun TimeSlider(
    times: List<String>,
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        times.forEach { time ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onTabSelected(time) }
            ) {
                Text(
                    text = if (time == "Now") "\uD83D\uDD25 Now" else time,
                    fontSize = if (time == selectedTab) 17.sp else 15.sp,
                    fontWeight = if (time == selectedTab) FontWeight.ExtraBold else FontWeight.Normal,
                    color = if (time == selectedTab) Color.Black else Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (time == selectedTab) BurntOrange else Color.Transparent)
                )
            }
        }
    }
}

// MARK: - Train Section Header

@Composable
private fun TrainSectionHeader(train: DeliveryTrain) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${train.targetDeliveryTime} Delivery Train",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
            CountdownView(deadlineStr = train.orderDeadline, statusType = train.statusType)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.Fireplace, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
    }
}

// MARK: - Convert ISO deadline string to remaining minutes

@Composable
private fun CountdownView(deadlineStr: String, statusType: String?) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    val deadlineMs = remember(deadlineStr) {
        try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            fmt.parse(deadlineStr)?.time ?: 0L
        } catch (e: Exception) {
            try {
                val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                fmt.timeZone = TimeZone.getTimeZone("UTC")
                fmt.parse(deadlineStr)?.time ?: 0L
            } catch (e2: Exception) { 0L }
        }
    }

    val secondsRemaining = maxOf(0, (deadlineMs - now) / 1000)
    val minutesRemaining = (secondsRemaining / 60).toInt()

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp))
        Text(
            when {
                statusType == "closing_soon" -> "Ordering closing soon"
                statusType == "closing_in" -> {
                    val mins = secondsRemaining / 60
                    val secs = secondsRemaining % 60
                    "Orders closing in ${mins}:${secs.toString().padStart(2, '0')}"
                }
                minutesRemaining > 0 -> "$minutesRemaining min left"
                else -> "Ordering closed"
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (minutesRemaining > 0 || statusType != null) BurntOrange else Color.Red
        )
    }
}

// MARK: - Batch Card

@Composable
private fun BatchCard(item: BatchItem, onClick: () -> Unit = {}) {
    var showNoStoreAlert by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            if (item.brandId != null) {
                onClick()
            } else {
                showNoStoreAlert = true
            }
        },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box {
            // Image from brand carousel or placeholder
            Box(
                modifier = Modifier.fillMaxWidth().height(160.dp)
            ) {
                if (item.imageUrl.isNotEmpty()) {
                    SubcomposeAsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.restaurantName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(modifier = Modifier.fillMaxSize().background(SystemGray6), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Gray, strokeWidth = 2.dp)
                            }
                        },
                        error = {
                            Box(modifier = Modifier.fillMaxSize().background(SystemGray6), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(SystemGray6),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // Spot Progress Circle overlay
            SpotProgressCircle(
                spotsLeft = item.spotsRemaining,
                totalSpots = item.targetOrdersRequired,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp)
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                item.restaurantName,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "$${String.format("%.2f", item.originalPrice)}",
                        fontSize = 15.sp,
                        color = Color.Gray
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "$${String.format("%.2f", item.discountedPrice)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                        Text(
                            "Get ${item.discountPercent}% off",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BurntOrange,
                            modifier = Modifier
                                .background(BurntOrange.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Join Batch", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showNoStoreAlert) {
        AlertDialog(
            onDismissRequest = { showNoStoreAlert = false },
            title = { Text("Store Not Available") },
            text = { Text("This brand doesn't have a store page yet.") },
            confirmButton = { TextButton(onClick = { showNoStoreAlert = false }) { Text("OK") } }
        )
    }
}

// MARK: - Spot Progress Circle

@Composable
private fun SpotProgressCircle(
    spotsLeft: Int,
    totalSpots: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSpots > 0) spotsLeft.toFloat() / totalSpots else 0f

    val spotLabel = when (spotsLeft) {
        1 -> "1 SPOT LEFT"
        2 -> "2 SPOTS LEFT"
        else -> "$spotsLeft SPOTS LEFT"
    }

    val spotColor = when (spotsLeft) {
        1 -> SpotRed
        2 -> SpotAmber
        else -> SpotBlue
    }

    val spotColorAlpha = spotColor.copy(alpha = 0.8f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(70.dp)) {
                // Background circle
                drawCircle(color = Color.Gray.copy(alpha = 0.2f), radius = size.minDimension / 2,
                    center = Offset(size.width / 2, size.height / 2))
                // Progress arc
                drawArc(
                    color = Color(0xFFFF9800),
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = 6f, cap = StrokeCap.Round),
                    size = Size(size.width, size.height),
                    topLeft = Offset(0f, 0f)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$spotsLeft", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (spotsLeft == 1) "spot" else "spots", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
            }
        }

        Text(
            text = spotLabel,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier
                .background(spotColorAlpha, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
