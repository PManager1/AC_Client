package com.example.birdy.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Simple location manager that caches the user's last known GPS coordinates.
 * Used by StoreApi to append lat/lng to backend requests (e.g., /nearest-store).
 */
object LocationManager {

    private const val TAG = "LocationManager"

    /** Cached coordinates — updated on each fetch */
    var latitude: Double = 0.0
        private set
    var longitude: Double = 0.0
        private set
    var hasLocation: Boolean = false
        private set

    private var fusedClient: FusedLocationProviderClient? = null

    /** Initialize with application context — call once from BirdyApp or MainActivity */
    fun init(context: Context) {
        fusedClient = LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Fetches the current location. Tries getCurrentLocation first, falls back to lastLocation.
     * Caches the result in [latitude] / [longitude].
     */
    suspend fun fetchLocation(context: Context): Pair<Double, Double> {
        if (fusedClient == null) init(context)

        // Check permission
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "⚠️ No location permission — returning (0,0)")
            return Pair(0.0, 0.0)
        }

        return try {
            val location = getCurrentLocationOnce()
            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
                hasLocation = true
                Log.d(TAG, "📍 Location: ($latitude, $longitude)")
            } else {
                Log.w(TAG, "⚠️ Could not get current location")
            }
            Pair(latitude, longitude)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching location: ${e.message}")
            Pair(latitude, longitude)
        }
    }

    private suspend fun getCurrentLocationOnce(): Location? {
        val client = fusedClient ?: return null
        return suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        cont.resume(location)
                    } else {
                        // Fallback to last known location
                        try {
                            val lastLoc = client.lastLocation.result
                            cont.resume(lastLoc)
                        } catch (_: Exception) {
                            cont.resume(null)
                        }
                    }
                }
                .addOnFailureListener { _ ->
                    cont.resume(null)
                }
            cont.invokeOnCancellation { cts.cancel() }
        }
    }
}