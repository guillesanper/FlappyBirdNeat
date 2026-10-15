package com.neat.flappybirdneat.simulation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Regresión: al terminar un entrenamiento rápido, running pasaba a false antes que fastMode, así
 * que quien escuchaba el fin (la oferta de reproducir la mejor generación) lo veía aún en curso y
 * el replay se rechazaba. Con "Detener", running caía mientras el hilo seguía usando el motor.
 */
class FastTrainingEndTest {

    @Test
    void listenersOfTheEndOfTrainingSeeItFinished() {
        SimulationController controller = new SimulationController(10, 800, 600, 2L);
        List<Boolean> fastModeWhenStopped = new ArrayList<>();
        controller.runningProperty().addListener((obs, wasRunning, running) -> {
            if (wasRunning && !running) fastModeWhenStopped.add(controller.isFastMode());
        });

        controller.beginFastSimulation(2);
        controller.trainFast(2, (done, max) -> {}, Runnable::run);

        assertEquals(List.of(false), fastModeWhenStopped);
    }

    @Test
    void stoppingAFastTrainingKeepsItRunningUntilItsGenerationEnds() {
        SimulationController controller = new SimulationController(10, 800, 600, 2L);
        controller.beginFastSimulation(50);

        controller.stopSimulation();

        assertTrue(controller.runningProperty().get(), "the training thread still owns the engine");
        controller.trainFast(50, (done, max) -> {}, Runnable::run);
        assertFalse(controller.runningProperty().get());
        assertFalse(controller.isFastMode());
    }
}
