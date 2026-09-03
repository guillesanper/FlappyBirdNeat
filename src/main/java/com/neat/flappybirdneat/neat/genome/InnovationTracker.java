package com.neat.flappybirdneat.neat.genome;

import java.util.HashMap;
import java.util.Map;

/**
 * Registro global de innovation numbers y de ids de nodo, compartido entre todos
 * los genomas de una misma ejecución. Garantiza que dos conexiones equivalentes
 * (mismo par nodo origen/destino), aparecidas en genomas distintos, reciban el
 * mismo innovation number: es lo que permite alinear genes por posición en el
 * crossover NEAT (fase 2b) en vez de por orden arbitrario.
 */
public class InnovationTracker {
    private final Map<Long, Integer> connectionInnovations = new HashMap<>();
    private int nextInnovationNumber = 0;
    private int nextNodeId = 0;

    public int nextNodeId() {
        return nextNodeId++;
    }

    public int getInnovationNumber(int inNode, int outNode) {
        long key = key(inNode, outNode);
        return connectionInnovations.computeIfAbsent(key, k -> nextInnovationNumber++);
    }

    private long key(int inNode, int outNode) {
        return ((long) inNode << 32) | (outNode & 0xFFFFFFFFL);
    }
}
