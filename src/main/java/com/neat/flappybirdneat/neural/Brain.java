package com.neat.flappybirdneat.neural;

/**
 * Abstracción de "cerebro" de un agente: cualquier estructura capaz de mapear
 * entradas sensoriales a salidas de acción. Implementada por la MLP de topología
 * fija (NeuralNetwork) y por el genoma NEAT de topología evolutiva (Genome).
 */
public interface Brain {
    double[] feedForward(double[] inputs);
}
