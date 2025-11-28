package com.neat.flappybirdneat.simulation;

import com.neat.flappybirdneat.config.GeneticOperatorsConfig;
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
    // Fitness considerado óptimo - si se alcanza, se detiene el entrenamiento automáticamente
    private static final double OPTIMAL_FITNESS_THRESHOLD = 80000.0;

    // Propiedades observables para actualizar la UI
    private final IntegerProperty currentGeneration = new SimpleIntegerProperty(1);
    private final DoubleProperty bestFitness = new SimpleDoubleProperty(0);
    private final DoubleProperty averageFitness = new SimpleDoubleProperty(0);
    private final IntegerProperty aliveCount = new SimpleIntegerProperty(0);
    private final BooleanProperty running = new SimpleBooleanProperty(false);

    // Datos para gráficos
    private final List<Double> bestFitnessHistory = new ArrayList<>();
    private final List<Double> avgFitnessHistory = new ArrayList<>();
    private final List<Double> bestAbsoluteFitnessHistory = new ArrayList<>();

    // Referencias al juego y población
    private Population population;
    private FlappyBirdGame game;
    private int populationSize;
    private int canvasWidth;
    private int canvasHeight;
    private HistoryManager historyManager;
    private GeneticOperatorsConfig operatorsConfig;

    // Parámetros de simulación
    private boolean fastMode = false;
    private int targetGenerations = 0;
    private boolean replayMode = false; // Indica si estamos reproduciendo el mejor agente


    /**
     * Constructor
     */
    public SimulationController(int populationSize, int canvasWidth, int canvasHeight) {
        this.populationSize = populationSize;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.historyManager = new HistoryManager();
        this.operatorsConfig = new GeneticOperatorsConfig();

        resetSimulation();
    }

    /**
     * Reinicia completamente la simulación
     */
    public void resetSimulation() {
        population = new Population(populationSize);
        operatorsConfig.applyTo(population); // Aplicar operadores configurados
        game = new FlappyBirdGame(canvasWidth, canvasHeight);

        currentGeneration.set(1);
        bestFitness.set(0);
        averageFitness.set(0);
        aliveCount.set(populationSize);

        bestFitnessHistory.clear();
        avgFitnessHistory.clear();
        bestAbsoluteFitnessHistory.clear();

        // Añadir valores iniciales al historial
        bestFitnessHistory.add(0.0);
        avgFitnessHistory.add(0.0);
        bestAbsoluteFitnessHistory.add(0.0);

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
        // Calcular fitness promedio y mejor de esta generación antes de evolucionar
        double totalFitness = 0;
        double bestFitnessThisGen = Double.NEGATIVE_INFINITY;
        for (FlappyBirdAgent agent : population.getAgents()) {
            double fitness = agent.getFitness();
            totalFitness += fitness;
            if (fitness > bestFitnessThisGen) {
                bestFitnessThisGen = fitness;
            }
        }
        double avgFitness = totalFitness / populationSize;

        // Guardar esta generación en el historial
        historyManager.addGenerationData(bestFitnessThisGen, aliveCount.get(), population, game.getPipes());

        // Guardar historial para gráficos
        bestFitnessHistory.add(bestFitnessThisGen);
        avgFitnessHistory.add(avgFitness);

        // Actualizar mejor fitness absoluto
        double previousAbsolute = bestAbsoluteFitnessHistory.isEmpty() ? 0.0 :
                                  bestAbsoluteFitnessHistory.get(bestAbsoluteFitnessHistory.size() - 1);
        bestAbsoluteFitnessHistory.add(Math.max(bestFitnessThisGen, previousAbsolute));

        // Evolucionar población
        population.naturalSelection();

        // Reiniciar juego y agentes
        game.reset();
        for (FlappyBirdAgent agent : population.getAgents()) {
            agent.reset();
        }

        // Actualizar propiedades
        currentGeneration.set(currentGeneration.get() + 1);
        bestFitness.set(bestFitnessThisGen); // Mejor de esta generación, no el histórico
        aliveCount.set(populationSize);

        System.out.println("Generación " + currentGeneration.get() +
                " - Mejor Fitness: " + bestFitness.get() +
                " - Fitness Promedio: " + avgFitness);
    }

    /**
     * Ejecuta rápidamente un número específico de generaciones
     * Optimizado para máximo rendimiento - actualiza UI solo cada 10 generaciones
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

                // Variables para acumular datos antes de actualizar UI
                final int UI_UPDATE_INTERVAL = 10; // Actualizar UI cada 10 generaciones

                for (int i = 0; i < generations && !isCancelled(); i++) {
                    // Ejecutar generación actual hasta que todos mueran
                    boolean allDead = false;
                    int alive = populationSize;

                    while (!allDead && !isCancelled()) {
                        // Actualizar juego sin renderizar (modo headless)
                        game.update(population.getAgents());

                        // Solo contar vivos, no calcular todo en cada frame
                        alive = 0;
                        for (FlappyBirdAgent agent : population.getAgents()) {
                            if (!agent.isDead()) alive++;
                        }

                        // Verificar si todos están muertos
                        allDead = alive == 0;
                    }

                    if (isCancelled()) break;

                    // Calcular estadísticas solo al final de la generación
                    double totalFitness = 0;
                    double bestFitnessThisGen = Double.NEGATIVE_INFINITY;
                    for (FlappyBirdAgent agent : population.getAgents()) {
                        double fitness = agent.getFitness();
                        totalFitness += fitness;
                        if (fitness > bestFitnessThisGen) {
                            bestFitnessThisGen = fitness;
                        }
                    }
                    final double avgFitness = totalFitness / populationSize;
                    final double currentBestFitness = bestFitnessThisGen; // Mejor de esta generación

                    // Guardar esta generación en el historial
                    historyManager.addGenerationData(currentBestFitness, alive, population, game.getPipes());

                    if (currentBestFitness > globalBestFitness) {
                        globalBestFitness = currentBestFitness;
                        bestGeneration = initialGeneration + i;
                    }

                    // DETECCIÓN DE FITNESS ÓPTIMO: Si alcanzamos un fitness muy alto, detener entrenamiento
                    if (currentBestFitness >= OPTIMAL_FITNESS_THRESHOLD) {
                        final int currentGen = initialGeneration + i + 1;
                        final double bestFit = currentBestFitness;
                        final double avgFit = avgFitness;

                        // Guardar datos para gráficos
                        bestFitnessHistory.add(currentBestFitness);
                        avgFitnessHistory.add(avgFitness);

                        // Mantener el mejor absoluto
                        double previousAbsolute = bestAbsoluteFitnessHistory.isEmpty() ? 0.0 :
                                                  bestAbsoluteFitnessHistory.get(bestAbsoluteFitnessHistory.size() - 1);
                        bestAbsoluteFitnessHistory.add(Math.max(currentBestFitness, previousAbsolute));

                        Platform.runLater(() -> {
                            bestFitness.set(bestFit);
                            averageFitness.set(avgFit);
                            currentGeneration.set(currentGen);
                            aliveCount.set(0);
                            updateProgress(1, 1); // Completar barra de progreso
                        });

                        System.out.println("\n╔════════════════════════════════════════════╗");
                        System.out.println("║  🎯 ¡FITNESS ÓPTIMO ALCANZADO! 🎯        ║");
                        System.out.println("╠════════════════════════════════════════════╣");
                        System.out.println("║  Generación: " + currentGen);
                        System.out.println("║  Fitness: " + String.format("%.2f", bestFit));
                        System.out.println("║  Detención automática activada            ║");
                        System.out.println("╚════════════════════════════════════════════╝\n");

                        // Salir del bucle - hemos encontrado el óptimo
                        break;
                    }

                    // Guardar datos para gráficos (siempre)
                    bestFitnessHistory.add(currentBestFitness);
                    avgFitnessHistory.add(avgFitness);

                    // Mantener el mejor absoluto
                    double previousAbsolute = bestAbsoluteFitnessHistory.isEmpty() ? 0.0 :
                                              bestAbsoluteFitnessHistory.get(bestAbsoluteFitnessHistory.size() - 1);
                    bestAbsoluteFitnessHistory.add(Math.max(currentBestFitness, previousAbsolute));

                    // Actualizar UI solo cada N generaciones o en la última
                    if (i % UI_UPDATE_INTERVAL == 0 || i == generations - 1) {
                        final int currentGen = initialGeneration + i + 1;
                        final double bestFit = currentBestFitness;
                        final double avgFit = avgFitness;
                        final int finalI = i;
                        final int finalAlive = alive;

                        Platform.runLater(() -> {
                            bestFitness.set(bestFit);
                            averageFitness.set(avgFit);
                            currentGeneration.set(currentGen);
                            aliveCount.set(finalAlive);
                            updateProgress(finalI + 1, generations);
                        });

                        System.out.println("Generación " + currentGen +
                                " - Mejor Fitness: " + String.format("%.2f", bestFit) +
                                " - Fitness Promedio: " + String.format("%.2f", avgFit));
                    }

                    // Evolucionar población
                    population.naturalSelection();

                    // Reiniciar juego y agentes
                    game.reset();
                    for (FlappyBirdAgent agent : population.getAgents()) {
                        agent.reset();
                    }
                }

                // Al final de la simulación
                final int finalBestGeneration = bestGeneration;
                final double finalGlobalBestFitness = globalBestFitness;
                final boolean reachedOptimal = finalGlobalBestFitness >= OPTIMAL_FITNESS_THRESHOLD;

                Platform.runLater(() -> {
                    running.set(false);
                    fastMode = false;
                    updateProgress(1, 1); // Completar la barra de progreso

                    if (reachedOptimal) {
                        // Ya se mostró el mensaje de fitness óptimo arriba
                        System.out.println("Usa el botón '▶ Ver Mejor Individuo' para reproducir el agente óptimo.\n");
                    } else {
                        System.out.println("\n=== SIMULACIÓN COMPLETADA ===");
                        System.out.println("Mejor generación: " + finalBestGeneration +
                                " con fitness: " + String.format("%.2f", finalGlobalBestFitness));
                        System.out.println("==============================\n");
                    }
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
     * Crea una población especial con solo el mejor agente para visualización
     * @return Population con solo el mejor agente clonado
     */
    public Population createBestAgentOnlyPopulation() {
        GenerationData bestGenData = historyManager.getBestGeneration();
        if (bestGenData == null) {
            return null;
        }

        Population bestPopulation = bestGenData.getSavedPopulation();
        FlappyBirdAgent bestAgent = bestPopulation.getBestAgent();

        // Crear una nueva población con solo el mejor agente
        Population singleAgentPop = new Population(1);

        // Copiar el mejor agente y también establecerlo como el mejor de la población
        FlappyBirdAgent clonedBestAgent = new FlappyBirdAgent(bestAgent);
        singleAgentPop.getAgents()[0] = clonedBestAgent;

        // IMPORTANTE: También copiar el bestAgent al campo bestAgent de la población
        // para que drawGame lo reconozca como el mejor
        FlappyBirdAgent bestAgentForPop = new FlappyBirdAgent(bestAgent);
        // Acceder directamente al bestAgent a través de un método que lo establezca
        // Como no tenemos un setter directo, lo forzaremos reiniciando el agente
        singleAgentPop.getAgents()[0].setFitness(bestAgent.getFitness());

        return singleAgentPop;
    }

    /**
     * Inicia la reproducción del mejor individuo encontrado
     */
    public void playBestAgentOnly() {
        GenerationData bestGenData = historyManager.getBestGeneration();
        if (bestGenData == null) {
            System.out.println("No hay mejor generación guardada aún");
            return;
        }

        // Resetear el juego
        game.reset();

        // Crear población con solo el mejor agente
        Population bestAgentPop = createBestAgentOnlyPopulation();
        if (bestAgentPop != null) {
            this.population = bestAgentPop;

            // Reiniciar el agente pero preservar su cerebro
            for (FlappyBirdAgent agent : this.population.getAgents()) {
                agent.reset();
            }

            fastMode = false;
            replayMode = true; // IMPORTANTE: Activar modo replay para que no evolucione
            running.set(true);

            System.out.println("\n=== REPRODUCIENDO MEJOR AGENTE ===");
            System.out.println("Fitness alcanzado: " + String.format("%.2f", historyManager.getBestFitnessEver()));
            System.out.println("===================================\n");
        }
    }

    /**
     * @return true si estamos en modo replay (reproduciendo el mejor agente)
     */
    public boolean isReplayMode() {
        return replayMode;
    }

    /**
     * Desactiva el modo replay
     */
    public void exitReplayMode() {
        replayMode = false;
    }

    /**
     * Detiene la simulación rápida
     */
    public void stopSimulation() {
        running.set(false);
    }

    /**
     * @return El umbral de fitness considerado óptimo
     */
    public static double getOptimalFitnessThreshold() {
        return OPTIMAL_FITNESS_THRESHOLD;
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public void setCurrentPopulation(Population population) {
        this.population = population;
    }

    /**
     * Actualiza la configuración guardada con los operadores actuales
     */
    public void updateOperatorsConfig() {
        operatorsConfig.updateFrom(population);
    }

    /**
     * Obtiene la configuración de operadores genéticos
     */
    public GeneticOperatorsConfig getOperatorsConfig() {
        return operatorsConfig;
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
    public List<Double> getBestAbsoluteFitnessHistory() { return bestAbsoluteFitnessHistory; }
    public Population getPopulation() { return population; }
    public FlappyBirdGame getGame() { return game; }
    public boolean isFastMode() { return fastMode; }
}