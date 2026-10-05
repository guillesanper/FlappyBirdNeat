package com.neat.flappybirdneat.neat.mutation;

import com.neat.flappybirdneat.neural.NeuralNetwork;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MutationOperatorsTest {

    private static final int INPUT_SIZE = 4;
    private static final int HIDDEN_SIZE = 8;
    private static final int OUTPUT_SIZE = 1;

    static Stream<MutacionStrategy> strategies() {
        return Stream.of(new MutacionGaussiana(), new MutacionUniforme(), new MutacionNoUniforme(100));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void zeroMutationRateLeavesNetworkUnchanged(MutacionStrategy strategy) {
        strategy.setRandom(new Random(1));
        NeuralNetwork network = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(2));
        double[] inputs = {0.1, -0.2, 0.3, -0.4};
        double[] before = network.feedForward(inputs);

        strategy.mutate(network, 0.0);

        double[] after = network.feedForward(inputs);
        assertArrayEquals(before, after);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void mutationPreservesNetworkTopology(MutacionStrategy strategy) {
        strategy.setRandom(new Random(3));
        NeuralNetwork network = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(4));

        strategy.mutate(network, 1.0);

        assertEquals(INPUT_SIZE, network.getInputSize());
        assertEquals(HIDDEN_SIZE, network.getHiddenSize());
        assertEquals(OUTPUT_SIZE, network.getOutputSize());
    }

    @Test
    void uniformMutationReplacesWeightsWithinRange() {
        // Regresión: MutacionUniforme delegaba en la mutación gaussiana y nunca aplicaba
        // reemplazo uniforme real. Con tasa 1.0, todo peso mutado debe caer en [-1, 1].
        MutacionUniforme strategy = new MutacionUniforme();
        strategy.setRandom(new Random(6));
        NeuralNetwork network = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(7));

        strategy.mutate(network, 1.0);

        for (double[] row : network.getWeightsInputHidden()) {
            for (double weight : row) {
                assertTrue(weight >= -1.0 && weight <= 1.0);
            }
        }
        for (double[] row : network.getWeightsHiddenOutput()) {
            for (double weight : row) {
                assertTrue(weight >= -1.0 && weight <= 1.0);
            }
        }
    }

    @Test
    void uniformMutationActuallyChangesWeightsWithFullRate() {
        // Con seeds fijas y tasa 1.0, el reemplazo uniforme debe producir pesos distintos
        // a los originales (la probabilidad de que new random == old random es despreciable).
        MutacionUniforme strategy = new MutacionUniforme();
        strategy.setRandom(new Random(8));
        NeuralNetwork network = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(9));
        double[] inputs = {0.1, 0.2, 0.3, 0.4};
        double[] before = network.feedForward(inputs);

        strategy.mutate(network, 1.0);

        double[] after = network.feedForward(inputs);
        assertFalse(java.util.Arrays.equals(before, after));
    }

    @Test
    void nonUniformMutationMagnitudeDecreasesAsGenerationsAdvance() {
        MutacionNoUniforme strategy = new MutacionNoUniforme(0.5, 100, 2.0);

        double initialMagnitude = strategy.getMagnitudeActual();
        strategy.update(50);
        double midMagnitude = strategy.getMagnitudeActual();
        strategy.update(99);
        double lateMagnitude = strategy.getMagnitudeActual();

        assertTrue(midMagnitude < initialMagnitude);
        assertTrue(lateMagnitude < midMagnitude);
    }

    @Test
    void nonUniformMutationActuallyAppliesDecreasingMagnitude() {
        // Regresión: antes de la corrección, mutate() ignoraba la magnitud calculada
        // y usaba siempre la magnitud fija por defecto de NeuralNetwork.mutate(rate).
        MutacionNoUniforme strategy = new MutacionNoUniforme(0.5, 100, 2.0);
        strategy.setRandom(new Random(11));
        strategy.update(90); // Magnitud ya muy pequeña (cerca del final de la evolución)

        NeuralNetwork network = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(12));
        double[][] before = deepCopy(network.getWeightsInputHidden());

        strategy.mutate(network, 1.0);

        double maxDelta = 0;
        double[][] after = network.getWeightsInputHidden();
        for (int i = 0; i < before.length; i++) {
            for (int j = 0; j < before[i].length; j++) {
                maxDelta = Math.max(maxDelta, Math.abs(after[i][j] - before[i][j]));
            }
        }

        double magnitude = strategy.getMagnitudeActual();
        // El ruido gaussiano rara vez excede ~4 desviaciones estándar.
        assertTrue(maxDelta < magnitude * 4,
                "El cambio máximo (" + maxDelta + ") excede con holgura la magnitud esperada (" + magnitude + ")");
    }

    private double[][] deepCopy(double[][] source) {
        double[][] copy = new double[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }
}
