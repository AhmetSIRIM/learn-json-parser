package io.github.ahmetsirim.json

/**
 * Converts a JSON document into a flat list of tokens.
 *
 * Tokenizes eagerly into a list rather than streaming tokens on demand:
 * the whole input is already in memory as a String, so the token list
 * costs little extra, and it buys the parser free lookahead and makes
 * failing tests easy to read (the full stream is visible). A streaming
 * lexer only pays off when the input itself arrives as a stream.
 */
class Tokenizer(private val input: String) {

    private var position = 0

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (true) {
            val token = nextToken()
            tokens += token
            if (token == Token.EndOfInput) return tokens
        }
    }

    private fun nextToken(): Token {
        skipWhitespace()
        if (position == input.length) return Token.EndOfInput
        val char = input[position]
        position++
        return when (char) {
            '{' -> Token.BeginObject
            '}' -> Token.EndObject
            '[' -> Token.BeginArray
            ']' -> Token.EndArray
            ':' -> Token.NameSeparator
            ',' -> Token.ValueSeparator
            else -> TODO("unexpected character")
        }
    }

    /**
     * JSON whitespace is exactly space, tab, LF and CR (RFC 8259).
     * Char.isWhitespace() would also accept Unicode spaces such as NBSP,
     * silently widening the grammar beyond the spec.
     */
    private fun skipWhitespace() {
        while (position < input.length && input[position] in JSON_WHITESPACE) {
            position++
        }
    }

    private companion object {
        val JSON_WHITESPACE = charArrayOf(' ', '\t', '\n', '\r')
    }
}
