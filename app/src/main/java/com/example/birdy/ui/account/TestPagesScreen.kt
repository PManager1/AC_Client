package com.example.birdy.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color constants
private val OrangeSecNavyBlue = Color(0xFF1B2A4A)
private val OrangeTitle = Color(0xFFF27836)
private val OrangeSec5 = Color(0xFFF5F0EB)
private val OrangeSec6 = Color(0xFFE5E5EA)
private val OrangeSec7 = Color(0xFF1C1C1E)
private val OrangeSec2 = Color(0xFF8E8E93)

/**
 * Test Pages Screen — mirrors iOS IC/Birdy/SettingsView.swift
 * Shows a list of test page links, with "ChatView" at the top.
 * Tapping a link navigates to the corresponding test screen.
 */
@Composable
fun TestPagesScreen(
    onBack: () -> Unit = {},
    onNavigateToChatView: () -> Unit = {},
    onNavigateToNewHomeBatch: () -> Unit = {},
    onNavigateToNewDriver: () -> Unit = {}
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
            text = "Settings Test pages",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = OrangeSecNavyBlue,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Test Pages Section
        Surface(
            shape = RoundedCornerShape(15.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column {
                // ChatView — at the top, matches iOS SettingsView
                TestPageRow(
                    title = "ChatView",
                    showDivider = true,
                    onClick = onNavigateToChatView
                )
                TestPageRow(
                    title = "NewHomeBatch",
                    showDivider = true,
                    onClick = onNavigateToNewHomeBatch
                )
                TestPageRow(
                    title = "NewDriver",
                    showDivider = false,
                    onClick = onNavigateToNewDriver
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun TestPageRow(
    title: String,
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
                imageVector = Icons.Default.ChevronRight,
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