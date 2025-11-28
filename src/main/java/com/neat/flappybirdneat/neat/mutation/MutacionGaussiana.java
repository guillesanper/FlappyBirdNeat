package com.neat.flappybirdneat.neat.mutation;

import com.neat.flappybirdneat.neural.NeuralNetwork;

/**
 * Mutación gaussiana (ya existente en NeuralNetwork).
 * Añade ruido gaussiano a los pesos con una magnitud fija.
 * Esta clase es un wrapper para mantener consistencia con las otras estrategias.
 */
public class MutacionGaussiana implements MutacionStrategy {

    private final double magnitude;

    public MutacionGaussiana() {
        this.magnitude = 0.1;
    }

    public MutacionGaussiana(double magnitude) {
        this.magnitude = magnitude;
    }

    @Override
    public void mutate(NeuralNetwork network, double mutationRate) {
        // Delegar al método mutate existente en NeuralNetwork
        network.mutate(mutationRate);
    }
}
