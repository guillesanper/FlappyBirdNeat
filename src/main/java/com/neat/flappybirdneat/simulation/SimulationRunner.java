package com.neat.flappybirdneat.simulation;

import com.neat.flappybirdneat.game.FlappyBirdGame;
import com.neat.flappybirdneat.history.GenerationData;
import com.neat.flappybirdneat.history.HistoryManager;
import com.neat.flappybirdneat.history.RunHistory;
import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.neat.Population;

import java.util.ArrayList;
import java.util.List;

public class SimulationRunner {
    private Population population;
    private FlappyBirdGame game;
    private HistoryManager historyManager;
    private int currentGeneration;
    private boolean isRunning;
    private int targetGenerations;
    private List<GenerationData> generationHistory;

    public SimulationRunner(int populationSize, int canvasWidth, int canvasHeight) {
        population = new Population(populationSize);
        game = new FlappyBirdGame(canvasWidth, canvasHeight);
        historyManager = new HistoryManager();
        currentGeneration = 1;
        generationHistory = new ArrayList<>();
    }

    public void runSimulation(int generations) {
        isRunning = true;
        targetGenerations = generations;
        historyManager.startNewRun();

        // Guardar estado inicial
        saveGenerationState();

        while (currentGeneration <= targetGenerations && isRunning) {
            // Ejecutar la generación actual
            runGeneration();

            // Guardar estado
            saveGenerationState();

            // Evolucionar a la siguiente generación
            population.naturalSelection();
            currentGeneration++;

            // Reiniciar el juego y los agentes
            game.reset();
            for (FlappyBirdAgent agent : population.getAgents()) {
                agent.reset();
            }
        }

        isRunning = false;
    }

    private void runGeneration() {
        boolean allDead = false;
        int maxSteps = 2000; // Límite para evitar simulaciones infinitas
        int steps = 0;

        while (!allDead && steps < maxSteps) {
            game.update(population.getAgents());

            // Comprobar si todos los agentes están muertos
            allDead = true;
            for (FlappyBirdAgent agent : population.getAgents()) {
                if (!agent.isDead()) {
                    allDead = false;
                    break;
                }
            }
            steps++;
        }

        // Actualizar historial
        int aliveCount = countAliveAgents();
        historyManager.addGenerationData(population.getBestFitness(), aliveCount,population);
    }

    private int countAliveAgents() {
        int count = 0;
        for (FlappyBirdAgent agent : population.getAgents()) {
            if (!agent.isDead()) count++;
        }
        return count;
    }

    private void saveGenerationState() {
        // Guardar estado de la generación para visualización posterior
        GenerationData data = new GenerationData(
                population.getBestFitness(),
                countAliveAgents(),
                population.deepCopy()
        );
        generationHistory.add(data);
    }

    public void stopSimulation() {
        isRunning = false;
    }

    public List<GenerationData> getGenerationHistory() {
        return generationHistory;
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public Population getPopulation() {
        return population;
    }

    public int getCurrentGeneration() {
        return currentGeneration;
    }

}