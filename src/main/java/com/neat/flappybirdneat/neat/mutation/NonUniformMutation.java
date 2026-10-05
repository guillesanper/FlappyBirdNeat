package com.neat.flappybirdneat.neat.mutation;

import com.neat.flappybirdneat.neural.NeuralNetwork;

/**
 * Mutación no uniforme.
 * La magnitud de la mutación decrece con las generaciones.
 * Permite mayor exploración al inicio y mayor explotación al final.
 *
 * magnitude(t) = magnitude_inicial * (1 - t/T)^b
 * donde t = generación actual, T = generación máxima, b = parámetro de forma
 */
public class NonUniformMutation implements MutationStrategy {

    private final double initialMagnitude;
    private final int maxGenerations;
    private final double beta;
    private int currentGeneration;

    public NonUniformMutation(int maxGenerations) {
        this.initialMagnitude = 0.2;
        this.maxGenerations = maxGenerations;
        this.beta = 2.0;
        this.currentGeneration = 0;
    }

    public NonUniformMutation(double initialMagnitude, int maxGenerations, double beta) {
        this.initialMagnitude = initialMagnitude;
        this.maxGenerations = maxGenerations;
        this.beta = beta;
        this.currentGeneration = 0;
    }

    @Override
    public void mutate(NeuralNetwork network, double mutationRate) {
        // Calcular magnitud actual y aplicarla realmente a los pesos
        double t = (double) currentGeneration / maxGenerations;
        double currentMagnitude = initialMagnitude * Math.pow(1 - t, beta);

        network.mutate(mutationRate, currentMagnitude);
    }

    @Override
    public void update(int generation) {
        this.currentGeneration = generation;
    }

    public double getCurrentMagnitude() {
        double t = (double) currentGeneration / maxGenerations;
        return initialMagnitude * Math.pow(1 - t, beta);
    }
}
