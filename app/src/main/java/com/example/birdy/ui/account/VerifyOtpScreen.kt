package com.example.birdy.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
private val OrangeSec2 = Color(0xFF8E8E93)

// Matches iOS VerifyOtp.swift
@Composable
fun VerifyOtpScreen(
    phoneNumber: String,
    onBack: () -> Unit = {},
    onVerified: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var otp by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var resendLoading by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Title
        Text(
            text = "Sign In to U-DO",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = OrangeTitle
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Subtitle
        Text(
            text = "Enter the OTP code sent to ${phoneNumber}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = OrangeSecNavyBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // OTP Input Field
        OutlinedTextField(
            value = otp,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }
                otp = digits.take(4)
                errorMessage = null
            },
            placeholder = {
                Text(
                    text = "Enter OTP",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = OrangeSec2.copy(alpha = 0.5f),
                unfocusedBorderColor = OrangeSec2.copy(alpha = 0.5f)
            ),
            singleLine = true,
            textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        )

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

        // Verify OTP button
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Button(
                onClick = {
                    if (otp.isEmpty()) {
                        errorMessage = "Please enter the OTP code."
                        return@Button
                    }
                    loading = true
                    scope.launch {
                        val result = handleVerifyOTP(context, phoneNumber, otp)
                        loading = false
                        if (result.first) {
                            showSuccessDialog = true
                        } else {
                            errorMessage = result.second
                        }
                    }
                },
                enabled = otp.isNotEmpty(),
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
                    text = if (loading) "Verifying..." else "Verify OTP",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Resend Code button
        if (resendLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Button(
                onClick = {
                    resendLoading = true
                    scope.launch {
                        val result = handleSendOTP(phoneNumber)
                        resendLoading = false
                        if (result.first) {
                            errorMessage = "OTP resent successfully! Check your phone."
                        } else {
                            errorMessage = result.second
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (resendLoading) "Resending..." else "Resend Code",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = OrangeTitle
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }

    // Success dialog — matches iOS showSuccessAlert
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onVerified()
            },
            title = {
                Text(
                    text = "Success",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("OTP verified successfully!")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onVerified()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

// ── Verify OTP — mirrors iOS handleVerifyOTP() ──────────────────
// Returns (success: Boolean, message: String)
private suspend fun handleVerifyOTP(
    context: android.content.Context,
    phoneNumber: String,
    otp: String
): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("${Config.API_BASE_URL}/verify-otp")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }

            // Same body as iOS: {"phoneNumber": "...", "otp": "..."}
            val body = """{"phoneNumber":"$phoneNumber","otp":"$otp"}"""
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

                    Pair(true, "Verified")
                } else {
                    Pair(false, "No token received from server")
                }
            } else {
                Pair(false, "Invalid code. Please try again. (HTTP $statusCode)")
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.localizedMessage}")
        }
    }
}

// Reuse handleSendOTP from SignIn.kt (Resend OTP — same API call)
private suspend fun handleSendOTP(phoneNumber: String): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            val cleanedNumber = phoneNumber.filter { it.isDigit() }
            val finalNumber = "+1$cleanedNumber"

            val url = URL("${Config.API_BASE_URL}/send-otp-aws")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }

            val body = """{"phoneNumber":"$finalNumber"}"""
            conn.outputStream.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
            }

            val statusCode = conn.responseCode
            conn.disconnect()

            if (statusCode == 200) {
                Pair(true, "OTP sent")
            } else {
                Pair(false, "Failed to resend OTP (HTTP $statusCode)")
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.localizedMessage}")
        }
    }
}