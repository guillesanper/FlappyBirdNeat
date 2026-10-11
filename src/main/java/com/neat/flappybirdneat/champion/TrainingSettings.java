package com.neat.flappybirdneat.champion;

import com.neat.flappybirdneat.config.GeneticOperatorsConfig;
import com.neat.flappybirdneat.neat.genome.NeatConfig;
import com.neat.flappybirdneat.simulation.EngineType;
import java.util.LinkedHashMap;
import java.util.Map;

/** The settings of a training run worth recording next to its champion. */
public final class TrainingSettings {

    private TrainingSettings() {}

    /**
     * @param maxFrames frame cap per generation (fitness is frames survived, so it caps the fitness)
     * @param operators the GA's operators (ignored for NEAT)
     * @param neat      NEAT's parameters (ignored for the GA)
     */
    public static Map<String, Object> of(
            EngineType engine, int population, int maxFrames, GeneticOperatorsConfig operators, NeatConfig neat) {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("population", population);
        settings.put("maxFrames", maxFrames);
        if (engine == EngineType.GA) {
            settings.put("selection", name(operators.getSelectionStrategy()));
            settings.put("crossover", name(operators.getCrossoverStrategy()));
            settings.put("mutation", name(operators.getMutationStrategy()));
            settings.put("scaling", name(operators.getScalingStrategy()));
        } else {
            settings.put("compatibilityThreshold", neat.getCompatibilityThreshold());
            settings.put("targetSpeciesCount", neat.getTargetSpeciesCount());
            settings.put("weightMutationRate", neat.getWeightMutationRate());
            settings.put("addConnectionRate", neat.getAddConnectionRate());
            settings.put("addNodeRate", neat.getAddNodeRate());
            settings.put("survivalThreshold", neat.getSurvivalThreshold());
            settings.put("stagnationLimit", neat.getStagnationLimit());
        }
        return settings;
    }

    private static String name(Object strategy) {
        return strategy == null ? "none" : strategy.getClass().getSimpleName();
    }
}
