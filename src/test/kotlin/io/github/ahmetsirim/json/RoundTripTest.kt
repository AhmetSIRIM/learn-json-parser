package io.github.ahmetsirim.json

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * The capstone: parse(serialize(value)) == value across many random
 * trees. Parsing alone can be checked against hand-picked examples;
 * the round-trip law checks the parser and serializer AGAINST EACH
 * OTHER, so any asymmetry between the two (a formatter that drops
 * -0.0's sign, an escape the parser reads but the writer never emits)
 * surfaces without anyone having thought of the example.
 *
 * Every failure message names the seed that produced it; plugging that
 * seed into RandomJsonGenerator replays the exact tree forever.
 */
class RoundTripTest {

    private companion object {
        const val BASE_SEED = 20_260_719L
        const val ROUNDS = 200
    }

    @Test
    fun `compact round-trip returns the original tree`() {
        repeat(ROUNDS) { round ->
            val seed = BASE_SEED + round
            val original = RandomJsonGenerator(seed).generate()

            withClue("seed $seed, document: ${original.toCompactJson()}") {
                parseJson(original.toCompactJson()) shouldBe original
            }
        }
    }

    /**
     * Pretty output threads the same values through newlines and
     * indentation, so this variant additionally exercises the
     * tokenizer's whitespace and line tracking on every tree.
     */
    @Test
    fun `pretty round-trip returns the original tree`() {
        repeat(ROUNDS) { round ->
            val seed = BASE_SEED + round
            val original = RandomJsonGenerator(seed).generate()

            withClue("seed $seed, document: ${original.toPrettyJson()}") {
                parseJson(original.toPrettyJson()) shouldBe original
            }
        }
    }

    /**
     * Text-level fixpoint: compact output is canonical, so one more
     * lap through parse and serialize must not change a byte. Catches
     * value-preserving but text-unstable formatting (e.g. a number
     * printed differently after re-parsing).
     */
    @Test
    fun `compact text is a fixpoint of parse then serialize`() {
        repeat(ROUNDS) { round ->
            val seed = BASE_SEED + round
            val text = RandomJsonGenerator(seed).generate().toCompactJson()

            withClue("seed $seed") {
                parseJson(text).toCompactJson() shouldBe text
            }
        }
    }
}
