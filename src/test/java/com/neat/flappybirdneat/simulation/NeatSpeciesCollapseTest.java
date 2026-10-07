package com.neat.flappybirdneat.simulation;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.config.GeneticOperatorsConfig;
import org.junit.jupiter.api.Test;

/**
 * Regression test for NEAT species collapse. With a fixed compatibility threshold and no stagnation
 * handling, these seeds (unsolved in the original 30-seed study) ended every one of their last
 * generations with a single species stuck around three pipes, so speciation stopped protecting
 * anything. A dynamic threshold and stagnation culling must keep several species alive.
 */
class NeatSpeciesCollapseTest {

    private static final int GENERATIONS = 150;
    private static final int TAIL = 50;

    @Test
    void unsolvedSeedsNoLongerCollapseToASingleSpecies() {
        for (long seed : new long[] {2, 10}) {
            int singleSpeciesTail = 0;
            try (TrainingEngine engine = new TrainingEngine(50, 800, 600, seed)) {
                engine.reset(EngineType.NEAT, new GeneticOperatorsConfig());
                for (int gen = 1; gen <= GENERATIONS; gen++) {
                    GenerationStats stats = engine.runGeneration(20_000);
                    if (gen > GENERATIONS - TAIL && stats.species() == 1) singleSpeciesTail++;
                }
            }
            assertTrue(
                    singleSpeciesTail < TAIL / 2,
                    "seed " + seed + " spent " + singleSpeciesTail + " of its last " + TAIL
                            + " generations with a single species");
        }
    }
}
