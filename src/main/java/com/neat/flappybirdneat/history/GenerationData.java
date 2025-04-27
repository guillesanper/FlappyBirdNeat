package com.neat.flappybirdneat.history;

import com.neat.flappybirdneat.neat.Population;

import java.io.Serializable;

public class GenerationData implements Serializable {
    private double bestFitness;
    private int aliveCount;
    private Population savedPopulation;
    private int generationNumber;


    public GenerationData(double bestFitness, int aliveCount, Population population) {
        this.bestFitness = bestFitness;
        this.aliveCount = aliveCount;
        this.savedPopulation = population;
    }

    public int getGenerationNumber() {
        return generationNumber;
    }

    public void setGenerationNumber(int generationNumber) {
        this.generationNumber = generationNumber;
    }

    public double getBestFitness() {
        return bestFitness;
    }

    public int getAliveCount() {
        return aliveCount;
    }

    public Population getSavedPopulation() {
        return savedPopulation;
    }
}