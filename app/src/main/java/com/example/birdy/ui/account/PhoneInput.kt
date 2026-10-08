package com.example.birdy.ui.account

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

// US phone input: state holds raw digits only, dashes are drawn by
// UsPhoneVisualTransformation so Compose keeps the cursor in place.

// "1234567890" -> "123-456-7890". A dash only appears once a digit follows it.
fun formatUsPhone(digits: String): String {
    val sb = StringBuilder()
    for ((index, digit) in digits.withIndex()) {
        if (index == 3 || index == 6) sb.append('-')
        sb.append(digit)
    }
    return sb.toString()
}

// onValueChange reducer. Strips a leading country code "1" only for
// paste/autofill (e.g. "+1 (202) 491-0597"), never on a single keystroke,
// and rejects input that would exceed 10 digits instead of truncating it.
fun nextUsPhoneDigits(previous: String, input: String): String {
    var digits = input.filter(Char::isDigit)
    val isPasteOrAutofill = input.contains('+') || digits.length - previous.length > 1
    if (digits.length == 11 && digits[0] == '1' && isPasteOrAutofill) {
        digits = digits.drop(1)
    }
    return if (digits.length > 10) previous else digits
}

internal fun usPhoneOriginalToTransformed(offset: Int, digitCount: Int): Int {
    val o = offset.coerceIn(0, digitCount)
    val t = o + (if (o > 3) 1 else 0) + (if (o > 6) 1 else 0)
    return t.coerceIn(0, formatUsPhone("0".repeat(digitCount)).length)
}

internal fun usPhoneTransformedToOriginal(offset: Int, digitCount: Int): Int {
    val o = offset - (if (offset > 3) 1 else 0) - (if (offset > 7) 1 else 0)
    return o.coerceIn(0, digitCount)
}

object UsPhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitCount = text.text.length
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) =
                usPhoneOriginalToTransformed(offset, digitCount)

            override fun transformedToOriginal(offset: Int) =
                usPhoneTransformedToOriginal(offset, digitCount)
        }
        return TransformedText(AnnotatedString(formatUsPhone(text.text)), mapping)
    }
}
