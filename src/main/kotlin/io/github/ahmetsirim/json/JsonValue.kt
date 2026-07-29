package io.github.ahmetsirim.json

/**
 * The parsed form of a JSON document: a small closed tree of six value
 * kinds, one per JSON type.
 *
 * Sealed so a `when` over a JsonValue is exhaustive without an else
 * branch; the serializer milestone leans on that to make "forgot a
 * case" a compile error. Wrapping primitives (JsonString over String)
 * costs an allocation but keeps null, "null" and JsonNull impossible
 * to confuse, which is exactly the confusion untyped trees invite.
 */
sealed interface JsonValue {

    data object JsonNull : JsonValue

    data class JsonBoolean(val value: Boolean) : JsonValue

    data class JsonNumber(val value: Double) : JsonValue

    data class JsonString(val value: String) : JsonValue

    data class JsonArray(val elements: List<JsonValue>) : JsonValue

    /**
     * Entries keep document order (LinkedHashMap under the hood), so
     * serializing later reproduces the input's key order even though
     * JSON itself attaches no meaning to it.
     */
    data class JsonObject(val entries: Map<String, JsonValue>) : JsonValue
}
