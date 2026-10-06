package com.neat.flappybirdneat.simulation;

import com.neat.flappybirdneat.config.GeneticOperatorsConfig;
import com.neat.flappybirdneat.game.FlappyBirdGame;
import com.neat.flappybirdneat.neat.EvolvingPopulation;
import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.neat.Population;
import com.neat.flappybirdneat.neat.genome.ConnectionGene;
import com.neat.flappybirdneat.neat.genome.Genome;
import com.neat.flappybirdneat.neat.genome.NeatConfig;
import com.neat.flappybirdneat.neat.genome.NeatPopulation;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

/**
 * Training core with no UI dependencies: owns the seeded generator, the population and the game,
 * plays generations and evolves the population. {@link SimulationController} adapts it to the
 * JavaFX UI (observable properties, history snapshots, background task) and the headless CLI
 * drives it directly.
 *
 * <p>All randomness comes from the seed passed to the constructor, so two engines with the same
 * seed, configuration and sequence of calls produce identical generations, whatever the number of
 * threads. The main generator drives evolution only; the pipes of generation {@code g} come from a
 * separate seed derived from the global seed and {@code g} (see {@link #pipeSeed}).
 *
 * <p><b>Parallel evaluation.</b> Agents never interact, so instead of sharing one game every agent
 * plays its own {@link FlappyBirdGame} over the same pipe sequence. That makes a generation's
 * evaluation embarrassingly parallel: agents are spread over a {@link ForkJoinPool} (a fixed number
 * of platform threads suits this CPU-bound work better than virtual threads, which pay off on
 * blocking I/O). Each agent, its brain (including the activations the network visualizer reads) and
 * its game are touched by exactly one task; statistics, selection and reproduction run afterwards on
 * the calling thread, in agent order. The result is bit-for-bit the same with 1 or N threads, and the
 * same as stepping all agents through the shared live game frame by frame.
 */
public class TrainingEngine implements AutoCloseable {

    /** Inputs and outputs of every agent's brain, shared by both engines. */
    public static final int AGENT_INPUTS = 4;

    public static final int AGENT_OUTPUTS = 1;

    private final int populationSize;
    private final int canvasWidth;
    private final int canvasHeight;
    private final long seed;
    private final Random random;
    private final NeatConfig neatConfig = new NeatConfig();

    private EngineType engineType;
    private EvolvingPopulation population;
    private FlappyBirdGame game;
    private int generation;
    private int framesPlayed;
    private int threads = Runtime.getRuntime().availableProcessors();
    private ForkJoinPool pool;

    /**
     * Creates an engine without a population; call {@link #reset} before playing.
     */
    public TrainingEngine(int populationSize, int canvasWidth, int canvasHeight, long seed) {
        this.populationSize = populationSize;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.seed = seed;
        this.random = new Random(seed);
    }

    /**
     * Starts over with a fresh population of the given engine and a new game. The generator is not
     * reseeded: a reset continues its sequence, so a run's result depends on the resets before it.
     *
     * @param operators genetic operators for the GA (ignored by NEAT); null keeps the defaults
     */
    public void reset(EngineType engineType, GeneticOperatorsConfig operators) {
        this.engineType = engineType;
        if (engineType == EngineType.NEAT) {
            population = new NeatPopulation(populationSize, AGENT_INPUTS, AGENT_OUTPUTS, random, neatConfig);
        } else {
            Population fixedPopulation = new Population(populationSize, random);
            if (operators != null) {
                operators.applyTo(fixedPopulation);
            }
            population = fixedPopulation;
        }
        generation = 1;
        framesPlayed = 0;
        game = new FlappyBirdGame(canvasWidth, canvasHeight, pipeRandom());
    }

    /**
     * Seed of the pipe sequence of a generation. It is derived from the global seed alone (a
     * SplitMix64-style mix), not drawn from the main generator, so every agent of the generation can
     * get its own copy of the sequence and the pipes do not depend on how many numbers evolution used.
     * As a side effect, the GA and NEAT face the same pipes in the same generation for a given seed.
     */
    public static long pipeSeed(long seed, int generation) {
        return mix(seed ^ mix(PIPE_STREAM + generation));
    }

    private static final long PIPE_STREAM = 0x5DEECE66DL;

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private Random pipeRandom() {
        return new Random(pipeSeed(seed, generation));
    }

    /** Number of threads that evaluate a generation's agents (1 evaluates them on the calling thread). */
    public void setThreads(int threads) {
        if (threads < 1) throw new IllegalArgumentException("threads must be at least 1, got " + threads);
        if (threads != this.threads) {
            close();
            this.threads = threads;
        }
    }

    public int getThreads() {
        return threads;
    }

    /**
     * Advances the current generation by one frame in the shared live game (all agents together, as
     * the UI draws them).
     *
     * @return true if every agent is dead
     */
    public boolean step() {
        game.update(population.getAgents());
        framesPlayed++;
        return aliveCount() == 0;
    }

    /**
     * Plays the current generation until every agent is dead or {@code maxFrames} frames have been
     * played, whichever comes first. The cap matters because fitness is frames survived: an agent
     * that never crashes would otherwise keep the generation running forever.
     *
     * @return the statistics of the played generation (the population is not evolved yet)
     */
    public GenerationStats playGeneration(int maxFrames) {
        if (framesPlayed > 0) {
            // Already partly played frame by frame in the live game: finish it there
            boolean allDead = aliveCount() == 0;
            while (!allDead && framesPlayed < maxFrames) {
                allDead = step();
            }
        } else {
            framesPlayed = evaluateInParallel(maxFrames);
        }
        return statistics();
    }

