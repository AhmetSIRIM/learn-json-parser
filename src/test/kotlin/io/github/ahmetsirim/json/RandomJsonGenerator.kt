package io.github.ahmetsirim.json

import io.github.ahmetsirim.json.JsonValue.JsonArray
import io.github.ahmetsirim.json.JsonValue.JsonBoolean
import io.github.ahmetsirim.json.JsonValue.JsonNull
import io.github.ahmetsirim.json.JsonValue.JsonNumber
import io.github.ahmetsirim.json.JsonValue.JsonObject
import io.github.ahmetsirim.json.JsonValue.JsonString
import kotlin.math.pow
import kotlin.random.Random

/**
 * Generates arbitrary [JsonValue] trees from a seed.
 *
 * Hand-rolled on purpose: a property-testing library (Kotest property,
 * jqwik) would add shrinking and edge-case heuristics, but the lesson
 * is that there is no magic underneath, only a seeded Random and
 * weighted recursion. The seed is the entire contract: same seed,
 * same tree, so a failure report naming its seed reproduces forever.
 */
class RandomJsonGenerator(seed: Long) {

    private val random = Random(seed)

    fun generate(): JsonValue = value(depth = 0)

    /**
     * Containers are only offered below MAX_DEPTH, which is what
     * guarantees termination: past the cutoff every branch is a leaf.
     */
    private fun value(depth: Int): JsonValue {
        val kindCount = if (depth < MAX_DEPTH) 6 else 4
        return when (random.nextInt(kindCount)) {
            0 -> JsonNull
            1 -> JsonBoolean(random.nextBoolean())
            2 -> number()
            3 -> JsonString(text())
            4 -> JsonArray(List(random.nextInt(MAX_CONTAINER_SIZE + 1)) { value(depth + 1) })
            else -> obj(depth)
        }
    }

    private fun obj(depth: Int): JsonObject {
        val entries = LinkedHashMap<String, JsonValue>()
        repeat(random.nextInt(MAX_CONTAINER_SIZE + 1)) {
            entries[text()] = value(depth + 1)
        }
        return JsonObject(entries)
    }

    /**
     * The shapes lean into the serializer's number decisions: plain
     * integers (printed without ".0"), fractions, exponent-scaled
     * magnitudes, and the occasional negative zero, the value most
     * likely to expose a careless formatter.
     */
    private fun number(): JsonNumber = when (random.nextInt(6)) {
        0, 1 -> JsonNumber(random.nextInt(from = -100_000, until = 100_001).toDouble())
        2, 3 -> JsonNumber(random.nextDouble(from = -1_000.0, until = 1_000.0))
        4 -> JsonNumber(random.nextDouble() * 10.0.pow(random.nextInt(from = -20, until = 21)))
        else -> JsonNumber(-0.0)
    }

    private fun text(): String {
        val length = random.nextInt(MAX_TEXT_PIECES + 1)
        return buildString {
            repeat(length) { append(TEXT_POOL[random.nextInt(TEXT_POOL.size)]) }
        }
    }

    private companion object {
        const val MAX_DEPTH = 4
        const val MAX_CONTAINER_SIZE = 4
        const val MAX_TEXT_PIECES = 8

        /**
         * Pool entries are strings, not chars, so a surrogate PAIR can
         * ride along as one piece. Escapes, a control character and
         * non-ASCII text are all present by design: a pool of plain
         * letters would test almost nothing about string handling.
         */
        val TEXT_POOL = listOf(
            "a", "Z", "7", " ", "_",
            "\"", "\\", "/", "\n", "\t", "\u0001",
            "é", "中",
            "😀",
        )
    }
}
