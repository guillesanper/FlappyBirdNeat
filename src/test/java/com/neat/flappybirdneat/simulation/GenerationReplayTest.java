package com.neat.flappybirdneat.simulation;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.history.GenerationData;
import com.neat.flappybirdneat.history.HistoryManager;
import com.neat.flappybirdneat.history.RunHistory;
import com.neat.flappybirdneat.neat.EvolvingPopulation;
import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Replaying a recorded generation in the simulation tab: it is faithful (same population, same
 * pipes, no evolution), loops on the same pipes, and leaving it puts the training back.
 */
class GenerationReplayTest {

    private static final int GENERATIONS = 6;

    private static SimulationController trained(SimulationController.Mode mode) {
        SimulationController controller = new SimulationController(20, 800, 600, 31L);
        controller.setMode(mode);
        controller.resetSimulation();
        controller.beginFastSimulation(GENERATIONS);
        controller.trainFast(GENERATIONS, (done, max) -> {}, Runnable::run);
        return controller;
    }

    private static List<GenerationData> generations(SimulationController controller) {
        return controller.getHistoryManager().getCurrentRun().getGenerationDataList();
    }

    /** Steps the live game until every agent dies; returns each agent's fitness. */
    private static List<Double> playUntilAllDie(SimulationController controller) {
        int frames = 0;
        while (!controller.updateFrame()) {
            assertTrue(++frames < 100_000, "the replay never ended");
        }
        List<Double> fitness = new ArrayList<>();
        for (FlappyBirdAgent agent : controller.getPopulation().getAgents()) {
            fitness.add(agent.getFitness());
        }
        return fitness;
    }

    @ParameterizedTest
    @EnumSource(SimulationController.Mode.class)
    void everyGenerationReplaysToItsTrainingFitnessAndRepeatsAfterARestart(SimulationController.Mode mode) {
        SimulationController controller = trained(mode);
        assertEquals(31L, controller.getHistoryManager().getCurrentRun().getSeed());

        for (GenerationData generation : generations(controller)) {
            controller.playGeneration(generation);
            assertTrue(controller.isReplayMode());
            assertTrue(controller.isReplayPipesOriginal());
            assertSame(generation, controller.replayedGenerationProperty().get());
            assertEquals("▶ REPLAY · Generación " + generation.getGenerationNumber(), controller.getReplayBanner());

            List<Double> firstLap = playUntilAllDie(controller);
            double best =
                    firstLap.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
            assertEquals(generation.getBestFitness(), best, "generation " + generation.getGenerationNumber());
            assertEquals(best, controller.getHighlightedAgent().getFitness());

            controller.restartReplay();
            assertEquals(20, controller.aliveCountProperty().get());
            assertEquals(firstLap, playUntilAllDie(controller));
        }
    }

    @ParameterizedTest
    @EnumSource(SimulationController.Mode.class)
    void leavingTheReplayRestoresTheTrainingPopulation(SimulationController.Mode mode) {
        SimulationController controller = trained(mode);
        EvolvingPopulation training = controller.getPopulation();
        FlappyBirdAgent[] agents = training.getAgents().clone();
        int generation = controller.currentGenerationProperty().get();

        // Several replays in a row: the training population is the one set aside by the first
        controller.playGeneration(generations(controller).get(2));
        playUntilAllDie(controller);
        controller.playBestAgentOnly();
        controller.playGeneration(generations(controller).get(0));
        assertNotSame(training, controller.getPopulation());

        controller.exitReplayMode();

        assertSame(training, controller.getPopulation());
        assertArrayEquals(agents, controller.getPopulation().getAgents());
        assertEquals(generation, controller.currentGenerationProperty().get());
        assertFalse(controller.isReplayMode());
        assertFalse(controller.replayModeProperty().get());
        assertNull(controller.replayedGenerationProperty().get());
        assertNull(controller.watchedChampionProperty().get());
        assertFalse(controller.runningProperty().get());
        assertSame(training.getBestAgent(), controller.getHighlightedAgent());
    }

    @ParameterizedTest
    @EnumSource(SimulationController.Mode.class)
    void leavingAChampionReplayRestoresTheTrainingPopulation(SimulationController.Mode mode) {
        SimulationController controller = trained(mode);
        EvolvingPopulation training = controller.getPopulation();

        controller.watchChampion(controller.bestChampion());
        controller.exitReplayMode();

        assertSame(training, controller.getPopulation());
        assertNull(controller.watchedChampionProperty().get());
        assertFalse(controller.isReplayMode());
    }

    @ParameterizedTest
    @EnumSource(SimulationController.Mode.class)
    void aTrainingStartedFromAReplayTrainsTheTrainingPopulation(SimulationController.Mode mode) {
        SimulationController controller = trained(mode);
        EvolvingPopulation training = controller.getPopulation();
        controller.playGeneration(generations(controller).get(1));

        controller.beginFastSimulation(1);

        assertSame(training, controller.getPopulation());
        assertFalse(controller.isReplayMode());
    }

    @ParameterizedTest
    @EnumSource(SimulationController.Mode.class)
    void generationsCannotBeReplayedDuringAFastTraining(SimulationController.Mode mode) {
        SimulationController controller = trained(mode);
        GenerationData generation = generations(controller).get(0);
        controller.beginFastSimulation(3);

        assertThrows(IllegalStateException.class, () -> controller.playGeneration(generation));
    }

    @ParameterizedTest
    @EnumSource(SimulationController.Mode.class)
    void aRunWithoutASeedReplaysOnDeterministicButDifferentPipes(SimulationController.Mode mode) {
        SimulationController controller = trained(mode);
        HistoryManager history = controller.getHistoryManager();
        // As loaded from a history saved before runs recorded their seed
        RunHistory legacy = new RunHistory();
        GenerationData generation = generations(controller).get(3);
        legacy.addGenerationData(generation);
        history.getRunHistories().add(legacy);
        generations(controller).remove(generation);

        controller.playGeneration(generation);
        assertFalse(controller.isReplayPipesOriginal());
        List<Double> firstLap = playUntilAllDie(controller);
        controller.restartReplay();

        assertEquals(firstLap, playUntilAllDie(controller));
    }

    /**
     * The main window offers to replay the best generation when a training ends: by then the
     * training thread must be done, and the replay must keep running once the training has wound up.
     */
    @ParameterizedTest
    @EnumSource(SimulationController.Mode.class)
    void theBestGenerationCanBeReplayedAsSoonAsTheTrainingIsOver(SimulationController.Mode mode) {
        SimulationController controller = new SimulationController(20, 800, 600, 5L);
        controller.setMode(mode);
        controller.resetSimulation();
        controller.fastModeProperty().addListener((obs, wasTraining, training) -> {
            if (!training) {
                controller.playGeneration(controller.getHistoryManager().getBestGeneration());
            }
        });
        controller.beginFastSimulation(3);

        controller.trainFast(3, (done, max) -> {}, Runnable::run);

        assertFalse(controller.isFastMode());
        assertTrue(controller.isReplayMode());
        assertTrue(controller.runningProperty().get());
        assertSame(
                controller.getHistoryManager().getBestGeneration(),
                controller.replayedGenerationProperty().get());
    }
}
