package com.example.birdy.ui.store

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options

/**
 * Custom Coil Fetcher that handles data: URI images (e.g., "data:image/jpeg;base64,/9j/4AAQ...").
 *
 * The Go backend sometimes returns menu item images as base64-encoded data URIs instead of
 * regular HTTPS URLs. Coil doesn't support these natively, so this fetcher strips the header,
 * decodes the Base64 payload into a Bitmap, and returns it directly as a DrawableResult.
 *
 * Registered in BirdyApp.kt's ImageLoader via .components { add(DataUriFetcher.Factory()) }
 */
class DataUriFetcher private constructor(
    private val data: String,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        // Parse: "data:image/jpeg;base64,/9j/4AAQ..."
        val headerEnd = data.indexOf(",")
        if (headerEnd == -1) {
            Log.w("DataUriFetcher", "⚠️ Invalid data URI (no comma found)")
            throw IllegalArgumentException("Invalid data URI")
        }

        val base64Payload = data.substring(headerEnd + 1)

        Log.d("DataUriFetcher", "🖼️ Decoding data: URI — payloadSize=${base64Payload.length}")

        // Decode Base64 → raw bytes → Bitmap → Drawable
        val bytes = Base64.decode(base64Payload, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalArgumentException("Failed to decode bitmap from data URI")

        return DrawableResult(
            drawable = BitmapDrawable(options.context.resources, bitmap),
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    /**
     * Factory that routes data: URI strings to our custom fetcher.
     * Non-data: URIs fall through to Coil's default OkHttp fetcher.
     */
    class Factory : Fetcher.Factory<String> {
        override fun create(
            data: String,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            return if (data.startsWith("data:", ignoreCase = true)) {
                DataUriFetcher(data, options)
            } else {
                null // Let Coil's default OkHttp fetcher handle regular URLs
            }
        }
    }
}