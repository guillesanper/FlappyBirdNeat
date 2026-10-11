package com.neat.flappybirdneat.simulation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Fast training runs on a background task; "Detener" (stopSimulation) must end it between
 * generations instead of letting it train every requested generation behind the UI's back.
 */
class FastTrainingStopTest {

    private static final int REQUESTED_GENERATIONS = 200;

    @Test
    void stopSimulationEndsFastTrainingAtTheNextGeneration() {
        SimulationController controller = new SimulationController(10, 800, 600, 4L);
        List<Long> progressUpdates = new ArrayList<>();

        controller.beginFastSimulation(REQUESTED_GENERATIONS);
        // The first progress update arrives after generation 1: press "Detener" right then
        controller.trainFast(
                REQUESTED_GENERATIONS,
                (done, max) -> {
                    progressUpdates.add(done);
                    controller.stopSimulation();
                },
                Runnable::run);

        // Initial point plus the one generation played before the stop
        assertEquals(2, controller.getBestFitnessHistory().size());
        assertEquals(1, controller.getHistoryManager().getCurrentRun().getGenerations());
        assertFalse(controller.runningProperty().get());
        assertFalse(controller.isFastMode());
        assertEquals(List.of(1L, 1L), progressUpdates, "the generation-1 update, then the final one");
    }

    @Test
    void fastTrainingRunsEveryRequestedGenerationWhenNotStopped() {
        SimulationController controller = new SimulationController(10, 800, 600, 4L);

        controller.beginFastSimulation(12);
        controller.trainFast(12, (done, max) -> {}, Runnable::run);

        assertEquals(13, controller.getBestFitnessHistory().size());
        assertFalse(controller.runningProperty().get());
        assertFalse(controller.isFastMode());
    }

    @Test
    void aStoppedRunCanBeFollowedByANewOne() {
        SimulationController controller = new SimulationController(10, 800, 600, 4L);
        controller.beginFastSimulation(50);
        controller.trainFast(50, (done, max) -> controller.stopSimulation(), Runnable::run);

        controller.beginFastSimulation(3);
        controller.trainFast(3, (done, max) -> {}, Runnable::run);

        assertEquals(3, controller.getHistoryManager().getCurrentRun().getGenerations());
    }

    @Test
    void historyRecordsTheNumberOfEveryGeneration() {
        SimulationController controller = new SimulationController(10, 800, 600, 4L);

        controller.beginFastSimulation(4);
        controller.trainFast(4, (done, max) -> {}, Runnable::run);

        assertEquals(
                List.of(1, 2, 3, 4),
                controller.getHistoryManager().getCurrentRun().getGenerationDataList().stream()
                        .map(data -> data.getGenerationNumber())
                        .toList());
    }
}
