package com.neat.flappybirdneat.simulation;

import java.util.Locale;

/** Evolution engine driving a {@link TrainingEngine}. */
public enum EngineType {
    /** Genetic algorithm over the weights of a fixed 4-8-1 MLP, with configurable operators. */
    GA,
    /** NEAT: weights and topology evolve, with speciation and historical-marking crossover. */
    NEAT;

    /** @return the engine named {@code name} ("ga" or "neat", case-insensitive) */
    public static EngineType fromName(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "ga" -> GA;
            case "neat" -> NEAT;
            default -> throw new IllegalArgumentException("Unknown engine '" + name + "' (expected neat or ga)");
        };
    }
}
