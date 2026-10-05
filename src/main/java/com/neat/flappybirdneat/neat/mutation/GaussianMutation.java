package com.neat.flappybirdneat.neat.mutation;

import com.neat.flappybirdneat.neural.NeuralNetwork;

/**
 * Mutación gaussiana (ya existente en NeuralNetwork).
 * Añade ruido gaussiano a los pesos con una magnitud fija.
 * Esta clase es un wrapper para mantener consistencia con las otras estrategias.
 */
public class GaussianMutation implements MutationStrategy {

    private final double magnitude;

    public GaussianMutation() {
        this.magnitude = 0.1;
    }

    public GaussianMutation(double magnitude) {
        this.magnitude = magnitude;
    }

    @Override
    public void mutate(NeuralNetwork network, double mutationRate) {
        // Delegar en NeuralNetwork con la magnitud configurada (sigma del ruido gaussiano)
        network.mutate(mutationRate, magnitude);
    }
}
