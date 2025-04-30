package com.neat.flappybirdneat.simulation;

import com.neat.flappybirdneat.game.FlappyBirdGame;
import com.neat.flappybirdneat.history.GenerationData;
import com.neat.flappybirdneat.history.HistoryManager;
import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.neat.Population;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador que gestiona la ejecución de simulaciones de FlappyBird NEAT,
 * permitiendo ejecutar generaciones rápidamente en modo headless.
 */
public class SimulationController {
    // Propiedades observables para actualizar la UI
    private final IntegerProperty currentGeneration = new SimpleIntegerProperty(1);
    private final DoubleProperty bestFitness = new SimpleDoubleProperty(0);
    private final DoubleProperty averageFitness = new SimpleDoubleProperty(0);
    private final IntegerProperty aliveCount = new SimpleIntegerProperty(0);
    private final BooleanProperty running = new SimpleBooleanProperty(false);

    // Datos para gráficos
    private final List<Double> bestFitnessHistory = new ArrayList<>();


    private final List<Double> avgFitnessHistory = new ArrayList<>();

    // Referencias al juego y población
    private Population population;
    private FlappyBirdGame game;
    private int populationSize;
    private int canvasWidth;
    private int canvasHeight;
    private HistoryManager historyManager;

    // Parámetros de simulación
    private boolean fastMode = false;
    private int targetGenerations = 0;


    /**
     * Constructor
     */
    public SimulationController(int populationSize, int canvasWidth, int canvasHeight) {
        this.populationSize = populationSize;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.historyManager = new HistoryManager();

        resetSimulation();
    }

    /**
     * Reinicia completamente la simulación
     */
    public void resetSimulation() {
        population = new Population(populationSize);
        game = new FlappyBirdGame(canvasWidth, canvasHeight);

        currentGeneration.set(1);
        bestFitness.set(0);
        averageFitness.set(0);
        aliveCount.set(populationSize);

        bestFitnessHistory.clear();
        avgFitnessHistory.clear();

        // Añadir valores iniciales al historial
        bestFitnessHistory.add(0.0);
        avgFitnessHistory.add(0.0);

        // Iniciar un nuevo historial de ejecución
        historyManager.startNewRun();
    }

    /**
     * Actualiza un solo frame de la simulación
     * @return true si todos los agentes están muertos
     */
    public boolean updateFrame() {
        if (running.get()) {
            game.update(population.getAgents());

            // Actualizar contador de agentes vivos
            int alive = 0;
            for (FlappyBirdAgent agent : population.getAgents()) {
                if (!agent.isDead()) alive++;
            }
            aliveCount.set(alive);

            // Calcular fitness promedio
            double totalFitness = 0;
            for (FlappyBirdAgent agent : population.getAgents()) {
                totalFitness += agent.getFitness();
            }
            averageFitness.set(totalFitness / populationSize);

            // Comprobar si todos los agentes están muertos
            return alive == 0;
        }
        return false;
    }

    /**
     * Evoluciona a la siguiente generación
     */
    public void nextGeneration() {
        // Calcular fitness promedio antes de evolucionar
        double totalFitness = 0;
        for (FlappyBirdAgent agent : population.getAgents()) {
            totalFitness += agent.getFitness();
        }
        double avgFitness = totalFitness / populationSize;

        // Guardar esta generación en el historial
        historyManager.addGenerationData(population.getBestFitness(), aliveCount.get(), population,game.getPipes());

        // Guardar historial para gráficos
        bestFitnessHistory.add(population.getBestFitness());
        avgFitnessHistory.add(avgFitness);

        // Evolucionar población
        population.naturalSelection();

        // Reiniciar juego y agentes
        game.reset();
        for (FlappyBirdAgent agent : population.getAgents()) {
            agent.reset();
        }

        // Actualizar propiedades
        currentGeneration.set(currentGeneration.get() + 1);
        bestFitness.set(population.getBestFitness());
        aliveCount.set(populationSize);

        System.out.println("Generación " + currentGeneration.get() +
                " - Mejor Fitness: " + bestFitness.get() +
                " - Fitness Promedio: " + avgFitness);
    }

