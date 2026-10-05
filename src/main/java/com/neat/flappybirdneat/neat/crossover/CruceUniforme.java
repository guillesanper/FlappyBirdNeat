package com.neat.flappybirdneat.neat.crossover;

import com.neat.flappybirdneat.neural.NeuralNetwork;
import java.util.Random;

/**
 * Implementación de cruce uniforme.
 * Cada gen (peso/bias) tiene 50% de probabilidad de venir de cada padre.
 */
public class CruceUniforme implements CruceStrategy {
    private Random random = new Random();

    @Override
    public void setRandom(Random random) {
        this.random = random;
    }

    @Override
    public NeuralNetwork crossover(NeuralNetwork parent1, NeuralNetwork parent2) {
        NeuralNetwork child = new NeuralNetwork(
                parent1.getInputSize(),
                parent1.getHiddenSize(),
                parent1.getOutputSize()
        );

        // Cruzar pesos de entrada a capa oculta
        for (int i = 0; i < parent1.getInputSize(); i++) {
            for (int j = 0; j < parent1.getHiddenSize(); j++) {
                if (random.nextBoolean()) {
                    child.getWeightsInputHidden()[i][j] = parent1.getWeightsInputHidden()[i][j];
                } else {
                    child.getWeightsInputHidden()[i][j] = parent2.getWeightsInputHidden()[i][j];
                }
            }
        }

        // Cruzar pesos y bias de capa oculta
        for (int i = 0; i < parent1.getHiddenSize(); i++) {
            child.getBiasHidden()[i] = random.nextBoolean() ?
                    parent1.getBiasHidden()[i] : parent2.getBiasHidden()[i];

            for (int j = 0; j < parent1.getOutputSize(); j++) {
                if (random.nextBoolean()) {
                    child.getWeightsHiddenOutput()[i][j] = parent1.getWeightsHiddenOutput()[i][j];
                } else {
                    child.getWeightsHiddenOutput()[i][j] = parent2.getWeightsHiddenOutput()[i][j];
                }
            }
        }

        // Cruzar bias de salida
        for (int i = 0; i < parent1.getOutputSize(); i++) {
            child.getBiasOutput()[i] = random.nextBoolean() ?
                    parent1.getBiasOutput()[i] : parent2.getBiasOutput()[i];
        }

        return child;
    }
}
