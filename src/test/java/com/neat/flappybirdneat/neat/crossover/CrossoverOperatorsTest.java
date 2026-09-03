package com.neat.flappybirdneat.neat.crossover;

import com.neat.flappybirdneat.neural.NeuralNetwork;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Invariantes comunes a todas las estrategias de cruce: la red hija debe conservar
 * la topología de los padres y, para las estrategias con aleatoriedad propia,
 * ser determinista con la misma semilla.
 */
class CrossoverOperatorsTest {

    private static final int INPUT_SIZE = 4;
    private static final int HIDDEN_SIZE = 8;
    private static final int OUTPUT_SIZE = 1;

    static Stream<CruceStrategy> strategies() {
        return Stream.of(new CruceUniforme(), new CrucePuntoUnico(), new CruceAritmetico());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void childPreservesParentTopology(CruceStrategy strategy) {
        strategy.setRandom(new Random(1));
        NeuralNetwork parent1 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(10));
        NeuralNetwork parent2 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(20));

        NeuralNetwork child = strategy.crossover(parent1, parent2);

        assertEquals(INPUT_SIZE, child.getInputSize());
        assertEquals(HIDDEN_SIZE, child.getHiddenSize());
        assertEquals(OUTPUT_SIZE, child.getOutputSize());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void sameSeedProducesIdenticalChild(CruceStrategy strategy) {
        NeuralNetwork parent1 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(10));
        NeuralNetwork parent2 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(20));
        double[] inputs = {0.1, 0.2, 0.3, 0.4};

        strategy.setRandom(new Random(55));
        NeuralNetwork childA = strategy.crossover(parent1, parent2);

        strategy.setRandom(new Random(55));
        NeuralNetwork childB = strategy.crossover(parent1, parent2);

        assertArrayEquals(childA.feedForward(inputs), childB.feedForward(inputs));
    }

    @Test
    void uniformCrossoverGenesComeFromEitherParent() {
        CruceUniforme strategy = new CruceUniforme();
        strategy.setRandom(new Random(3));
        NeuralNetwork parent1 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(1));
        NeuralNetwork parent2 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(2));

        NeuralNetwork child = strategy.crossover(parent1, parent2);

        for (int i = 0; i < INPUT_SIZE; i++) {
            for (int j = 0; j < HIDDEN_SIZE; j++) {
                double value = child.getWeightsInputHidden()[i][j];
                assertTrue(value == parent1.getWeightsInputHidden()[i][j]
                                || value == parent2.getWeightsInputHidden()[i][j],
                        "El gen hijo no proviene de ninguno de los dos padres");
            }
        }
    }

    @Test
    void arithmeticCrossoverIsMidpointForDefaultAlpha() {
        CruceAritmetico strategy = new CruceAritmetico(0.5);
        NeuralNetwork parent1 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(1));
        NeuralNetwork parent2 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(2));

        NeuralNetwork child = strategy.crossover(parent1, parent2);

        for (int i = 0; i < INPUT_SIZE; i++) {
            for (int j = 0; j < HIDDEN_SIZE; j++) {
                double expected = (parent1.getWeightsInputHidden()[i][j]
                        + parent2.getWeightsInputHidden()[i][j]) / 2.0;
                assertEquals(expected, child.getWeightsInputHidden()[i][j], 1e-9);
            }
        }
    }

    @Test
    void arithmeticCrossoverIsDeterministic() {
        // No depende de Random: mismos padres deben producir siempre el mismo hijo.
        CruceAritmetico strategy = new CruceAritmetico(0.3);
        NeuralNetwork parent1 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(1));
        NeuralNetwork parent2 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(2));
        double[] inputs = {0.1, -0.2, 0.3, -0.4};

        NeuralNetwork childA = strategy.crossover(parent1, parent2);
        NeuralNetwork childB = strategy.crossover(parent1, parent2);

        assertArrayEquals(childA.feedForward(inputs), childB.feedForward(inputs));
    }
}
