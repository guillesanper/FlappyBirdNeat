package com.neat.flappybirdneat.neat.mutation;

import com.neat.flappybirdneat.neural.NeuralNetwork;

/**
 * Interfaz para estrategias de mutación de redes neuronales.
 */
public interface MutacionStrategy {
    /**
     * Aplica mutación a una red neuronal.
     * @param network Red neuronal a mutar
     * @param mutationRate Tasa de mutación (probabilidad por peso)
     */
    void mutate(NeuralNetwork network, double mutationRate);

    /**
     * Actualiza parámetros internos de la estrategia (ej. generación actual).
     * @param generation Número de generación actual
     */
    default void update(int generation) {
        // Implementación por defecto: no hacer nada
    }
}
