package com.neat.flappybirdneat.simulation;

import static com.neat.flappybirdneat.simulation.TrainingEngine.AGENT_INPUTS;
import static com.neat.flappybirdneat.simulation.TrainingEngine.AGENT_OUTPUTS;

import com.neat.flappybirdneat.BuildInfo;
import com.neat.flappybirdneat.champion.Champion;
import com.neat.flappybirdneat.champion.ChampionMetadata;
import com.neat.flappybirdneat.champion.TrainingSettings;
import com.neat.flappybirdneat.config.GeneticOperatorsConfig;
import com.neat.flappybirdneat.game.FlappyBirdGame;
import com.neat.flappybirdneat.history.GenerationData;
import com.neat.flappybirdneat.history.HistoryManager;
import com.neat.flappybirdneat.history.RunHistory;
import com.neat.flappybirdneat.neat.EvolvingPopulation;
import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.neat.Population;
import com.neat.flappybirdneat.neat.genome.Genome;
import com.neat.flappybirdneat.neat.genome.NeatPopulation;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UI adapter over {@link TrainingEngine}: exposes the training state as JavaFX observable
 * properties, keeps the per-generation chart series and history snapshots, runs fast training on
 * a background task and handles the replays (best agent, champion or a recorded generation). The training itself (seeded generator,
 * population, game) lives in the engine, which has no JavaFX dependencies.
 */
public class SimulationController {
    private static final Logger LOG = LoggerFactory.getLogger(SimulationController.class);

    // Fitness considerado óptimo - si se alcanza, se detiene el entrenamiento automáticamente
    private static final double OPTIMAL_FITNESS_THRESHOLD = 80000.0;

    // Sal para el generador derivado de las réplicas del mejor agente (ver derivedRandom)
    private static final long REPLAY_SALT = -1;
    // Sal para el generador derivado de la población de un campeón cargado
    private static final long CHAMPION_SALT = -2;

    private static final String BEST_AGENT_BANNER = "★ REPRODUCIENDO MEJOR AGENTE ★";
    private static final String GENERATION_BANNER = "▶ REPLAY · Generación %d";

    /** Modo de evolución: MLP de topología fija (operadores configurables) o NEAT real. */
    public enum Mode {
        FIXED_MLP,
        NEAT
    }

    // Propiedades observables para actualizar la UI
    private final IntegerProperty currentGeneration = new SimpleIntegerProperty(1);
    private final DoubleProperty bestFitness = new SimpleDoubleProperty(0);
    private final DoubleProperty averageFitness = new SimpleDoubleProperty(0);
    private final IntegerProperty aliveCount = new SimpleIntegerProperty(0);
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    // Campeón que se está viendo (--demo, --watch, "Ver campeón"); null durante el entrenamiento
    private final ObjectProperty<Champion> watchedChampion = new SimpleObjectProperty<>();
    // Replay en curso (mejor agente, campeón o generación): la población de entrenamiento está apartada
    private final BooleanProperty replayMode = new SimpleBooleanProperty(false);
    // Generación grabada que se está reproduciendo; null en los demás replays y en el entrenamiento
    private final ObjectProperty<GenerationData> replayedGeneration = new SimpleObjectProperty<>();

    // Datos para gráficos
    private final List<Double> bestFitnessHistory = new ArrayList<>();
    private final List<Double> avgFitnessHistory = new ArrayList<>();
    private final List<Double> bestAbsoluteFitnessHistory = new ArrayList<>();
    private final List<Double> minFitnessHistory = new ArrayList<>();
    private final List<Integer> speciesCountHistory = new ArrayList<>();
    private final List<Double> diversityHistory = new ArrayList<>();

    // Núcleo de entrenamiento (semilla, población y juego)
    private final TrainingEngine engine;
    private final int populationSize;
    private HistoryManager historyManager;
    private GeneticOperatorsConfig operatorsConfig;

