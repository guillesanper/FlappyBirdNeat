package com.neat.flappybirdneat.neat.selection;

/**
 * Clase auxiliar que encapsula información de un individuo para la selección.
 * Almacena índice, fitness, probabilidad y probabilidad acumulada.
 */
public class Selectable implements Comparable<Selectable> {
    private final int index;
    private final double fitness;
    private double prob;
    private double accProb;

    public Selectable(int index, double fitness) {
        this.index = index;
        this.fitness = fitness;
    }

    public int getIndex() {
        return index;
    }

    public double getFitness() {
        return fitness;
    }

    public double getProb() {
        return prob;
    }

    public void setProb(double prob) {
        this.prob = prob;
    }

    public double getAccProb() {
        return accProb;
    }

    public void setAccProb(double accProb) {
        this.accProb = accProb;
    }

    @Override
    public int compareTo(Selectable o) {
        return Double.compare(this.fitness, o.fitness);
    }
}
