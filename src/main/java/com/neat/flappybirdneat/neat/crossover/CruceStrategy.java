package com.neat.flappybirdneat.neat.crossover;

import com.neat.flappybirdneat.neural.NeuralNetwork;

import java.util.Random;

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

    /**
     * Sustituye el generador aleatorio, para reproducibilidad (tests, semillas fijas).
     * Las estrategias sin aleatoriedad propia pueden ignorar esta llamada.
     * @param random Generador aleatorio a usar
     */
    default void setRandom(Random random) {
    }
}
