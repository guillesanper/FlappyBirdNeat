package com.neat.flappybirdneat.benchmark;

import com.neat.flappybirdneat.neat.crossover.CrossoverStrategy;
import com.neat.flappybirdneat.neat.mutation.MutationStrategy;
import com.neat.flappybirdneat.neat.scaling.ScalingStrategy;
import com.neat.flappybirdneat.neat.selection.SelectionStrategy;
import java.util.function.Supplier;

/**
 * Combinación de operadores genéticos (modo Fixed MLP) a comparar en un benchmark.
 * Guarda fábricas ({@link Supplier}) en lugar de instancias porque cada semilla/ejecución
 * necesita su propia instancia de estrategia (algunas, como {@code NonUniformMutation},
 * llevan estado interno que no debe compartirse entre ejecuciones).
 */
public class BenchmarkConfig {
    private final String label;
    private final Supplier<SelectionStrategy> selectionSupplier;
    private final Supplier<ScalingStrategy> scalingSupplier;
    private final Supplier<MutationStrategy> mutationSupplier;
    private final Supplier<CrossoverStrategy> crossoverSupplier;

    public BenchmarkConfig(
            String label,
            Supplier<SelectionStrategy> selectionSupplier,
            Supplier<ScalingStrategy> scalingSupplier,
            Supplier<MutationStrategy> mutationSupplier,
            Supplier<CrossoverStrategy> crossoverSupplier) {
        this.label = label;
        this.selectionSupplier = selectionSupplier;
        this.scalingSupplier = scalingSupplier;
        this.mutationSupplier = mutationSupplier;
        this.crossoverSupplier = crossoverSupplier;
    }

    public String getLabel() {
        return label;
    }

    public SelectionStrategy newSelection() {
        return selectionSupplier.get();
    }

    public ScalingStrategy newScaling() {
        return scalingSupplier != null ? scalingSupplier.get() : null;
    }

    public MutationStrategy newMutation() {
        return mutationSupplier.get();
    }

    public CrossoverStrategy newCrossover() {
        return crossoverSupplier.get();
    }
}
