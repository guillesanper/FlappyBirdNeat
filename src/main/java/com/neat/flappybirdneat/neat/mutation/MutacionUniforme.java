package com.neat.flappybirdneat.neat.mutation;

import com.neat.flappybirdneat.neural.NeuralNetwork;
import java.util.Random;

/**
 * Mutación uniforme.
 * Reemplaza completamente el peso por un nuevo valor aleatorio en el rango [-1, 1].
 * Proporciona mayor diversidad pero es más disruptiva que la gaussiana.
 */
public class MutacionUniforme implements MutacionStrategy {

    private Random random;

    public MutacionUniforme() {
        this.random = new Random();
    }

    @Override
    public void setRandom(Random random) {
        this.random = random;
    }

    @Override
    public void mutate(NeuralNetwork network, double mutationRate) {
        // Reemplaza completamente (no perturba) cada peso/bias mutado por un nuevo valor en [-1, 1]
        double[][] weightsInputHidden = network.getWeightsInputHidden();
        for (double[] row : weightsInputHidden) {
            for (int j = 0; j < row.length; j++) {
                if (random.nextDouble() < mutationRate) {
                    row[j] = random.nextDouble() * 2 - 1;
                }
            }
        }

        double[] biasHidden = network.getBiasHidden();
        for (int i = 0; i < biasHidden.length; i++) {
            if (random.nextDouble() < mutationRate) {
                biasHidden[i] = random.nextDouble() * 2 - 1;
            }
        }

        double[][] weightsHiddenOutput = network.getWeightsHiddenOutput();
        for (double[] row : weightsHiddenOutput) {
            for (int j = 0; j < row.length; j++) {
                if (random.nextDouble() < mutationRate) {
                    row[j] = random.nextDouble() * 2 - 1;
                }
            }
        }

        double[] biasOutput = network.getBiasOutput();
        for (int i = 0; i < biasOutput.length; i++) {
            if (random.nextDouble() < mutationRate) {
                biasOutput[i] = random.nextDouble() * 2 - 1;
            }
        }
    }
}
