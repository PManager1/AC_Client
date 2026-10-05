package com.example.birdy.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// MARK: - Service Area (udo3)
// Same flow as the web address modal and the iOS app:
//   1. POST /addresses/check right after an address is picked
//   2. Out of zone → OutOfZoneSheet → POST /zone-interest ("Notify me")
// The server re-checks on address save and on order, so these calls are UX only.
// All network calls block: run them on Dispatchers.IO.

/**
 * Response of POST /api/v1/addresses/check. When [inZone] is false it carries
 * everything the "not here yet" sheet needs; the copy comes from the server so
 * web, iOS and Android say the same thing.
 */
data class ZoneCheckResult(
    val inZone: Boolean,
    val reason: String? = null,          // "out_of_zone", "paused", "geocode_failed"
    val address: String? = null,
    val city: String? = null,
    val zip: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val heading: String? = null,
    val message: String? = null,
    val detail: String? = null,
    val contact: String? = null,         // email or phone on file, for "We'll notify you at …"
    val alreadyOnList: Boolean = false
) {
    val isPaused: Boolean get() = reason == "paused"
    val couldNotVerify: Boolean get() = reason == "geocode_failed"

    companion object {
        fun fromJson(obj: JSONObject) = ZoneCheckResult(
            inZone = obj.optBoolean("inZone", false),
            reason = obj.optStringOrNull("reason"),
            address = obj.optStringOrNull("address"),
            city = obj.optStringOrNull("city"),
            zip = obj.optStringOrNull("zip"),
            latitude = obj.optDoubleOrNull("latitude"),
            longitude = obj.optDoubleOrNull("longitude"),
            heading = obj.optStringOrNull("heading"),
            message = obj.optStringOrNull("message"),
            detail = obj.optStringOrNull("detail"),
            contact = obj.optStringOrNull("contact"),
            alreadyOnList = obj.optBoolean("alreadyOnList", false)
        )
    }
}

/**
 * 422 from address save / order when the address isn't served:
 * {"error": "out_of_zone" | "service_paused" | "geocode_failed", "city", "zip", "message", "detail"}
 */
class ServiceAreaException(
    val code: String,
    val city: String?,
    val zip: String?,
    override val message: String?,
    val detail: String?
) : Exception(message) {

    /** Same shape as a check result, so the UI can show OutOfZoneSheet for it. */
    fun toZoneCheckResult(address: String?) = ZoneCheckResult(
        inZone = false,
        reason = when (code) {
            "service_paused" -> "paused"
            "geocode_failed" -> "geocode_failed"
            else -> "out_of_zone"
        },
        address = address,
        city = city,
        zip = zip,
        heading = if (code == "service_paused") "We're paused for now" else message,
        message = message,
        detail = detail ?: message
    )

    companion object {
        private val CODES = setOf("out_of_zone", "service_paused", "geocode_failed")

        /** Null unless it's a 422 with one of the service-area error codes. */
        fun parse(status: Int, body: String?): ServiceAreaException? {
            if (status != 422 || body.isNullOrBlank()) return null
            val obj = try { JSONObject(body) } catch (e: Exception) { return null }
            val code = obj.optString("error", "")
            if (code !in CODES) return null
            return ServiceAreaException(
                code = code,
                city = obj.optStringOrNull("city"),
                zip = obj.optStringOrNull("zip"),
                message = obj.optStringOrNull("message"),
                detail = obj.optStringOrNull("detail")
            )
        }
    }
}

object ServiceAreaService {

    /** A newly picked address (the server geocodes the text itself). */
    fun checkAddress(street: String, cityStateZip: String, token: String): ZoneCheckResult =
        check(JSONObject().put("street", street).put("cityStateZip", cityStateZip), token)

    /** A saved address (the server uses its stored coordinates). */
    fun checkSavedAddress(addressId: String, token: String): ZoneCheckResult =
        check(JSONObject().put("addressId", addressId), token)

    /** The phone's current location. */
    fun checkLocation(latitude: Double, longitude: Double, token: String): ZoneCheckResult =
        check(JSONObject().put("latitude", latitude).put("longitude", longitude), token)

    /** "Notify me". Returns the server status: "joined" or "already". */
    fun join(contact: String, place: ZoneCheckResult, token: String): String {
        val body = JSONObject().apply {
            put("contact", contact)
            put("reason", if (place.isPaused) "paused" else "out_of_zone")
            place.address?.let { put("address", it) }
            place.city?.let { put("city", it) }
            place.zip?.let { put("zipCode", it) }
            place.latitude?.let { put("latitude", it) }
            place.longitude?.let { put("longitude", it) }
        }
        val (status, response) = post("/zone-interest", body, token)
        if (status != 200 && status != 201) {
            val msg = try { JSONObject(response).optStringOrNull("message") } catch (e: Exception) { null }
            throw Exception(msg ?: "Couldn't add you to the list. Please try again.")
        }
        return JSONObject(response).optString("status", "joined")
    }

    private fun check(body: JSONObject, token: String): ZoneCheckResult {
        val (status, response) = post("/addresses/check", body, token)
        if (status != 200) throw Exception("Couldn't check this address. Please try again.")
        return ZoneCheckResult.fromJson(JSONObject(response))
    }

    private fun post(path: String, body: JSONObject, token: String): Pair<Int, String> {
        val connection = URL("${Config.API_BASE_URL}$path").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            println("🔍 [ServiceArea] POST $path → $status")
            status to text
        } finally {
            connection.disconnect()
        }
    }
}

// MARK: - Contact validation (same rules as the server's Udo.ZoneInterest.parse_contact)

private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
private val PHONE_CHARS_REGEX = Regex("^[\\d\\s().+-]+$")

fun isValidContact(value: String): Boolean {
    val v = value.trim()
    if (EMAIL_REGEX.matches(v)) return true
    return PHONE_CHARS_REGEX.matches(v) && v.count { it.isDigit() } >= 10
}

// JSON helpers: org.json's optString returns "null"/"" for missing values
private fun JSONObject.optStringOrNull(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).ifEmpty { null }

private fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (!has(key) || isNull(key)) null else optDouble(key).takeUnless { it.isNaN() }
