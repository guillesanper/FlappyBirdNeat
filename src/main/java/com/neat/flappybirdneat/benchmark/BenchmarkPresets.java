package com.neat.flappybirdneat.benchmark;

import com.neat.flappybirdneat.neat.crossover.CruceAritmetico;
import com.neat.flappybirdneat.neat.crossover.CrucePuntoUnico;
import com.neat.flappybirdneat.neat.crossover.CruceUniforme;
import com.neat.flappybirdneat.neat.mutation.MutacionGaussiana;
import com.neat.flappybirdneat.neat.mutation.MutacionNoUniforme;
import com.neat.flappybirdneat.neat.mutation.MutacionUniforme;
import com.neat.flappybirdneat.neat.scaling.EscaladoSigma;
import com.neat.flappybirdneat.neat.selection.SeleccionRuleta;
import com.neat.flappybirdneat.neat.selection.SeleccionTorneoDeterministico;
import com.neat.flappybirdneat.neat.selection.SeleccionTruncamiento;

import java.util.List;

/**
 * Combinaciones de operadores predefinidas para el modo benchmark (comparativa de operadores).
 * Cada preset representa una estrategia evolutiva completa y razonable, no una mezcla arbitraria.
 */
public final class BenchmarkPresets {

    private BenchmarkPresets() {
    }

    /** @param generations usado por {@code MutacionNoUniforme}, que decrece su magnitud a lo largo de las generaciones. */
    public static List<BenchmarkConfig> defaultConfigs(int generations) {
        return List.of(
                new BenchmarkConfig("Ruleta + Gaussiana + Uniforme",
                        SeleccionRuleta::new, () -> null, MutacionGaussiana::new, CruceUniforme::new),
                new BenchmarkConfig("Torneo Determinístico + Uniforme + Punto Único",
                        SeleccionTorneoDeterministico::new, () -> null, MutacionUniforme::new, CrucePuntoUnico::new),
                new BenchmarkConfig("Truncamiento + No Uniforme + Aritmético",
                        SeleccionTruncamiento::new, () -> null,
                        () -> new MutacionNoUniforme(Math.max(generations, 1)), CruceAritmetico::new),
                new BenchmarkConfig("Ruleta + Escalado Sigma + Gaussiana + Uniforme",
                        SeleccionRuleta::new, EscaladoSigma::new, MutacionGaussiana::new, CruceUniforme::new)
        );
    }
}
