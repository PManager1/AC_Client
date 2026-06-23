package com.example.birdy.data

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.example.birdy.ui.fooddelivery.Address
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object DeliveryAddressManager {

    var selectedAddress: Address? = null
        private set
    var useCurrentLocation: Boolean = true
        private set
    var showZoneBanner: MutableState<Boolean> = mutableStateOf(false)
        private set

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

    fun dismissZoneBanner() {
        showZoneBanner.value = false
    }

    private fun checkZone(address: Address) {
        if (address.id == "current_location") return
        val lat = address.latitude
        val lng = address.longitude
        if (lat == 0.0 && lng == 0.0) { showZoneBanner.value = false; return }

        Thread {
            try {
                val json = JSONObject().apply {
                    put("latitude", lat)
                    put("longitude", lng)
                }
                val url = URL("${Config.API_BASE_URL}/check-zone")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.outputStream.write(json.toString().toByteArray())
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val result = JSONObject(response)
                showZoneBanner.value = !result.optBoolean("insideZone", true)
            } catch (e: Exception) {
                Log.e("DeliveryAddressManager", "checkZone error: ${e.message}")
                showZoneBanner.value = false
            }
        }.start()
    }

    fun extractZip(cityStateZip: String): String {
        val match = Regex("\\b(\\d{5}(?:-\\d{4})?)\\b").find(cityStateZip)
        return match?.groupValues?.getOrNull(1) ?: ""
    }
}
