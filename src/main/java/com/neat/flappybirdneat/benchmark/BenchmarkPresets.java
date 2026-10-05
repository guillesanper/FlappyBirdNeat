package com.neat.flappybirdneat.benchmark;

import com.neat.flappybirdneat.neat.crossover.ArithmeticCrossover;
import com.neat.flappybirdneat.neat.crossover.SinglePointCrossover;
import com.neat.flappybirdneat.neat.crossover.UniformCrossover;
import com.neat.flappybirdneat.neat.mutation.GaussianMutation;
import com.neat.flappybirdneat.neat.mutation.NonUniformMutation;
import com.neat.flappybirdneat.neat.mutation.UniformMutation;
import com.neat.flappybirdneat.neat.scaling.SigmaScaling;
import com.neat.flappybirdneat.neat.selection.RouletteSelection;
import com.neat.flappybirdneat.neat.selection.DeterministicTournamentSelection;
import com.neat.flappybirdneat.neat.selection.TruncationSelection;

import java.util.List;

/**
 * Combinaciones de operadores predefinidas para el modo benchmark (comparativa de operadores).
 * Cada preset representa una estrategia evolutiva completa y razonable, no una mezcla arbitraria.
 */
public final class BenchmarkPresets {

    private BenchmarkPresets() {
    }

    /** @param generations usado por {@code NonUniformMutation}, que decrece su magnitud a lo largo de las generaciones. */
    public static List<BenchmarkConfig> defaultConfigs(int generations) {
        return List.of(
                new BenchmarkConfig("Ruleta + Gaussiana + Uniforme",
                        RouletteSelection::new, () -> null, GaussianMutation::new, UniformCrossover::new),
                new BenchmarkConfig("Torneo Determinístico + Uniforme + Punto Único",
                        DeterministicTournamentSelection::new, () -> null, UniformMutation::new, SinglePointCrossover::new),
                new BenchmarkConfig("Truncamiento + No Uniforme + Aritmético",
                        TruncationSelection::new, () -> null,
                        () -> new NonUniformMutation(Math.max(generations, 1)), ArithmeticCrossover::new),
                new BenchmarkConfig("Ruleta + Escalado Sigma + Gaussiana + Uniforme",
                        RouletteSelection::new, SigmaScaling::new, GaussianMutation::new, UniformCrossover::new)
        );
    }
}
