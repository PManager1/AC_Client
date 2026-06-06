package com.example.birdy.data

import com.example.birdy.ui.fooddelivery.Address
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object AddressService {

    fun getAddresses(token: String): List<Address> {
        println("🔍 [AddressService] Fetching all addresses...")
        return try {
            val url = URL("${Config.API_BASE_URL}/addresses")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000

            val responseCode = connection.responseCode
            println("🔍 [AddressService] Response status: $responseCode")

            if (responseCode in 200..299) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val addresses = parseAddresses(json)
                println("✅ [AddressService] Successfully fetched ${addresses.size} addresses")
                addresses
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No details"
                println("❌ [AddressService] Failed to fetch addresses. Status: $responseCode. Body: $errorBody")
                emptyList()
            }
        } catch (e: Exception) {
            println("❌ [AddressService] Failed to fetch addresses: ${e.message}")
            emptyList()
        }
    }

    private fun parseAddresses(json: String): List<Address> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Address(
                id = obj.optString("id", ""),
                street = obj.optString("street", ""),
                cityStateZip = obj.optString("cityStateZip", ""),
                gateCode = obj.optString("gateCode", "").ifEmpty { null },
                isDefault = obj.optBoolean("isDefault", false),
                latitude = obj.optDouble("latitude", 0.0),
                longitude = obj.optDouble("longitude", 0.0)
            )
        }
    }

    fun setDefaultAddress(addressId: String, token: String): Address? {
        println("🔍 [AddressService] Setting default address: $addressId")
        return try {
            val url = URL("${Config.API_BASE_URL}/addresses/$addressId/set-default")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000

            val responseCode = connection.responseCode
            println("🔍 [AddressService] Response status: $responseCode")

            if (responseCode in 200..299) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val obj = org.json.JSONObject(json)
                val address = Address(
                    id = obj.optString("id", ""),
                    street = obj.optString("street", ""),
                    cityStateZip = obj.optString("cityStateZip", ""),
                    gateCode = obj.optString("gateCode", "").ifEmpty { null },
                    isDefault = obj.optBoolean("isDefault", false),
                    latitude = obj.optDouble("latitude", 0.0),
                    longitude = obj.optDouble("longitude", 0.0)
                )
                println("✅ [AddressService] Successfully set default address: ${address.street}")
                address
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No details"
                println("❌ [AddressService] Failed to set default. Status: $responseCode. Body: $errorBody")
                null
            }
        } catch (e: Exception) {
            println("❌ [AddressService] Failed to set default address: ${e.message}")
            null
        }
    }
}
