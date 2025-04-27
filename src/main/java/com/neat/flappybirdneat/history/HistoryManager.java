package com.neat.flappybirdneat.history;

import com.neat.flappybirdneat.neat.Population;

import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class HistoryManager {
    private List<RunHistory> runHistories;
    private RunHistory currentRun;
    private double bestFitnessEver;
    private GenerationData bestGeneration;

    public HistoryManager() {
        runHistories = new ArrayList<>();
        currentRun = new RunHistory();
        bestFitnessEver = 0.0;
        bestGeneration = null;
    }

    public void startNewRun() {
        if (currentRun.getGenerations() > 0) {
            runHistories.add(currentRun);
        }
        currentRun = new RunHistory();
    }

    public void addGenerationData(double bestFitness, int aliveCount, Population pop) {
        GenerationData data = new GenerationData(bestFitness, aliveCount, pop.deepCopy());
        currentRun.addGenerationData(data);

        // Registrar el mejor de todos los tiempos
        if (bestFitness > bestFitnessEver) {
            bestFitnessEver = bestFitness;
            bestGeneration = data;
        }
    }

    public GenerationData getBestGeneration() {
        return bestGeneration;
    }

    public double getBestFitnessEver() {
        return bestFitnessEver;
    }

    public List<RunHistory> getRunHistories() {
        return runHistories;
    }

    public RunHistory getCurrentRun() {
        return currentRun;
    }

    // Métodos para guardar y cargar historiales
    public void saveToFile(String filename) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
            out.writeObject(runHistories);
            out.writeObject(bestGeneration);
            out.writeDouble(bestFitnessEver);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void loadFromFile(String filename) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            runHistories = (List<RunHistory>) in.readObject();
            bestGeneration = (GenerationData) in.readObject();
            bestFitnessEver = in.readDouble();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}