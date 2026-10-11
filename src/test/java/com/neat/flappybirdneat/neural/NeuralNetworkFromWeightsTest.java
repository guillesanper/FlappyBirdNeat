package com.neat.flappybirdneat.neural;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class NeuralNetworkFromWeightsTest {

    @Test
    void rebuiltNetworkComputesTheSameOutputs() {
        NeuralNetwork original = new NeuralNetwork(4, 8, 1, new Random(3));

        NeuralNetwork rebuilt = NeuralNetwork.fromWeights(
                original.getWeightsInputHidden(),
                original.getWeightsHiddenOutput(),
                original.getBiasHidden(),
                original.getBiasOutput());

        Random inputs = new Random(9);
        for (int i = 0; i < 100; i++) {
            double[] input = {inputs.nextDouble(), inputs.nextDouble(), inputs.nextDouble(), inputs.nextDouble()};
            assertArrayEquals(original.feedForward(input), rebuilt.feedForward(input));
        }
        assertEquals(4, rebuilt.getInputSize());
        assertEquals(8, rebuilt.getHiddenSize());
        assertEquals(1, rebuilt.getOutputSize());
    }

    @Test
    void theWeightsAreCopied() {
        double[][] inputHidden = {{0.5}};
        NeuralNetwork network =
                NeuralNetwork.fromWeights(inputHidden, new double[][] {{1.0}}, new double[] {0}, new double[] {0});
        double before = network.feedForward(new double[] {1})[0];

        inputHidden[0][0] = 100;

        assertEquals(before, network.feedForward(new double[] {1})[0]);
    }

    @Test
    void mismatchedShapesAreRejected() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> NeuralNetwork.fromWeights(
                        new double[][] {{1, 2}}, new double[][] {{1}}, new double[] {0, 0}, new double[] {0}));
        assertTrue(e.getMessage().contains("weightsHiddenOutput"), e.getMessage());

        assertThrows(
                IllegalArgumentException.class,
                () -> NeuralNetwork.fromWeights(
                        new double[][] {{1, 2}, {1}}, new double[][] {{1}, {1}}, new double[] {0, 0}, new double[] {0
                        }));
        assertThrows(
                IllegalArgumentException.class,
                () -> NeuralNetwork.fromWeights(
                        new double[0][], new double[][] {{1}}, new double[] {0}, new double[] {0}));
    }

    @Test
    void nonFiniteValuesAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> NeuralNetwork.fromWeights(
                        new double[][] {{Double.NaN}}, new double[][] {{1}}, new double[] {0}, new double[] {0}));
        assertThrows(
                IllegalArgumentException.class,
                () -> NeuralNetwork.fromWeights(
                        new double[][] {{1}}, new double[][] {{1}}, new double[] {0}, new double[] {
                            Double.POSITIVE_INFINITY
                        }));
    }
}
