package com.example.birdy.ui.account

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.birdy.data.AuthManager
import com.example.birdy.data.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
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
 * ProfileScreen — mirrors iOS ProfileN.swift
 *
 * Edit Profile page with:
 *  - Profile image (tap to change URL)
 *  - Basic Information (Professional Name, Service Type, Profile Image URL)
 *  - Pricing (flatFee, hourlyRate)
 *  - About (multi-line, no char limit)
 *  - Badges (comma-separated text field)
 *  - Save button → PATCH /meProfile
 *  - Change tracking (only sends modified fields)
 *
 * Fetches profile data on load via GET /me
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Original values for change tracking (mirrors iOS nameChanged, serviceChanged, etc.)
    var originalName by remember { mutableStateOf("") }
    var originalService by remember { mutableStateOf("") }
    var originalProfileImageUrl by remember { mutableStateOf("") }
    var originalFlatFee by remember { mutableStateOf("") }
    var originalHourlyRate by remember { mutableStateOf("") }
    var originalAbout by remember { mutableStateOf("") }
    var originalBadges by remember { mutableStateOf("") }

    // Editable states (mirrors iOS @State private vars)
    var name by remember { mutableStateOf("") }
    var service by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }
    var flatFee by remember { mutableStateOf("") }
    var hourlyRate by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var badgeNames by remember { mutableStateOf("") }

    // UI state
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Local image preview during upload
    var localImageUri by remember { mutableStateOf<Uri?>(null) }

    // Upload profile image: compress → upload to backend → update URL
    suspend fun uploadProfileImage(uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { isUploading = true }

                // Read image bytes from URI
                val inputStream = context.contentResolver.openInputStream(uri) ?: run {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Could not read image"
                        showErrorDialog = true
                        isUploading = false
                    }
                    return@withContext
                }
                val originalBytes = inputStream.readBytes()
                inputStream.close()

                // Compress: resize to max 1200px + JPEG 70% quality
                val compressedBytes = compressAndResizeImage(originalBytes, maxDimension = 1200, quality = 70)
                println("📸 Image compressed: ${originalBytes.size} bytes → ${compressedBytes.size} bytes")

                val token = AuthManager.getToken(context) ?: run {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Authentication required"
                        showErrorDialog = true
                        isUploading = false
                    }
                    return@withContext
                }

                // Upload via multipart POST to /upload/image
                val boundary = "Boundary-${System.currentTimeMillis()}"
                val url = URL("${Config.API_BASE_URL}/upload/image")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                    connectTimeout = 30000
                    readTimeout = 30000
                }

                val fileName = "profile_${System.currentTimeMillis()}.jpg"
                val body = ByteArrayOutputStream()
                body.write("--$boundary\r\n".toByteArray())
                body.write("Content-Disposition: form-data; name=\"image\"; filename=\"$fileName\"\r\n".toByteArray())
                body.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
                body.write(compressedBytes)
                body.write("\r\n--$boundary--\r\n".toByteArray())

                conn.outputStream.use { os ->
                    os.write(body.toByteArray())
                    os.flush()
                }

                val statusCode = conn.responseCode
                if (statusCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val uploadedUrl = json.optString("url", "")
                    conn.disconnect()

                    withContext(Dispatchers.Main) {
                        if (uploadedUrl.isNotEmpty()) {
                            profileImageUrl = uploadedUrl
                            AuthManager.setProfileImageUrl(uploadedUrl)
                            println("✅ Profile image uploaded: $uploadedUrl")
                        }
                        isUploading = false
                    }
                } else {
                    val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $statusCode"
                    conn.disconnect()
                    withContext(Dispatchers.Main) {
                        errorMessage = "Upload failed: $errorBody"
                        showErrorDialog = true
                        isUploading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Upload error: ${e.localizedMessage}"
                    showErrorDialog = true
                    isUploading = false
                }
            }
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            localImageUri = it
            scope.launch {
                uploadProfileImage(it)
            }
        }
    }

    // Fetch profile on first composition (mirrors iOS init from profile object)
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

                val url = URL("${Config.API_BASE_URL}/me")
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

                    // Extract serviceProfile → providerDetails (mirrors iOS ProfessionalProfile)
                    if (json.has("serviceProfile")) {
                        val serviceProfile = json.getJSONObject("serviceProfile")
                        if (serviceProfile.has("providerDetails")) {
                            val details = serviceProfile.getJSONObject("providerDetails")
                            withContext(Dispatchers.Main) {
                                // Build name from user's first + last
                                val firstName = json.optJSONObject("user")?.optString("firstName", "") ?: ""
                                val lastName = json.optJSONObject("user")?.optString("lastName", "") ?: ""
                                val fullName = "$firstName $lastName".trim()

                                name = details.optString("name", fullName)
                                service = details.optString("service", "")
                                profileImageUrl = details.optString("profileImage", "")
                                flatFee = details.optString("flatFee", "")
                                hourlyRate = details.optString("hourlyRate", "")
                                about = details.optString("about", "")

                                // Save rating from providerDetails (matches iOS profile.rating)
                                val ratingValue = details.optDouble("rating", 5.0).toFloat()
                                AuthManager.setUserRating(ratingValue)

                                // Persist profile image to AuthManager
                                AuthManager.setProfileImageUrl(profileImageUrl)

                                // Load badges as comma-separated string (matches iOS badgeNames)
                                val badgesArray = details.optJSONArray("badges")
                                if (badgesArray != null) {
                                    val badges = mutableListOf<String>()
                                    for (i in 0 until badgesArray.length()) {
                                        // Handle badges that are either strings or objects with "name" field
                                        val badge = badgesArray.get(i)
                                        if (badge is String) {
                                            badges.add(badge)
                                        } else if (badge is JSONObject) {
                                            badges.add(badge.optString("name", ""))
                                        }
                                    }
                                    badgeNames = badges.joinToString(", ")
                                }

                                // Store originals for change tracking
                                originalName = name
                                originalService = service
                                originalProfileImageUrl = profileImageUrl
                                originalFlatFee = flatFee
                                originalHourlyRate = hourlyRate
                                originalAbout = about
                                originalBadges = badgeNames
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

    // Save profile via PATCH /meProfile (mirrors iOS saveProfile)
    // Only sends fields that have actually been changed
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

                // Build payload — only include changed fields (mirrors iOS change tracking)
                val jsonObject = JSONObject()

                if (name != originalName) {
                    jsonObject.put("name", name)
                }
                if (service != originalService) {
                    jsonObject.put("service", service)
                }
                if (profileImageUrl != originalProfileImageUrl) {
                    jsonObject.put("profileImage", profileImageUrl)
                }
                if (flatFee != originalFlatFee) {
                    jsonObject.put("flatFee", flatFee.replace("$", ""))
                }
                if (hourlyRate != originalHourlyRate) {
                    jsonObject.put("hourlyRate", hourlyRate.replace("$", ""))
                }
                if (about != originalAbout) {
                    jsonObject.put("about", about.trim())
                }
                if (badgeNames != originalBadges) {
                    // Send badges as array of objects (matches iOS format)
                    val badgesArray = org.json.JSONArray()
                    badgeNames.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { badge ->
                        badgesArray.put(JSONObject().put("name", badge).put("color", "#85D9C4"))
                    }
                    jsonObject.put("badges", badgesArray)
                }

                // If no fields changed, don't make the request (matches iOS)
                if (jsonObject.length() == 0) {
                    withContext(Dispatchers.Main) {
                        isSaving = false
                        showSuccessDialog = true
                    }
                    return@withContext
                }

                val url = URL("${Config.API_BASE_URL}/meProfile")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PATCH"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                conn.outputStream.use { os ->
                    os.write(jsonObject.toString().toByteArray(Charsets.UTF_8))
                }

                val statusCode = conn.responseCode
                conn.disconnect()

                withContext(Dispatchers.Main) {
                    isSaving = false
                    if (statusCode == 200) {
                        // Update originals to new values
                        originalName = name
                        originalService = service
                        originalProfileImageUrl = profileImageUrl
                        originalFlatFee = flatFee
                        originalHourlyRate = hourlyRate
                        originalAbout = about
                        originalBadges = badgeNames

                        // Persist profile image to AuthManager
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
        Any()
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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                // ── Top Bar: Back + "Edit Profile" + Save (matches iOS) ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp)
                        .background(Color.White)
                        .padding(horizontal = 15.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OrangeTitle,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Edit Profile",
                        fontSize = 20.sp,
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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OrangeTitle
                            )
                        }
                    }
                }

                // ── Scrollable Content (matches iOS ScrollView) ──
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Profile Image Section — tap to pick, loading overlay during upload
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isUploading) { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Box {
                            // Profile image circle — show local preview during upload, else remote
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploading && localImageUri != null) {
                                    // Show locally selected image as preview while uploading
                                    AsyncImage(
                                        model = localImageUri,
                                        contentDescription = "Uploading",
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

                            // Camera icon overlay (hidden during upload)
                            if (!isUploading) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset(y = 16.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(OrangeTitle.copy(alpha = 0.8f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Change Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Loading overlay — spinner + "Uploading..." on top of the image
                        if (isUploading) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Uploading...",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // Header (matches iOS "Edit Profile" heading)
                    Text(
                        text = "Edit Profile",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeSec7,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Instructions (matches iOS subtitle)
                    Text(
                        text = "Update your profile information to keep your details current and accurate.",
                        fontSize = 15.sp,
                        color = OrangeSec2,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Form sections inside a rounded container (matches iOS VStack with background)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OrangeSec6, RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp)
                    ) {
                        // ── Basic Information Section (matches iOS) ──
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Basic Info",
                                    tint = OrangeTitle,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Basic Information",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OrangeSec7
                                )
                            }

                            Spacer(modifier = Modifier.height(15.dp))

                            // Professional Name (matches iOS "Professional Name")
                            ProfileInputField(
                                placeholder = "Professional Name",
                                value = name,
                                onValueChange = { name = it }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Service Type (matches iOS "Service Type")
                            ProfileInputField(
                                placeholder = "Service Type",
                                value = service,
                                onValueChange = { service = it }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Profile Image URL (matches iOS "Profile Image URL")
                            ProfileInputField(
                                placeholder = "Profile Image URL",
                                value = profileImageUrl,
                                onValueChange = { profileImageUrl = it }
                            )
                        }

                        HorizontalDivider()

                        // ── Pricing Section (matches iOS) ──
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = "Pricing",
                                    tint = OrangeTitle,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pricing",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OrangeSec7
                                )
                            }

                            Spacer(modifier = Modifier.height(15.dp))

                            // Flat Fee (matches iOS "Flat Fee (e.g. $45)")
                            ProfileInputField(
                                placeholder = "Flat Fee (e.g. $45)",
                                value = flatFee,
                                onValueChange = { flatFee = it }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Hourly Rate (matches iOS "Hourly Rate (e.g. $50/hr)")
                            ProfileInputField(
                                placeholder = "Hourly Rate (e.g. $50/hr)",
                                value = hourlyRate,
                                onValueChange = { hourlyRate = it }
                            )
                        }

                        HorizontalDivider()

                        // ── About Section (matches iOS with icon header) ──
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = "About",
                                    tint = OrangeTitle,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "About",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OrangeSec7
                                )
                            }

                            Spacer(modifier = Modifier.height(15.dp))

                            // Multi-line text field (matches iOS TextEditor, no char limit)
                            OutlinedTextField(
                                value = about,
                                onValueChange = { about = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                placeholder = {
                                    Text(
                                        "Tell customers about your services...",
                                        color = Color.Gray.copy(alpha = 0.6f),
                                        fontSize = 17.sp
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 17.sp,
                                    color = OrangeSec7
                                )
                            )
                        }

                        HorizontalDivider()

                        // ── Badges Section (matches iOS — comma-separated text field) ──
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Badges",
                                    tint = OrangeTitle,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Badges",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OrangeSec7
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Separate badge names with commas",
                                fontSize = 14.sp,
                                color = OrangeSec2
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Comma-separated text field (matches iOS TextField for badges)
                            ProfileInputField(
                                placeholder = "e.g. Background Check Cleared, Certified",
                                value = badgeNames,
                                onValueChange = { badgeNames = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Save Changes Button (matches iOS bottom button) ──
                    Button(
                        onClick = {
                            isSaving = true
                            scope.launch { saveProfile() }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeTitle,
                            disabledContainerColor = OrangeTitle.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Save Changes",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
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

// MARK: - Image Compression Helper
/**
 * Resizes image to fit within maxDimension×maxDimension and compresses as JPEG.
 * Typical: 5-12MB photo → 200-500KB
 */
private fun compressAndResizeImage(imageBytes: ByteArray, maxDimension: Int = 1200, quality: Int = 70): ByteArray {
    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return imageBytes

    // Calculate scale to fit within maxDimension
    val scale: Float = if (bitmap.width > bitmap.height) {
        maxDimension.toFloat() / bitmap.width
    } else {
        maxDimension.toFloat() / bitmap.height
    }

    // Only downscale, never upscale
    val finalScale = minOf(scale, 1.0f)
    val newWidth = (bitmap.width * finalScale).toInt()
    val newHeight = (bitmap.height * finalScale).toInt()

    val resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

    // Recycle original if it's different from resized
    if (resized != bitmap) bitmap.recycle()

    val stream = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    resized.recycle()
    return stream.toByteArray()
}

// MARK: - Profile Input Field (matches iOS TextField with shadow styling)

@Composable
private fun ProfileInputField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, color = Color.Gray.copy(alpha = 0.6f), fontSize = 17.sp)
        },
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(8.dp)),
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent
        ),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 17.sp,
            color = OrangeSec7
        )
    )
}