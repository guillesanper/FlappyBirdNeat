package com.neat.flappybirdneat.neat.crossover;

import com.neat.flappybirdneat.neural.NeuralNetwork;

/**
 * Interfaz para estrategias de cruce de redes neuronales.
 */
public interface CruceStrategy {
    /**
     * Realiza el cruce entre dos redes neuronales padres.
     * @param parent1 Primera red neuronal padre
     * @param parent2 Segunda red neuronal padre
     * @return Nueva red neuronal hijo resultado del cruce
     */
    NeuralNetwork crossover(NeuralNetwork parent1, NeuralNetwork parent2);
}
