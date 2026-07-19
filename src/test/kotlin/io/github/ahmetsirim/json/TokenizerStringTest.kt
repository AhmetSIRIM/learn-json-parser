package io.github.ahmetsirim.json

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

/**
 * Covers string scanning: the only token type whose lexeme and value
 * differ, because escape sequences are decoded while scanning.
 *
 * JSON inputs are written as ordinary Kotlin strings, so every JSON
 * backslash appears as a doubled backslash here. Each test states the
 * decoded value, which is what the parser will eventually hand to
 * user code.
 */
class TokenizerStringTest {

    private fun singleToken(input: String): Token = Tokenizer(input).tokenize().first()

    @Test
    fun `scans a plain string`() {
        singleToken("\"hello\"") shouldBe Token.StringValue("hello")
    }

    @Test
    fun `scans the empty string`() {
        singleToken("\"\"") shouldBe Token.StringValue("")
    }

    @Test
    fun `decodes the simple escape sequences`() {
        singleToken("\"q:\\\" b:\\\\ s:\\/ nl:\\n cr:\\r tab:\\t bs:\\b\"") shouldBe
            Token.StringValue("q:\" b:\\ s:/ nl:\n cr:\r tab:\t bs:\b")
    }

    /**
     * Kotlin has no \f escape of its own, so the decoded form feed is
     * asserted through its code point instead of a string literal.
     */
    @Test
    fun `decodes the form feed escape to code point 12`() {
        val token = singleToken("\"\\f\"") as Token.StringValue

        token.value.single().code shouldBe 0x0C
    }

    @Test
    fun `decodes a unicode escape`() {
        singleToken("\"caf\\u00e9\"") shouldBe Token.StringValue("café")
    }

    @Test
    fun `accepts uppercase hex digits in unicode escapes`() {
        singleToken("\"\\u00E9\"") shouldBe Token.StringValue("é")
    }

    /**
     * Characters outside the Basic Multilingual Plane arrive as TWO
     * consecutive unicode escapes (a UTF-16 surrogate pair). Appending
     * each decoded unit to a Kotlin String pairs them up naturally,
     * because Kotlin strings are UTF-16 under the hood.
     */
    @Test
    fun `decodes a surrogate pair written as two unicode escapes`() {
        singleToken("\"\\ud83d\\ude00\"") shouldBe Token.StringValue("😀")
    }

    @Test
    fun `rejects an unterminated string`() {
        val failure = shouldThrow<JsonParseException> {
            Tokenizer("\"abc").tokenize()
        }

        failure.message shouldContain "Unterminated"
    }

    @Test
    fun `rejects an unknown escape sequence`() {
        val failure = shouldThrow<JsonParseException> {
            Tokenizer("\"\\x\"").tokenize()
        }

        failure.message shouldContain "\\x"
    }

    /**
     * RFC 8259 forbids raw control characters (U+0000..U+001F) inside
     * strings; they must be escaped. A literal newline inside a string
     * is therefore an error, not a line break.
     */
    @Test
    fun `rejects a raw control character inside a string`() {
        shouldThrow<JsonParseException> {
            Tokenizer("\"a\nb\"").tokenize()
        }
    }

    @Test
    fun `rejects a unicode escape with a non-hex digit`() {
        shouldThrow<JsonParseException> {
            Tokenizer("\"\\u00g1\"").tokenize()
        }
    }

    /**
     * Kotlin's toIntOrNull(radix) quietly accepts a leading sign, so a
     * naive hex parse would let "\u+0FF" through. The scanner must
     * demand four actual hex DIGITS, not "whatever toIntOrNull takes".
     */
    @Test
    fun `rejects a unicode escape with a leading sign`() {
        shouldThrow<JsonParseException> {
            Tokenizer("\"\\u+0FF\"").tokenize()
        }
    }

    @Test
    fun `rejects a unicode escape cut short by end of input`() {
        shouldThrow<JsonParseException> {
            Tokenizer("\"\\u00").tokenize()
        }
    }
}