    /**
     * Ejecuta rápidamente un número específico de generaciones
     * @param generations Número de generaciones a ejecutar
     */
    public void runFastSimulation(int generations) {
        if (running.get()) return;

        running.set(true);
        fastMode = true;
        targetGenerations = generations;

        // Iniciar un nuevo historial de ejecución
        historyManager.startNewRun();

        Task<Void> simulationTask = new Task<>() {
            @Override
            protected Void call() {
                double globalBestFitness = bestFitness.get();
                int bestGeneration = 0;

                int initialGeneration = currentGeneration.get();
                for (int i = 0; i < generations && !isCancelled(); i++) {
                    // Ejecutar generación actual hasta que todos mueran
                    boolean allDead = false;
                    while (!allDead && !isCancelled()) {
                        // Actualizar juego sin renderizar
                        game.update(population.getAgents());

                        // Calcular promedio de fitness
                        double totalFitness = 0;
                        int alive = 0;
                        for (FlappyBirdAgent agent : population.getAgents()) {
                            totalFitness += agent.getFitness();
                            if (!agent.isDead()) alive++;
                        }
                        final double avgFitness = totalFitness / populationSize;

                        // Actualizar propiedades en el hilo de la UI
                        int finalAlive = alive;
                        Platform.runLater(() -> {
                            averageFitness.set(avgFitness);
                            aliveCount.set(finalAlive);
                        });

                        // Verificar si todos están muertos
                        allDead = alive == 0;
                    }

                    if (isCancelled()) break;

                    // Verificar si esta generación tiene el mejor fitness hasta ahora
                    final double currentBestFitness = population.getBestFitness();

                    // Guardar esta generación en el historial siempre
                    historyManager.addGenerationData(currentBestFitness, aliveCount.get(), population,game.getPipes());

                    if (currentBestFitness > globalBestFitness) {
                        globalBestFitness = currentBestFitness;
                        bestGeneration = initialGeneration + i;
                    }

                    // Pasar a la siguiente generación
                    final int currentGen = initialGeneration + i + 1;

                    // Calcular fitness para estadísticas
                    double totalFitness = 0;
                    for (FlappyBirdAgent agent : population.getAgents()) {
                        totalFitness += agent.getFitness();
                    }
                    final double avgFitness = totalFitness / populationSize;
                    final double bestFit = population.getBestFitness();

                    int finalI = i;
                    Platform.runLater(() -> {
                        // Guardar datos para gráficos
                        bestFitnessHistory.add(bestFit);
                        avgFitnessHistory.add(avgFitness);

                        // Actualizar UI
                        bestFitness.set(bestFit);
                        averageFitness.set(avgFitness);
                        currentGeneration.set(currentGen);

                        // Notificar progreso
                        updateProgress(finalI + 1, generations);
                        game.update(population.getAgents());
                    });

                    // Evolucionar población
                    population.naturalSelection();

                    // Reiniciar juego y agentes
                    game.reset();
                    for (FlappyBirdAgent agent : population.getAgents()) {
                        agent.reset();
                    }

                    System.out.println("Generación " + currentGen +
                            " - Mejor Fitness: " + bestFit +
                            " - Fitness Promedio: " + avgFitness);
                }

                // Al final de la simulación, guarde una nota sobre la mejor generación
                final int finalBestGeneration = bestGeneration;
                double finalGlobalBestFitness = globalBestFitness;
                Platform.runLater(() -> {
                    running.set(false);
                    fastMode = false;
                    System.out.println("Mejor generación: " + finalBestGeneration +
                            " con fitness: " + finalGlobalBestFitness);
                });

                return null;
            }
        };

        // Iniciar la tarea en un hilo separado
        Thread simulationThread = new Thread(simulationTask);
        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    // Método para reproducir la generación con el mejor individuo
    public void playBestHistoricalGeneration() {
        GenerationData bestGenData = historyManager.getBestGeneration();
        game.setPipes(bestGenData.getSavedPipes());
        if (bestGenData != null) {
            playHistoricalGeneration(bestGenData.getSavedPopulation());
        }
    }

    // Método para reproducir una generación histórica
    public void playHistoricalGeneration(Population savedPopulation) {
        // Resetear el juego pero usar la población guardada
        game.reset();
        // Clonar la población para no modificar el original histórico
        this.population = savedPopulation.deepCopy();

        // Reiniciar los agentes
        for (FlappyBirdAgent agent : this.population.getAgents()) {
            agent.reset();
        }

        fastMode = false;
        running.set(true);
        // La visualización se hará a través del gameLoop en FlappyBirdNEAT
    }

    /**
     * Detiene la simulación rápida
     */
    public void stopSimulation() {
        running.set(false);
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public void setCurrentPopulation(Population population) {
        this.population = population;
    }

    // Getters para propiedades observables
    public IntegerProperty currentGenerationProperty() { return currentGeneration; }
    public DoubleProperty bestFitnessProperty() { return bestFitness; }
    public DoubleProperty averageFitnessProperty() { return averageFitness; }
    public IntegerProperty aliveCountProperty() { return aliveCount; }
    public BooleanProperty runningProperty() { return running; }

    // Getters para datos y objetos
    public List<Double> getBestFitnessHistory() { return bestFitnessHistory; }
    public List<Double> getAvgFitnessHistory() { return avgFitnessHistory; }
    public Population getPopulation() { return population; }
    public FlappyBirdGame getGame() { return game; }
    public boolean isFastMode() { return fastMode; }
}