    // Parámetros de simulación
    // Entrenamiento rápido en curso: baja cuando su hilo ha terminado de verdad, no al pedir detenerlo
    private final BooleanProperty fastMode = new SimpleBooleanProperty(false);
    // Escrito desde el hilo de la UI y leído desde el del entrenamiento rápido
    private volatile boolean stopRequested = false;
    private int targetGenerations = 0;
    private String replayBanner = BEST_AGENT_BANNER;
    // Población y juego de entrenamiento apartados durante un replay (null fuera de un replay)
    private TrainingEngine.Snapshot trainingSnapshot;
    private boolean trainingWasRunning;
    // Tubos de cada vuelta del replay de una generación (null en los demás replays)
    private Supplier<Random> replayPipes;
    private boolean replayPipesOriginal = true;
    // Agente dibujado en rojo en el replay de una generación: el que marcó su mejor fitness
    private FlappyBirdAgent replayedFittest;
    private Mode mode = Mode.FIXED_MLP;
    private final long seed;

    /**
     * Constructor con semilla global. Toda la aleatoriedad de la simulación sale de esta semilla:
     * inicialización y evolución de la población (Fixed MLP o NEAT), estrategias de operadores
     * genéticos y generación de tubos. Dos controladores con la misma semilla, la misma
     * configuración y la misma secuencia de acciones producen exactamente las mismas curvas de fitness.
     */
    public SimulationController(int populationSize, int canvasWidth, int canvasHeight, long seed) {
        this.populationSize = populationSize;
        this.seed = seed;
        this.engine = new TrainingEngine(populationSize, canvasWidth, canvasHeight, seed);
        this.historyManager = new HistoryManager(seed);
        this.operatorsConfig = new GeneticOperatorsConfig();

        resetSimulation();
    }

    /**
     * Cambia el modo de evolución (Fixed MLP vs NEAT). Solo tiene efecto en el próximo
     * {@link #resetSimulation()}, ya que cada modo usa un tipo de población distinto.
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
    }

    /**
     * Reinicia completamente la simulación
     */
    public void resetSimulation() {
        engine.reset(mode == Mode.NEAT ? EngineType.NEAT : EngineType.GA, operatorsConfig);
        // La población apartada por un replay ya no sirve: se empieza de cero
        trainingSnapshot = null;
        clearReplayState();

        currentGeneration.set(1);
        bestFitness.set(0);
        averageFitness.set(0);
        aliveCount.set(populationSize);

        bestFitnessHistory.clear();
        avgFitnessHistory.clear();
        bestAbsoluteFitnessHistory.clear();
        minFitnessHistory.clear();
        speciesCountHistory.clear();
        diversityHistory.clear();

        // Añadir valores iniciales al historial
        bestFitnessHistory.add(0.0);
        avgFitnessHistory.add(0.0);
        bestAbsoluteFitnessHistory.add(0.0);
        minFitnessHistory.add(0.0);
        speciesCountHistory.add(getSpeciesCount());
        diversityHistory.add(engine.getPopulation().diversity());

        // Iniciar un nuevo historial de ejecución
        historyManager.startNewRun();
    }

    /**
     * Actualiza un solo frame de la simulación
     * @return true si todos los agentes están muertos
     */
    public boolean updateFrame() {
        if (running.get()) {
            boolean allDead = engine.step();
            aliveCount.set(engine.aliveCount());
            averageFitness.set(engine.meanFitness());
            return allDead;
        }
        return false;
    }

