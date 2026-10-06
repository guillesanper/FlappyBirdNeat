package com.neat.flappybirdneat.simulation;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.config.GeneticOperatorsConfig;
import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.neural.Brain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Parallel evaluation must not change results: every agent plays its own game over the same
 * per-generation pipes, so the thread count and frame-by-frame vs batch play are irrelevant.
 */
class ParallelEvaluationTest {

    private static final int POPULATION = 50;
    private static final int GENERATIONS = 8;
    private static final int MAX_FRAMES = 5_000;
    private static final long SEED = 2024L;

    private static TrainingEngine newEngine(EngineType type, int threads) {
        TrainingEngine engine = new TrainingEngine(POPULATION, 800, 600, SEED);
        engine.setThreads(threads);
        engine.reset(type, new GeneticOperatorsConfig());
        return engine;
    }

    private static List<GenerationStats> run(EngineType type, int threads) {
        try (TrainingEngine engine = newEngine(type, threads)) {
            List<GenerationStats> stats = new ArrayList<>();
            for (int i = 0; i < GENERATIONS; i++) {
                stats.add(engine.runGeneration(MAX_FRAMES));
            }
            return stats;
        }
    }

    @ParameterizedTest
    @EnumSource(EngineType.class)
    void resultsAreIdenticalWithOneAndManyThreads(EngineType type) {
        List<GenerationStats> sequential = run(type, 1);

        for (int threads : new int[] {2, 4, 8}) {
            assertEquals(sequential, run(type, threads), threads + " threads");
        }
    }

    @ParameterizedTest
    @EnumSource(EngineType.class)
    void frameByFramePlayInTheSharedGameMatchesParallelEvaluation(EngineType type) {
        // The UI steps every agent through one shared game; fast training evaluates them apart
        try (TrainingEngine live = newEngine(type, 1);
                TrainingEngine parallel = newEngine(type, 4)) {
            for (int gen = 0; gen < GENERATIONS; gen++) {
                boolean allDead = false;
                for (int frame = 0; !allDead && frame < MAX_FRAMES; frame++) {
                    allDead = live.step();
                }

                assertEquals(parallel.playGeneration(MAX_FRAMES), live.statistics(), "generation " + (gen + 1));
                live.evolve();
                parallel.evolve();
            }
        }
    }

    @ParameterizedTest
    @EnumSource(EngineType.class)
    void finishingAPartlyPlayedGenerationMatchesPlayingItInOneGo(EngineType type) {
        try (TrainingEngine partly = newEngine(type, 4);
                TrainingEngine whole = newEngine(type, 4)) {
            for (int frame = 0; frame < 30; frame++) {
                partly.step();
            }
            assertEquals(whole.playGeneration(MAX_FRAMES), partly.playGeneration(MAX_FRAMES));
        }
    }

    @ParameterizedTest
    @EnumSource(EngineType.class)
    void noTwoAgentsShareABrain(EngineType type) {
        // Brains keep their last activations for the visualizer, so a brain shared by two agents
        // would be written by two evaluation threads at once
        try (TrainingEngine engine = newEngine(type, 4)) {
            for (int gen = 0; gen < GENERATIONS; gen++) {
                Set<Brain> brains = Collections.newSetFromMap(new IdentityHashMap<>());
                for (FlappyBirdAgent agent : engine.getPopulation().getAgents()) {
                    assertTrue(brains.add(agent.getBrain()), "brain shared in generation " + (gen + 1));
                }
                engine.runGeneration(MAX_FRAMES);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(EngineType.class)
    void pipesDependOnlyOnTheSeedAndTheGeneration(EngineType type) {
        try (TrainingEngine engine = newEngine(type, 2);
                TrainingEngine reference = newEngine(EngineType.GA, 1)) {
            List<Float> gaps = new ArrayList<>();
            for (int gen = 0; gen < 4; gen++) {
                float gap = engine.getGame().getPipes().get(0).getGapY();
                assertEquals(reference.getGame().getPipes().get(0).getGapY(), gap, "generation " + (gen + 1));
                gaps.add(gap);
                engine.runGeneration(MAX_FRAMES);
                reference.runGeneration(MAX_FRAMES);
            }
            assertEquals(4, gaps.stream().distinct().count(), "each generation gets its own pipes");
        }
    }

    @Test
    void pipeSeedsAreStableAndDistinct() {
        assertEquals(TrainingEngine.pipeSeed(42L, 3), TrainingEngine.pipeSeed(42L, 3));
        assertNotEquals(TrainingEngine.pipeSeed(42L, 3), TrainingEngine.pipeSeed(42L, 4));
        assertNotEquals(TrainingEngine.pipeSeed(42L, 3), TrainingEngine.pipeSeed(43L, 3));
    }
}
