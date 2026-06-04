package com.example.birdy.ui.account

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val OrangeSecNavyBlue = Color(0xFF1B2A4A)
private val OrangeTitle = Color(0xFFF27836)
private val OrangeSec5 = Color(0xFFF5F0EB)
private val OrangeSec6 = Color(0xFFE5E5EA)
private val OrangeSec7 = Color(0xFF1C1C1E)
private val OrangeSec2 = Color(0xFF8E8E93)

// Matches iOS Settings pattern (Settings button in Account)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
    onNavigateToBugReporter: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OrangeSec5)
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
            text = "Settings",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = OrangeSecNavyBlue,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Account Section
        SettingsSectionHeader(title = "Account")
        SettingsCard {
            SettingsRow(
                title = "Edit Profile",
                icon = Icons.Default.Person,
                showDivider = true,
                onClick = { /* TODO: navigate to edit profile */ }
            )
            SettingsRow(
                title = "Privacy",
                icon = Icons.Default.PrivacyTip,
                showDivider = true,
                onClick = { /* TODO: navigate to privacy */ }
            )
            SettingsRow(
                title = "Security",
                icon = Icons.Default.Lock,
                showDivider = false,
                onClick = { /* TODO: navigate to security */ }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Preferences Section
        SettingsSectionHeader(title = "Preferences")
        SettingsCard {
            SettingsRow(
                title = "Notifications",
                icon = Icons.Default.Notifications,
                showDivider = true,
                onClick = onNavigateToNotifications
            )
            SettingsRow(
                title = "Language",
                icon = Icons.Default.Info,
                showDivider = true,
                onClick = onNavigateToLanguage
            )
            SettingsRow(
                title = "Dark Mode",
                icon = Icons.Default.DarkMode,
                showDivider = false,
                onClick = { /* TODO: toggle dark mode */ }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Support Section
        SettingsSectionHeader(title = "Support")
        SettingsCard {
            SettingsRow(
                title = "Bug Reporter",
                icon = Icons.Default.Info,
                showDivider = true,
                onClick = onNavigateToBugReporter
            )
            SettingsRow(
                title = "Data & Storage",
                icon = Icons.Default.Storage,
                showDivider = false,
                onClick = { /* TODO: navigate to data & storage */ }
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = OrangeSecNavyBlue
        )
    }
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    icon: ImageVector,
    showDivider: Boolean = true,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = OrangeTitle,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = OrangeSec7,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = OrangeSec2,
                modifier = Modifier.size(20.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = OrangeSec6,
                thickness = 1.dp,
                modifier = Modifier.padding(start = 50.dp)
            )
        }
    }
}