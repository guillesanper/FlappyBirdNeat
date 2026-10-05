package com.neat.flappybirdneat.neat.scaling;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;

/**
 * Interfaz para los métodos de escalado de fitness.
 * El escalado ajusta los valores de fitness para mejorar la selección.
 */
public interface ScalingStrategy {
    /**
     * Escala el fitness de todos los agentes de la población.
     * @param agents Array de agentes
     */
    void scaleFitness(FlappyBirdAgent[] agents);
}
