package com.neat.flappybirdneat.neat.scaling;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;

/**
 * Escalado Sigma (Sigma truncation).
 * Escala el fitness usando la media y desviación estándar:
 * f' = max(0, f - media + 2*sigma)
 * Útil para mantener presión selectiva constante.
 */
public class EscaladoSigma implements Escalado {

    @Override
    public void escalarFitness(FlappyBirdAgent[] poblacion) {
        double media = 0;
        double desviacion = 0;
        int n = poblacion.length;

        // Calcular media
        for (FlappyBirdAgent agent : poblacion) {
            media += agent.getFitness();
        }
        media /= n;

        // Calcular desviación estándar
        for (FlappyBirdAgent agent : poblacion) {
            desviacion += Math.pow(agent.getFitness() - media, 2);
        }
        desviacion = Math.sqrt(desviacion / n);

        // Aplicar escalado sigma
        for (FlappyBirdAgent agent : poblacion) {
            double nuevoFitness = Math.max(0, agent.getFitness() - media + 2 * desviacion);
            agent.setFitness(nuevoFitness);
        }
    }
}
