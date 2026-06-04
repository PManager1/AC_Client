package com.example.birdy.ui.account

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PestControl
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val BurntOrange = Color(0xFFCC5500)
private val OrangeModerate = Color(0xFFFF8C00)
private val OffWhite = Color(0xFFF5F5F5)

// Matches iOS BugReporter.swift
@Composable
fun BugReporterScreen(
    onBack: () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessAlert by remember { mutableStateOf(false) }

    // Success Alert Dialog
    if (showSuccessAlert) {
        AlertDialog(
            onDismissRequest = {
                showSuccessAlert = false
                title = ""
                description = ""
            },
            title = {
                Text(
                    text = "Thank You!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Your bug report has been submitted successfully.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessAlert = false
                        title = ""
                        description = ""
                    }
                ) {
                    Text("OK", color = BurntOrange)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
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
                    tint = Color.Gray
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            // Header
            Icon(
                imageVector = Icons.Default.PestControl,
                contentDescription = "Bug",
                tint = OrangeModerate,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bug Report",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = BurntOrange
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Help us improve your experience",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Form
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "What happened?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BurntOrange
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            text = "e.g., Checkout button is greyed out",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Gray.copy(alpha = 0.2f),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f)
                    ),
                    singleLine = true
                )
            }

            // Description Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Give us the details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BurntOrange
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = {
                        Text(
                            text = "Explain what you were trying to do...",
                            color = Color.Gray.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Gray.copy(alpha = 0.2f),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f)
                    ),
                    maxLines = 6
                )
            }

            // Submit Button
            Button(
                onClick = {
                    isSubmitting = true
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(1500) // Simulate API call (matches iOS 1.5s delay)
                        isSubmitting = false
                        showSuccessAlert = true
                        println("Bug Report Submitted:")
                        println("Title: $title")
                        println("Description: $description")
                    }
                },
                enabled = title.isNotEmpty() && description.isNotEmpty() && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (title.isNotEmpty() && description.isNotEmpty() && !isSubmitting)
                        OrangeModerate
                    else
                        Color.Gray.copy(alpha = 0.5f),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Submit",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Submit Report",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Footer Info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            Text(
                text = "Your feedback helps us build a better app",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}