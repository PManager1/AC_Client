package com.example.birdy.ui.inbox

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Matches iOS RequestDetail.swift
@Composable
fun RequestDetailScreen(
    onBack: () -> Unit = {}
) {
    var showEarningsDetails by remember { mutableStateOf(false) }
    var showUpfrontFareDetails by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // MARK: - Top Navigation and Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "Trip Details",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { /* TODO: open help */ }) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "Help",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        HorizontalDivider()

        // MARK: - Trip Info and Price
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "U-DOX • Sep 21, 2025 • 5:10 PM",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )
            Text(
                text = "$13.56",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Upfront fare: $9.56",
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray
            )
        }

        // MARK: - Tip Notification Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF2F2F7),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Tip",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "You earned more for this trip because your rider left a tip. Sweet!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // MARK: - Map View Placeholder
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFE0E0E0),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Start",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(80.dp)
                            .background(Color.Blue)
                            .padding(start = 10.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.PinDrop,
                        contentDescription = "End",
                        tint = Color.Red,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(start = 10.dp)
                    )
                }
            }
        }

        // MARK: - Trip Metrics
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Column {
                Text("Duration", fontSize = 15.sp, color = Color.Gray)
                Text("11 min 14 sec", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("Distance", fontSize = 15.sp, color = Color.Gray)
                Text("2.06 mi", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // MARK: - Trip Addresses
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color.Black, RoundedCornerShape(5.dp))
                    )
                    Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.Black))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Jefferson Dr SW, Washington, DC, US",
                    fontSize = 15.sp
                )
            }

            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.Black, RoundedCornerShape(5.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "14th St SE, Washington, DC, US",
                    fontSize = 15.sp
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // MARK: - Earnings and Points Section
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.PinDrop,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "14th St SE, Washington, DC, US",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("3 points earned", fontSize = 17.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("$4.00 tip included", fontSize = 17.sp, modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE0E0E0)
                ) {
                    Text(
                        text = "Send thanks for tip",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { /* TODO */ }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // MARK: - Your Earnings Section
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Text("Your earnings", fontSize = 22.sp, fontWeight = FontWeight.Bold)

            // Fare row (expandable)
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEarningsDetails = !showEarningsDetails },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fare", modifier = Modifier.weight(1f))
                    Text("$9.56")
                    Icon(
                        imageVector = if (showEarningsDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showEarningsDetails) "Collapse" else "Expand",
                        modifier = Modifier.size(16.dp)
                    )
                }
                AnimatedVisibility(visible = showEarningsDetails) {
                    Text(
                        text = "Fare breakdown details go here.",
                        fontSize = 15.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }

            // Tip row
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Tip", modifier = Modifier.weight(1f))
                Text("$4.00")
            }

            HorizontalDivider()

            // Total
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Your earnings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
                Text("$13.56", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }

            // Weekly breakdown link
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF2F2F7)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Tap to see the weekly breakdown of customer price and U-DO service fees at drivers.u-do.com",
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // MARK: - Upfront Fare Section
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showUpfrontFareDetails = !showUpfrontFareDetails },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Upfront Fare: $9.56",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (showUpfrontFareDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (showUpfrontFareDetails) "Collapse" else "Expand",
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = showUpfrontFareDetails) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 20.dp)
                ) {
                    Text("Upfront Fare", fontSize = 22.sp, fontWeight = FontWeight.Bold)

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Fare", modifier = Modifier.weight(1f))
                        Text("$9.56")
                    }

                    HorizontalDivider()

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Total", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                        Text("$9.56", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}