    /**
     * Evoluciona a la siguiente generación
     */
    public void nextGeneration() {
        // Estadísticas de esta generación antes de evolucionar
        GenerationStats stats = engine.statistics();
        double bestFitnessThisGen = stats.best();
        double avgFitness = stats.mean();
        double minFitnessThisGen = stats.min();
        int speciesCountThisGen = stats.species();
        double diversityThisGen = stats.diversity();

        // Guardar esta generación en el historial
        historyManager.addGenerationData(
                engine.getGeneration(),
                bestFitnessThisGen,
                avgFitness,
                minFitnessThisGen,
                aliveCount.get(),
                speciesCountThisGen,
                diversityThisGen,
                engine.getPopulation(),
                engine.getGame().getPipes());

        // Guardar historial para gráficos
        bestFitnessHistory.add(bestFitnessThisGen);
        avgFitnessHistory.add(avgFitness);
        minFitnessHistory.add(minFitnessThisGen);
        speciesCountHistory.add(speciesCountThisGen);
        diversityHistory.add(diversityThisGen);

        // Actualizar mejor fitness absoluto
        double previousAbsolute = bestAbsoluteFitnessHistory.isEmpty()
                ? 0.0
                : bestAbsoluteFitnessHistory.get(bestAbsoluteFitnessHistory.size() - 1);
        bestAbsoluteFitnessHistory.add(Math.max(bestFitnessThisGen, previousAbsolute));

        // Evolucionar población y reiniciar juego y agentes
        engine.evolve();

        // Actualizar propiedades
        currentGeneration.set(currentGeneration.get() + 1);
        bestFitness.set(bestFitnessThisGen); // Mejor de esta generación, no el histórico
        aliveCount.set(populationSize);

        LOG.info(
                "Generation {} - best fitness {} - average fitness {}",
                currentGeneration.get(),
                String.format("%.2f", bestFitness.get()),
                String.format("%.2f", avgFitness));
    }

