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
import java.util.Random;

/**
 * Training core with no UI dependencies: owns the seeded generator, the population and the game,
 * plays generations and evolves the population. {@link SimulationController} adapts it to the
 * JavaFX UI (observable properties, history snapshots, background task) and the headless CLI
 * drives it directly.
 *
 * <p>All randomness comes from the seed passed to the constructor, so two engines with the same
 * seed, configuration and sequence of calls produce identical generations.
 */
public class TrainingEngine {

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
        game = new FlappyBirdGame(canvasWidth, canvasHeight, random);
        generation = 1;
        framesPlayed = 0;
    }

    /**
     * Advances the current generation by one frame.
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
        boolean allDead = false;
        while (!allDead && framesPlayed < maxFrames) {
            allDead = step();
        }
        return statistics();
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
        game.reset();
        for (FlappyBirdAgent agent : population.getAgents()) {
            agent.reset();
        }
        generation++;
        framesPlayed = 0;
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
        game.reset();
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
}
