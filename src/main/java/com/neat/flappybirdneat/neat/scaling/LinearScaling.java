package com.neat.flappybirdneat.neat.scaling;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;

/**
 * Escalado lineal de fitness.
 * Aplica una transformación lineal: f' = a*f + b
 * Evita fitness negativos que podrían causar problemas en la selección.
 */
public class LinearScaling implements ScalingStrategy {

    private final double a;
    private final double b;

    public LinearScaling() {
        this.a = 1.5;
        this.b = 0.5;
    }

    public LinearScaling(double a, double b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public void scaleFitness(FlappyBirdAgent[] agents) {
        double maxFitness = Double.MIN_VALUE;
        double minFitness = Double.MAX_VALUE;

        // Encontrar máximo y mínimo
        for (FlappyBirdAgent agent : agents) {
            double fit = agent.getFitness();
            if (fit > maxFitness) maxFitness = fit;
            if (fit < minFitness) minFitness = fit;
        }

        double gmin = 0; // Valor mínimo permitido para evitar fitness negativos

        // Aplicar escalado lineal
        for (FlappyBirdAgent agent : agents) {
            double fit = agent.getFitness();
            double scaledFitness = Math.max(gmin, a * fit + b);
            agent.setFitness(scaledFitness);
        }
    }
}