    /**
     * Ejecuta rápidamente un número específico de generaciones
     * Optimizado para máximo rendimiento - actualiza UI solo cada 10 generaciones
     * @param generations Número de generaciones a ejecutar
     */
    public void runFastSimulation(int generations) {
        // fastMode sigue activo mientras una ejecución detenida termina su generación en curso
        if (running.get() || fastMode.get()) return;

        beginFastSimulation(generations);

        Task<Void> simulationTask = new Task<>() {
            @Override
            protected Void call() {
                trainFast(generations, this::updateProgress, Platform::runLater);
                return null;
            }
        };

        // Iniciar la tarea en un hilo separado
        Thread simulationThread = new Thread(simulationTask);
        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    /** Barra de progreso del entrenamiento rápido (la de la {@link Task}, en la app). */
    @FunctionalInterface
    interface ProgressSink {
        void update(long workDone, long max);
    }

    /** Prepara el estado de un entrenamiento rápido antes de lanzar {@link #trainFast}. */
    void beginFastSimulation(int generations) {
        // Se entrena la población de entrenamiento, no la de un replay
        exitReplayMode();
        running.set(true);
        fastMode.set(true);
        stopRequested = false;
        targetGenerations = generations;

        // Iniciar un nuevo historial de ejecución
        historyManager.startNewRun();
    }

    /**
     * Cuerpo del entrenamiento rápido, en el hilo de la tarea. Termina tras {@code generations}
     * generaciones, al alcanzar el fitness óptimo o, entre generaciones, cuando se llama a
     * {@link #stopSimulation()}.
     * @param ui ejecuta las actualizaciones de propiedades y progreso (Platform::runLater en la app)
     */
    void trainFast(int generations, ProgressSink progress, Executor ui) {
        double globalBestFitness = bestFitness.get();
        int bestGeneration = 0;

        int initialGeneration = currentGeneration.get();

        // Variables para acumular datos antes de actualizar UI
        final int UI_UPDATE_INTERVAL = 10; // Actualizar UI cada 10 generaciones

        for (int i = 0; i < generations && !stopRequested; i++) {
            // Ejecutar la generación hasta que todos mueran. El fitness de un agente es el nº
            // de frames que sobrevive, así que cortar la generación al llegar al umbral óptimo
            // evita que un agente que ya no muere la deje corriendo indefinidamente (la
            // detección de óptimo de abajo nunca llegaría).
            GenerationStats stats = engine.playGeneration((int) OPTIMAL_FITNESS_THRESHOLD);

            final int alive = stats.alive();
            final double avgFitness = stats.mean();
            final double currentBestFitness = stats.best(); // Mejor de esta generación
            final double currentMinFitness = stats.min();
            final int currentSpeciesCount = stats.species();
            final double currentDiversity = stats.diversity();

            // Guardar esta generación en el historial
            historyManager.addGenerationData(
                    engine.getGeneration(),
                    currentBestFitness,
                    avgFitness,
                    currentMinFitness,
                    alive,
                    currentSpeciesCount,
                    currentDiversity,
                    engine.getPopulation(),
                    engine.getGame().getPipes());

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
                minFitnessHistory.add(currentMinFitness);
                speciesCountHistory.add(currentSpeciesCount);
                diversityHistory.add(currentDiversity);

                // Mantener el mejor absoluto
                double previousAbsolute = bestAbsoluteFitnessHistory.isEmpty()
                        ? 0.0
                        : bestAbsoluteFitnessHistory.get(bestAbsoluteFitnessHistory.size() - 1);
                bestAbsoluteFitnessHistory.add(Math.max(currentBestFitness, previousAbsolute));

                ui.execute(() -> {
                    bestFitness.set(bestFit);
                    averageFitness.set(avgFit);
                    currentGeneration.set(currentGen);
                    aliveCount.set(0);
                    progress.update(1, 1); // Completar barra de progreso
                });

                LOG.info(
                        "Optimal fitness reached at generation {} (fitness {}); stopping training",
                        currentGen,
                        String.format("%.2f", bestFit));

                // Salir del bucle - hemos encontrado el óptimo
                break;
            }

            // Guardar datos para gráficos (siempre)
            bestFitnessHistory.add(currentBestFitness);
            avgFitnessHistory.add(avgFitness);
            minFitnessHistory.add(currentMinFitness);
            speciesCountHistory.add(currentSpeciesCount);
            diversityHistory.add(currentDiversity);

            // Mantener el mejor absoluto
            double previousAbsolute = bestAbsoluteFitnessHistory.isEmpty()
                    ? 0.0
                    : bestAbsoluteFitnessHistory.get(bestAbsoluteFitnessHistory.size() - 1);
            bestAbsoluteFitnessHistory.add(Math.max(currentBestFitness, previousAbsolute));

            // Actualizar UI solo cada N generaciones o en la última
            if (i % UI_UPDATE_INTERVAL == 0 || i == generations - 1) {
                final int currentGen = initialGeneration + i + 1;
                final double bestFit = currentBestFitness;
                final double avgFit = avgFitness;
                final int finalI = i;
                final int finalAlive = alive;

                ui.execute(() -> {
                    bestFitness.set(bestFit);
                    averageFitness.set(avgFit);
                    currentGeneration.set(currentGen);
                    aliveCount.set(finalAlive);
                    progress.update(finalI + 1, generations);
                });

                LOG.info(
                        "Generation {} - best fitness {} - average fitness {}",
                        currentGen,
                        String.format("%.2f", bestFit),
                        String.format("%.2f", avgFit));
            }

            // Evolucionar población y reiniciar juego y agentes
            engine.evolve();
        }

        // Al final de la simulación
        final int finalBestGeneration = bestGeneration;
        final double finalGlobalBestFitness = globalBestFitness;
        final boolean reachedOptimal = finalGlobalBestFitness >= OPTIMAL_FITNESS_THRESHOLD;

        ui.execute(() -> {
            running.set(false);
            fastMode.set(false);
            progress.update(1, 1); // Completar la barra de progreso

            if (!reachedOptimal) {
                LOG.info(
                        "Training finished. Best generation: {} with fitness {}",
                        finalBestGeneration,
                        String.format("%.2f", finalGlobalBestFitness));
            }
        });
    }

