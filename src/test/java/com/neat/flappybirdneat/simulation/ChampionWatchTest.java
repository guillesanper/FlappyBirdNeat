package com.neat.flappybirdneat.simulation;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.champion.Champion;
import com.neat.flappybirdneat.champion.ChampionRun;
import com.neat.flappybirdneat.neat.EvolvingPopulation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Exporting the best agent of a UI training as a champion, and watching a champion. */
class ChampionWatchTest {

    private static SimulationController trained(SimulationController.Mode mode, long seed) {
        SimulationController controller = new SimulationController(15, 800, 600, seed);
        controller.setMode(mode);
        controller.resetSimulation();
        controller.beginFastSimulation(5);
        controller.trainFast(5, (done, max) -> {}, Runnable::run);
        return controller;
    }

    @Test
    void noChampionBeforeAnyGeneration() {
        assertNull(new SimulationController(5, 800, 600, 1L).bestChampion());
    }

    @ParameterizedTest
    @EnumSource(SimulationController.Mode.class)
    void bestChampionIsTheRecordAgentWithItsRunMetadata(SimulationController.Mode mode) {
        SimulationController controller = trained(mode, 23L);

        Champion champion = controller.bestChampion();

        assertEquals(mode == SimulationController.Mode.NEAT ? EngineType.NEAT : EngineType.GA, champion.engine());
        assertEquals(23L, champion.metadata().seed());
        assertEquals(
                controller.getHistoryManager().getBestFitnessEver(),
                champion.metadata().fitness());
        assertEquals(15L, champion.metadata().training().get("population"));
        // Replayed on the pipes of its generation it lasts exactly as long as in the training
        long pipes = TrainingEngine.pipeSeed(23L, champion.metadata().generation());
        int cap = (int) SimulationController.getOptimalFitnessThreshold();
        assertEquals(champion.metadata().fitness(), ChampionRun.play(champion, pipes, cap));
    }

    @ParameterizedTest
    @EnumSource(SimulationController.Mode.class)
    void watchingAChampionReplaysItAloneUntilTheSimulationIsReset(SimulationController.Mode mode) {
        Champion champion = trained(mode, 4L).bestChampion();
        // Watched from a controller in the other mode: the population follows the champion
        SimulationController viewer = new SimulationController(15, 800, 600, 9L);

        viewer.watchChampion(champion);

        EvolvingPopulation population = viewer.getPopulation();
        assertEquals(1, population.getAgents().length);
        assertSame(population.getAgents()[0], population.getBestAgent());
        assertEquals(0, population.getAgents()[0].getFitness());
        assertTrue(viewer.isReplayMode());
        assertTrue(viewer.runningProperty().get());
        assertTrue(viewer.getReplayBanner().contains("CAMPEÓN"), viewer.getReplayBanner());
        assertTrue(
                viewer.getReplayBanner().contains("gen " + champion.metadata().generation()), viewer.getReplayBanner());
        assertEquals(
                champion.brain().feedForward(new double[] {0.2, 0.1, 0.5, 0.4})[0],
                population.getAgents()[0].getBrain().feedForward(new double[] {0.2, 0.1, 0.5, 0.4})[0]);

        viewer.updateFrame();
        assertEquals(1, population.getAgents()[0].getFitness());

        viewer.stopSimulation();
        viewer.resetSimulation();
        assertFalse(viewer.isReplayMode());
        assertEquals(15, viewer.getPopulation().getAgents().length);
    }

    @Test
    void theBestAgentReplayKeepsItsOwnBanner() {
        SimulationController controller = trained(SimulationController.Mode.NEAT, 2L);
        controller.watchChampion(controller.bestChampion());
        controller.stopSimulation();

        controller.playBestAgentOnly();

        assertEquals("★ REPRODUCIENDO MEJOR AGENTE ★", controller.getReplayBanner());
    }

    @Test
    void championsCannotBeWatchedDuringAFastTraining() {
        SimulationController controller = trained(SimulationController.Mode.FIXED_MLP, 2L);
        Champion champion = controller.bestChampion();
        controller.beginFastSimulation(3);

        assertThrows(IllegalStateException.class, () -> controller.watchChampion(champion));
    }
}
