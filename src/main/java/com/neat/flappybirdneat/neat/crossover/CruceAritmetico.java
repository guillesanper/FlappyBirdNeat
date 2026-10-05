package com.neat.flappybirdneat.neat.crossover;

import com.neat.flappybirdneat.neural.NeuralNetwork;

/**
 * Implementación de cruce aritmético.
 * Los genes del hijo son una combinación lineal de los padres: child = alpha * parent1 + (1-alpha) * parent2
 */
public class CruceAritmetico implements CruceStrategy {
    private final double alpha;

    /**
     * Constructor con alpha por defecto (0.5)
     */
    public CruceAritmetico() {
        this.alpha = 0.5;
    }

    /**
     * Constructor con alpha personalizado
     * @param alpha Peso del primer padre (entre 0 y 1)
     */
    public CruceAritmetico(double alpha) {
        this.alpha = Math.max(0.0, Math.min(1.0, alpha));
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
                child.getWeightsInputHidden()[i][j] =
                        alpha * parent1.getWeightsInputHidden()[i][j] +
                        (1 - alpha) * parent2.getWeightsInputHidden()[i][j];
            }
        }

        // Cruzar bias de capa oculta
        for (int i = 0; i < parent1.getHiddenSize(); i++) {
            child.getBiasHidden()[i] =
                    alpha * parent1.getBiasHidden()[i] +
                    (1 - alpha) * parent2.getBiasHidden()[i];
        }

        // Cruzar pesos de capa oculta a salida
        for (int i = 0; i < parent1.getHiddenSize(); i++) {
            for (int j = 0; j < parent1.getOutputSize(); j++) {
                child.getWeightsHiddenOutput()[i][j] =
                        alpha * parent1.getWeightsHiddenOutput()[i][j] +
                        (1 - alpha) * parent2.getWeightsHiddenOutput()[i][j];
            }
        }

        // Cruzar bias de salida
        for (int i = 0; i < parent1.getOutputSize(); i++) {
            child.getBiasOutput()[i] =
                    alpha * parent1.getBiasOutput()[i] +
                    (1 - alpha) * parent2.getBiasOutput()[i];
        }

        return child;
    }
}
