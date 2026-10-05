package com.neat.flappybirdneat.config;

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

        // Debug: mostrar qué configuración se está aplicando
        System.out.println("🔧 Aplicando configuración de operadores genéticos:");
        System.out.println("  - Cruce: " + (crossoverStrategy != null ? crossoverStrategy.getClass().getSimpleName() : "null"));
        System.out.println("  - Selección: " + (selectionStrategy != null ? selectionStrategy.getClass().getSimpleName() : "null"));
        System.out.println("  - Mutación: " + (mutationStrategy != null ? mutationStrategy.getClass().getSimpleName() : "null"));
        System.out.println("  - Escalado: " + (scalingStrategy != null ? scalingStrategy.getClass().getSimpleName() : "Ninguno"));
    }

    /**
     * Actualiza la configuración desde una población
     */
    public void updateFrom(Population population) {
        this.selectionStrategy = population.getSelectionStrategy();
        this.scalingStrategy = population.getScalingStrategy();
        this.mutationStrategy = population.getMutationStrategy();
        this.crossoverStrategy = population.getCrossoverStrategy();

        // Debug: mostrar qué configuración se está guardando
        System.out.println("💾 Guardando configuración de operadores genéticos:");
        System.out.println("  - Cruce: " + (crossoverStrategy != null ? crossoverStrategy.getClass().getSimpleName() : "null"));
        System.out.println("  - Selección: " + (selectionStrategy != null ? selectionStrategy.getClass().getSimpleName() : "null"));
        System.out.println("  - Mutación: " + (mutationStrategy != null ? mutationStrategy.getClass().getSimpleName() : "null"));
        System.out.println("  - Escalado: " + (scalingStrategy != null ? scalingStrategy.getClass().getSimpleName() : "Ninguno"));
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
