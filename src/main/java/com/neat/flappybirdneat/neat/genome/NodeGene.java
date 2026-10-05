package com.neat.flappybirdneat.neat.genome;

/**
 * Gen de nodo del genoma NEAT: un identificador global y su tipo.
 */
public class NodeGene {
    private final int id;
    private final NodeType type;

    public NodeGene(int id, NodeType type) {
        this.id = id;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public NodeType getType() {
        return type;
    }
}
