package com.neat.flappybirdneat.neat.genome;

/**
 * Tipo de un nodo del genoma NEAT.
 * BIAS es un nodo de entrada especial con valor constante 1.0, en vez de un
 * bias por-nodo como en la MLP densa: así el propio bias es una conexión más
 * y puede mutar/desaparecer con las mutaciones estructurales (fase 2b).
 */
public enum NodeType {
    INPUT,
    BIAS,
    HIDDEN,
    OUTPUT
}
