package com.example.birdy.data

/**
 * App configuration — mirrors iOS BirdyKit/Config.swift
 */
object Config {
    // const val API_BASE_URL = "http://10.0.2.2:3030"           // Android Emulator → Local
    const val API_BASE_URL = "https://tcdlm857gf.execute-api.us-east-1.amazonaws.com/dev/api/v1"  // AWS Development

    // WebSocket API (separate API Gateway deployment from REST API) — matches iOS BirdyKit/Config.swift
    const val WS_API_BASE_URL = "wss://fg1a60piqh.execute-api.us-east-1.amazonaws.com/dev"

    // Stripe — matches iOS BirdyKit/Config.swift
    const val STRIPE_PUBLISHABLE_KEY = "pk_live_51SFypI0MYmEMIsHRC9TByy2moodTCLKiIQJi4rR8fBbh57vTStvOTpHiFGsMsQ6B4GRYMW6RnvEzx2JPGpL4tfDi003HADHoC0"

    // Mapbox — public access token for map rendering (loaded from local.properties via BuildConfig)
    val MAPBOX_ACCESS_TOKEN: String = com.example.birdy.BuildConfig.MAPBOX_ACCESS_TOKEN
}
