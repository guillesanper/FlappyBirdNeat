package com.neat.flappybirdneat.neat.crossover;

import com.neat.flappybirdneat.neural.NeuralNetwork;
import java.util.Random;

/**
 * Implementación de cruce por punto único.
 * Se elige un punto de corte aleatorio y se intercambian los genes.
 */
public class CrucePuntoUnico implements CruceStrategy {
    private final Random random = new Random();

    @Override
    public NeuralNetwork crossover(NeuralNetwork parent1, NeuralNetwork parent2) {
        NeuralNetwork child = new NeuralNetwork(
                parent1.getInputSize(),
                parent1.getHiddenSize(),
                parent1.getOutputSize()
        );

        // Calcular el número total de genes (pesos + bias)
        int totalGenes = parent1.getInputSize() * parent1.getHiddenSize() +
                         parent1.getHiddenSize() +
                         parent1.getHiddenSize() * parent1.getOutputSize() +
                         parent1.getOutputSize();

        // Elegir punto de corte aleatorio
        int crossoverPoint = random.nextInt(totalGenes);
        int currentGene = 0;

        // Cruzar pesos de entrada a capa oculta
        for (int i = 0; i < parent1.getInputSize(); i++) {
            for (int j = 0; j < parent1.getHiddenSize(); j++) {
                if (currentGene < crossoverPoint) {
                    child.getWeightsInputHidden()[i][j] = parent1.getWeightsInputHidden()[i][j];
                } else {
                    child.getWeightsInputHidden()[i][j] = parent2.getWeightsInputHidden()[i][j];
                }
                currentGene++;
            }
        }

        // Cruzar bias de capa oculta
        for (int i = 0; i < parent1.getHiddenSize(); i++) {
            if (currentGene < crossoverPoint) {
                child.getBiasHidden()[i] = parent1.getBiasHidden()[i];
            } else {
                child.getBiasHidden()[i] = parent2.getBiasHidden()[i];
            }
            currentGene++;
        }

        // Cruzar pesos de capa oculta a salida
        for (int i = 0; i < parent1.getHiddenSize(); i++) {
            for (int j = 0; j < parent1.getOutputSize(); j++) {
                if (currentGene < crossoverPoint) {
                    child.getWeightsHiddenOutput()[i][j] = parent1.getWeightsHiddenOutput()[i][j];
                } else {
                    child.getWeightsHiddenOutput()[i][j] = parent2.getWeightsHiddenOutput()[i][j];
                }
                currentGene++;
            }
        }

        // Cruzar bias de salida
        for (int i = 0; i < parent1.getOutputSize(); i++) {
            if (currentGene < crossoverPoint) {
                child.getBiasOutput()[i] = parent1.getBiasOutput()[i];
            } else {
                child.getBiasOutput()[i] = parent2.getBiasOutput()[i];
            }
            currentGene++;
        }

        return child;
    }
}
