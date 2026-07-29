package io.github.ahmetsirim.json

import io.github.ahmetsirim.json.JsonValue.JsonArray
import io.github.ahmetsirim.json.JsonValue.JsonBoolean
import io.github.ahmetsirim.json.JsonValue.JsonNumber
import io.github.ahmetsirim.json.JsonValue.JsonObject
import io.github.ahmetsirim.json.JsonValue.JsonString
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Covers pretty output, pinned to the same layout JSON.stringify
 * produces with a two-space indent: every element and entry on its
 * own line, closing brackets back at the parent's indent, and empty
 * containers inline as [] and {} (a lone pair of brackets across two
 * lines would waste space and say nothing).
 *
 * Expected texts are built with trimMargin so the tests show the
 * intended shape literally.
 */
class SerializerPrettyTest {

    @Test
    fun `scalars have no layout to speak of`() {
        JsonNumber(1.0).toPrettyJson() shouldBe "1"
        JsonString("hi").toPrettyJson() shouldBe "\"hi\""
    }

    @Test
    fun `array elements land on their own lines`() {
        val value = JsonArray(listOf(JsonNumber(1.0), JsonNumber(2.0)))

        value.toPrettyJson() shouldBe
            """
            |[
            |  1,
            |  2
            |]
            """.trimMargin()
    }

    @Test
    fun `object entries land on their own lines with spaced colons`() {
        val value = JsonObject(
            mapOf(
                "a" to JsonNumber(1.0),
                "b" to JsonBoolean(true),
            ),
        )

        value.toPrettyJson() shouldBe
            """
            |{
            |  "a": 1,
            |  "b": true
            |}
            """.trimMargin()
    }

    @Test
    fun `nesting indents by two spaces per level`() {
        val value = JsonObject(
            mapOf(
                "user" to JsonObject(
                    mapOf(
                        "tags" to JsonArray(listOf(JsonString("x"), JsonString("y"))),
                    ),
                ),
            ),
        )

        value.toPrettyJson() shouldBe
            """
            |{
            |  "user": {
            |    "tags": [
            |      "x",
            |      "y"
            |    ]
            |  }
            |}
            """.trimMargin()
    }

    @Test
    fun `empty containers stay inline`() {
        val value = JsonObject(
            mapOf(
                "list" to JsonArray(emptyList()),
                "map" to JsonObject(emptyMap()),
            ),
        )

        value.toPrettyJson() shouldBe
            """
            |{
            |  "list": [],
            |  "map": {}
            |}
            """.trimMargin()
    }
}