    /**
     * Crea una población especial con solo el mejor agente para visualización
     * @return Población con solo el mejor agente clonado
     */
    public EvolvingPopulation createBestAgentOnlyPopulation() {
        GenerationData bestGenData = historyManager.getBestGeneration();
        if (bestGenData == null) {
            return null;
        }

        // The snapshot is taken before the generation evolves: its fittest agent is the one that
        // set the record, while getBestAgent() would still be the best of earlier generations
        EvolvingPopulation bestPopulation = bestGenData.getSavedPopulation();
        FlappyBirdAgent bestAgent = bestPopulation.fittestAgent();
        FlappyBirdAgent clonedBestAgent = new FlappyBirdAgent(bestAgent);
        clonedBestAgent.setFitness(bestAgent.getFitness());

        if (bestPopulation instanceof Population) {
            // Población de réplica: no evoluciona (modo replay), así que no necesita operadores
            return Population.singleAgent(clonedBestAgent, derivedRandom(REPLAY_SALT));
        }

        return NeatPopulation.singleAgent(
                clonedBestAgent, AGENT_INPUTS, AGENT_OUTPUTS, derivedRandom(REPLAY_SALT), engine.getNeatConfig());
    }

    /**
     * Inicia la reproducción del mejor individuo encontrado. No se puede llamar durante un
     * entrenamiento rápido: su hilo sigue jugando y evolucionando la población hasta acabar la
     * generación en curso, aunque se haya pedido detenerlo.
     */
    public void playBestAgentOnly() {
        if (fastMode.get()) throw new IllegalStateException("Stop the training before replaying the best agent");
        GenerationData bestGenData = historyManager.getBestGeneration();
        if (bestGenData == null) {
            LOG.warn("No best generation recorded yet; nothing to replay");
            return;
        }

        // Crear población con solo el mejor agente (resetea también el juego)
        EvolvingPopulation bestAgentPop = createBestAgentOnlyPopulation();
        if (bestAgentPop != null) {
            startReplay(bestAgentPop, null);
            replayBanner = BEST_AGENT_BANNER;

            LOG.info("Replaying best agent (fitness {})", String.format("%.2f", historyManager.getBestFitnessEver()));
        }
    }

    /**
     * Replays a champion (e.g. loaded from a file) alone, in a loop, like the best-agent replay. It
     * replaces the current population until the simulation is reset; must not be called during a
     * fast training.
     */
    public void watchChampion(Champion champion) {
        if (fastMode.get()) throw new IllegalStateException("Stop the training before watching a champion");
        FlappyBirdAgent agent = champion.newAgent();
        Random random = derivedRandom(CHAMPION_SALT);
        EvolvingPopulation single = champion.engine() == EngineType.NEAT
                ? NeatPopulation.singleAgent(agent, AGENT_INPUTS, AGENT_OUTPUTS, random, engine.getNeatConfig())
                : Population.singleAgent(agent, random);
        startReplay(single, null);
        watchedChampion.set(champion);
        // Short enough not to cover the score, drawn at the top centre
        replayBanner = String.format(
                Locale.ROOT,
                "★ CAMPEÓN %s · gen %d ★",
                champion.engine() == EngineType.NEAT ? "NEAT" : "MLP",
                champion.metadata().generation());
        LOG.info(
                "Watching a {} champion (seed {}, generation {}, fitness {})",
                champion.engine(),
                champion.metadata().seed(),
                champion.metadata().generation(),
                String.format("%.0f", champion.metadata().fitness()));
    }

