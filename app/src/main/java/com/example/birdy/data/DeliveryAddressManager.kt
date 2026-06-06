package com.example.birdy.data

import com.example.birdy.ui.fooddelivery.Address

object DeliveryAddressManager {

    var selectedAddress: Address? = null
        private set
    var useCurrentLocation: Boolean = true
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
    }

    fun selectCurrentLocation() {
        useCurrentLocation = true
        selectedAddress = null
    }
}
