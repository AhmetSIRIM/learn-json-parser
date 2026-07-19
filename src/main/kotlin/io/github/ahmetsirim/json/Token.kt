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

    /**
     * Emitted exactly once, at the end of every token stream. An explicit
     * end marker lets the parser always look at "the next token" without a
     * null check; the alternative (a nullable peek) spreads null handling
     * across every parse function.
     */
    data object EndOfInput : Token
}
