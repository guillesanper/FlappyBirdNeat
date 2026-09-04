package com.neat.flappybirdneat.benchmark;

import com.neat.flappybirdneat.neat.crossover.CruceStrategy;
import com.neat.flappybirdneat.neat.mutation.MutacionStrategy;
import com.neat.flappybirdneat.neat.scaling.Escalado;
import com.neat.flappybirdneat.neat.selection.Seleccion;

import java.util.function.Supplier;

/**
 * Combinación de operadores genéticos (modo Fixed MLP) a comparar en un benchmark.
 * Guarda fábricas ({@link Supplier}) en lugar de instancias porque cada semilla/ejecución
 * necesita su propia instancia de estrategia (algunas, como {@code MutacionNoUniforme},
 * llevan estado interno que no debe compartirse entre ejecuciones).
 */
public class BenchmarkConfig {
    private final String label;
    private final Supplier<Seleccion> seleccionSupplier;
    private final Supplier<Escalado> escaladoSupplier;
    private final Supplier<MutacionStrategy> mutacionSupplier;
    private final Supplier<CruceStrategy> cruceSupplier;

    public BenchmarkConfig(String label, Supplier<Seleccion> seleccionSupplier, Supplier<Escalado> escaladoSupplier,
                            Supplier<MutacionStrategy> mutacionSupplier, Supplier<CruceStrategy> cruceSupplier) {
        this.label = label;
        this.seleccionSupplier = seleccionSupplier;
        this.escaladoSupplier = escaladoSupplier;
        this.mutacionSupplier = mutacionSupplier;
        this.cruceSupplier = cruceSupplier;
    }

    public String getLabel() { return label; }
    public Seleccion newSeleccion() { return seleccionSupplier.get(); }
    public Escalado newEscalado() { return escaladoSupplier != null ? escaladoSupplier.get() : null; }
    public MutacionStrategy newMutacion() { return mutacionSupplier.get(); }
    public CruceStrategy newCruce() { return cruceSupplier.get(); }
}
