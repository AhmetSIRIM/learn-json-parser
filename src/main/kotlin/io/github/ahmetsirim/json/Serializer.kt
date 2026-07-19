package io.github.ahmetsirim.json

import io.github.ahmetsirim.json.JsonValue.JsonArray
import io.github.ahmetsirim.json.JsonValue.JsonBoolean
import io.github.ahmetsirim.json.JsonValue.JsonNull
import io.github.ahmetsirim.json.JsonValue.JsonNumber
import io.github.ahmetsirim.json.JsonValue.JsonObject
import io.github.ahmetsirim.json.JsonValue.JsonString
import kotlin.math.abs
import kotlin.math.floor

/** Renders the value tree as the shortest valid JSON text. */
fun JsonValue.toCompactJson(): String = JsonWriter(indentUnit = null).render(this)

/**
 * Renders the value tree indented for human eyes, two spaces per
 * level, matching JSON.stringify's layout. The indent width is fixed
 * on purpose; a real library makes it a parameter, and that is the
 * only difference.
 */
fun JsonValue.toPrettyJson(): String = JsonWriter(indentUnit = "  ").render(this)

/**
 * One traversal serves both formats: compact and pretty differ only
 * in what happens BETWEEN tokens, so layout is isolated in
 * [newlineIndent] and the colon, and the tree walk is written once.
 * Two hand-rolled walkers would inevitably drift apart.
 */
private class JsonWriter(private val indentUnit: String?) {

    private val out = StringBuilder()
    private var depth = 0

    fun render(value: JsonValue): String {
        writeValue(value)
        return out.toString()
    }

    private fun writeValue(value: JsonValue) {
        when (value) {
            JsonNull -> out.append("null")
            is JsonBoolean -> out.append(value.value)
            is JsonNumber -> out.append(formatNumber(value.value))
            is JsonString -> writeString(value.value)
            is JsonArray -> writeContainer('[', ']', value.elements) { element ->
                writeValue(element)
            }
            is JsonObject -> writeContainer('{', '}', value.entries.toList()) { (key, entryValue) ->
                writeString(key)
                out.append(if (indentUnit == null) ":" else ": ")
                writeValue(entryValue)
            }
        }
    }

    /**
     * Empty containers stay inline as [] and {}; a lone bracket pair
     * across two lines wastes space and says nothing.
     */
    private fun <ITEM> writeContainer(
        open: Char,
        close: Char,
        items: List<ITEM>,
        writeItem: (ITEM) -> Unit,
    ) {
        if (items.isEmpty()) {
            out.append(open).append(close)
            return
        }
        out.append(open)
        depth++
        items.forEachIndexed { index, item ->
            if (index > 0) out.append(',')
            newlineIndent()
            writeItem(item)
        }
        depth--
        newlineIndent()
        out.append(close)
    }

    private fun newlineIndent() {
        if (indentUnit == null) return
        out.append('\n')
        repeat(depth) { out.append(indentUnit) }
    }

    /**
     * Integral doubles print as integers (1.0 -> "1"), mirroring
     * JSON.stringify, but only below 10^15: past 2^53 a Double cannot
     * hold exact integers anyway, so larger values keep Kotlin's
     * exponent form. Negative zero is excluded from the integer path
     * because "-0.0" and "0" parse back to Doubles with different bit
     * patterns, and the round-trip law compares with boxed equality
     * that tells them apart.
     *
     * NaN and the infinities fail fast: JSON.stringify's alternative,
     * silently writing null, turns an arithmetic bug into corrupted
     * data.
     */
    private fun formatNumber(value: Double): String {
        require(value.isFinite()) { "JSON cannot represent $value" }
        val isNegativeZero = value == 0.0 && 1.0 / value < 0
        val isIntegral = !isNegativeZero && value == floor(value) && abs(value) < 1e15
        return if (isIntegral) value.toLong().toString() else value.toString()
    }

    /**
     * Escapes exactly what RFC 8259 requires: quote, backslash and the
     * control range, nothing else. Forward slash stays bare (escaping
     * it is only a courtesy for embedding JSON in HTML script tags),
     * and non-ASCII text passes through raw; encoders that force
     * everything to backslash-u form are catering to ASCII-only
     * transports.
     */
    private fun writeString(text: String) {
        out.append('"')
        for (char in text) {
            when (char) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                '\b' -> out.append("\\b")
                '\u000C' -> out.append("\\f")
                else -> {
                    if (char < ' ') {
                        out.append("\\u").append(char.code.toString(16).padStart(4, '0'))
                    } else {
                        out.append(char)
                    }
                }
            }
        }
        out.append('"')
    }
}
