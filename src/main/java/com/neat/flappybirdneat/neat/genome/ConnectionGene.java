package com.neat.flappybirdneat.neat.genome;

/**
 * Gen de conexión del genoma NEAT: enlaza dos nodos (por id) con un peso,
 * puede deshabilitarse (en vez de eliminarse, para preservar historia evolutiva)
 * y lleva un innovation number que permite alinear genes entre genomas distintos
 * en el crossover NEAT (fase 2b).
 */
public class ConnectionGene {
    private final int inNode;
    private final int outNode;
    private double weight;
    private boolean enabled;
    private final int innovationNumber;

    public ConnectionGene(int inNode, int outNode, double weight, boolean enabled, int innovationNumber) {
        this.inNode = inNode;
        this.outNode = outNode;
        this.weight = weight;
        this.enabled = enabled;
        this.innovationNumber = innovationNumber;
    }

    public int getInNode() {
        return inNode;
    }

    public int getOutNode() {
        return outNode;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getInnovationNumber() {
        return innovationNumber;
    }
}
