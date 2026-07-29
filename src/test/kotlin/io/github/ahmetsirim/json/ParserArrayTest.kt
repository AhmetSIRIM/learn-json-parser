package io.github.ahmetsirim.json

import io.github.ahmetsirim.json.JsonValue.JsonArray
import io.github.ahmetsirim.json.JsonValue.JsonBoolean
import io.github.ahmetsirim.json.JsonValue.JsonNull
import io.github.ahmetsirim.json.JsonValue.JsonNumber
import io.github.ahmetsirim.json.JsonValue.JsonString
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Covers the first recursive production: array = [ value *( , value ) ].
 * The nesting test is the heart of the milestone; recursion falls out
 * of parseValue calling parseArray calling parseValue.
 */
class ParserArrayTest {

    @Test
    fun `parses an empty array`() {
        parseJson("[]") shouldBe JsonArray(emptyList())
    }

    @Test
    fun `parses a single-element array`() {
        parseJson("[1]") shouldBe JsonArray(listOf(JsonNumber(1.0)))
    }

    @Test
    fun `parses mixed scalars separated by commas`() {
        parseJson("[1, \"two\", true, null]") shouldBe JsonArray(
            listOf(
                JsonNumber(1.0),
                JsonString("two"),
                JsonBoolean(true),
                JsonNull,
            ),
        )
    }

    @Test
    fun `parses nested arrays`() {
        parseJson("[[1], [[2]]]") shouldBe JsonArray(
            listOf(
                JsonArray(listOf(JsonNumber(1.0))),
                JsonArray(listOf(JsonArray(listOf(JsonNumber(2.0))))),
            ),
        )
    }

    @Test
    fun `rejects a missing comma between elements`() {
        shouldThrow<JsonParseException> {
            parseJson("[1 2]")
        }
    }

    /** JSON allows no trailing comma, unlike JavaScript and Kotlin. */
    @Test
    fun `rejects a trailing comma`() {
        shouldThrow<JsonParseException> {
            parseJson("[1,]")
        }
    }

    @Test
    fun `rejects an unterminated array`() {
        shouldThrow<JsonParseException> {
            parseJson("[1, 2")
        }
    }
}
