package com.example.birdy.data

import com.birdy.kit.config.AppConfig

/**
 * App configuration — mirrors iOS BirdyKit/Config.swift
 */
object Config {
    // const val API_BASE_URL = "http://10.0.2.2:3030"           // Android Emulator → Local
    // const val API_BASE_URL = "https://tcdlm857gf.execute-api.us-east-1.amazonaws.com/dev/api/v1"  // AWS Development
    // const val API_BASE_URL = "https://udo1.gigalixirapp.com"
    // const val API_BASE_URL = "http://10.0.2.2:4000/api/v1"   // Android Emulator → local Elixir
    // Always production: emulator and phones share the live environment (same server as AP).
    const val API_BASE_URL = "https://udo1.gigalixirapp.com/api/v1"

    // WebSocket API (separate API Gateway deployment from REST API) — matches iOS BirdyKit/Config.swift
    const val WS_API_BASE_URL = "wss://fg1a60piqh.execute-api.us-east-1.amazonaws.com/dev"

    // Client keys: from GET /appconfig (BirdyKitAndroid AppConfig), else the baked-in key there.
    // Stripe: launch default only. Checkout and Wallet use the server's key (/payments/intents, /payments/config).
    val STRIPE_PUBLISHABLE_KEY: String get() = AppConfig.stripePublishableKey

    // Mapbox — public access token for map rendering (loaded from local.properties via BuildConfig)
    val MAPBOX_ACCESS_TOKEN: String get() = AppConfig.mapboxToken ?: com.example.birdy.BuildConfig.MAPBOX_ACCESS_TOKEN

    // Google Places/Geocoding REST (send with AppConfig.googleHeaders()). The Maps SDK key is in the manifest.
    val GOOGLE_API_KEY: String get() = AppConfig.googleApiKey
}
