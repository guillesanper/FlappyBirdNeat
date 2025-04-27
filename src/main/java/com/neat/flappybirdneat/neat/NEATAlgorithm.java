package com.neat.flappybirdneat.neat;

import com.neat.flappybirdneat.game.FlappyBirdGame;
import com.neat.flappybirdneat.game.Pipe;
import com.neat.flappybirdneat.history.HistoryManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Clase que gestiona la lógica del algoritmo genético NEAT,
 * separada de la interfaz gráfica.
 */
public class NEATAlgorithm {
    // Configuración
    private final int populationSize;
    private final int maxGenerations;

    // Componentes principales
    private Population population;
    private FlappyBirdGame game;
    private HistoryManager historyManager;

    // Estado actual
    private int currentGeneration = 1;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private boolean fastMode = false;

    // Almacenamiento de resultados
    private List<Double> bestFitnessHistory = new ArrayList<>();
    private List<Double> avgFitnessHistory = new ArrayList<>();

    /**
     * Constructor
     * @param populationSize Tamaño de la población de agentes
     * @param maxGenerations Número máximo de generaciones
     * @param gameWidth Ancho del juego
     * @param gameHeight Alto del juego
     */
    public NEATAlgorithm(int populationSize, int maxGenerations, int gameWidth, int gameHeight) {
        this.populationSize = populationSize;
        this.maxGenerations = maxGenerations;

        // Inicializar componentes
        this.population = new Population(populationSize);
        this.game = new FlappyBirdGame(gameWidth, gameHeight);
        this.historyManager = new HistoryManager();

        // Inicializar historial
        bestFitnessHistory.add(0.0);
        avgFitnessHistory.add(0.0);
    }

    /**
     * Actualiza un solo frame de la simulación
     * @return true si todos los agentes están muertos
     */
    public boolean updateFrame() {
        game.update(population.getAgents());

        // Comprobar si todos los agentes están muertos
        boolean allDead = true;
        for (FlappyBirdAgent agent : population.getAgents()) {
            if (!agent.isDead()) {
                allDead = false;
                break;
            }
        }

        return allDead;
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

        // Guardar estadísticas
        bestFitnessHistory.add(population.getBestFitness());
        avgFitnessHistory.add(avgFitness);

        // Guardar en historial
        historyManager.addGenerationData(population.getBestFitness(), countAliveAgents(),population);

        // Evolucionar población
        population.naturalSelection();

        // Reiniciar juego y agentes
        game.reset();
        for (FlappyBirdAgent agent : population.getAgents()) {
            agent.reset();
        }

        currentGeneration++;

        System.out.println("Generación " + currentGeneration +
                " - Mejor Fitness: " + population.getBestFitness() +
                " - Fitness Promedio: " + avgFitness);
    }

    /**
     * Ejecuta varias generaciones en modo rápido (sin pausa para visualización)
     * @param generations Número de generaciones a ejecutar
     * @param callback Interfaz funcional para llamar después de cada generación
     */
    public void runGenerations(int generations, GenerationCallback callback) {
        if (isRunning.getAndSet(true)) return;

        fastMode = true;

        Thread simulationThread = new Thread(() -> {
            int startGeneration = currentGeneration;

            for (int i = 0; i < generations && isRunning.get(); i++) {
                // Correr esta generación hasta que todos mueran
                boolean allDead = false;
                int steps = 0;
                int maxSteps = 2000; // Evitar bucles infinitos

                while (!allDead && steps < maxSteps && isRunning.get()) {
                    allDead = updateFrame();
                    steps++;
                }

                if (!isRunning.get()) break;

                // Calcular estadísticas
                double totalFitness = 0;
                for (FlappyBirdAgent agent : population.getAgents()) {
                    totalFitness += agent.getFitness();
                }
                double avgFitness = totalFitness / populationSize;
                double bestFit = population.getBestFitness();

                // Llamar callback con los datos actualizados
                if (callback != null) {
                    callback.onGenerationComplete(
                            currentGeneration,
                            bestFit,
                            avgFitness,
                            countAliveAgents(),
                            game.getScore()
                    );
                }

                // Pasar a la siguiente generación
                nextGeneration();
            }

            isRunning.set(false);
            fastMode = false;
        });

        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    /**
     * Detiene la simulación en curso
     */
    public void stopSimulation() {
        isRunning.set(false);
    }

    /**
     * Reinicia completamente la simulación
     */
    public void reset() {
        stopSimulation();

        population = new Population(populationSize);
        game.reset();

        currentGeneration = 1;

        bestFitnessHistory.clear();
        avgFitnessHistory.clear();
        bestFitnessHistory.add(0.0);
        avgFitnessHistory.add(0.0);

        historyManager.startNewRun();
    }

    /**
     * Cuenta agentes vivos
     */
    private int countAliveAgents() {
        int count = 0;
        for (FlappyBirdAgent agent : population.getAgents()) {
            if (!agent.isDead()) count++;
        }
        return count;
    }

    // Getters
    public Population getPopulation() { return population; }
    public FlappyBirdGame getGame() { return game; }
    public HistoryManager getHistoryManager() { return historyManager; }
    public int getCurrentGeneration() { return currentGeneration; }
    public boolean isRunning() { return isRunning.get(); }
    public boolean isFastMode() { return fastMode; }
    public List<Double> getBestFitnessHistory() { return bestFitnessHistory; }
    public List<Double> getAvgFitnessHistory() { return avgFitnessHistory; }

    /**
     * Interfaz funcional para recibir callbacks después de cada generación
     */
    public interface GenerationCallback {
        void onGenerationComplete(int generation, double bestFitness, double avgFitness,
                                  int aliveCount, int score);
    }
}