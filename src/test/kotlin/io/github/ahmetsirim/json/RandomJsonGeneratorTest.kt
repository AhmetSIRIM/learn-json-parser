package io.github.ahmetsirim.json

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

/**
 * Pins the generator's one contract: the seed decides everything.
 * Reproducibility is what makes random testing debuggable; without it
 * a failing round-trip case evaporates on the next run.
 */
class RandomJsonGeneratorTest {

    @Test
    fun `the same seed generates the same tree`() {
        RandomJsonGenerator(seed = 42L).generate() shouldBe
            RandomJsonGenerator(seed = 42L).generate()
    }

    @Test
    fun `different seeds generate different trees`() {
        val trees = (1L..20L).map { seed -> RandomJsonGenerator(seed).generate() }

        trees.toSet().size shouldNotBe 1
    }
}
