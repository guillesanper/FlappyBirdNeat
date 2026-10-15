package com.neat.flappybirdneat.simulation;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.champion.Champion;
import com.neat.flappybirdneat.history.GenerationData;
import com.neat.flappybirdneat.neat.EvolvingPopulation;
import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Replaying a recorded generation in the simulation tab, and going back to the training. */
class GenerationReplayTest {

    private static SimulationController trained(SimulationController.Mode mode, long seed, int generations) {
        SimulationController controller = new SimulationController(15, 800, 600, seed);
        controller.setMode(mode);
        controller.resetSimulation();
        controller.beginFastSimulation(generations);
        controller.trainFast(generations, (done, max) -> {}, Runnable::run);
        return controller;
    }

    /** Plays the current population until every agent dies and returns the best fitness reached. */
    private static double playUntilAllDead(SimulationController controller) {
        int safety = 1_000_000;
        while (!controller.updateFrame()) {
            assertTrue(--safety > 0, "the replay should end");
        }
        double best = 0;
        for (FlappyBirdAgent agent : controller.getPopulation().getAgents()) {
            best = Math.max(best, agent.getFitness());
        }
        return best;
    }

    @ParameterizedTest
    @EnumSource(SimulationController.Mode.class)
    void replayingAGenerationReproducesItsTrainingAndRepeatsItselfOnRestart(SimulationController.Mode mode) {
        SimulationController controller = trained(mode, 7L, 5);
        GenerationData third = controller
                .getHistoryManager()
                .getCurrentRun()
                .getGenerationDataList()
                .get(2);

        controller.playGeneration(third);

        assertTrue(controller.isReplayMode());
        assertSame(third, controller.replayedGenerationProperty().get());
        assertEquals(third.getBestFitness(), playUntilAllDead(controller));
        controller.restartReplay();
        assertEquals(third.getBestFitness(), playUntilAllDead(controller), "same pipes on every loop");
    }

    @Test
    void everyRecordedGenerationKeepsTheSeedOfItsPipes() {
        SimulationController controller = trained(SimulationController.Mode.FIXED_MLP, 11L, 3);

        List<GenerationData> generations =
                controller.getHistoryManager().getCurrentRun().getGenerationDataList();
        for (GenerationData generation : generations) {
            assertEquals(TrainingEngine.pipeSeed(11L, generation.getGenerationNumber()), generation.getPipeSeed());
        }
    }

    @Test
    void leavingAReplayRestoresTheTrainingPopulation() {
        SimulationController controller = trained(SimulationController.Mode.NEAT, 3L, 4);
        EvolvingPopulation training = controller.getPopulation();
        int generation = controller.getPopulation().getGeneration();
        GenerationData first = controller
                .getHistoryManager()
                .getCurrentRun()
                .getGenerationDataList()
                .get(0);

        controller.playGeneration(first);
        assertNotSame(training, controller.getPopulation());
        assertTrue(controller.replayingProperty().get());

        controller.exitReplayMode();

        assertSame(training, controller.getPopulation());
        assertEquals(generation, controller.getPopulation().getGeneration());
        assertFalse(controller.isReplayMode());
        assertFalse(controller.replayingProperty().get());
        assertNull(controller.replayedGenerationProperty().get());
    }

    /**
     * Regresión: tras ver un campeón, empezar un entrenamiento entrenaba la población de un solo
     * agente del replay en vez de la del entrenamiento.
     */
    @Test
    void trainingAfterWatchingAChampionTrainsTheTrainingPopulation() {
        SimulationController controller = trained(SimulationController.Mode.FIXED_MLP, 5L, 3);
        Champion champion = controller.bestChampion();
        EvolvingPopulation training = controller.getPopulation();

        controller.watchChampion(champion);
        assertEquals(1, controller.getPopulation().getAgents().length);
        controller.beginFastSimulation(1);

        assertSame(training, controller.getPopulation());
        assertEquals(15, controller.getPopulation().getAgents().length);
        assertFalse(controller.isReplayMode());
    }
}
