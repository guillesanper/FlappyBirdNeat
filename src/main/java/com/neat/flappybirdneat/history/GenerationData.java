package com.neat.flappybirdneat.history;

import com.neat.flappybirdneat.game.Pipe;
import com.neat.flappybirdneat.neat.Population;

import java.io.Serializable;
import java.util.List;

public class GenerationData implements Serializable {
    private double bestFitness;
    private int aliveCount;
    private Population savedPopulation;
    private int generationNumber;
    private List<Pipe> savedPipes;


    public GenerationData(double bestFitness, int aliveCount, Population population, List<Pipe> savedPipes) {
        this.bestFitness = bestFitness;
        this.aliveCount = aliveCount;
        this.savedPopulation = population;
        this.savedPipes = savedPipes;
    }

    public List<Pipe> getSavedPipes() {
        return savedPipes;
    }

    public void setSavedPipes(List<Pipe> savedPipes) {
        this.savedPipes = savedPipes;
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