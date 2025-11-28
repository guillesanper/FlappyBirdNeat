package com.neat.flappybirdneat.neat.scaling;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;

/**
 * Escalado lineal de fitness.
 * Aplica una transformación lineal: f' = a*f + b
 * Evita fitness negativos que podrían causar problemas en la selección.
 */
public class EscaladoLineal implements Escalado {

    private final double a;
    private final double b;

    public EscaladoLineal() {
        this.a = 1.5;
        this.b = 0.5;
    }

    public EscaladoLineal(double a, double b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public void escalarFitness(FlappyBirdAgent[] poblacion) {
        double maxFitness = Double.MIN_VALUE;
        double minFitness = Double.MAX_VALUE;

        // Encontrar máximo y mínimo
        for (FlappyBirdAgent agent : poblacion) {
            double fit = agent.getFitness();
            if (fit > maxFitness) maxFitness = fit;
            if (fit < minFitness) minFitness = fit;
        }

        double gmin = 0; // Valor mínimo permitido para evitar fitness negativos

        // Aplicar escalado lineal
        for (FlappyBirdAgent agent : poblacion) {
            double fit = agent.getFitness();
            double nuevoFitness = Math.max(gmin, a * fit + b);
            agent.setFitness(nuevoFitness);
        }
    }
}
