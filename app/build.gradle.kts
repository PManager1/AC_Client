import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// Load secrets from local.properties
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}
val mapboxToken: String = localProperties.getProperty("MAPBOX_ACCESS_TOKEN", "")
val googleMapsKey: String = localProperties.getProperty("GOOGLE_MAPS_API_KEY", "")

android {
    namespace = "com.example.birdy"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.birdy"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        // Inject Mapbox token into BuildConfig and AndroidManifest
        buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"$mapboxToken\"")
        manifestPlaceholders["MAPBOX_ACCESS_TOKEN"] = mapboxToken

        // Inject Google Maps API key into BuildConfig and AndroidManifest (same pattern)
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"$googleMapsKey\"")
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = googleMapsKey
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // ViewModel Compose (for viewModel() in composables)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Coil (image loading — matches iOS AsyncImage)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Stripe Android SDK (matches iOS Stripe SDK)
    implementation("com.stripe:stripe-android:21.4.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Google Maps SDK for Android
    implementation("com.google.android.gms:play-services-maps:19.1.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Mapbox Maps SDK
    implementation("com.mapbox.maps:android:11.7.1")

    // Firebase Realtime Database + Cloud Messaging (Push Notifications)
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}