package com.example.birdy.ui.account

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.birdy.data.AuthManager
import com.example.birdy.data.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val OrangeTitle = Color(0xFFF27836)
private val OrangeSecNavyBlue = Color(0xFF1B2A4A)
private val OrangeSec5 = Color(0xFFF5F0EB)
private val OrangeSec2 = Color(0xFF8E8E93)

// Matches iOS SignInView.swift
@Composable
fun SignInScreen(
    onBack: () -> Unit = {},
    onOtpSent: (String) -> Unit = {},
    onGuestLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var phoneNumber by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var demoLoading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OrangeSec5)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Close button (top-right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .padding(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title
        Text(
            text = "Sign In to Birdy",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = OrangeTitle
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Phone number input
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Phone Number for OTP code",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = OrangeSecNavyBlue
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { input ->
                    val formatted = formatPhoneNumber(input)
                    phoneNumber = formatted.first
                    errorMessage = formatted.second
                },
                placeholder = {
                    Text(
                        text = "123-456-7890",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = OrangeSec2.copy(alpha = 0.5f),
                    unfocusedBorderColor = OrangeSec2.copy(alpha = 0.5f)
                ),
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp)
            )
        }

        // Error message
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Send OTP button
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Button(
                onClick = {
                    loading = true
                    // TODO: call API to send OTP
                    onOtpSent(phoneNumber)
                    loading = false
                },
                enabled = isValidPhoneNumber(phoneNumber),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeTitle,
                    disabledContainerColor = OrangeTitle.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Send me OTP",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Guest Login button — matches iOS handleDemoLogin()
        if (demoLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Button(
                onClick = {
                    demoLoading = true
                    scope.launch {
                        val result = handleDemoLogin(context)
                        demoLoading = false
                        if (result.first) {
                            // Token saved successfully — show success dialog
                            successMessage = result.second
                            showSuccessDialog = true
                        } else {
                            errorMessage = result.second
                        }
                    }
                },
                enabled = !demoLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9500),
                    disabledContainerColor = Color(0xFFFF9500).copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = if (demoLoading) "Logging in..." else "Guest Login",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Terms text
        Text(
            text = "By providing your phone number, you agree to receive a one-time text message from Birdy for account verification. Message and data rates may apply. Message frequency varies. Reply STOP to opt-out, HELP for help. View our Privacy Policy at Birdyone.com/privacy.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.weight(1f))
    }

    // Success dialog — matches iOS showDemoSuccessAlert
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onGuestLogin()
            },
            title = {
                Text(
                    text = "Success",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(successMessage)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onGuestLogin()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

// ── Guest Login — mirrors iOS handleDemoLogin() ──────────────────
// Returns (success: Boolean, message: String)
private suspend fun handleDemoLogin(context: android.content.Context): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("${Config.API_BASE_URL}/demo-login")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }

            // Same body as iOS: {"phoneNumber": "6502003406"}
            val body = """{"phoneNumber":"6502003406"}"""
            conn.outputStream.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
            }

            val statusCode = conn.responseCode
            val responseStr = if (statusCode in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $statusCode"
            }
            conn.disconnect()

            if (statusCode == 200) {
                val json = JSONObject(responseStr)
                val token = json.optString("token", "")
                val message = json.optString("message", "Demo login successful!")

                if (token.isNotEmpty()) {
                    // Save token (same as iOS AuthManager.shared.setToken(token))
                    AuthManager.setToken(token, context)

                    // Extract user info if present
                    if (json.has("user")) {
                        val user = json.getJSONObject("user")
                        AuthManager.setUserFirstName(user.optString("firstName", ""))
                        AuthManager.setUserLastName(user.optString("lastName", ""))
                        AuthManager.setUserEmail(user.optString("email", ""))
                        AuthManager.setUserID(user.optString("_id", ""))
                        AuthManager.setProfileImageUrl(user.optString("picture", ""))
                    }

                    Pair(true, message)
                } else {
                    Pair(false, "No token in response")
                }
            } else {
                Pair(false, "Login failed: HTTP $statusCode")
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.localizedMessage}")
        }
    }
}

private fun formatPhoneNumber(input: String): Pair<String, String?> {
    val digits = input.filter { it.isDigit() }
    if (digits.length > 10) return Pair(input, "Phone number cannot exceed 10 digits")

    var formatted = ""
    for ((index, digit) in digits.withIndex()) {
        if (index == 3 || index == 6) formatted += "-"
        formatted += digit
    }

    val error = if (digits.length < 10 && digits.isNotEmpty()) "Phone number must be 10 digits" else null
    return Pair(formatted, error)
}

private fun isValidPhoneNumber(phone: String): Boolean {
    return phone.filter { it.isDigit() }.length == 10
}