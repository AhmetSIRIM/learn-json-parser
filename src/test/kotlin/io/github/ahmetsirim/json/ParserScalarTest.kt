package io.github.ahmetsirim.json

import io.github.ahmetsirim.json.JsonValue.JsonBoolean
import io.github.ahmetsirim.json.JsonValue.JsonNull
import io.github.ahmetsirim.json.JsonValue.JsonNumber
import io.github.ahmetsirim.json.JsonValue.JsonString
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

/**
 * Covers parsing of single-scalar documents, the smallest complete
 * JSON texts. RFC 8259 allows any value as a top-level document, not
 * just objects and arrays (older RFC 4627 was stricter, and plenty of
 * real-world parsers still disagree here).
 */
class ParserScalarTest {

    @Test
    fun `parses true`() {
        parseJson("true") shouldBe JsonBoolean(true)
    }

    @Test
    fun `parses false`() {
        parseJson("false") shouldBe JsonBoolean(false)
    }

    @Test
    fun `parses null`() {
        parseJson("null") shouldBe JsonNull
    }

    @Test
    fun `parses a number`() {
        parseJson("42") shouldBe JsonNumber(42.0)
    }

    @Test
    fun `parses a string`() {
        parseJson("\"hi\"") shouldBe JsonString("hi")
    }

    @Test
    fun `parses a scalar surrounded by whitespace`() {
        parseJson("  true \n") shouldBe JsonBoolean(true)
    }

    /**
     * A document is ONE value. Without an explicit end-of-input check
     * the parser would accept "true false" and silently drop the rest,
     * a classic recursive-descent bug.
     */
    @Test
    fun `rejects trailing content after the value`() {
        val failure = shouldThrow<JsonParseException> {
            parseJson("true false")
        }

        failure.message shouldContain "end of input"
    }

    @Test
    fun `rejects an empty document`() {
        shouldThrow<JsonParseException> {
            parseJson("")
        }
    }
}
