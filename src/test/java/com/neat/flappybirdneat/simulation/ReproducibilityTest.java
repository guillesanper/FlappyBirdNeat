package com.neat.flappybirdneat.simulation;

import com.neat.flappybirdneat.benchmark.BenchmarkPresets;
import com.neat.flappybirdneat.benchmark.BenchmarkResult;
import com.neat.flappybirdneat.benchmark.BenchmarkRunner;
import com.neat.flappybirdneat.config.GeneticOperatorsConfig;
import com.neat.flappybirdneat.neat.EvolvingPopulation;
import com.neat.flappybirdneat.neat.crossover.SinglePointCrossover;
import com.neat.flappybirdneat.neat.mutation.NonUniformMutation;
import com.neat.flappybirdneat.neat.scaling.BoltzmannScaling;
import com.neat.flappybirdneat.neat.selection.ProbabilisticTournamentSelection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end reproducibility: a run is fully determined by its global seed, including with
 * non-default genetic operators (each of which draws from the injected generator) and with
 * replays of the best agent opened mid-run.
 */
class ReproducibilityTest {

    private static final int POPULATION = 30;
    private static final int GENERATIONS = 8;
    private static final int MAX_FRAMES_PER_GENERATION = 5_000;

    @Test
    void sameSeedAndNonDefaultOperatorsGiveIdenticalFitnessCurves() {
        List<List<Double>> first = runWithNonDefaultOperators(1234L, false);
        List<List<Double>> second = runWithNonDefaultOperators(1234L, false);

        assertEquals(first, second);
        assertEquals(GENERATIONS + 1, first.get(0).size(), "initial point plus one per generation");
    }

    @Test
    void differentSeedsGiveDifferentCurvesWithNonDefaultOperators() {
        assertNotEquals(runWithNonDefaultOperators(1L, false), runWithNonDefaultOperators(2L, false));
    }

    @Test
    void openingABestAgentReplayDoesNotPerturbTheRun() {
        // Replays draw from a derived generator, never from the one driving evolution.
        assertEquals(runWithNonDefaultOperators(99L, false), runWithNonDefaultOperators(99L, true));
    }

    @Test
    void evolvingAReplayCopyOfASavedGenerationDoesNotPerturbTheRun() {
        // The replay window evolves an independent copy of a history snapshot.
        assertEquals(runNeat(5L, false), runNeat(5L, true));
    }

    @Test
    void neatModeIsReproducibleOverSeveralGenerations() {
        assertEquals(runNeat(7L, false), runNeat(7L, false));
    }

    @Test
    void benchmarkIsReproducibleForAGivenBaseSeed() {
        List<BenchmarkResult> first = BenchmarkRunner.run(BenchmarkPresets.defaultConfigs(4), 500L, 2, 4,
                15, 800, 600, null);
        List<BenchmarkResult> second = BenchmarkRunner.run(BenchmarkPresets.defaultConfigs(4), 500L, 2, 4,
                15, 800, 600, null);

        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).getMeanCurve(), second.get(i).getMeanCurve());
            assertEquals(first.get(i).getStdDevCurve(), second.get(i).getStdDevCurve());
        }
    }

    /** @return best, average and minimum fitness curves of a Fixed-MLP run with non-default operators. */
    private static List<List<Double>> runWithNonDefaultOperators(long seed, boolean openReplayMidRun) {
        SimulationController controller = new SimulationController(POPULATION, 800, 600, seed);
        GeneticOperatorsConfig operators = controller.getOperatorsConfig();
        operators.setSelectionStrategy(new ProbabilisticTournamentSelection(0.8));
        operators.setScalingStrategy(new BoltzmannScaling(50.0, 0.95));
        operators.setMutationStrategy(new NonUniformMutation(0.3, GENERATIONS, 2.0));
        operators.setCrossoverStrategy(new SinglePointCrossover());
        controller.setMode(SimulationController.Mode.FIXED_MLP);
        controller.resetSimulation();
        controller.runningProperty().set(true);

        for (int gen = 0; gen < GENERATIONS; gen++) {
            playOneGeneration(controller);
            if (openReplayMidRun && gen == GENERATIONS / 2) {
                assertNotNull(controller.createBestAgentOnlyPopulation());
            }
        }
        return List.of(
                new ArrayList<>(controller.getBestFitnessHistory()),
                new ArrayList<>(controller.getAvgFitnessHistory()),
                new ArrayList<>(controller.getMinFitnessHistory()));
    }

    private static List<Double> runNeat(long seed, boolean evolveReplayMidRun) {
        SimulationController controller = new SimulationController(POPULATION, 800, 600, seed);
        controller.setMode(SimulationController.Mode.NEAT);
        controller.resetSimulation();
        controller.runningProperty().set(true);
        for (int gen = 0; gen < GENERATIONS; gen++) {
            playOneGeneration(controller);
            if (evolveReplayMidRun && gen == GENERATIONS / 2) {
                EvolvingPopulation snapshot = controller.getHistoryManager().getCurrentRun()
                        .getGenerationDataList().get(gen).getSavedPopulation();
                EvolvingPopulation replay = snapshot.deepCopy(controller.derivedRandom(gen));
                replay.naturalSelection();
                replay.naturalSelection();
            }
        }
        return new ArrayList<>(controller.getBestFitnessHistory());
    }

    private static void playOneGeneration(SimulationController controller) {
        boolean allDead = false;
        int frames = 0;
        while (!allDead && frames++ < MAX_FRAMES_PER_GENERATION) {
            allDead = controller.updateFrame();
        }
        controller.nextGeneration();
    }
}
