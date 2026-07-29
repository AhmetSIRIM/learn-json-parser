package io.github.ahmetsirim.json

import io.github.ahmetsirim.json.JsonValue.JsonArray
import io.github.ahmetsirim.json.JsonValue.JsonBoolean
import io.github.ahmetsirim.json.JsonValue.JsonNumber
import io.github.ahmetsirim.json.JsonValue.JsonObject
import io.github.ahmetsirim.json.JsonValue.JsonString
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Covers object = { string : value *( , string : value ) }, the last
 * production. Objects reuse the array's separator loop shape; the new
 * material is the key rules: keys must be strings, and duplicate keys
 * resolve to last-one-wins.
 */
class ParserObjectTest {

    @Test
    fun `parses an empty object`() {
        parseJson("{}") shouldBe JsonObject(emptyMap())
    }

    @Test
    fun `parses a single entry`() {
        parseJson("{\"a\": 1}") shouldBe JsonObject(mapOf("a" to JsonNumber(1.0)))
    }

    @Test
    fun `parses multiple entries preserving document order`() {
        val result = parseJson("{\"z\": 1, \"a\": 2}") as JsonObject

        result.entries.keys.toList() shouldBe listOf("z", "a")
    }

    @Test
    fun `parses nested objects and arrays`() {
        parseJson("{\"user\": {\"tags\": [\"x\"], \"active\": true}}") shouldBe JsonObject(
            mapOf(
                "user" to JsonObject(
                    mapOf(
                        "tags" to JsonArray(listOf(JsonString("x"))),
                        "active" to JsonBoolean(true),
                    ),
                ),
            ),
        )
    }

    /**
     * RFC 8259 only says names SHOULD be unique and leaves duplicate
     * behavior undefined. Last-one-wins matches what JavaScript's
     * JSON.parse and most mainstream parsers do; erroring out is the
     * stricter alternative some security-focused parsers choose.
     */
    @Test
    fun `duplicate keys resolve to the last value`() {
        parseJson("{\"k\": 1, \"k\": 2}") shouldBe JsonObject(mapOf("k" to JsonNumber(2.0)))
    }

    @Test
    fun `rejects a non-string key`() {
        shouldThrow<JsonParseException> {
            parseJson("{1: true}")
        }
    }

    @Test
    fun `rejects a missing colon`() {
        shouldThrow<JsonParseException> {
            parseJson("{\"a\" 1}")
        }
    }

    @Test
    fun `rejects a trailing comma`() {
        shouldThrow<JsonParseException> {
            parseJson("{\"a\": 1,}")
        }
    }

    @Test
    fun `rejects an unterminated object`() {
        shouldThrow<JsonParseException> {
            parseJson("{\"a\": 1")
        }
    }
}
