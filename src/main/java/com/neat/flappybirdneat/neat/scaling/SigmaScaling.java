package com.neat.flappybirdneat.neat.scaling;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;

/**
 * Escalado Sigma (Sigma truncation).
 * Escala el fitness usando la media y desviación estándar:
 * f' = max(0, f - media + 2*sigma)
 * Útil para mantener presión selectiva constante.
 */
public class SigmaScaling implements ScalingStrategy {

    @Override
    public void scaleFitness(FlappyBirdAgent[] agents) {
        double mean = 0;
        double stdDev = 0;
        int n = agents.length;

        // Calcular media
        for (FlappyBirdAgent agent : agents) {
            mean += agent.getFitness();
        }
        mean /= n;

        // Calcular desviación estándar
        for (FlappyBirdAgent agent : agents) {
            stdDev += Math.pow(agent.getFitness() - mean, 2);
        }
        stdDev = Math.sqrt(stdDev / n);

        // Aplicar escalado sigma
        for (FlappyBirdAgent agent : agents) {
            double scaledFitness = Math.max(0, agent.getFitness() - mean + 2 * stdDev);
            agent.setFitness(scaledFitness);
        }
    }
}
