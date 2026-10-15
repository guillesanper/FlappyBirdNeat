package com.neat.flappybirdneat.history;

import com.neat.flappybirdneat.game.Pipe;
import com.neat.flappybirdneat.neat.EvolvingPopulation;
import java.io.Serializable;
import java.util.List;

public class GenerationData implements Serializable {
    private double bestFitness;
    private double avgFitness;
    private double minFitness;
    private int aliveCount;
    /** Nº de especies en modo NEAT, o -1 en modo Fixed MLP (no aplica). */
    private int speciesCount;
    /** Diversidad genética de la población en esta generación (ver {@link EvolvingPopulation#diversity()}). */
    private double diversity;

    private EvolvingPopulation savedPopulation;
    private int generationNumber;
    private List<Pipe> savedPipes;
    // Semilla de la secuencia de tubos que jugó esta generación (TrainingEngine.pipeSeed), para
    // reproducirla tal cual; null si no se registró
    private Long pipeSeed;

    public GenerationData(
            double bestFitness,
            double avgFitness,
            double minFitness,
            int aliveCount,
            int speciesCount,
            double diversity,
            EvolvingPopulation population,
            List<Pipe> savedPipes) {
        this.bestFitness = bestFitness;
        this.avgFitness = avgFitness;
        this.minFitness = minFitness;
        this.aliveCount = aliveCount;
        this.speciesCount = speciesCount;
        this.diversity = diversity;
        this.savedPopulation = population;
        this.savedPipes = savedPipes;
    }

    /** @return la semilla de los tubos que jugó esta generación, o null si no se registró */
    public Long getPipeSeed() {
        return pipeSeed;
    }

    public void setPipeSeed(long pipeSeed) {
        this.pipeSeed = pipeSeed;
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

    public double getAvgFitness() {
        return avgFitness;
    }

    public double getMinFitness() {
        return minFitness;
    }

    public int getAliveCount() {
        return aliveCount;
    }

    public int getSpeciesCount() {
        return speciesCount;
    }

    public double getDiversity() {
        return diversity;
    }

    public EvolvingPopulation getSavedPopulation() {
        return savedPopulation;
    }
}
