package io.github.ahmetsirim.json

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

/**
 * Covers line and column reporting, both as structured data on the
 * exception (asserted exactly) and inside the human-readable message.
 *
 * Positions are 1-based like every editor's status bar, so a reported
 * position can be jumped to directly.
 */
class ErrorPositionTest {

    @Test
    fun `tokens carry the position of their first character`() {
        val tokens = Tokenizer("{\n  \"a\"}").tokenize()

        tokens.map { it.position } shouldBe listOf(
            TextPosition(line = 1, column = 1),
            TextPosition(line = 2, column = 3),
            TextPosition(line = 2, column = 6),
            TextPosition(line = 2, column = 7),
        )
    }

    @Test
    fun `tokenizer error on the first line points at the offending column`() {
        val failure = shouldThrow<JsonParseException> {
            Tokenizer("  #").tokenize()
        }

        failure.position shouldBe TextPosition(line = 1, column = 3)
    }

    @Test
    fun `tokenizer error after newlines reports the right line`() {
        val failure = shouldThrow<JsonParseException> {
            Tokenizer("{\n  #\n}").tokenize()
        }

        failure.position shouldBe TextPosition(line = 2, column = 3)
    }

    /**
     * Raw newlines can only ever be consumed as whitespace BETWEEN
     * tokens (strings reject them), so an error inside a token is
     * always on the line where the token started. This input errors
     * inside the keyword on line 3.
     */
    @Test
    fun `error inside a token reports the token's line`() {
        val failure = shouldThrow<JsonParseException> {
            Tokenizer("[\ntrue,\ntrux\n]").tokenize()
        }

        failure.position shouldBe TextPosition(line = 3, column = 1)
    }

    @Test
    fun `unterminated string points one past the input`() {
        val failure = shouldThrow<JsonParseException> {
            Tokenizer("\"abc").tokenize()
        }

        failure.position shouldBe TextPosition(line = 1, column = 5)
    }

    @Test
    fun `windows line endings do not skew the column`() {
        val failure = shouldThrow<JsonParseException> {
            Tokenizer("[\r\n  #]").tokenize()
        }

        failure.position shouldBe TextPosition(line = 2, column = 3)
    }

    /**
     * The parser reports the position of the offending TOKEN, handed
     * over by the tokenizer; the parser itself never looks at text.
     */
    @Test
    fun `parser error carries the offending token's position`() {
        val failure = shouldThrow<JsonParseException> {
            parseJson("[1,\n 2\n false]")
        }

        failure.position shouldBe TextPosition(line = 3, column = 2)
    }

    @Test
    fun `trailing content error points at the extra content`() {
        val failure = shouldThrow<JsonParseException> {
            parseJson("{}\ntrue")
        }

        failure.position shouldBe TextPosition(line = 2, column = 1)
    }

    @Test
    fun `the human-readable message spells out the position`() {
        val failure = shouldThrow<JsonParseException> {
            Tokenizer("{\n  #\n}").tokenize()
        }

        failure.message shouldContain "line 2, column 3"
    }
}
