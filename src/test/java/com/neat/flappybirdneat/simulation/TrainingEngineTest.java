package com.neat.flappybirdneat.simulation;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.config.GeneticOperatorsConfig;
import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TrainingEngineTest {

    private static final int POPULATION = 20;
    private static final int MAX_FRAMES = 5_000;

    private static TrainingEngine newEngine(EngineType type, long seed) {
        TrainingEngine engine = new TrainingEngine(POPULATION, 800, 600, seed);
        engine.reset(type, new GeneticOperatorsConfig());
        return engine;
    }

    @ParameterizedTest
    @EnumSource(EngineType.class)
    void playGenerationRunsUntilEveryAgentIsDead(EngineType type) {
        TrainingEngine engine = newEngine(type, 3L);

        GenerationStats stats = engine.playGeneration(MAX_FRAMES);

        assertEquals(1, stats.generation());
        assertEquals(0, stats.alive());
        assertEquals(0, engine.aliveCount());
        // Fitness is frames survived, so the generation lasts exactly as long as its best agent
        assertEquals(stats.best(), stats.frames());
        assertTrue(stats.min() <= stats.mean() && stats.mean() <= stats.best());
        assertFalse(stats.solved(MAX_FRAMES));
    }

    @ParameterizedTest
    @EnumSource(EngineType.class)
    void playGenerationStopsAtTheFrameCap(EngineType type) {
        TrainingEngine engine = newEngine(type, 3L);

        GenerationStats stats = engine.playGeneration(1);

        assertEquals(1, stats.frames());
        assertEquals(1.0, stats.best());
        assertTrue(stats.solved(1));
        assertEquals(POPULATION, stats.alive() + countDead(engine));
    }

    @ParameterizedTest
    @EnumSource(EngineType.class)
    void evolveAdvancesTheGenerationAndResetsTheAgents(EngineType type) {
        TrainingEngine engine = newEngine(type, 3L);

        GenerationStats first = engine.runGeneration(MAX_FRAMES);
        GenerationStats second = engine.playGeneration(MAX_FRAMES);

        assertEquals(1, first.generation());
        assertEquals(2, second.generation());
        assertEquals(2, engine.getGeneration());
        assertEquals(POPULATION, engine.getPopulation().getAgents().length);
    }

    @Test
    void gaStatisticsHaveNoSpeciesOrTopology() {
        GenerationStats stats = newEngine(EngineType.GA, 1L).playGeneration(MAX_FRAMES);

        assertEquals(-1, stats.species());
        assertTrue(Double.isNaN(stats.meanNodes()));
        assertTrue(Double.isNaN(stats.meanConnections()));
        assertTrue(stats.diversity() > 0);
    }

    @Test
    void neatStatisticsReportSpeciesAndTopology() {
        TrainingEngine engine = newEngine(EngineType.NEAT, 1L);

        GenerationStats initial = engine.playGeneration(MAX_FRAMES);
        // Minimal genome: 4 inputs + bias + 1 output, each input and the bias wired to the output
        assertEquals(6.0, initial.meanNodes());
        assertEquals(5.0, initial.meanConnections());

        for (int i = 0; i < 10; i++) {
            engine.evolve();
            engine.playGeneration(MAX_FRAMES);
        }
        GenerationStats later = engine.statistics();
        assertTrue(later.species() >= 1);
        assertTrue(later.meanNodes() >= 6.0);
    }

    @ParameterizedTest
    @EnumSource(EngineType.class)
    void sameSeedGivesIdenticalStatistics(EngineType type) {
        assertEquals(run(type, 11L, 5), run(type, 11L, 5));
    }

    @ParameterizedTest
    @EnumSource(EngineType.class)
    void controllerAndEngineProduceTheSameCurve(EngineType type) {
        // The UI controller is an adapter over the engine: frame-by-frame play through it must give
        // exactly the curve the engine produces on its own with the same seed.
        SimulationController controller = new SimulationController(POPULATION, 800, 600, 21L);
        controller.setMode(
                type == EngineType.NEAT ? SimulationController.Mode.NEAT : SimulationController.Mode.FIXED_MLP);
        controller.resetSimulation();
        controller.runningProperty().set(true);
        for (int gen = 0; gen < 5; gen++) {
            boolean allDead = false;
            int frames = 0;
            while (!allDead && frames++ < MAX_FRAMES) {
                allDead = controller.updateFrame();
            }
            controller.nextGeneration();
        }

        // The controller resets once in its constructor (GA) and once more after setMode
        TrainingEngine engine = new TrainingEngine(POPULATION, 800, 600, 21L);
        engine.reset(EngineType.GA, new GeneticOperatorsConfig());
        engine.reset(type, new GeneticOperatorsConfig());
        List<Double> engineCurve = new ArrayList<>();
        for (int gen = 0; gen < 5; gen++) {
            engineCurve.add(engine.runGeneration(MAX_FRAMES).best());
        }

        assertEquals(engineCurve, controller.getBestFitnessHistory().subList(1, 6));
    }

    private static List<GenerationStats> run(EngineType type, long seed, int generations) {
        TrainingEngine engine = newEngine(type, seed);
        List<GenerationStats> stats = new ArrayList<>();
        for (int i = 0; i < generations; i++) {
            stats.add(engine.runGeneration(MAX_FRAMES));
        }
        return stats;
    }

    private static int countDead(TrainingEngine engine) {
        int dead = 0;
        for (FlappyBirdAgent agent : engine.getPopulation().getAgents()) {
            if (agent.isDead()) dead++;
        }
        return dead;
    }
}
