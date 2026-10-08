package com.example.birdy.ui.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneInputTest {

    @Test
    fun formatUsPhone_noTrailingDash() {
        assertEquals("", formatUsPhone(""))
        assertEquals("1", formatUsPhone("1"))
        assertEquals("123", formatUsPhone("123"))
        assertEquals("123-4", formatUsPhone("1234"))
        assertEquals("123-456", formatUsPhone("123456"))
        assertEquals("123-456-7", formatUsPhone("1234567"))
        assertEquals("123-456-7890", formatUsPhone("1234567890"))
    }

    @Test
    fun offsetMapping_staysInBoundsAndRoundTrips() {
        for (len in listOf(0, 1, 3, 4, 6, 7, 10)) {
            val formattedLen = formatUsPhone("9".repeat(len)).length
            for (o in 0..len) {
                val t = usPhoneOriginalToTransformed(o, len)
                assertTrue("len=$len o=$o t=$t", t in 0..formattedLen)
                assertEquals("len=$len o=$o", o, usPhoneTransformedToOriginal(t, len))
            }
            for (t in 0..formattedLen) {
                assertTrue(usPhoneTransformedToOriginal(t, len) in 0..len)
            }
        }
    }

    @Test
    fun cursorSitsAfterTypedDigit() {
        // After typing the 4th digit, cursor is after "123-4"
        assertEquals(5, usPhoneOriginalToTransformed(4, 4))
        // After typing the 7th digit, cursor is after "123-456-7"
        assertEquals(9, usPhoneOriginalToTransformed(7, 7))
    }

    @Test
    fun nextUsPhoneDigits_handlesPasteAndAutofill() {
        assertEquals("2024910597", nextUsPhoneDigits("", "+1 (202) 491-0597"))
        assertEquals("2024910597", nextUsPhoneDigits("", "+12024910597"))
        assertEquals("2024910597", nextUsPhoneDigits("", "12024910597"))
    }

    @Test
    fun nextUsPhoneDigits_rejectsExtraKeystroke() {
        assertEquals("1234567890", nextUsPhoneDigits("1234567890", "12345678905"))
        assertEquals("2024910597", nextUsPhoneDigits("2024910597", "20249105971"))
    }

    @Test
    fun nextUsPhoneDigits_normalTyping() {
        assertEquals("202491059", nextUsPhoneDigits("20249105", "202491059"))
        assertEquals("20249105", nextUsPhoneDigits("202491059", "20249105"))
        assertEquals("1", nextUsPhoneDigits("", "1"))
    }
}
