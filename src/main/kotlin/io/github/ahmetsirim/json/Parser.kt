package io.github.ahmetsirim.json

import io.github.ahmetsirim.json.JsonValue.JsonArray
import io.github.ahmetsirim.json.JsonValue.JsonBoolean
import io.github.ahmetsirim.json.JsonValue.JsonNull
import io.github.ahmetsirim.json.JsonValue.JsonNumber
import io.github.ahmetsirim.json.JsonValue.JsonString

/** Parses a complete JSON document into a [JsonValue] tree. */
fun parseJson(input: String): JsonValue =
    Parser(Tokenizer(input).tokenize()).parseDocument()

/**
 * Recursive-descent parser: one function per grammar production, and
 * the call stack IS the nesting of the document. This is the most
 * direct way to turn a grammar into code; the price is that document
 * depth becomes stack depth, so a hostile deeply-nested input can
 * overflow the stack where an explicit-stack parser would not.
 */
internal class Parser(private val tokens: List<Token>) {

    private var index = 0

    fun parseDocument(): JsonValue {
        val value = parseValue()
        val trailing = advance()
        if (trailing != Token.EndOfInput) {
            throw JsonParseException("Expected end of input but found $trailing")
        }
        return value
    }

    private fun parseValue(): JsonValue =
        when (val token = advance()) {
            Token.TrueLiteral -> JsonBoolean(true)
            Token.FalseLiteral -> JsonBoolean(false)
            Token.NullLiteral -> JsonNull
            is Token.StringValue -> JsonString(token.value)
            is Token.NumberValue -> JsonNumber(token.value)
            Token.BeginArray -> parseArray()
            else -> throw JsonParseException("Expected a value but found $token")
        }

    /**
     * Called with BeginArray already consumed. The empty case is
     * peeked for up front; afterwards the loop invariant is simple:
     * a value, then either ']' (done) or ',' (again). Trailing commas
     * fail naturally because ',' loops back into parseValue, which
     * then meets ']' instead of a value.
     */
    private fun parseArray(): JsonValue {
        if (peek() == Token.EndArray) {
            advance()
            return JsonArray(emptyList())
        }
        val elements = mutableListOf<JsonValue>()
        while (true) {
            elements += parseValue()
            when (val token = advance()) {
                Token.EndArray -> return JsonArray(elements)
                Token.ValueSeparator -> continue
                else -> throw JsonParseException("Expected ',' or ']' in array but found $token")
            }
        }
    }

    private fun peek(): Token = tokens[index]

    private fun advance(): Token = tokens[index++]
}
