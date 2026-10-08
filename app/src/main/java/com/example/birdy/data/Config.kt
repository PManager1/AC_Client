package com.example.birdy.data

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

    // Stripe — launch default only. Checkout and Wallet use the server's key (/payments/intents, /payments/config).
    const val STRIPE_PUBLISHABLE_KEY = "pk_test_51SFypI0MYmEMIsHRtYIOAUZM2RcBNIjQA2QAqo24SsxN16RqMI8pX2rNg3PiPUpHrTpZfm20gQexljYH0ZS5erdG00jfHGuiTs"

    // Mapbox — public access token for map rendering (loaded from local.properties via BuildConfig)
    val MAPBOX_ACCESS_TOKEN: String = com.example.birdy.BuildConfig.MAPBOX_ACCESS_TOKEN

    // Google — matches iOS BirdyKit/Config.swift
    const val GOOGLE_API_KEY = "AIzaSyDTm4xeMjg5_GFa2YYUE6zsk2-vagqlAno"
}
