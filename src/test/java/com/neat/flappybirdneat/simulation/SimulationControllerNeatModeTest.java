package com.neat.flappybirdneat.simulation;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.neat.genome.Genome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que el modo NEAT funciona de punta a punta a través de {@link SimulationController}:
 * los agentes llevan un {@link Genome} como cerebro, el bucle de juego headless los hace jugar
 * y morir con normalidad, y evolucionar una generación no rompe nada (tamaño de población
 * constante, generación avanza, hay al menos una especie).
 */
class SimulationControllerNeatModeTest {

    @Test
    void neatModeRunsAgentsWithGenomeBrains() {
        SimulationController controller = new SimulationController(20, 800, 600);
        controller.setMode(SimulationController.Mode.NEAT);
        controller.resetSimulation();

        for (FlappyBirdAgent agent : controller.getPopulation().getAgents()) {
            assertInstanceOf(Genome.class, agent.getBrain());
        }
    }

    @Test
    void neatModePlaysAGenerationAndEvolvesWithoutErrors() {
        SimulationController controller = new SimulationController(20, 800, 600);
        controller.setMode(SimulationController.Mode.NEAT);
        controller.resetSimulation();
        controller.runningProperty().set(true);

        // Jugar hasta que todos los agentes mueran (como haría el bucle de juego real).
        boolean allDead = false;
        int safetyLimit = 100_000;
        while (!allDead && safetyLimit-- > 0) {
            allDead = controller.updateFrame();
        }
        assertTrue(safetyLimit > 0, "Los agentes deberían morir en un tiempo razonable");

        int generationBefore = controller.getPopulation().getGeneration();
        controller.nextGeneration();

        assertEquals(generationBefore + 1, controller.getPopulation().getGeneration());
        assertEquals(20, controller.getPopulation().getAgents().length);
        assertTrue(controller.getSpeciesCount() >= 1);

        for (FlappyBirdAgent agent : controller.getPopulation().getAgents()) {
            assertInstanceOf(Genome.class, agent.getBrain());
            assertFalse(agent.isDead(), "Los agentes deben resetearse (vivos) para la nueva generación");
        }
    }

    @Test
    void fixedMlpModeReportsNoSpecies() {
        SimulationController controller = new SimulationController(10, 800, 600);
        controller.setMode(SimulationController.Mode.FIXED_MLP);
        controller.resetSimulation();

        assertEquals(-1, controller.getSpeciesCount());
    }
}
