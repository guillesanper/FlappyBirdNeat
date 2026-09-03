package com.neat.flappybirdneat.neat.scaling;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ScalingOperatorsTest {

    static Stream<Escalado> strategies() {
        return Stream.of(new EscaladoLineal(), new EscaladoSigma(), new EscaladoBoltzmann(100.0));
    }

    private FlappyBirdAgent[] agentsWithFitness(double... fitness) {
        FlappyBirdAgent[] agents = new FlappyBirdAgent[fitness.length];
        for (int i = 0; i < fitness.length; i++) {
            agents[i] = new FlappyBirdAgent(4, 8, 1);
            agents[i].setFitness(fitness[i]);
        }
        return agents;
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void scalingPreservesPopulationSize(Escalado strategy) {
        FlappyBirdAgent[] agents = agentsWithFitness(1, 5, 10, 20, 50);

        strategy.escalarFitness(agents);

        assertEquals(5, agents.length);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void scalingNeverProducesNegativeFitness(Escalado strategy) {
        FlappyBirdAgent[] agents = agentsWithFitness(-10, 0, 5, 100, 1000);

        strategy.escalarFitness(agents);

        for (FlappyBirdAgent agent : agents) {
            assertTrue(agent.getFitness() >= 0, "Fitness negativo tras escalado: " + agent.getFitness());
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void scalingPreservesRelativeOrderingOfDistinctFitness(Escalado strategy) {
        FlappyBirdAgent[] agents = agentsWithFitness(1, 5, 10, 20, 50);

        strategy.escalarFitness(agents);

        for (int i = 0; i < agents.length - 1; i++) {
            assertTrue(agents[i].getFitness() <= agents[i + 1].getFitness(),
                    "El escalado invirtió el orden relativo del fitness");
        }
    }

    @Test
    void linearScalingAppliesExpectedTransform() {
        EscaladoLineal strategy = new EscaladoLineal(2.0, 1.0);
        FlappyBirdAgent[] agents = agentsWithFitness(0, 3, 10);

        strategy.escalarFitness(agents);

        assertEquals(1.0, agents[0].getFitness(), 1e-9);
        assertEquals(7.0, agents[1].getFitness(), 1e-9);
        assertEquals(21.0, agents[2].getFitness(), 1e-9);
    }

    @Test
    void boltzmannScalingCoolsTemperatureAfterEachApplication() {
        EscaladoBoltzmann strategy = new EscaladoBoltzmann(100.0, 0.9);
        FlappyBirdAgent[] agents = agentsWithFitness(1, 2, 3);

        strategy.escalarFitness(agents);

        assertEquals(90.0, strategy.getTemperatura(), 1e-9);
    }
}
