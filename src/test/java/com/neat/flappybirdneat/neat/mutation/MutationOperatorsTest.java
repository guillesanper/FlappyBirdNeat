package com.neat.flappybirdneat.neat.mutation;

import com.neat.flappybirdneat.neural.NeuralNetwork;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MutationOperatorsTest {

    /** Generador que se pasa a las estrategias en cada llamada (JUnit crea una instancia por test). */
    private Random random = new Random(0);

    private static final int INPUT_SIZE = 4;
    private static final int HIDDEN_SIZE = 8;
    private static final int OUTPUT_SIZE = 1;

    static Stream<MutationStrategy> strategies() {
        return Stream.of(new GaussianMutation(), new UniformMutation(), new NonUniformMutation(100));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void zeroMutationRateLeavesNetworkUnchanged(MutationStrategy strategy) {
        random = new Random(1);
        NeuralNetwork network = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(2));
        double[] inputs = {0.1, -0.2, 0.3, -0.4};
        double[] before = network.feedForward(inputs);

        strategy.mutate(network, 0.0, random);

        double[] after = network.feedForward(inputs);
        assertArrayEquals(before, after);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void mutationPreservesNetworkTopology(MutationStrategy strategy) {
        random = new Random(3);
        NeuralNetwork network = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(4));

        strategy.mutate(network, 1.0, random);

        assertEquals(INPUT_SIZE, network.getInputSize());
        assertEquals(HIDDEN_SIZE, network.getHiddenSize());
        assertEquals(OUTPUT_SIZE, network.getOutputSize());
    }

    @Test
    void uniformMutationReplacesWeightsWithinRange() {
        // Regresión: UniformMutation delegaba en la mutación gaussiana y nunca aplicaba
        // reemplazo uniforme real. Con tasa 1.0, todo peso mutado debe caer en [-1, 1].
        UniformMutation strategy = new UniformMutation();
        random = new Random(6);
        NeuralNetwork network = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(7));

        strategy.mutate(network, 1.0, random);

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
        UniformMutation strategy = new UniformMutation();
        random = new Random(8);
        NeuralNetwork network = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(9));
        double[] inputs = {0.1, 0.2, 0.3, 0.4};
        double[] before = network.feedForward(inputs);

        strategy.mutate(network, 1.0, random);

        double[] after = network.feedForward(inputs);
        assertFalse(java.util.Arrays.equals(before, after));
    }

    @Test
    void nonUniformMutationMagnitudeDecreasesAsGenerationsAdvance() {
        NonUniformMutation strategy = new NonUniformMutation(0.5, 100, 2.0);

        double initialMagnitude = strategy.getCurrentMagnitude();
        strategy.update(50);
        double midMagnitude = strategy.getCurrentMagnitude();
        strategy.update(99);
        double lateMagnitude = strategy.getCurrentMagnitude();

        assertTrue(midMagnitude < initialMagnitude);
        assertTrue(lateMagnitude < midMagnitude);
    }

    @Test
    void nonUniformMutationActuallyAppliesDecreasingMagnitude() {
        // Regresión: antes de la corrección, mutate() ignoraba la magnitud calculada
        // y usaba siempre la magnitud fija por defecto de NeuralNetwork.mutate(rate).
        NonUniformMutation strategy = new NonUniformMutation(0.5, 100, 2.0);
        random = new Random(11);
        strategy.update(90); // Magnitud ya muy pequeña (cerca del final de la evolución)

        NeuralNetwork network = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(12));
        double[][] before = deepCopy(network.getWeightsInputHidden());

        strategy.mutate(network, 1.0, random);

        double maxDelta = 0;
        double[][] after = network.getWeightsInputHidden();
        for (int i = 0; i < before.length; i++) {
            for (int j = 0; j < before[i].length; j++) {
                maxDelta = Math.max(maxDelta, Math.abs(after[i][j] - before[i][j]));
            }
        }

        double magnitude = strategy.getCurrentMagnitude();
        // El ruido gaussiano rara vez excede ~4 desviaciones estándar.
        assertTrue(maxDelta < magnitude * 4,
                "El cambio máximo (" + maxDelta + ") excede con holgura la magnitud esperada (" + magnitude + ")");
    }

    @Test
    void gaussianMutationUsesConfiguredMagnitude() {
        // Regresión: GaussianMutation ignoraba su magnitud y aplicaba siempre sigma = 0.1.
        GaussianMutation small = new GaussianMutation(0.001);
        random = new Random(5);
        NeuralNetwork network = new NeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE, new Random(6));
        double[][] before = deepCopy(network.getWeightsInputHidden());

        small.mutate(network, 1.0, random);

        double[][] after = network.getWeightsInputHidden();
        for (int i = 0; i < before.length; i++) {
            for (int j = 0; j < before[i].length; j++) {
                assertTrue(Math.abs(after[i][j] - before[i][j]) < 0.001 * 6,
                        "Una mutación con sigma = 0.001 no debería mover un peso más de 6 sigmas");
            }
        }
    }

    private double[][] deepCopy(double[][] source) {
        double[][] copy = new double[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }
}
