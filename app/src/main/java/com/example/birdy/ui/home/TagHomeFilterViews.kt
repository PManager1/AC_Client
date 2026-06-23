package com.example.birdy.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

// MARK: - TagFilterCapsule (matches iOS TagFilterCapsule)

@Composable
fun TagFilterCapsule(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(25.dp))
            .background(if (isActive) Color(0xFFFF9500) else Color(0xFFF2F2F7))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (isActive) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) Color.White else Color.Black
            )
        }
    }
}

// MARK: - Delivery Fee Filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryFeeFilterSheet(
    currentSpeed: String?,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(currentSpeed ?: "Lowest") }
    val options = listOf("Lowest", "In between \$10–\$15", "Over \$15")

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(Color.White)
        ) {
            Text(
                text = "Delivery Fee",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(20.dp))

            options.forEach { option ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected == option) Color(0xFFFF9500).copy(alpha = 0.15f) else Color(0xFFF2F2F7))
                        .clickable { selected = option }
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                        if (selected == option) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFFFF9500)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onApply(selected) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500))
            ) {
                Text("View Results", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
        }
    }
}

// MARK: - Schedule Filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleFilterSheet(
    currentSlot: String?,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val allSlots = listOf(
        "11:00 AM", "11:30 AM", "12:00 PM", "12:30 PM",
        "1:00 PM", "1:30 PM", "2:00 PM", "2:30 PM",
        "3:00 PM", "3:30 PM", "4:00 PM", "4:30 PM",
        "5:00 PM", "5:30 PM", "6:00 PM", "6:30 PM",
        "7:00 PM", "7:30 PM", "8:00 PM", "8:30 PM", "9:00 PM"
    )

    var selectedDayIdx by remember { mutableStateOf(0) }
    var selectedSlot by remember { mutableStateOf<String?>(currentSlot) }

    val dayFormatter = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    val dayOptions = remember {
        val cal = Calendar.getInstance()
        (0 until 7).map { offset ->
            val date = cal.clone() as Calendar
            date.add(Calendar.DAY_OF_YEAR, offset)
            val label = when (offset) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> dayFormatter.format(date.time)
            }
            Triple(date, label, dateFormatter.format(date.time))
        }
    }

    // Filter past slots for today
    val filteredSlots = remember(selectedDayIdx) {
        if (selectedDayIdx == 0) {
            val now = Calendar.getInstance()
            val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
            allSlots.filter { slot ->
                val slotDate = fmt.parse(slot)
                if (slotDate != null) {
                    val cal = Calendar.getInstance().apply { time = slotDate }
                    val slotMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                    slotMinutes > nowMinutes
                } else true
            }
        } else allSlots
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(Color.White)
        ) {
            Text(
                text = "Schedule Order",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Day strip
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                dayOptions.forEachIndexed { idx, (_, label, sublabel) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedDayIdx == idx) Color(0xFFFF9500).copy(alpha = 0.15f) else Color(0xFFF2F2F7))
                            .clickable { selectedDayIdx = idx; selectedSlot = null }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (selectedDayIdx == idx) FontWeight.Bold else FontWeight.Normal,
                            color = Color.Black
                        )
                        Text(
                            text = sublabel,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time slot list — vertical, centered
            if (filteredSlots.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No more slots available today",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please select a different day above",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 260.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredSlots) { slot ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedSlot == slot) Color(0xFFFF9500).copy(alpha = 0.1f) else Color.White)
                                .clickable { selectedSlot = slot }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = slot,
                                    fontSize = 16.sp,
                                    fontWeight = if (selectedSlot == slot) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.Black
                                )
                                if (selectedSlot == slot) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9500)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    selectedSlot?.let { onApply(it) }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSlot != null && filteredSlots.isNotEmpty()) Color(0xFFFF9500) else Color.Gray
                ),
                enabled = selectedSlot != null && filteredSlots.isNotEmpty()
            ) {
                Text("View Results", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
        }
    }
}

// MARK: - Ratings Filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingsFilterSheet(
    currentRating: Double?,
    onApply: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderIndex by remember { mutableStateOf(0f) }
    val ratingValues = listOf(0.0, 4.5, 4.6, 4.9)
    val ratingLabels = listOf("Any", "4.5", "4.6", "4.9")

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(Color.White)
        ) {
            Text(
                text = "Minimum Rating",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Current value
            Text(
                text = ratingLabels[sliderIndex.toInt()],
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFF9500),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Slider
            Slider(
                value = sliderIndex,
                onValueChange = { sliderIndex = it },
                valueRange = 0f..3f,
                steps = 2,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF9500),
                    activeTrackColor = Color(0xFFFF9500)
                )
            )

            // Step labels
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ratingLabels.forEachIndexed { i, label ->
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = if (sliderIndex.toInt() == i) Color(0xFFFF9500) else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onApply(ratingValues[sliderIndex.toInt()]) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500))
            ) {
                Text("View Results", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
        }
    }
}

// MARK: - Price Filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceFilterSheet(
    currentTier: Int?,
    onApply: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTier by remember { mutableStateOf(currentTier ?: 2) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(Color.White)
        ) {
            Text(
                text = "Price Range",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("$", "$$", "$$$").forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedTier == index + 1,
                        onClick = { selectedTier = index + 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = Color(0xFFFF9500)
                        )
                    ) {
                        Text(label, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (selectedTier) {
                    1 -> "Budget-friendly options"
                    2 -> "Moderate pricing"
                    3 -> "Premium selection"
                    else -> ""
                },
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onApply(selectedTier) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500))
            ) {
                Text("View Results", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
        }
    }
}
