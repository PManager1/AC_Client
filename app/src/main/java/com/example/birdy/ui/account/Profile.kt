package com.example.birdy.ui.account

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
 *  - Profile image (tap to change via camera/gallery)
 *  - Basic Information (Professional Name, Service Type, Profile Image URL)
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

    // Original values for change tracking
    var originalFirstName by remember { mutableStateOf("") }
    var originalLastName by remember { mutableStateOf("") }
    var originalEmail by remember { mutableStateOf("") }
    var originalPhone by remember { mutableStateOf("") }
    // Editable states
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }

    // UI state
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showPhoneSheet by remember { mutableStateOf(false) }

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
                                // Load user personal info from user object
                                val userObj = json.optJSONObject("user")
                                firstName = userObj?.optString("firstName", "") ?: ""
                                lastName = userObj?.optString("lastName", "") ?: ""
                                email = userObj?.optString("email", "") ?: ""
                                phoneNumber = userObj?.optString("phoneNumber", "") ?: ""
                                profileImageUrl = details.optString("profileImage", "")

                                // Save rating from providerDetails
                                val ratingValue = details.optDouble("rating", 5.0).toFloat()
                                AuthManager.setUserRating(ratingValue)

                                // Persist profile image to AuthManager
                                AuthManager.setProfileImageUrl(profileImageUrl)

                                // Store originals for change tracking
                                originalFirstName = firstName
                                originalLastName = lastName
                                originalEmail = email
                                originalPhone = phoneNumber
                            }
                        }
                    }

                    withContext(Dispatchers.Main) { isLoading = false }
                } else {
                    println("❌ GET /me failed — Status: $statusCode, Body: $responseStr")
                    withContext(Dispatchers.Main) {
                        errorMessage = "Server error ($statusCode): $responseStr"
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

                if (firstName != originalFirstName) {
                    jsonObject.put("firstName", firstName)
                }
                if (lastName != originalLastName) {
                    jsonObject.put("lastName", lastName)
                }
                if (email != originalEmail) {
                    jsonObject.put("email", email)
                }
                if (phoneNumber != originalPhone) {
                    jsonObject.put("phoneNumber", phoneNumber)
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
                        originalFirstName = firstName
                        originalLastName = lastName
                        originalEmail = email
                        originalPhone = phoneNumber

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

                            // First Name
                            ProfileInputField(
                                placeholder = "First Name",
                                value = firstName,
                                onValueChange = { firstName = it }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Last Name
                            ProfileInputField(
                                placeholder = "Last Name",
                                value = lastName,
                                onValueChange = { lastName = it }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Email
                            ProfileInputField(
                                placeholder = "Email",
                                value = email,
                                onValueChange = { email = it }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Phone Number — read-only with pencil icon to open change sheet
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { /* read-only */ },
                                    placeholder = {
                                        Text("Phone Number", color = Color.Gray.copy(alpha = 0.6f), fontSize = 17.sp)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .shadow(2.dp, RoundedCornerShape(8.dp)),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    readOnly = true,
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        disabledContainerColor = Color.White.copy(alpha = 0.6f),
                                        disabledBorderColor = Color.Transparent
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 17.sp,
                                        color = OrangeSec7
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { showPhoneSheet = true },
                                    modifier = Modifier.offset(y = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Change Phone",
                                        tint = OrangeTitle,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
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

    // ── Phone Change Bottom Sheet (matches iOS PhoneChangeSheet) ──
    if (showPhoneSheet) {
        PhoneChangeSheet(
            currentPhone = phoneNumber,
            onDismiss = { showPhoneSheet = false }
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

// MARK: - Phone Change Bottom Sheet (matches iOS PhoneChangeSheet)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneChangeSheet(
    currentPhone: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var newPhone by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    fun formatPhone(input: String): String {
        val digits = input.filter { it.isDigit() }.take(10)
        return when {
            digits.length >= 7 -> "${digits.substring(0, 3)}-${digits.substring(3, 6)}-${digits.substring(6)}"
            digits.length >= 4 -> "${digits.substring(0, 3)}-${digits.substring(3)}"
            else -> digits
        }
    }

    val isValid = newPhone.filter { it.isDigit() }.length == 10

    fun submitRequest() {
        val rawDigits = newPhone.filter { it.isDigit() }
        if (rawDigits.length != 10) {
            errorMsg = "Please enter a valid 10-digit phone number"
            showError = true
            return
        }

        val token = AuthManager.getToken(context)
        if (token == null) {
            errorMsg = "Authentication required"
            showError = true
            return
        }

        isSubmitting = true
        scope.launch {
            try {
                val statusCode = withContext(Dispatchers.IO) {
                    val url = URL("${Config.API_BASE_URL}/request-phone-change")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("Authorization", "Bearer $token")
                        doOutput = true
                        connectTimeout = 15000
                        readTimeout = 15000
                    }

                    val payload = JSONObject().apply {
                        put("currentPhone", currentPhone)
                        put("newPhone", newPhone)
                    }
                    conn.outputStream.use { os ->
                        os.write(payload.toString().toByteArray(Charsets.UTF_8))
                    }

                    val code = conn.responseCode
                    conn.disconnect()
                    code
                }

                isSubmitting = false
                if (statusCode == 200) {
                    showSuccess = true
                } else {
                    errorMsg = "Server error. Please try again."
                    showError = true
                }
            } catch (e: Exception) {
                isSubmitting = false
                errorMsg = e.localizedMessage ?: "Network error"
                showError = true
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(horizontal = 16.dp)
        ) {
            // Title
            Text(
                text = "Change Phone Number",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = OrangeSec7,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Info card: current phone + message
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F2F7), RoundedCornerShape(10.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone",
                        tint = OrangeTitle,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Current:",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OrangeSec2
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentPhone.isBlank()) "Not set" else currentPhone,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OrangeSec7
                    )
                }

                Text(
                    text = "Enter your new phone number below. Our team will review and update it within 24 hours.",
                    fontSize = 14.sp,
                    color = OrangeSec2
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // New phone input
            Text(
                text = "New Phone Number",
                fontSize = 14.sp,
                color = OrangeSec2,
                modifier = Modifier.padding(bottom = 5.dp)
            )
            OutlinedTextField(
                value = newPhone,
                onValueChange = { raw ->
                    val formatted = formatPhone(raw)
                    if (formatted != newPhone) newPhone = formatted
                },
                placeholder = {
                    Text("(123) 456-7890", color = Color.Gray.copy(alpha = 0.5f), fontSize = 17.sp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(8.dp)),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
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

            Spacer(modifier = Modifier.weight(1f))

            // Submit button
            Button(
                onClick = { submitRequest() },
                enabled = isValid && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isValid) OrangeTitle else Color.Gray.copy(alpha = 0.4f),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isSubmitting) "Submitting..." else "Submit Request",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }

    // Success dialog
    if (showSuccess) {
        AlertDialog(
            onDismissRequest = {
                showSuccess = false
                onDismiss()
            },
            title = { Text("Request Submitted!", fontWeight = FontWeight.Bold) },
            text = { Text("Our team has been notified. We'll update your phone number within 24 hours and confirm via email.") },
            confirmButton = {
                TextButton(onClick = {
                    showSuccess = false
                    onDismiss()
                }) {
                    Text("OK")
                }
            }
        )
    }

    // Error dialog
    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("Error", fontWeight = FontWeight.Bold) },
            text = { Text(errorMsg) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text("OK")
                }
            }
        )
    }
}
