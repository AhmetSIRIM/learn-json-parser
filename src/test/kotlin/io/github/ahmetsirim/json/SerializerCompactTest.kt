package io.github.ahmetsirim.json

import io.github.ahmetsirim.json.JsonValue.JsonArray
import io.github.ahmetsirim.json.JsonValue.JsonBoolean
import io.github.ahmetsirim.json.JsonValue.JsonNull
import io.github.ahmetsirim.json.JsonValue.JsonNumber
import io.github.ahmetsirim.json.JsonValue.JsonObject
import io.github.ahmetsirim.json.JsonValue.JsonString
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Covers compact serialization: the shortest valid text for a value
 * tree, no whitespace anywhere.
 *
 * The number and string cases carry the real logic; containers are
 * just joins. Note the asymmetry with parsing: many TEXTS map to one
 * value, but one value maps to exactly one compact text, which is what
 * makes the round-trip milestone testable.
 */
class SerializerCompactTest {

    @Test
    fun `serializes scalars`() {
        JsonNull.toCompactJson() shouldBe "null"
        JsonBoolean(true).toCompactJson() shouldBe "true"
        JsonBoolean(false).toCompactJson() shouldBe "false"
        JsonString("hi").toCompactJson() shouldBe "\"hi\""
    }

    /**
     * Integral doubles print without a fractional part, mirroring
     * JSON.stringify: the tree has no integer type, but "1.0" as
     * output would surprise everyone whose input was "1".
     */
    @Test
    fun `integral numbers print without a fractional part`() {
        JsonNumber(1.0).toCompactJson() shouldBe "1"
        JsonNumber(-42.0).toCompactJson() shouldBe "-42"
        JsonNumber(0.0).toCompactJson() shouldBe "0"
    }

    @Test
    fun `fractional numbers keep their decimals`() {
        JsonNumber(3.14).toCompactJson() shouldBe "3.14"
        JsonNumber(-0.5).toCompactJson() shouldBe "-0.5"
    }

    /**
     * Negative zero is a real Double value with its own bit pattern,
     * and JsonNumber's data-class equality (boxed Double.equals) tells
     * -0.0 and 0.0 apart. Printing it as "0" would break the
     * round-trip law parse(serialize(v)) == v one milestone later.
     */
    @Test
    fun `negative zero survives serialization`() {
        JsonNumber(-0.0).toCompactJson() shouldBe "-0.0"
    }

    @Test
    fun `huge numbers fall back to exponent notation`() {
        JsonNumber(1.0e300).toCompactJson() shouldBe "1.0E300"
    }

    /**
     * NaN and the infinities exist in Double but not in JSON. Failing
     * fast beats JSON.stringify's silent null substitution, which
     * turns an arithmetic bug into corrupted data downstream.
     */
    @Test
    fun `rejects non-finite numbers`() {
        shouldThrow<IllegalArgumentException> {
            JsonNumber(Double.NaN).toCompactJson()
        }
        shouldThrow<IllegalArgumentException> {
            JsonNumber(Double.POSITIVE_INFINITY).toCompactJson()
        }
    }

    @Test
    fun `escapes what JSON requires and nothing more`() {
        JsonString("q:\" b:\\ nl:\n tab:\t").toCompactJson() shouldBe
            "\"q:\\\" b:\\\\ nl:\\n tab:\\t\""
    }

    /** Control characters without a named escape use the u00XX form. */
    @Test
    fun `escapes unnamed control characters as unicode`() {
        JsonString("\u0001").toCompactJson() shouldBe "\"\\u0001\""
    }

    /**
     * Non-ASCII text passes through raw: JSON never REQUIRES escaping
     * it. Escaping everything to \ uXXXX is the paranoid-transport
     * alternative some encoders offer for ASCII-only channels.
     */
    @Test
    fun `non-ascii characters pass through unescaped`() {
        JsonString("café 😀").toCompactJson() shouldBe "\"café 😀\""
    }

    @Test
    fun `serializes arrays without whitespace`() {
        JsonArray(listOf(JsonNumber(1.0), JsonString("x"), JsonNull)).toCompactJson() shouldBe
            "[1,\"x\",null]"
    }

    @Test
    fun `serializes objects without whitespace`() {
        JsonObject(
            mapOf(
                "a" to JsonNumber(1.0),
                "b" to JsonArray(listOf(JsonBoolean(true))),
            ),
        ).toCompactJson() shouldBe "{\"a\":1,\"b\":[true]}"
    }

    @Test
    fun `serializes empty containers`() {
        JsonArray(emptyList()).toCompactJson() shouldBe "[]"
        JsonObject(emptyMap()).toCompactJson() shouldBe "{}"
    }
}
