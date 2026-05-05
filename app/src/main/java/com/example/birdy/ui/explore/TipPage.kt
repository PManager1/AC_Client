package com.example.birdy.ui.explore

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.birdy.data.CartManager
import kotlinx.coroutines.delay

// MARK: - Tip Page (full screen, matches iOS TipPage.swift)

@Composable
fun TipPage(
    onBack: () -> Unit,
    onTipSelected: (Double) -> Unit,
    subtotal: Double = CartManager.subtotal
) {
    var customTipText by remember { mutableStateOf(String.format("%.2f", subtotal * 0.20)) }
    var selectedPercentage by remember { mutableIntStateOf(20) }
    val focusRequester = remember { FocusRequester() }

    val currentTip = customTipText.toDoubleOrNull() ?: 0.0
    val newTotal = subtotal + currentTip

    // Auto-focus the text field after a short delay (matches iOS onAppear behavior)
    LaunchedEffect(Unit) {
        delay(400)
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ── Top Bar: ← Back | "Add a Tip" | Continue ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFFCC5500)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Add a Tip",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = {
                val value = customTipText.toDoubleOrNull()
                if (value != null && value >= 0) {
                    onTipSelected(value)
                }
                onBack()
            }) {
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCC5500)
                )
            }
        }

        // ── Scrollable content ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Custom Tip Input Card ──
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = Color(0xFFCC5500)
                    )
                    Text(
                        text = "Custom Amount",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF2F2F7), RoundedCornerShape(20.dp))
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    OutlinedTextField(
                        value = customTipText,
                        onValueChange = { customTipText = it },
                        placeholder = {
                            Text(
                                text = "0.00",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray.copy(alpha = 0.5f)
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            cursorColor = Color(0xFFCC5500)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                    )
                }
            }

            // ── Quick Tips ──
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick Tips",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(listOf(10, 15, 20, 25)) { percent ->
                        val isSelected = selectedPercentage == percent
                        val tipValue = subtotal * percent / 100.0

                        Column(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    if (isSelected) Color(0xFFCC5500) else Color(0xFFF2F2F7),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    selectedPercentage = percent
                                    customTipText = String.format("%.2f", tipValue)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$percent%",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.Black
                            )
                            Text(
                                text = "$${String.format("%.2f", tipValue)}",
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray
                            )
                        }
                    }
                }
            }

            // ── Recommended Tip (20%) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFCC5500).copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .clickable {
                        selectedPercentage = 20
                        customTipText = String.format("%.2f", subtotal * 0.20)
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFCC5500),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Recommended",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCC5500)
                    )
                    Text(
                        text = "20% • $${String.format("%.2f", subtotal * 0.20)}",
                        fontSize = 15.sp,
                        color = Color.Gray
                    )
                }
            }

            // ── New Total Summary ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F2F7), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal", fontSize = 17.sp, color = Color.Gray)
                    Text("$${String.format("%.2f", subtotal)}", fontSize = 17.sp, color = Color.Gray)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tip", fontSize = 17.sp, color = Color.Gray)
                    Text("$${String.format("%.2f", currentTip)}", fontSize = 17.sp, color = Color.Gray)
                }
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("New Total", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("$${String.format("%.2f", newTotal)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            // ── Continue Button ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFCC5500), RoundedCornerShape(10.dp))
                    .clickable {
                        val value = customTipText.toDoubleOrNull()
                        if (value != null && value >= 0) {
                            onTipSelected(value)
                        }
                        onBack()
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Continue",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}