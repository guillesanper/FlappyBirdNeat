package com.neat.flappybirdneat.neat.genome;

import java.util.HashMap;
import java.util.Map;

/**
 * Registro global de innovation numbers y de ids de nodo, compartido entre todos
 * los genomas de una misma ejecución. Garantiza que dos conexiones equivalentes
 * (mismo par nodo origen/destino), aparecidas en genomas distintos, reciban el
 * mismo innovation number: es lo que permite alinear genes por posición en el
 * crossover NEAT en vez de por orden arbitrario.
 */
public class InnovationTracker {
    private final Map<Long, Integer> connectionInnovations = new HashMap<>();
    private final Map<Integer, Integer> nodeSplitInnovations = new HashMap<>();
    private int nextInnovationNumber = 0;
    private int nextNodeId = 0;

    public int nextNodeId() {
        return nextNodeId++;
    }

    public int getInnovationNumber(int inNode, int outNode) {
        long key = key(inNode, outNode);
        return connectionInnovations.computeIfAbsent(key, k -> nextInnovationNumber++);
    }

    /**
     * Id de nodo para una mutación add-node que parte la conexión con el innovation
     * number dado. Si dos genomas distintos parten la misma conexión (mismo innovation
     * number), reciben el mismo id de nodo nuevo: así el crossover puede alinear ese
     * nodo entre ambos genomas en vez de tratarlo como una estructura no relacionada.
     */
    public int getNodeIdForSplit(int connectionInnovationNumber) {
        return nodeSplitInnovations.computeIfAbsent(connectionInnovationNumber, k -> nextNodeId());
    }

    private long key(int inNode, int outNode) {
        return ((long) inNode << 32) | (outNode & 0xFFFFFFFFL);
    }
}
