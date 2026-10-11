package com.neat.flappybirdneat.cli;

import com.neat.flappybirdneat.simulation.EngineType;
import java.nio.file.Path;

/**
 * Validated options of a headless training run.
 *
 * @param selection    GA selection key, or null for the default (likewise crossover, mutation, scaling)
 * @param saveChampion where to save the run's best agent as a champion file, or null not to save it
 */
public record CliOptions(
        EngineType engine,
        long seed,
        int generations,
        int population,
        int maxFrames,
        int threads,
        boolean stopOnSolve,
        String selection,
        String crossover,
        String mutation,
        String scaling,
        Path out,
        Path saveChampion) {}
