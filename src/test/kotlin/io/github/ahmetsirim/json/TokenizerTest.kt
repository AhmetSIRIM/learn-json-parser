package io.github.ahmetsirim.json

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

/**
 * Covers the tokenizer's handling of the JSON "skeleton": the six
 * structural characters that shape objects and arrays.
 *
 * The tokenizer is exercised through its public [Tokenizer.tokenize]
 * entry point only, so these tests keep passing if the internal
 * scanning strategy changes.
 */
class TokenizerTest {

    @Test
    fun `tokenizes all six structural characters`() {
        val tokens = Tokenizer("{}[]:,").tokenize()

        tokens shouldBe listOf(
            Token.BeginObject,
            Token.EndObject,
            Token.BeginArray,
            Token.EndArray,
            Token.NameSeparator,
            Token.ValueSeparator,
            Token.EndOfInput,
        )
    }

    /**
     * RFC 8259 limits insignificant whitespace to space, tab, LF and CR.
     * The mix below covers all four in one input.
     */
    @Test
    fun `skips whitespace between tokens`() {
        val tokens = Tokenizer(" \t{\n}\r\n").tokenize()

        tokens shouldBe listOf(Token.BeginObject, Token.EndObject, Token.EndOfInput)
    }

    @Test
    fun `empty input yields only end of input`() {
        Tokenizer("").tokenize() shouldBe listOf(Token.EndOfInput)
    }

    /**
     * The error message must name the offending character; "invalid
     * input" alone would leave the reader searching the whole document.
     */
    @Test
    fun `rejects an unexpected character and names it`() {
        val failure = shouldThrow<JsonParseException> {
            Tokenizer("{#}").tokenize()
        }

        failure.message shouldContain "'#'"
    }

    /**
     * "tru]" starts like the keyword true but is cut short. The scanner
     * must demand the whole word instead of accepting a prefix.
     */
    @Test
    fun `rejects a truncated keyword`() {
        val failure = shouldThrow<JsonParseException> {
            Tokenizer("[tru]").tokenize()
        }

        failure.message shouldContain "true"
    }

    @Test
    fun `tokenizes the three literal keywords`() {
        val tokens = Tokenizer("[true, false, null]").tokenize()

        tokens shouldBe listOf(
            Token.BeginArray,
            Token.TrueLiteral,
            Token.ValueSeparator,
            Token.FalseLiteral,
            Token.ValueSeparator,
            Token.NullLiteral,
            Token.EndArray,
            Token.EndOfInput,
        )
    }
}