    /**
     * Replays a recorded generation in a loop, faithfully: a copy of its population, unevolved, on
     * the pipes it faced in training (derived from its run's seed and its number), so it lasts
     * exactly as long as it did. Runs recorded without a seed get deterministic pipes that are not
     * the original ones. The training population is set aside until {@link #exitReplayMode()}; must
     * not be called during a fast training.
     */
    public void playGeneration(GenerationData generation) {
        if (fastMode.get()) throw new IllegalStateException("Stop the training before replaying a generation");
        int number = generation.getGenerationNumber();
        RunHistory run = historyManager.runOf(generation);
        Long runSeed = run == null ? null : run.getSeed();

        EvolvingPopulation replay = generation.getSavedPopulation().deepCopy();
        // Fitness of the snapshot are those of the training: the fittest one is drawn in red
        FlappyBirdAgent fittest = replay.fittestAgent();
        Supplier<Random> pipes;
        if (runSeed != null) {
            long pipeSeed = TrainingEngine.pipeSeed(runSeed, number);
            pipes = () -> new Random(pipeSeed);
        } else {
            pipes = () -> derivedRandom(number);
            LOG.warn("The run of generation {} has no recorded seed: its replay uses other pipes", number);
        }
        startReplay(replay, pipes.get());
        replayPipes = pipes;
        replayPipesOriginal = runSeed != null;
        replayedFittest = fittest;
        replayedGeneration.set(generation);
        replayBanner = String.format(Locale.ROOT, GENERATION_BANNER, number);
        LOG.info(
                "Replaying generation {} (best fitness {} in training)",
                number,
                String.format("%.2f", generation.getBestFitness()));
    }

    /**
     * Starts the next lap of the current replay once every agent has died. A recorded generation
     * goes back to the same pipes; the best agent and champions keep the game's generator, so they
     * face new pipes on every lap.
     */
    public void restartReplay() {
        if (replayPipes != null) {
            engine.replacePopulation(engine.getPopulation(), replayPipes.get());
        } else {
            engine.getGame().reset();
        }
        for (FlappyBirdAgent agent : engine.getPopulation().getAgents()) {
            agent.reset();
        }
        aliveCount.set(engine.aliveCount());
    }

    /** Swaps a replay population in, setting the training one aside unless a replay already did. */
    private void startReplay(EvolvingPopulation replay, Random pipes) {
        if (!replayMode.get()) {
            trainingSnapshot = engine.snapshot();
            trainingWasRunning = running.get();
        }
        if (pipes == null) {
            engine.replacePopulation(replay);
        } else {
            engine.replacePopulation(replay, pipes);
        }
        // Agents start from scratch, keeping their brains
        for (FlappyBirdAgent agent : replay.getAgents()) {
            agent.reset();
        }
        clearReplayState();
        replayMode.set(true); // Los replays no evolucionan
        aliveCount.set(engine.aliveCount());
        running.set(true);
    }

    private void clearReplayState() {
        replayMode.set(false);
        watchedChampion.set(null);
        replayedGeneration.set(null);
        replayPipes = null;
        replayPipesOriginal = true;
        replayedFittest = null;
    }

    /**
     * The agent of the best generation recorded so far, as a champion that can be saved.
     *
     * @return null if no generation has been played yet
     */
    public Champion bestChampion() {
        GenerationData best = historyManager.getBestGeneration();
        if (best == null) return null;

        FlappyBirdAgent agent = best.getSavedPopulation().fittestAgent();
        EngineType type = agent.getBrain() instanceof Genome ? EngineType.NEAT : EngineType.GA;
        BuildInfo build = BuildInfo.current();
        ChampionMetadata metadata = new ChampionMetadata(
                seed,
                best.getGenerationNumber(),
                agent.getFitness(),
                build.commit(),
                build.version(),
                TrainingSettings.of(
                        type,
                        populationSize,
                        (int) OPTIMAL_FITNESS_THRESHOLD,
                        operatorsConfig,
                        engine.getNeatConfig()));
        return Champion.of(agent, metadata);
    }

    /** @return the banner of the current replay (best agent or loaded champion) */
    public String getReplayBanner() {
        return replayBanner;
    }

    /**
     * @return true durante un replay (mejor agente, campeón o generación grabada)
     */
    public boolean isReplayMode() {
        return replayMode.get();
    }

    /** @return whether a replay is on, for the controls that only make sense during one */
    public ReadOnlyBooleanProperty replayModeProperty() {
        return replayMode;
    }

    /** @return the recorded generation being replayed, or null */
    public ReadOnlyObjectProperty<GenerationData> replayedGenerationProperty() {
        return replayedGeneration;
    }

    /** @return false when the replayed generation's run has no seed, so its pipes are not the original ones */
    public boolean isReplayPipesOriginal() {
        return replayPipesOriginal;
    }

