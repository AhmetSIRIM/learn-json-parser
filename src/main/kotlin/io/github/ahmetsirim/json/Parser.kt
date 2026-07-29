package io.github.ahmetsirim.json

import io.github.ahmetsirim.json.JsonValue.JsonArray
import io.github.ahmetsirim.json.JsonValue.JsonBoolean
import io.github.ahmetsirim.json.JsonValue.JsonNull
import io.github.ahmetsirim.json.JsonValue.JsonNumber
import io.github.ahmetsirim.json.JsonValue.JsonObject
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
 *
 * Positions come free with the tokens; the parser never touches the
 * source text, it just points at the token that broke the grammar.
 */
internal class Parser(private val tokens: List<PositionedToken>) {

    private var index = 0

    fun parseDocument(): JsonValue {
        val value = parseValue()
        val trailing = advance()
        if (trailing.token != Token.EndOfInput) {
            fail("Expected end of input but found ${trailing.token}", trailing)
        }
        return value
    }

    private fun parseValue(): JsonValue {
        val positioned = advance()
        return when (val token = positioned.token) {
            Token.TrueLiteral -> JsonBoolean(true)
            Token.FalseLiteral -> JsonBoolean(false)
            Token.NullLiteral -> JsonNull
            is Token.StringValue -> JsonString(token.value)
            is Token.NumberValue -> JsonNumber(token.value)
            Token.BeginArray -> parseArray()
            Token.BeginObject -> parseObject()
            else -> fail("Expected a value but found $token", positioned)
        }
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
            val positioned = advance()
            when (positioned.token) {
                Token.EndArray -> return JsonArray(elements)
                Token.ValueSeparator -> continue
                else -> fail("Expected ',' or ']' in array but found ${positioned.token}", positioned)
            }
        }
    }

    /**
     * Called with BeginObject already consumed. Same separator loop as
     * arrays; the object-specific rules are both about keys: only
     * strings may be keys, and a duplicate key overwrites the earlier
     * entry (last-one-wins, matching JavaScript's JSON.parse; RFC 8259
     * leaves duplicates undefined). A LinkedHashMap keeps document
     * order for faithful re-serialization.
     */
    private fun parseObject(): JsonValue {
        if (peek() == Token.EndObject) {
            advance()
            return JsonObject(emptyMap())
        }
        val entries = LinkedHashMap<String, JsonValue>()
        while (true) {
            val keyToken = advance()
            val key = keyToken.token
            if (key !is Token.StringValue) {
                fail("Object keys must be strings but found $key", keyToken)
            }
            val separator = advance()
            if (separator.token != Token.NameSeparator) {
                fail("Expected ':' after object key but found ${separator.token}", separator)
            }
            entries[key.value] = parseValue()
            val positioned = advance()
            when (positioned.token) {
                Token.EndObject -> return JsonObject(entries)
                Token.ValueSeparator -> continue
                else -> fail("Expected ',' or '}' in object but found ${positioned.token}", positioned)
            }
        }
    }

    private fun peek(): Token = tokens[index].token

    private fun advance(): PositionedToken = tokens[index++]

    private fun fail(message: String, at: PositionedToken): Nothing =
        throw JsonParseException(message, at.position)
}
