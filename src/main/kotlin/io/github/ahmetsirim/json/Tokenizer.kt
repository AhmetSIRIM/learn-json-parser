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
            '"' -> string()
            '-', in '0'..'9' -> number()
            else -> throw JsonParseException("Unexpected character '${input[position]}'")
        }
    }

    private fun consume(token: Token): Token {
        position++
        return token
    }

    /**
     * Decodes escapes while scanning, so the token already carries the
     * final value. Called with position on the opening quote.
     */
    private fun string(): Token {
        position++
        val builder = StringBuilder()
        while (true) {
            if (position == input.length) {
                throw JsonParseException("Unterminated string")
            }
            when (val char = input[position]) {
                '"' -> {
                    position++
                    return Token.StringValue(builder.toString())
                }
                '\\' -> {
                    position++
                    builder.append(escapeSequence())
                }
                else -> {
                    if (char < ' ') {
                        throw JsonParseException(
                            "Raw control character (code ${char.code}) inside string; use an escape",
                        )
                    }
                    builder.append(char)
                    position++
                }
            }
        }
    }

    /** Called with position just past the backslash. */
    private fun escapeSequence(): Char {
        if (position == input.length) {
            throw JsonParseException("Unterminated string")
        }
        val char = input[position]
        position++
        return when (char) {
            '"' -> '"'
            '\\' -> '\\'
            '/' -> '/'
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> unicodeEscape()
            else -> throw JsonParseException("Unknown escape sequence '\\$char'")
        }
    }

    /**
     * Reads exactly four hex digits and returns the UTF-16 unit they
     * name. A surrogate pair simply arrives as two of these in a row;
     * appending both units to the builder forms the full character, so
     * no pairing logic is needed here.
     */
    private fun unicodeEscape(): Char {
        if (position + 4 > input.length) {
            throw JsonParseException("Unterminated string")
        }
        val hex = input.substring(position, position + 4)
        // toIntOrNull(16) alone would accept a leading sign ("+0FF"),
        // so each character is checked against the hex alphabet first.
        if (hex.any { it.digitToIntOrNull(radix = 16) == null }) {
            throw JsonParseException("Invalid unicode escape '\\u$hex'")
        }
        position += 4
        return hex.toInt(radix = 16).toChar()
    }

    /**
     * number = [ minus ] int [ frac ] [ exp ]  (RFC 8259 section 6)
     *
     * The grammar is validated by hand and only the vetted lexeme is
     * handed to toDouble() for the arithmetic. Kotlin's parser is far
     * more permissive (it takes "+1", "1.", ".5", even "Infinity"), so
     * leaning on it for validation would silently widen the language.
     */
    private fun number(): Token {
        val start = position
        if (currentIs('-')) position++
        scanIntegerPart()
        if (currentIs('.')) {
            position++
            scanRequiredDigits("fraction")
        }
        if (currentIs('e') || currentIs('E')) {
            position++
            if (currentIs('+') || currentIs('-')) position++
            scanRequiredDigits("exponent")
        }
        return Token.NumberValue(input.substring(start, position).toDouble())
    }

    /**
     * int = zero / (digit1-9 *DIGIT). The leading-zero ban exists so a
     * JSON number can never be misread as octal.
     */
    private fun scanIntegerPart() {
        if (!currentIsDigit()) {
            throw JsonParseException("Expected a digit in number")
        }
        val firstDigit = input[position]
        position++
        if (firstDigit == '0') {
            if (currentIsDigit()) {
                throw JsonParseException("Leading zero in number")
            }
        } else {
            while (currentIsDigit()) position++
        }
    }

    private fun scanRequiredDigits(partName: String) {
        if (!currentIsDigit()) {
            throw JsonParseException("Expected a digit in number $partName")
        }
        while (currentIsDigit()) position++
    }

    private fun currentIs(char: Char): Boolean =
        position < input.length && input[position] == char

    private fun currentIsDigit(): Boolean =
        position < input.length && input[position] in '0'..'9'

    private fun keyword(word: String, token: Token): Token {
        if (!input.startsWith(word, position)) {
            throw JsonParseException("Expected keyword '$word'")
        }
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
