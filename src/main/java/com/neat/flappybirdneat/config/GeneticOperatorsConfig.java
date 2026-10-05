package com.neat.flappybirdneat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.neat.flappybirdneat.neat.Population;
import com.neat.flappybirdneat.neat.crossover.*;
import com.neat.flappybirdneat.neat.mutation.*;
import com.neat.flappybirdneat.neat.selection.*;
import com.neat.flappybirdneat.neat.scaling.*;

/**
 * Almacena la configuración de operadores genéticos
 * para persistir entre reinicios de simulación
 */
public class GeneticOperatorsConfig {
    private static final Logger LOG = LoggerFactory.getLogger(GeneticOperatorsConfig.class);

    private SelectionStrategy selectionStrategy;
    private ScalingStrategy scalingStrategy;
    private MutationStrategy mutationStrategy;
    private CrossoverStrategy crossoverStrategy;

    public GeneticOperatorsConfig() {
        // Valores por defecto
        selectionStrategy = new RouletteSelection();
        scalingStrategy = null;
        mutationStrategy = new GaussianMutation();
        crossoverStrategy = new UniformCrossover();
    }

    /**
     * Aplica la configuración guardada a una población
     */
    public void applyTo(Population population) {
        population.setSelectionStrategy(selectionStrategy);
        population.setScalingStrategy(scalingStrategy);
        population.setMutationStrategy(mutationStrategy);
        population.setCrossoverStrategy(crossoverStrategy);

        LOG.debug("Applying genetic operators: {}", this);
    }

    /**
     * Actualiza la configuración desde una población
     */
    public void updateFrom(Population population) {
        this.selectionStrategy = population.getSelectionStrategy();
        this.scalingStrategy = population.getScalingStrategy();
        this.mutationStrategy = population.getMutationStrategy();
        this.crossoverStrategy = population.getCrossoverStrategy();

        LOG.info("Genetic operators updated: {}", this);
    }

    @Override
    public String toString() {
        return "selection=" + simpleName(selectionStrategy) + ", crossover=" + simpleName(crossoverStrategy)
                + ", mutation=" + simpleName(mutationStrategy) + ", scaling=" + simpleName(scalingStrategy);
    }

    private static String simpleName(Object strategy) {
        return strategy != null ? strategy.getClass().getSimpleName() : "none";
    }

    // Getters
    public SelectionStrategy getSelectionStrategy() { return selectionStrategy; }
    public ScalingStrategy getScalingStrategy() { return scalingStrategy; }
    public MutationStrategy getMutationStrategy() { return mutationStrategy; }
    public CrossoverStrategy getCrossoverStrategy() { return crossoverStrategy; }

    // Setters
    public void setSelectionStrategy(SelectionStrategy s) { this.selectionStrategy = s; }
    public void setScalingStrategy(ScalingStrategy e) { this.scalingStrategy = e; }
    public void setMutationStrategy(MutationStrategy m) { this.mutationStrategy = m; }
    public void setCrossoverStrategy(CrossoverStrategy c) { this.crossoverStrategy = c; }
}
