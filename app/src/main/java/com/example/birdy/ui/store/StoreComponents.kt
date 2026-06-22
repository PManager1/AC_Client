package com.example.birdy.ui.store

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// MARK: - Toggle Button (Delivery / Pickup)

@Composable
fun ToggleBtn(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = if (isSelected) Color.White else Color.Gray,
        modifier = Modifier
            .background(if (isSelected) Color.Black else Color.Transparent, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

// MARK: - Header Circle Button

@Composable
fun HeaderCircleButton(
    icon: ImageVector,
    onClick: () -> Unit = {},
    tint: Color = Color.Black
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .shadow(4.dp, CircleShape)
            .background(Color.White, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tint
        )
    }
}