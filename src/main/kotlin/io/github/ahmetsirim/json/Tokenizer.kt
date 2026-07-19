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
        return when (input[position]) {
            '{' -> consume(Token.BeginObject)
            '}' -> consume(Token.EndObject)
            '[' -> consume(Token.BeginArray)
            ']' -> consume(Token.EndArray)
            ':' -> consume(Token.NameSeparator)
            ',' -> consume(Token.ValueSeparator)
            't' -> keyword("true", Token.TrueLiteral)
            'f' -> keyword("false", Token.FalseLiteral)
            'n' -> keyword("null", Token.NullLiteral)
            else -> TODO("unexpected character")
        }
    }

    private fun consume(token: Token): Token {
        position++
        return token
    }

    private fun keyword(word: String, token: Token): Token {
        if (!input.startsWith(word, position)) TODO("malformed keyword")
        position += word.length
        return token
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
