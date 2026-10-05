package com.example.birdy.data

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.example.birdy.ui.fooddelivery.Address
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object DeliveryAddressManager {

    var selectedAddress: Address? = null
        private set
    var useCurrentLocation: Boolean = true
        private set

    /**
     * Set when the selected address (or current location) isn't served.
     * HomeFD shows OutOfZoneSheet for it; null hides it.
     */
    var zoneResult: MutableState<ZoneCheckResult?> = mutableStateOf(null)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun currentCoordinates(gpsLat: Double, gpsLng: Double): Pair<Double, Double> {
        if (useCurrentLocation || selectedAddress == null) return Pair(gpsLat, gpsLng)
        val addr = selectedAddress!!
        if (addr.latitude == 0.0 && addr.longitude == 0.0) return Pair(gpsLat, gpsLng)
        return Pair(addr.latitude, addr.longitude)
    }

    fun selectAddress(address: Address) {
        selectedAddress = address
        useCurrentLocation = (address.id == "current_location")
        checkZone(address)
    }

    fun selectCurrentLocation() {
        useCurrentLocation = true
        selectedAddress = null
    }

    fun dismissZone() {
        zoneResult.value = null
    }

    /**
     * Same check as the web and iOS: saved addresses by id (the server uses its
     * own coordinates), the phone's location by lat/lng.
     */
    private fun checkZone(address: Address) {
        val token = AuthManager.getToken()
        val isCurrentLocation = address.id == "current_location"
        if (token.isNullOrEmpty() || (isCurrentLocation && address.latitude == 0.0 && address.longitude == 0.0)) {
            zoneResult.value = null
            return
        }

        scope.launch {
            val result = try {
                if (isCurrentLocation) {
                    ServiceAreaService.checkLocation(address.latitude, address.longitude, token)
                } else {
                    ServiceAreaService.checkSavedAddress(address.id, token)
                }
            } catch (e: Exception) {
                Log.e("DeliveryAddressManager", "checkZone error: ${e.message}")
                null
            }
            withContext(Dispatchers.Main) {
                // "Couldn't verify" isn't shown as a blocker here; save and checkout re-check.
                zoneResult.value = result?.takeUnless { it.inZone || it.couldNotVerify }
            }
        }
    }
}
