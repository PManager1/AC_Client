package com.example.birdy.ui.account

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PestControl
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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

private val BurntOrange = Color(0xFFCC5500)
private val OrangeModerate = Color(0xFFFF8C00)
private val OffWhite = Color(0xFFF5F5F5)

@Composable
fun BugReporterScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessAlert by remember { mutableStateOf(false) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var uploadedUrls by remember { mutableStateOf<List<String>>(emptyList()) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImages = selectedImages + it
            println("📸 Bug reporter: image selected — $it")
        }
    }

    if (showSuccessAlert) {
        AlertDialog(
            onDismissRequest = {
                showSuccessAlert = false
                title = ""
                description = ""
                selectedImages = emptyList()
                uploadedUrls = emptyList()
            },
            title = {
                Text("Thank You!", fontWeight = FontWeight.Bold)
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
                        selectedImages = emptyList()
                        uploadedUrls = emptyList()
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
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                        Text("e.g., Checkout button is greyed out", color = Color.Gray, fontSize = 14.sp)
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
                            "Explain what you were trying to do...",
                            color = Color.Gray.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
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

            // Screenshots Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Screenshots (optional)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BurntOrange
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(selectedImages) { index, uri ->
                        Box(modifier = Modifier.size(80.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Screenshot $index",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImages = selectedImages.toMutableList().also { it.removeAt(index) } },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color.Red,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                val check = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                                if (check == PackageManager.PERMISSION_GRANTED) {
                                    imagePickerLauncher.launch("image/*")
                                } else {
                                    // Fallback for older Android
                                    imagePickerLauncher.launch("image/*")
                                }
                            },
                            modifier = Modifier.size(80.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = BurntOrange)
                                Text("Add", fontSize = 11.sp, color = BurntOrange)
                            }
                        }
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    isSubmitting = true
                    scope.launch {
                        // 1. Upload images to GCS
                        val urls = mutableListOf<String>()
                        for (uri in selectedImages) {
                            try {
                                val bytes = withContext(Dispatchers.IO) {
                                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                } ?: continue
                                val url = uploadToGCS(bytes)
                                if (url != null) urls.add(url)
                            } catch (e: Exception) {
                                println("❌ Bug screenshot upload failed: ${e.message}")
                            }
                        }

                        // 2. Submit to backend
                        val success = submitToBackend(title, description, urls)

                        // 3. Show result
                        isSubmitting = false
                        if (success) {
                            showSuccessAlert = true
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isSubmitting) OrangeModerate else Color.Gray.copy(alpha = 0.5f),
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
                        Spacer(modifier = Modifier.width(8.dp))
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

        // Footer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        ) {
            Text(
                text = "Your feedback helps us build a better app",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

private suspend fun uploadToGCS(imageBytes: ByteArray): String? {
    return withContext(Dispatchers.IO) {
        try {
            val token = AuthManager.getToken()
            if (token.isNullOrEmpty()) {
                println("❌ Bug upload: no auth token")
                return@withContext null
            }

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

            val fileName = "bug_${System.currentTimeMillis()}.jpg"
            val body = ByteArrayOutputStream()
            body.write("--$boundary\r\n".toByteArray())
            body.write("Content-Disposition: form-data; name=\"image\"; filename=\"$fileName\"\r\n".toByteArray())
            body.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
            body.write(imageBytes)
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
                if (uploadedUrl.isNotEmpty()) {
                    println("✅ Bug screenshot uploaded: $uploadedUrl")
                    return@withContext uploadedUrl
                }
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $statusCode"
                println("❌ Bug upload failed: $errorBody")
                conn.disconnect()
            }
        } catch (e: Exception) {
            println("❌ Bug upload exception: ${e.message}")
        }
        return@withContext null
    }
}

private suspend fun submitToBackend(title: String, description: String, imageUrls: List<String>): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val token = AuthManager.getToken()
            if (token.isNullOrEmpty()) {
                println("❌ Bug report: no auth token")
                return@withContext false
            }
            val url = URL("${Config.API_BASE_URL}/bug-report")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 30000
                readTimeout = 30000
            }

            val jsonBody = JSONObject().apply {
                put("title", title)
                put("description", description)
                val urlsArray = org.json.JSONArray()
                imageUrls.forEach { urlsArray.put(it) }
                put("imageUrls", urlsArray)
            }

            conn.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray())
                os.flush()
            }

            val statusCode = conn.responseCode
            val responseBody = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            println("📥 Bug report response: HTTP $statusCode — $responseBody")
            return@withContext statusCode == 200
        } catch (e: Exception) {
            println("❌ Bug report submission failed: ${e.message}")
            return@withContext false
        }
    }
}
