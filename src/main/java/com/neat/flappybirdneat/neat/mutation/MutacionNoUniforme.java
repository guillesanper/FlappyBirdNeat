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
public class MutacionNoUniforme implements MutacionStrategy {

    private final double magnitudeInicial;
    private final int maxGeneraciones;
    private final double beta;
    private int generacionActual;

    public MutacionNoUniforme(int maxGeneraciones) {
        this.magnitudeInicial = 0.2;
        this.maxGeneraciones = maxGeneraciones;
        this.beta = 2.0;
        this.generacionActual = 0;
    }

    public MutacionNoUniforme(double magnitudeInicial, int maxGeneraciones, double beta) {
        this.magnitudeInicial = magnitudeInicial;
        this.maxGeneraciones = maxGeneraciones;
        this.beta = beta;
        this.generacionActual = 0;
    }

    @Override
    public void mutate(NeuralNetwork network, double mutationRate) {
        // Calcular magnitud actual y aplicarla realmente a los pesos
        double t = (double) generacionActual / maxGeneraciones;
        double magnitudeActual = magnitudeInicial * Math.pow(1 - t, beta);

        network.mutate(mutationRate, magnitudeActual);
    }

    @Override
    public void update(int generation) {
        this.generacionActual = generation;
    }

    public double getMagnitudeActual() {
        double t = (double) generacionActual / maxGeneraciones;
        return magnitudeInicial * Math.pow(1 - t, beta);
    }
}
