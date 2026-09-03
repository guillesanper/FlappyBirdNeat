package com.neat.flappybirdneat.neural;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class NeuralNetworkTest {

    @Test
    void feedForwardProducesOutputVectorOfExpectedSize() {
        NeuralNetwork network = new NeuralNetwork(4, 8, 1, new Random(42));

        double[] outputs = network.feedForward(new double[]{0.1, -0.2, 0.3, 0.4});

        assertEquals(1, outputs.length);
    }

    @Test
    void feedForwardOutputsAreBoundedBySigmoid() {
        NeuralNetwork network = new NeuralNetwork(4, 8, 1, new Random(1));

        double[] outputs = network.feedForward(new double[]{5, -5, 100, -100});

        for (double output : outputs) {
            assertTrue(output > 0.0 && output < 1.0);
        }
    }

    @Test
    void feedForwardIsDeterministicForSameInputs() {
        NeuralNetwork network = new NeuralNetwork(4, 8, 1, new Random(7));
        double[] inputs = {0.5, -0.5, 0.25, -0.25};

        double[] first = network.feedForward(inputs);
        double[] second = network.feedForward(inputs);

        assertArrayEquals(first, second);
    }

    @Test
    void sameSeedProducesIdenticalWeights() {
        NeuralNetwork a = new NeuralNetwork(4, 8, 1, new Random(123));
        NeuralNetwork b = new NeuralNetwork(4, 8, 1, new Random(123));

        double[] inputs = {0.1, 0.2, 0.3, 0.4};
        assertArrayEquals(a.feedForward(inputs), b.feedForward(inputs));
    }

    @Test
    void differentSeedsProduceDifferentWeights() {
        NeuralNetwork a = new NeuralNetwork(4, 8, 1, new Random(1));
        NeuralNetwork b = new NeuralNetwork(4, 8, 1, new Random(2));

        double[] inputs = {0.1, 0.2, 0.3, 0.4};
        assertNotEquals(a.feedForward(inputs)[0], b.feedForward(inputs)[0]);
    }

    @Test
    void setBrainCopiesWeightsAndBias() {
        NeuralNetwork source = new NeuralNetwork(4, 8, 1, new Random(5));
        NeuralNetwork target = new NeuralNetwork(4, 8, 1, new Random(99));

        target.setBrain(source);

        double[] inputs = {0.3, -0.1, 0.7, -0.9};
        assertArrayEquals(source.feedForward(inputs), target.feedForward(inputs));
    }

    @Test
    void copyConstructorProducesIndependentDeepCopy() {
        NeuralNetwork original = new NeuralNetwork(4, 8, 1, new Random(11));
        NeuralNetwork copy = new NeuralNetwork(original);

        // Mutating the copy heavily must not change the original's behaviour.
        copy.mutate(1.0, 5.0);

        double[] inputs = {0.2, 0.2, 0.2, 0.2};
        double[] originalOutput = original.feedForward(inputs);
        double[] copyOutput = copy.feedForward(inputs);

        assertNotEquals(originalOutput[0], copyOutput[0]);
    }

    @Test
    void mutateWithZeroRateLeavesWeightsUnchanged() {
        NeuralNetwork network = new NeuralNetwork(4, 8, 1, new Random(3));
        double[] inputs = {0.1, -0.4, 0.6, 0.2};
        double[] before = network.feedForward(inputs);

        network.mutate(0.0, 0.5);

        double[] after = network.feedForward(inputs);
        assertArrayEquals(before, after);
    }
}
