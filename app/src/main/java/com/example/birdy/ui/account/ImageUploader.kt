package com.example.birdy.ui.account

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.birdy.data.AuthManager
import com.example.birdy.data.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Strips EXIF metadata by re-encoding the bitmap as a compressed JPEG.
 */
fun sanitizeBitmap(bitmap: Bitmap): ByteArray? {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
    return stream.toByteArray()
}

/**
 * Uploads image data to GCS via the backend upload endpoint.
 * @param data Image bytes (may contain EXIF — will be stripped)
 * @param mimeType MIME type (e.g., "image/jpeg")
 * @param folder GCS folder path (e.g., "Users/Verification")
 * @return The public GCS URL, or null on failure
 */
suspend fun uploadToGCS(data: ByteArray, mimeType: String, folder: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val token = AuthManager.getToken()
            if (token.isNullOrEmpty()) {
                println("❌ GCS upload: no auth token")
                return@withContext null
            }

            // Strip EXIF / metadata before uploading
            val finalData = sanitizeBitmap(BitmapFactory.decodeByteArray(data, 0, data.size)) ?: data

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

            val ext = mimeType.split("/").lastOrNull() ?: "jpg"
            val fileName = "upload_${System.currentTimeMillis()}.$ext"

            val body = ByteArrayOutputStream()
            body.write("--$boundary\r\n".toByteArray())
            body.write("Content-Disposition: form-data; name=\"image\"; filename=\"$fileName\"\r\n".toByteArray())
            body.write("Content-Type: $mimeType\r\n\r\n".toByteArray())
            body.write(finalData)
            body.write("\r\n".toByteArray())

            // Form field: folder
            body.write("--$boundary\r\n".toByteArray())
            body.write("Content-Disposition: form-data; name=\"folder\"\r\n\r\n".toByteArray())
            body.write(folder.toByteArray())
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
                    println("✅ GCS upload: $uploadedUrl")
                    return@withContext uploadedUrl
                }
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $statusCode"
                println("❌ GCS upload failed: $errorBody")
                conn.disconnect()
            }
        } catch (e: Exception) {
            println("❌ GCS upload exception: ${e.message}")
        }
        return@withContext null
    }
}
