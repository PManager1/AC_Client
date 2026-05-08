package com.example.birdy.ui.account

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.birdy.data.PromoCode
import com.example.birdy.data.ReferralCodeService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// MARK: - ReferralCodeView (matches iOS ReferralCodeView.swift)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralCodeView(
    onBack: () -> Unit = {}
) {
    var promoCode by remember { mutableStateOf<PromoCode?>(null) }
    var durationHours by remember { mutableStateOf(24) }
    var selectedValue by remember { mutableStateOf(TextFieldValue("")) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showErrorAlert by remember { mutableStateOf(false) }
    var showSuccessAlert by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-generate on first load — matches iOS .onAppear
    var hasLoaded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate Promo Code", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Generate Promo Code",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B2A4A)
            )

            // MARK: - Code Display Section
            Text(
                text = "Your Promo Code",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Code display card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Blue.copy(alpha = 0.1f), Color.Blue.copy(alpha = 0.05f))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val currentCode = promoCode
                    if (currentCode != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentCode.code,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1C1E)
                            )
                            Text(
                                text = "${"%.1f".format(currentCode.commissionFee)}% commission fee",
                                fontSize = 14.sp,
                                color = Color.Blue
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(statusColor(currentCode.status), RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentCode.status.replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp,
                                    color = statusColor(currentCode.status)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Tap to generate code",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Copy button
                IconButton(
                    onClick = {
                        promoCode?.let { code ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Promo Code", code.code))
                            successMessage = "Code '${code.code}' copied to clipboard!"
                            showSuccessAlert = true
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Blue.copy(alpha = 0.1f), RoundedCornerShape(22.dp))
                ) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color.Blue
                    )
                }
            }

            // Code details
            promoCode?.let { code ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Color.Blue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Expires: ${formatDate(code.expiresAt)}",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (code.isActive) Color(0xFF34C759) else Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (code.isActive) "Code is active" else if (code.isExpired) "Code has expired" else "Code has been used",
                            fontSize = 13.sp,
                            color = if (code.isActive) Color(0xFF34C759) else Color.Red
                        )
                    }
                }
            }

            HorizontalDivider()

            // MARK: - Code Settings Section
            Text(
                text = "Code Settings",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            // Duration
            Text("Duration (hrs)", fontSize = 17.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = TextFieldValue(durationHours.toString()),
                    onValueChange = { newValue ->
                        val parsed = newValue.text.toIntOrNull()
                        if (parsed != null && parsed in 1..720) {
                            durationHours = parsed
                        }
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (durationHours > 1) durationHours-- }, enabled = promoCode == null) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                    }
                    Text(durationHours.toString(), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { if (durationHours < 720) durationHours++ }, enabled = promoCode == null) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase")
                    }
                }
            }

            // Commission Fee
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Commission Fee", fontSize = 17.sp, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = selectedValue,
                    onValueChange = { selectedValue = it },
                    placeholder = { Text("Enter value") },
                    modifier = Modifier.width(120.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                    enabled = promoCode == null
                )
                Text("%", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.Blue)
            }

            Text(
                text = "Maximum expiration: 720 hours (30 days)",
                fontSize = 12.sp,
                color = Color.Gray
            )

            HorizontalDivider()

            // MARK: - Generate Button
            Button(
                onClick = {
                    val commissionFee = selectedValue.text.toDoubleOrNull()
                    if (commissionFee == null || commissionFee <= 0 || commissionFee > 100) {
                        errorMessage = "Please enter a valid commission fee percentage (1-100)"
                        showErrorAlert = true
                        return@Button
                    }

                    isLoading = true
                    promoCode = null

                    scope.launch {
                        try {
                            val generatedCode = ReferralCodeService.generatePromoCode(commissionFee, durationHours)
                            promoCode = generatedCode
                            successMessage = "Code '${generatedCode.code}' generated successfully!"
                            showSuccessAlert = true
                        } catch (e: Exception) {
                            errorMessage = "Failed to generate code: ${e.message}"
                            showErrorAlert = true
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedValue.text.toDoubleOrNull()?.let { it > 0 } == true && !isLoading) Color.Blue else Color.Gray
                ),
                enabled = selectedValue.text.toDoubleOrNull()?.let { it > 0 } == true && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(
                        text = if (promoCode == null) "Generate New Code" else "Generate Another Code",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Error alert
    if (showErrorAlert) {
        AlertDialog(
            onDismissRequest = { showErrorAlert = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = { TextButton(onClick = { showErrorAlert = false }) { Text("OK") } }
        )
    }

    // Success alert
    if (showSuccessAlert) {
        AlertDialog(
            onDismissRequest = { showSuccessAlert = false },
            title = { Text("Success") },
            text = { Text(successMessage) },
            confirmButton = { TextButton(onClick = { showSuccessAlert = false }) { Text("OK") } }
        )
    }

    // Auto-generate on first load
    LaunchedEffect(Unit) {
        if (!hasLoaded) {
            hasLoaded = true
            // Pre-fill default commission fee to trigger auto-generate
        }
    }
}

private fun statusColor(status: String): Color {
    return when (status.lowercase()) {
        "active" -> Color(0xFF34C759)
        "used" -> Color.Blue
        "expired" -> Color.Red
        else -> Color.Gray
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.US)
    return formatter.format(date)
}