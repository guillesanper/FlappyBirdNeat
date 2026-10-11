package com.neat.flappybirdneat.simulation;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.neat.EvolvingPopulation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** The best-agent replay: a one-agent population whose best agent is the replayed one. */
class BestAgentReplayTest {

    private static SimulationController trainedController(SimulationController.Mode mode) {
        SimulationController controller = new SimulationController(10, 800, 600, 11L);
        controller.setMode(mode);
        controller.resetSimulation();
        controller.beginFastSimulation(3);
        controller.trainFast(3, (done, max) -> {}, Runnable::run);
        return controller;
    }

    @ParameterizedTest
    @EnumSource(SimulationController.Mode.class)
    void theReplayedAgentIsTheBestAgentOfItsPopulation(SimulationController.Mode mode) {
        SimulationController controller = trainedController(mode);

        EvolvingPopulation replay = controller.createBestAgentOnlyPopulation();

        assertEquals(1, replay.getAgents().length);
        // The renderer highlights, and the network window shows, the population's best agent:
        // with the GA it used to be a random agent that never played
        assertSame(replay.getAgents()[0], replay.getBestAgent());
    }

    @ParameterizedTest
    @EnumSource(SimulationController.Mode.class)
    void resettingTheSimulationLeavesReplayMode(SimulationController.Mode mode) {
        SimulationController controller = trainedController(mode);
        controller.playBestAgentOnly();
        assertTrue(controller.isReplayMode());

        // "Detener" then "Reiniciar Simulación": the new population must not be drawn as a replay
        controller.stopSimulation();
        controller.resetSimulation();

        assertFalse(controller.isReplayMode());
    }

    @ParameterizedTest
    @EnumSource(SimulationController.Mode.class)
    void startingATrainingLeavesReplayMode(SimulationController.Mode mode) {
        SimulationController controller = trainedController(mode);
        controller.playBestAgentOnly();
        controller.stopSimulation();

        controller.beginFastSimulation(1);

        assertFalse(controller.isReplayMode());
    }
}
