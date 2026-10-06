package com.neat.flappybirdneat.cli;

import com.neat.flappybirdneat.config.GeneticOperatorsConfig;
import com.neat.flappybirdneat.neat.crossover.CrossoverFactory;
import com.neat.flappybirdneat.neat.mutation.MutationFactory;
import com.neat.flappybirdneat.neat.mutation.MutationStrategy;
import com.neat.flappybirdneat.neat.mutation.NonUniformMutation;
import com.neat.flappybirdneat.neat.scaling.ScalingFactory;
import com.neat.flappybirdneat.neat.selection.SelectionFactory;
import com.neat.flappybirdneat.simulation.EngineType;
import com.neat.flappybirdneat.simulation.GenerationStats;
import com.neat.flappybirdneat.simulation.TrainingEngine;
import java.io.IOException;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runs a whole headless training with {@link TrainingEngine}, streaming the CSV as it goes. */
public final class HeadlessTraining {

    private static final Logger LOG = LoggerFactory.getLogger(HeadlessTraining.class);

    /** Same playfield as the UI, so headless and visual runs are comparable. */
    static final int CANVAS_WIDTH = 800;

    static final int CANVAS_HEIGHT = 600;

    private static final int LOG_EVERY = 10;

    private HeadlessTraining() {}

    /**
     * @param solvedGeneration first generation in which an agent reached the frame cap, or -1
     */
    public record Summary(
            CliOptions options,
            int generationsRun,
            double bestFitness,
            int bestGeneration,
            int solvedGeneration,
            long totalMillis) {}

    public static Summary run(CliOptions options) throws IOException {
        TrainingEngine engine = new TrainingEngine(options.population(), CANVAS_WIDTH, CANVAS_HEIGHT, options.seed());
        engine.reset(options.engine(), operators(options));
        LOG.info(
                "Training {} with seed {}: {} generations of {} agents, at most {} frames each",
                options.engine(),
                options.seed(),
                options.generations(),
                options.population(),
                options.maxFrames());

        long start = System.nanoTime();
        double bestFitness = Double.NEGATIVE_INFINITY;
        int bestGeneration = 0;
        int solvedGeneration = -1;
        int generationsRun = 0;

        try (TrainingCsvWriter csv =
                new TrainingCsvWriter(Files.newBufferedWriter(options.out()), options.maxFrames())) {
            for (int i = 0; i < options.generations(); i++) {
                long generationStart = System.nanoTime();
                GenerationStats stats = engine.runGeneration(options.maxFrames());
                long wallMillis = (System.nanoTime() - generationStart) / 1_000_000;
                csv.writeRow(stats, wallMillis);
                generationsRun++;

                if (stats.best() > bestFitness) {
                    bestFitness = stats.best();
                    bestGeneration = stats.generation();
                }
                boolean solved = stats.solved(options.maxFrames());
                if (solved && solvedGeneration < 0) solvedGeneration = stats.generation();

                if (stats.generation() % LOG_EVERY == 0 || i == options.generations() - 1 || solved) {
                    LOG.info(
                            "Generation {}/{}: best {}, mean {} ({} ms)",
                            stats.generation(),
                            options.generations(),
                            String.format("%.0f", stats.best()),
                            String.format("%.2f", stats.mean()),
                            wallMillis);
                }
                if (solved && options.stopOnSolve()) break;
            }
        }
        long totalMillis = (System.nanoTime() - start) / 1_000_000;
        return new Summary(options, generationsRun, bestFitness, bestGeneration, solvedGeneration, totalMillis);
    }

    /** GA operators from the options; NEAT has its own reproduction and ignores them. */
    private static GeneticOperatorsConfig operators(CliOptions options) {
        GeneticOperatorsConfig operators = new GeneticOperatorsConfig();
        if (options.engine() != EngineType.GA) return operators;

        if (options.selection() != null) {
            operators.setSelectionStrategy(SelectionFactory.getInstance().getSelectionStrategy(options.selection()));
        }
        if (options.crossover() != null) {
            operators.setCrossoverStrategy(CrossoverFactory.getInstance().getCrossoverStrategy(options.crossover()));
        }
        if (options.mutation() != null) {
            MutationStrategy mutation = MutationFactory.getInstance().getMutationStrategy(options.mutation());
            if (mutation instanceof NonUniformMutation) {
                // Non-uniform mutation decays its step over a horizon: make it the length of the run
                mutation = MutationFactory.getInstance().getMutationStrategy(options.mutation(), options.generations());
            }
            operators.setMutationStrategy(mutation);
        }
        if (options.scaling() != null) {
            operators.setScalingStrategy(ScalingFactory.getInstance().getScalingStrategy(options.scaling()));
        }
        return operators;
    }
}
