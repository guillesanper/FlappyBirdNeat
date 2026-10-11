package com.neat.flappybirdneat.neat.scaling;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ScalingOperatorsTest {

    static Stream<ScalingStrategy> strategies() {
        return Stream.of(new LinearScaling(), new SigmaScaling(), new BoltzmannScaling(100.0));
    }

    private FlappyBirdAgent[] agentsWithFitness(double... fitness) {
        FlappyBirdAgent[] agents = new FlappyBirdAgent[fitness.length];
        for (int i = 0; i < fitness.length; i++) {
            agents[i] = new FlappyBirdAgent(4, 8, 1, new Random(i));
            agents[i].setFitness(fitness[i]);
        }
        return agents;
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void scalingPreservesPopulationSize(ScalingStrategy strategy) {
        FlappyBirdAgent[] agents = agentsWithFitness(1, 5, 10, 20, 50);

        strategy.scaleFitness(agents);

        assertEquals(5, agents.length);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void scalingNeverProducesNegativeFitness(ScalingStrategy strategy) {
        FlappyBirdAgent[] agents = agentsWithFitness(-10, 0, 5, 100, 1000);

        strategy.scaleFitness(agents);

        for (FlappyBirdAgent agent : agents) {
            assertTrue(agent.getFitness() >= 0, "Fitness negativo tras escalado: " + agent.getFitness());
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void scalingPreservesRelativeOrderingOfDistinctFitness(ScalingStrategy strategy) {
        FlappyBirdAgent[] agents = agentsWithFitness(1, 5, 10, 20, 50);

        strategy.scaleFitness(agents);

        for (int i = 0; i < agents.length - 1; i++) {
            assertTrue(
                    agents[i].getFitness() <= agents[i + 1].getFitness(),
                    "El escalado invirtió el orden relativo del fitness");
        }
    }

    private static double mean(FlappyBirdAgent[] agents) {
        double sum = 0;
        for (FlappyBirdAgent agent : agents) sum += agent.getFitness();
        return sum / agents.length;
    }

    @Test
    void linearScalingKeepsTheMeanAndGivesTheBestCTimesIt() {
        FlappyBirdAgent[] agents = agentsWithFitness(100, 120, 140, 160, 180);
        double meanBefore = mean(agents);

        new LinearScaling(2.0).scaleFitness(agents);

        assertEquals(meanBefore, mean(agents), 1e-9);
        assertEquals(2.0 * meanBefore, agents[4].getFitness(), 1e-9);
    }

    @Test
    void linearScalingPinsTheWorstToZeroWhenFullPressureWouldMakeItNegative() {
        // Media 88: llevar el mejor a 2 * 88 dejaría al peor (40) negativo, así que se usa la
        // recta que lleva el mínimo a 0 conservando la media
        FlappyBirdAgent[] agents = agentsWithFitness(40, 100, 100, 100, 100);
        double meanBefore = mean(agents);

        new LinearScaling(2.0).scaleFitness(agents);

        assertEquals(meanBefore, mean(agents), 1e-9);
        assertEquals(0.0, agents[0].getFitness(), 1e-9);
        assertTrue(agents[4].getFitness() < 2.0 * meanBefore, "la presión queda por debajo de C");
    }

    @Test
    void linearScalingCompressesADominantBestToCTimesTheMean() {
        // Un superindividuo: sin escalado acapararía casi toda la ruleta
        FlappyBirdAgent[] agents = agentsWithFitness(0, 10, 10, 10, 1000);
        double meanBefore = mean(agents);

        new LinearScaling(2.0).scaleFitness(agents);

        assertEquals(meanBefore, mean(agents), 1e-9);
        assertEquals(2.0 * meanBefore, agents[4].getFitness(), 1e-9);
        for (FlappyBirdAgent agent : agents) assertTrue(agent.getFitness() >= 0);
    }

    @Test
    void linearScalingLeavesEqualFitnessUnchanged() {
        FlappyBirdAgent[] agents = agentsWithFitness(50, 50, 50);

        new LinearScaling(2.0).scaleFitness(agents);

        for (FlappyBirdAgent agent : agents) assertEquals(50.0, agent.getFitness(), 1e-9);
    }

    @Test
    void linearScalingRejectsPressureBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> new LinearScaling(0.5));
    }

    @Test
    void boltzmannScalingStaysFiniteForHighFitness() {
        FlappyBirdAgent[] agents = agentsWithFitness(80000, 20000, 500);
        new BoltzmannScaling(100.0).scaleFitness(agents);

        for (FlappyBirdAgent agent : agents) {
            assertTrue(Double.isFinite(agent.getFitness()), "fitness escalado no finito: " + agent.getFitness());
        }
        assertTrue(agents[0].getFitness() > agents[1].getFitness());
        assertTrue(agents[1].getFitness() >= agents[2].getFitness());
    }

    @Test
    void boltzmannScalingCoolsTemperatureAfterEachApplication() {
        BoltzmannScaling strategy = new BoltzmannScaling(100.0, 0.9);
        FlappyBirdAgent[] agents = agentsWithFitness(1, 2, 3);

        strategy.scaleFitness(agents);

        assertEquals(90.0, strategy.getTemperature(), 1e-9);
    }
}
