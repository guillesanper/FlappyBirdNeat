package com.neat.flappybirdneat.neat.crossover;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.neural.NeuralNetwork;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Invariantes comunes a todas las estrategias de cruce: la red hija debe conservar
 * la topología de los padres y, para las estrategias con aleatoriedad propia,
 * ser determinista con la misma semilla.
 */
class CrossoverOperatorsTest {

    /** Generador que se pasa a las estrategias en cada llamada (JUnit crea una instancia por test). */
    private Random random = new Random(0);

    private static final int INPUT_SIZE = 4;
    private static final int HIDDEN_SIZE = 8;
    private static final int OUTPUT_SIZE = 1;

    static Stream<CrossoverStrategy> strategies() {
        return Stream.of(new UniformCrossover(), new SinglePointCrossover(), new ArithmeticCrossover());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void childPreservesParentTopology(CrossoverStrategy strategy) {
        random = new Random(1);
        NeuralNetwork parent1 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(10));
        NeuralNetwork parent2 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(20));

        NeuralNetwork child = strategy.crossover(parent1, parent2, random);

        assertEquals(INPUT_SIZE, child.getInputSize());
        assertEquals(HIDDEN_SIZE, child.getHiddenSize());
        assertEquals(OUTPUT_SIZE, child.getOutputSize());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void sameSeedProducesIdenticalChild(CrossoverStrategy strategy) {
        NeuralNetwork parent1 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(10));
        NeuralNetwork parent2 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(20));
        double[] inputs = {0.1, 0.2, 0.3, 0.4};

        random = new Random(55);
        NeuralNetwork childA = strategy.crossover(parent1, parent2, random);

        random = new Random(55);
        NeuralNetwork childB = strategy.crossover(parent1, parent2, random);

        assertArrayEquals(childA.feedForward(inputs), childB.feedForward(inputs));
    }

    @Test
    void uniformCrossoverGenesComeFromEitherParent() {
        UniformCrossover strategy = new UniformCrossover();
        random = new Random(3);
        NeuralNetwork parent1 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(1));
        NeuralNetwork parent2 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(2));

        NeuralNetwork child = strategy.crossover(parent1, parent2, random);

        for (int i = 0; i < INPUT_SIZE; i++) {
            for (int j = 0; j < HIDDEN_SIZE; j++) {
                double value = child.getWeightsInputHidden()[i][j];
                assertTrue(
                        value == parent1.getWeightsInputHidden()[i][j]
                                || value == parent2.getWeightsInputHidden()[i][j],
                        "El gen hijo no proviene de ninguno de los dos padres");
            }
        }
    }

    @Test
    void arithmeticCrossoverIsMidpointForDefaultAlpha() {
        ArithmeticCrossover strategy = new ArithmeticCrossover(0.5);
        NeuralNetwork parent1 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(1));
        NeuralNetwork parent2 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(2));

        NeuralNetwork child = strategy.crossover(parent1, parent2, random);

        for (int i = 0; i < INPUT_SIZE; i++) {
            for (int j = 0; j < HIDDEN_SIZE; j++) {
                double expected = (parent1.getWeightsInputHidden()[i][j] + parent2.getWeightsInputHidden()[i][j]) / 2.0;
                assertEquals(expected, child.getWeightsInputHidden()[i][j], 1e-9);
            }
        }
    }

    @Test
    void arithmeticCrossoverIsDeterministic() {
        // No depende de Random: mismos padres deben producir siempre el mismo hijo.
        ArithmeticCrossover strategy = new ArithmeticCrossover(0.3);
        NeuralNetwork parent1 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(1));
        NeuralNetwork parent2 = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(2));
        double[] inputs = {0.1, -0.2, 0.3, -0.4};

        NeuralNetwork childA = strategy.crossover(parent1, parent2, random);
        NeuralNetwork childB = strategy.crossover(parent1, parent2, random);

        assertArrayEquals(childA.feedForward(inputs), childB.feedForward(inputs));
    }
}