    /** Lets every agent play alone on the generation's pipes; returns the frames the longest one lasted. */
    private int evaluateInParallel(int maxFrames) {
        FlappyBirdAgent[] agents = population.getAgents();
        long pipes = pipeSeed(seed, generation);
        if (threads == 1) {
            int frames = 0;
            for (FlappyBirdAgent agent : agents) {
                frames = Math.max(frames, playAlone(agent, pipes, maxFrames));
            }
            return frames;
        }

        List<Callable<Integer>> tasks = new ArrayList<>(agents.length);
        for (FlappyBirdAgent agent : agents) {
            tasks.add(() -> playAlone(agent, pipes, maxFrames));
        }
        int frames = 0;
        try {
            for (Future<Integer> result : pool().invokeAll(tasks)) {
                frames = Math.max(frames, result.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while evaluating generation " + generation, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Evaluating generation " + generation + " failed", e.getCause());
        }
        return frames;
    }

    /** Plays one agent in its own game until it dies or reaches {@code maxFrames}; returns the frames played. */
    private int playAlone(FlappyBirdAgent agent, long pipes, int maxFrames) {
        FlappyBirdGame ownGame = new FlappyBirdGame(canvasWidth, canvasHeight, new Random(pipes));
        FlappyBirdAgent[] single = {agent};
        int frames = 0;
        while (!agent.isDead() && frames < maxFrames) {
            ownGame.update(single);
            frames++;
        }
        return frames;
    }

    private ForkJoinPool pool() {
        if (pool == null) {
            pool = new ForkJoinPool(threads);
        }
        return pool;
    }

    /** @return the statistics of the current generation as played so far */
    public GenerationStats statistics() {
        FlappyBirdAgent[] agents = population.getAgents();
        double total = 0;
        double best = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        int alive = 0;
        for (FlappyBirdAgent agent : agents) {
            double fitness = agent.getFitness();
            total += fitness;
            best = Math.max(best, fitness);
            min = Math.min(min, fitness);
            if (!agent.isDead()) alive++;
        }

        double meanNodes = Double.NaN;
        double meanConnections = Double.NaN;
        if (engineType == EngineType.NEAT) {
            long nodes = 0;
            long connections = 0;
            for (FlappyBirdAgent agent : agents) {
                Genome genome = (Genome) agent.getBrain();
                nodes += genome.getNodes().size();
                for (ConnectionGene connection : genome.getConnections()) {
                    if (connection.isEnabled()) connections++;
                }
            }
            meanNodes = (double) nodes / agents.length;
            meanConnections = (double) connections / agents.length;
        }

        return new GenerationStats(
                generation,
                best,
                total / populationSize,
                min,
                alive,
                framesPlayed,
                getSpeciesCount(),
                population.diversity(),
                meanNodes,
                meanConnections);
    }

    /** Breeds the next generation from the current one and resets the game and the agents. */
    public void evolve() {
        population.naturalSelection();
        for (FlappyBirdAgent agent : population.getAgents()) {
            agent.reset();
        }
        generation++;
        framesPlayed = 0;
        game.reset(pipeRandom());
    }

    /**
     * Plays the current generation (see {@link #playGeneration}) and then evolves the population.
     * NEAT speciates a generation while evolving it, so unlike {@link #statistics()} (which can only
     * report the species of the previous generation) the result counts the species the played
     * generation was divided into.
     */
    public GenerationStats runGeneration(int maxFrames) {
        GenerationStats stats = playGeneration(maxFrames);
        evolve();
        return stats.withSpecies(getSpeciesCount());
    }

    /** @return number of agents of the current generation that are still alive */
    public int aliveCount() {
        int alive = 0;
        for (FlappyBirdAgent agent : population.getAgents()) {
            if (!agent.isDead()) alive++;
        }
        return alive;
    }

    /** @return mean fitness of the current generation so far */
    public double meanFitness() {
        double total = 0;
        for (FlappyBirdAgent agent : population.getAgents()) {
            total += agent.getFitness();
        }
        return total / populationSize;
    }

    /** @return number of NEAT species, or -1 when the engine is the GA */
    public int getSpeciesCount() {
        return population instanceof NeatPopulation neatPopulation ? neatPopulation.getSpeciesCount() : -1;
    }

    /**
     * Swaps in another population, e.g. a single-agent replay of the best agent. The game is reset
     * and the swapped-in population is never evolved by the caller.
     */
    public void replacePopulation(EvolvingPopulation replacement) {
        population = replacement;
        game.reset(pipeRandom());
        framesPlayed = 0;
    }

    public EvolvingPopulation getPopulation() {
        return population;
    }

    public FlappyBirdGame getGame() {
        return game;
    }

    public EngineType getEngineType() {
        return engineType;
    }

    public NeatConfig getNeatConfig() {
        return neatConfig;
    }

    /** @return 1-based number of the generation being played */
    public int getGeneration() {
        return generation;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public long getSeed() {
        return seed;
    }

    /** Stops the evaluation threads; the engine keeps working and starts new ones if needed. */
    @Override
    public void close() {
        if (pool != null) {
            pool.shutdown();
            pool = null;
        }
    }
}
