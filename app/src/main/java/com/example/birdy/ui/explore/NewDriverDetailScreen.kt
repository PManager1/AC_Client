package com.example.birdy.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

private val PrimaryGreen = Color(0xFF008000)
private val SystemGray6 = Color(0xFFF2F2F7)

// MARK: - Constants

private object DriverRouteConstants {
    val pickup = "Chinatown Restaurant Hub"
    val dropoff = "GWU Zone Hotspot (Thurston Hall Front Loop)"
}

@Composable
fun NewDriverDetailScreen(entry: RestaurantEntry, onBack: () -> Unit = {}) {
    val formattedDeadline = remember {
        val fmt = SimpleDateFormat("h:mm a", Locale.US)
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, 30)
        fmt.format(cal.time)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(entry.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "$${entry.payoutValue} payout",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Itinerary Stop 1: Pickup
            ItineraryStopCard(
                icon = "\uD83D\uDCCD",
                title = "Stop 1: Pickup Hub",
                subtitle = DriverRouteConstants.pickup,
                detail = "Must arrive by: $formattedDeadline",
                accentColor = Color(0xFFFF9800)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Arrow
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Itinerary Stop 2: Dropoff
            ItineraryStopCard(
                icon = "\uD83D\uDCCD",
                title = "Stop 2: Coordinated Drop-off",
                subtitle = DriverRouteConstants.dropoff,
                detail = "4 Stacked Orders",
                accentColor = PrimaryGreen
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Note about map (placeholder)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SystemGray6)
            ) {
                Text(
                    "Map view coming soon. The route goes from Chinatown Hub to GWU drop-off zone.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// MARK: - Itinerary Stop Card

@Composable
private fun ItineraryStopCard(
    icon: String,
    title: String,
    subtitle: String,
    detail: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SystemGray6, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(icon, fontSize = 24.sp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
            Text(detail, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}
