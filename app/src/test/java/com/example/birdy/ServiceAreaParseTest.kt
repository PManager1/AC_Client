package com.example.birdy

import com.example.birdy.data.ServiceAreaException
import com.example.birdy.data.ZoneCheckResult
import com.example.birdy.data.isValidContact
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parsing of the udo3 service-area responses (POST /addresses/check and the 422s). */
class ServiceAreaParseTest {

    @Test
    fun inZone() {
        val result = ZoneCheckResult.fromJson(JSONObject("""{"inZone": true}"""))
        assertTrue(result.inZone)
        assertNull(result.reason)
        assertFalse(result.alreadyOnList)
    }

    @Test
    fun outOfZoneCarriesEverythingTheSheetNeeds() {
        val json = """
            {"inZone": false, "reason": "out_of_zone",
             "address": "2325 Hollins Ferry Rd, Baltimore, MD 21230",
             "city": "Baltimore", "zip": "21230", "latitude": 39.2847, "longitude": -76.6205,
             "heading": "We're not in Baltimore yet", "message": "We're not in Baltimore yet",
             "detail": "We're starting in Washington, DC, and expanding soon. Want us to let you know when we arrive?",
             "contact": "jay@example.com", "alreadyOnList": true}
        """
        val result = ZoneCheckResult.fromJson(JSONObject(json))

        assertFalse(result.inZone)
        assertFalse(result.isPaused)
        assertFalse(result.couldNotVerify)
        assertEquals("Baltimore", result.city)
        assertEquals("21230", result.zip)
        assertEquals(39.2847, result.latitude!!, 0.0001)
        assertEquals("We're not in Baltimore yet", result.heading)
        assertEquals("jay@example.com", result.contact)
        assertTrue(result.alreadyOnList)
    }

    @Test
    fun pausedAndNullFields() {
        val json = """
            {"inZone": false, "reason": "paused", "city": null, "zip": null,
             "latitude": null, "longitude": null, "contact": null,
             "heading": "We're paused for now", "alreadyOnList": false}
        """
        val result = ZoneCheckResult.fromJson(JSONObject(json))

        assertTrue(result.isPaused)
        assertNull(result.city)
        assertNull(result.latitude)
        assertNull(result.contact)
    }

    @Test
    fun geocodeFailed() {
        val result = ZoneCheckResult.fromJson(JSONObject("""{"inZone": false, "reason": "geocode_failed", "message": "We couldn't verify that address."}"""))
        assertTrue(result.couldNotVerify)
    }

    @Test
    fun serviceAreaExceptionFrom422() {
        val body = """{"error": "out_of_zone", "city": "Baltimore", "zip": "21230", "message": "We're not in Baltimore yet", "detail": "We're starting in Washington, DC"}"""
        val e = ServiceAreaException.parse(422, body)

        assertNotNull(e)
        assertEquals("out_of_zone", e!!.code)
        assertEquals("Baltimore", e.city)

        val asResult = e.toZoneCheckResult("2325 Hollins Ferry Rd, Baltimore, MD")
        assertFalse(asResult.inZone)
        assertEquals("out_of_zone", asResult.reason)
        assertEquals("We're not in Baltimore yet", asResult.heading)
    }

    @Test
    fun pausedExceptionMapsToPausedReason() {
        val e = ServiceAreaException.parse(422, """{"error": "service_paused", "message": "We're not taking orders right now."}""")
        assertTrue(e!!.toZoneCheckResult(null).isPaused)
    }

    @Test
    fun otherErrorsAreNotServiceAreaErrors() {
        assertNull(ServiceAreaException.parse(422, """{"error": "Validation failed"}"""))
        assertNull(ServiceAreaException.parse(500, """{"error": "out_of_zone"}"""))
        assertNull(ServiceAreaException.parse(422, "not json"))
        assertNull(ServiceAreaException.parse(422, null))
    }

    // Same cases as the server's Udo.ZoneInterest tests
    @Test
    fun contactValidation() {
        assertTrue(isValidContact("jay@example.com"))
        assertTrue(isValidContact(" Jay@Example.com "))
        assertTrue(isValidContact("(202) 555-0143"))
        assertTrue(isValidContact("+1 202-555-0143"))
        assertFalse(isValidContact("jay@"))
        assertFalse(isValidContact("12345"))
        assertFalse(isValidContact(""))
        assertFalse(isValidContact("call me maybe"))
    }
}