    /**
     * @return the agent to draw in red and to show in the network window: in a generation replay,
     *     the one that set the generation's best fitness; otherwise the population's best agent
     */
    public FlappyBirdAgent getHighlightedAgent() {
        return replayedFittest != null
                ? replayedFittest
                : engine.getPopulation().getBestAgent();
    }

    /**
     * Ends the replay (if any) and puts the training population back with its game, exactly as it
     * was when the first replay set it aside.
     */
    public void exitReplayMode() {
        if (trainingSnapshot != null) {
            engine.restore(trainingSnapshot);
            trainingSnapshot = null;
            running.set(trainingWasRunning);
            aliveCount.set(engine.aliveCount());
        }
        clearReplayState();
    }

    /**
     * Detiene la simulación rápida
     */
    public void stopSimulation() {
        stopRequested = true;
        running.set(false);
    }

    /**
     * @return El umbral de fitness considerado óptimo
     */
    public static double getOptimalFitnessThreshold() {
        return OPTIMAL_FITNESS_THRESHOLD;
    }

    /** @return la semilla global de esta simulación (para registrarla y poder reproducir la ejecución). */
    public long getSeed() {
        return seed;
    }

    /**
     * Generador determinista derivado de la semilla global, para usos auxiliares (repeticiones
     * visuales, réplicas del mejor agente) que no deben consumir números del generador principal:
     * así abrir una repetición no altera la evolución en curso.
     * @param salt distingue usos distintos (p. ej. el nº de generación que se reproduce)
     */
    public Random derivedRandom(long salt) {
        return new Random(seed * 0x9E3779B97F4A7C15L + salt);
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    /**
     * Actualiza la configuración guardada con los operadores actuales (solo aplicable en modo Fixed MLP).
     */
    public void updateOperatorsConfig() {
        if (engine.getPopulation() instanceof Population fixedPopulation) {
            operatorsConfig.updateFrom(fixedPopulation);
        }
    }

    /**
     * Obtiene la configuración de operadores genéticos
     */
    public GeneticOperatorsConfig getOperatorsConfig() {
        return operatorsConfig;
    }

    // Getters para propiedades observables
    public IntegerProperty currentGenerationProperty() {
        return currentGeneration;
    }

    public DoubleProperty bestFitnessProperty() {
        return bestFitness;
    }

    public DoubleProperty averageFitnessProperty() {
        return averageFitness;
    }

    public IntegerProperty aliveCountProperty() {
        return aliveCount;
    }

    /** @return el campeón que se está viendo, o null si se muestra el entrenamiento */
    public ReadOnlyObjectProperty<Champion> watchedChampionProperty() {
        return watchedChampion;
    }

    public BooleanProperty runningProperty() {
        return running;
    }

    // Getters para datos y objetos
    public List<Double> getBestFitnessHistory() {
        return bestFitnessHistory;
    }

    public List<Double> getAvgFitnessHistory() {
        return avgFitnessHistory;
    }

    public List<Double> getBestAbsoluteFitnessHistory() {
        return bestAbsoluteFitnessHistory;
    }

    public List<Double> getMinFitnessHistory() {
        return minFitnessHistory;
    }

    public List<Integer> getSpeciesCountHistory() {
        return speciesCountHistory;
    }

    public List<Double> getDiversityHistory() {
        return diversityHistory;
    }

    public EvolvingPopulation getPopulation() {
        return engine.getPopulation();
    }

    /** @return nº de especies actuales en modo NEAT, o -1 si el modo activo es Fixed MLP. */
    public int getSpeciesCount() {
        return engine.getSpeciesCount();
    }

    public FlappyBirdGame getGame() {
        return engine.getGame();
    }

    public boolean isFastMode() {
        return fastMode.get();
    }

    /** @return whether a fast training is running; it turns false once its thread is done */
    public ReadOnlyBooleanProperty fastModeProperty() {
        return fastMode;
    }
}
