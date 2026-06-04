package com.example.birdy.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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

private val OrangeSecNavyBlue = Color(0xFF1B2A4A)
private val BurntOrange = Color(0xFFCC5500)
private val OrangeSec2 = Color(0xFF8E8E93)
private val BackgroundGrey = Color(0xFFF2F2F7)

// Matches iOS Notifications.swift
@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {}
) {
    var emailNotificationsEnabled by remember { mutableStateOf(true) }
    var appOnlyNotificationsEnabled by remember { mutableStateOf(false) }
    var hasChanges by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // Back Button Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 16.dp, end = 16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OrangeSecNavyBlue
                )
            }
        }

        // Header
        Text(
            text = "Notifications",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = OrangeSecNavyBlue,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Instructions
        Text(
            text = "Choose how you would like to receive notifications about your bookings and account activity.",
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = OrangeSec2,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = BackgroundGrey)

        Spacer(modifier = Modifier.height(16.dp))

        // Notifications Options Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundGrey, RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            // Email Notifications Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email",
                    tint = if (emailNotificationsEnabled) BurntOrange else Color.Gray,
                    modifier = Modifier.weight(0.1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Email Notifications",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OrangeSecNavyBlue,
                    modifier = Modifier.weight(0.7f)
                )
                Switch(
                    checked = emailNotificationsEnabled,
                    onCheckedChange = { newValue ->
                        emailNotificationsEnabled = newValue
                        if (!newValue && !appOnlyNotificationsEnabled) {
                            appOnlyNotificationsEnabled = true
                        }
                        hasChanges = (emailNotificationsEnabled != true || appOnlyNotificationsEnabled != false)
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = BurntOrange,
                        checkedThumbColor = Color.White
                    )
                )
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // App Only Notifications Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "App Only",
                    tint = if (appOnlyNotificationsEnabled) BurntOrange else Color.Gray,
                    modifier = Modifier.weight(0.1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "App Only Notifications",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OrangeSecNavyBlue,
                    modifier = Modifier.weight(0.7f)
                )
                Switch(
                    checked = appOnlyNotificationsEnabled,
                    onCheckedChange = { newValue ->
                        appOnlyNotificationsEnabled = newValue
                        if (!newValue && !emailNotificationsEnabled) {
                            emailNotificationsEnabled = true
                        }
                        hasChanges = (emailNotificationsEnabled != true || appOnlyNotificationsEnabled != false)
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = BurntOrange,
                        checkedThumbColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Save Button
        Button(
            onClick = {
                println("Saving preferences: Email: $emailNotificationsEnabled, App Only: $appOnlyNotificationsEnabled")
                hasChanges = false
            },
            enabled = hasChanges,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hasChanges) BurntOrange else Color.Gray.copy(alpha = 0.5f),
                disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = "Save Changes",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}