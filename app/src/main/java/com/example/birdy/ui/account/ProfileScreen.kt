package com.example.birdy.ui.account

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.birdy.data.AuthManager
import com.example.birdy.data.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// iOS color constants
private val OrangeTitle = Color(0xFFF27836)
private val OrangeSecNavyBlue = Color(0xFF1B2A4A)
private val OrangeSec2 = Color(0xFF8E8E93)
private val OrangeSec5 = Color(0xFFF5F0EB)
private val OrangeSec6 = Color(0xFFE5E5EA)
private val OrangeSec7 = Color(0xFF1C1C1E)

/**
 * ProfileScreen — mirrors iOS Profile.swift
 *
 * Edit Profile page with:
 *  - Profile image (tap to change)
 *  - Basic Information (firstName, lastName, email, phone)
 *  - Pricing (flatFee, hourlyRate)
 *  - About (multi-line, 500 char limit)
 *  - Badges (display only for now)
 *  - Save button → PATCH /DriverProfile
 *
 * Fetches profile data on load via GET /me_driver
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State for text fields
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var flatFee by remember { mutableStateOf("") }
    var hourlyRate by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }
    var selectedBadges by remember { mutableStateOf<List<String>>(emptyList()) }

    // UI state
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Image picker
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        // TODO: Upload image to GCS and update profileImageUrl
    }

    // Fetch profile on first composition
    suspend fun fetchProfile() {
        withContext(Dispatchers.IO) {
            try {
                val token = AuthManager.getToken(context) ?: run {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Authentication required"
                        showErrorDialog = true
                        isLoading = false
                    }
                    return@withContext
                }

                val url = URL("${Config.API_BASE_URL}/me_driver")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Cache-Control", "no-cache")
                    connectTimeout = 15000
                    readTimeout = 15000
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

                    // Extract user fields
                    if (json.has("user")) {
                        val user = json.getJSONObject("user")
                        withContext(Dispatchers.Main) {
                            firstName = user.optString("firstName", "")
                            lastName = user.optString("lastName", "")
                            email = user.optString("email", "")
                            phoneNumber = user.optString("phoneNumber", "")
                        }
                    }

                    // Extract serviceProfile → providerDetails
                    if (json.has("serviceProfile")) {
                        val serviceProfile = json.getJSONObject("serviceProfile")
                        if (serviceProfile.has("providerDetails")) {
                            val details = serviceProfile.getJSONObject("providerDetails")
                            withContext(Dispatchers.Main) {
                                flatFee = details.optString("flatFee", "")
                                hourlyRate = details.optString("hourlyRate", "")
                                about = details.optString("about", "")
                                profileImageUrl = details.optString("profileImage", "")

                                // Persist to AuthManager so Account page can display it
                                AuthManager.setProfileImageUrl(profileImageUrl)

                                // Load badges
                                val badgesArray = details.optJSONArray("badges")
                                if (badgesArray != null) {
                                    val badges = mutableListOf<String>()
                                    for (i in 0 until badgesArray.length()) {
                                        badges.add(badgesArray.getString(i))
                                    }
                                    selectedBadges = badges
                                }
                            }
                        }
                    }

                    withContext(Dispatchers.Main) { isLoading = false }
                } else {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Server error: $statusCode"
                        showErrorDialog = true
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Connection error: ${e.localizedMessage}"
                    showErrorDialog = true
                    isLoading = false
                }
            }
        }
    }

    // Save profile via PATCH /DriverProfile
    suspend fun saveProfile() {
        withContext(Dispatchers.IO) {
            try {
                val token = AuthManager.getToken(context) ?: run {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Authentication required"
                        showErrorDialog = true
                    }
                    return@withContext
                }

                val url = URL("${Config.API_BASE_URL}/DriverProfile")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PATCH"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                val payload = JSONObject().apply {
                    put("firstName", firstName)
                    put("lastName", lastName)
                    put("email", email)
                    put("phoneNumber", phoneNumber)
                    put("flatFee", flatFee)
                    put("hourlyRate", hourlyRate)
                    put("about", about)
                    put("badges", org.json.JSONArray(selectedBadges))
                    if (profileImageUrl.isNotBlank()) {
                        put("profileImage", profileImageUrl)
                    }
                }

                conn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                val statusCode = conn.responseCode
                conn.disconnect()

                withContext(Dispatchers.Main) {
                    isSaving = false
                    if (statusCode == 200) {
                        // Update AuthManager with new name and profile image
                        AuthManager.setUserFirstName(firstName)
                        AuthManager.setUserLastName(lastName)
                        AuthManager.setProfileImageUrl(profileImageUrl)
                        showSuccessDialog = true
                    } else {
                        errorMessage = "Save failed: HTTP $statusCode"
                        showErrorDialog = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSaving = false
                    errorMessage = "Connection error: ${e.localizedMessage}"
                    showErrorDialog = true
                }
            }
        }
    }

    // Trigger fetch on load
    remember {
        scope.launch { fetchProfile() }
        Any() // Return a non-unit value
    }

    // ── UI ──────────────────────────────────────────────────
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = OrangeTitle)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Loading profile...", color = OrangeSec2, fontSize = 16.sp)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // ── Top Bar: Back + Save ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = OrangeTitle,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Edit Profile",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeSecNavyBlue
                )
                TextButton(
                    onClick = {
                        isSaving = true
                        scope.launch { saveProfile() }
                    },
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = OrangeTitle
                        )
                    } else {
                        Text(
                            text = "Save",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeTitle
                        )
                    }
                }
            }

            HorizontalDivider(color = OrangeSec6, thickness = 1.dp)

            // ── Scrollable Content ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // MARK: - Profile Image
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.clickable { imagePickerLauncher.launch("image/*") }
                    ) {
                        // Profile image or placeholder
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(OrangeSec6),
                            contentAlignment = Alignment.Center
                        ) {
                            val displayUri = selectedImageUri
                            if (displayUri != null) {
                                AsyncImage(
                                    model = displayUri,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (profileImageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = profileImageUrl,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }
                        // Camera icon overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(y = 4.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(OrangeTitle.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Photo",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Header + subtitle
                Text(
                    text = "Edit Profile",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeSec7
                )
                Text(
                    text = "Update your profile information to keep your details current and accurate.",
                    fontSize = 14.sp,
                    color = OrangeSec2
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = OrangeSec6)
                Spacer(modifier = Modifier.height(12.dp))

                // MARK: - Basic Information Section
                SectionCard(title = "Basic Information", icon = Icons.Default.Person) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileField(label = "First Name", value = firstName, onValueChange = { firstName = it })
                    Spacer(modifier = Modifier.height(10.dp))
                    ProfileField(label = "Last Name", value = lastName, onValueChange = { lastName = it })
                    Spacer(modifier = Modifier.height(10.dp))
                    ProfileField(label = "Email", value = email, onValueChange = { email = it })
                    Spacer(modifier = Modifier.height(10.dp))
                    ProfileField(label = "Phone Number", value = phoneNumber, onValueChange = { input ->
                        phoneNumber = formatPhoneNumber(input)
                    }, placeholder = "(123) 456-7890")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // MARK: - Pricing Section
                SectionCard(title = "Pricing", icon = Icons.Default.AttachMoney) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileField(label = "Flat Fee", value = flatFee, onValueChange = { input ->
                        flatFee = input.filter { it.isDigit() || it == '.' }
                    })
                    Spacer(modifier = Modifier.height(10.dp))
                    ProfileField(label = "Hourly Rate", value = hourlyRate, onValueChange = { input ->
                        hourlyRate = input.filter { it.isDigit() || it == '.' }
                    })
                }

                Spacer(modifier = Modifier.height(16.dp))

                // MARK: - About Section
                Text(
                    text = "About",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeSecNavyBlue
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "About You",
                            fontSize = 14.sp,
                            color = OrangeSec2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = about,
                            onValueChange = { if (it.length <= 500) about = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            placeholder = {
                                Text(
                                    "Tell customers about yourself, your experience, and what makes you a great service provider...",
                                    color = Color.Gray.copy(alpha = 0.6f),
                                    fontSize = 14.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeSec5,
                                unfocusedBorderColor = OrangeSec5,
                                focusedContainerColor = OrangeSec6,
                                unfocusedContainerColor = OrangeSec6
                            ),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 16.sp,
                                color = OrangeSec7
                            )
                        )
                        Text(
                            text = "${about.length}/500 characters",
                            fontSize = 12.sp,
                            color = OrangeSec2,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // MARK: - Badges Section
                Text(
                    text = "Badges",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeSecNavyBlue
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (selectedBadges.isNotEmpty()) {
                            Text(
                                text = "Selected Badges",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OrangeSec7
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Display badges as chips
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                selectedBadges.forEach { badge ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = OrangeTitle.copy(alpha = 0.1f),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        ) {
                                            Text(
                                                text = badge,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = OrangeTitle
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        // Add badges placeholder
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OrangeSec6,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* TODO: Badge selector */ }
                                    .padding(vertical = 12.dp, horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "+ Add Badges",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OrangeTitle
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Badges help customers understand your qualifications and services",
                            fontSize = 12.sp,
                            color = OrangeSec2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // ── Error Dialog ──
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error", fontWeight = FontWeight.Bold) },
            text = { Text(errorMessage ?: "An error occurred") },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // ── Success Dialog ──
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Success", fontWeight = FontWeight.Bold) },
            text = { Text("Your profile has been saved successfully!") },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

// MARK: - Section Card (grouped fields with icon header)

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = OrangeSec6,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = OrangeTitle,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OrangeSec7
                )
            }
            content()
        }
    }
}

// MARK: - Profile Field (label + text input)

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = ""
) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            color = OrangeSec2
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = if (placeholder.isNotBlank()) {
                { Text(placeholder, color = Color.Gray, fontSize = 16.sp) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 16.sp,
                color = OrangeSec7
            )
        )
    }
}

// Phone number formatter (matches iOS formatPhoneNumber)
private fun formatPhoneNumber(input: String): String {
    val digits = input.filter { it.isDigit() }.take(10)
    return buildString {
        for ((i, d) in digits.withIndex()) {
            if (i == 3 || i == 6) append('-')
            append(d)
        }
    }
}