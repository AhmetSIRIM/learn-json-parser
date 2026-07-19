package io.github.ahmetsirim.json

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

/**
 * Covers number scanning against RFC 8259 section 6:
 * number = [ minus ] int [ frac ] [ exp ].
 *
 * The rejection tests matter most here. Kotlin's own toDouble() accepts
 * far more than JSON does ("+1", "1.", ".5", "Infinity"), so every
 * shape below that JSON forbids would silently pass through a scanner
 * that leans on toDouble() for validation.
 */
class TokenizerNumberTest {

    private fun singleToken(input: String): Token = Tokenizer(input).tokenize().first().token

    @Test
    fun `scans an integer`() {
        singleToken("42") shouldBe Token.NumberValue(42.0)
    }

    @Test
    fun `scans zero`() {
        singleToken("0") shouldBe Token.NumberValue(0.0)
    }

    @Test
    fun `scans a negative integer`() {
        singleToken("-7") shouldBe Token.NumberValue(-7.0)
    }

    @Test
    fun `scans a fraction`() {
        singleToken("3.14") shouldBe Token.NumberValue(3.14)
    }

    @Test
    fun `scans an exponent`() {
        singleToken("1e3") shouldBe Token.NumberValue(1000.0)
    }

    @Test
    fun `scans an uppercase exponent with explicit plus`() {
        singleToken("2E+2") shouldBe Token.NumberValue(200.0)
    }

    @Test
    fun `scans a negative exponent`() {
        singleToken("5e-1") shouldBe Token.NumberValue(0.5)
    }

    @Test
    fun `scans the full grammar in one number`() {
        singleToken("-12.34e-5") shouldBe Token.NumberValue(-12.34e-5)
    }

    /**
     * JSON forbids leading zeros ("0123") so numbers cannot be misread
     * as octal, a real-world bug source the grammar closed by design.
     */
    @Test
    fun `rejects a leading zero`() {
        val failure = shouldThrow<JsonParseException> {
            Tokenizer("0123").tokenize()
        }

        failure.message shouldContain "zero"
    }

    @Test
    fun `rejects a bare minus sign`() {
        shouldThrow<JsonParseException> {
            Tokenizer("-").tokenize()
        }
    }

    @Test
    fun `rejects a fraction with no digits`() {
        shouldThrow<JsonParseException> {
            Tokenizer("1.").tokenize()
        }
    }

    @Test
    fun `rejects an exponent with no digits`() {
        shouldThrow<JsonParseException> {
            Tokenizer("1e").tokenize()
        }
    }

    /** JSON has no leading dot; .5 must be written 0.5. */
    @Test
    fun `rejects a number starting with a dot`() {
        shouldThrow<JsonParseException> {
            Tokenizer(".5").tokenize()
        }
    }

    /** JSON has no unary plus, unlike Kotlin's toDouble(). */
    @Test
    fun `rejects a leading plus sign`() {
        shouldThrow<JsonParseException> {
            Tokenizer("+1").tokenize()
        }
    }
}
