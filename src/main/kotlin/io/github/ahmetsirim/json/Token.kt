package io.github.ahmetsirim.json

/**
 * One lexical unit of a JSON document.
 *
 * A sealed interface so the parser can `when` over tokens exhaustively:
 * adding a token type then breaks compilation at every unhandled site
 * instead of failing at runtime. Names mirror the RFC 8259 grammar
 * productions (begin-object, name-separator, ...) rather than the
 * glyphs, so parser code reads like the spec.
 */
sealed interface Token {
    data object BeginObject : Token
    data object EndObject : Token
    data object BeginArray : Token
    data object EndArray : Token
    data object NameSeparator : Token
    data object ValueSeparator : Token

    data object TrueLiteral : Token
    data object FalseLiteral : Token
    data object NullLiteral : Token

    /**
     * Carries the DECODED value: escape sequences are resolved during
     * scanning, so no later stage ever re-reads the raw lexeme. The
     * alternative (storing the raw text and decoding in the parser)
     * splits string knowledge across two layers for no gain.
     */
    data class StringValue(val value: String) : Token

    /**
     * JSON numbers land in a Double, mirroring the JavaScript data model
     * the format was born from. Integers beyond 2^53 silently lose
     * precision; a production parser offers BigDecimal or raw-text
     * access for that case. One numeric type keeps the value tree small
     * and equality checks trivial.
     */
    data class NumberValue(val value: Double) : Token

    /**
     * Emitted exactly once, at the end of every token stream. An explicit
     * end marker lets the parser always look at "the next token" without a
     * null check; the alternative (a nullable peek) spreads null handling
     * across every parse function.
     */
    data object EndOfInput : Token
}
