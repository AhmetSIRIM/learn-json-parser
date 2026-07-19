package io.github.ahmetsirim.json

/**
 * Thrown when the input is not valid JSON.
 *
 * A thrown exception keeps call sites clean for the common
 * trust-the-input case; the alternative, a Result-returning API, only
 * pays off when malformed input is an expected, inline-handled case.
 * Unchecked because Kotlin has no checked exceptions anyway.
 */
class JsonParseException(
    message: String,
    val position: TextPosition? = null,
) : RuntimeException(if (position == null) message else "$message at $position")
