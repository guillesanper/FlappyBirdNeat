package com.neat.flappybirdneat.history;

import com.neat.flappybirdneat.game.Pipe;
import com.neat.flappybirdneat.neat.EvolvingPopulation;

import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class HistoryManager {
    // Límite de generaciones a mantener en el historial para evitar fugas de memoria
    private static final int MAX_GENERATIONS_PER_RUN = 500;
    private static final int MAX_RUNS = 10;

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

            // Limitar el número de ejecuciones guardadas
            while (runHistories.size() > MAX_RUNS) {
                runHistories.remove(0); // Eliminar la ejecución más antigua
            }
        }
        currentRun = new RunHistory();
    }

    public void addGenerationData(double bestFitness, double avgFitness, double minFitness, int aliveCount,
                                   int speciesCount, double diversity, EvolvingPopulation pop, List<Pipe> savedPipes) {
        GenerationData data = new GenerationData(bestFitness, avgFitness, minFitness, aliveCount, speciesCount,
                diversity, pop.deepCopy(), new ArrayList<>(savedPipes));
        currentRun.addGenerationData(data);

        // Registrar el mejor de todos los tiempos
        if (bestFitness > bestFitnessEver) {
            bestFitnessEver = bestFitness;
            bestGeneration = data;
        }

        // Limitar el número de generaciones guardadas para evitar fugas de memoria
        // Solo mantenemos las últimas MAX_GENERATIONS_PER_RUN generaciones
        List<GenerationData> generations = currentRun.getGenerationDataList();
        if (generations.size() > MAX_GENERATIONS_PER_RUN) {
            // Eliminar la generación más antigua, pero mantener la mejor siempre
            GenerationData toRemove = generations.get(0);

            // Si la que vamos a eliminar es la mejor de todos los tiempos, no la eliminamos
            if (toRemove != bestGeneration) {
                generations.remove(0);
            } else {
                // Si es la mejor, eliminar la segunda más antigua
                if (generations.size() > 1) {
                    generations.remove(1);
                }
            }
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