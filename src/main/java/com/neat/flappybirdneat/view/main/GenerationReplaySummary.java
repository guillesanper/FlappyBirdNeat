package com.neat.flappybirdneat.view.main;

import com.neat.flappybirdneat.history.GenerationData;
import java.util.Locale;

/**
 * Texto de la barra lateral mientras se reproduce una generación grabada, en lugar de la generación
 * y el mejor fitness del entrenamiento. Tres líneas como máximo: el panel lateral no tiene altura
 * para más.
 */
final class GenerationReplaySummary {

    private GenerationReplaySummary() {}

    /**
     * @param alive          agents of the replay still alive
     * @param total          agents of the replay
     * @param originalPipes  false when the run has no recorded seed and the pipes are not the original ones
     */
    static String describe(GenerationData generation, int alive, int total, boolean originalPipes) {
        return String.format(
                Locale.ROOT,
                "Replay · generación %d%s%nFitness en el entrenamiento: %.0f%nVivos: %d/%d",
                generation.getGenerationNumber(),
                originalPipes ? "" : " (otros tubos)",
                generation.getBestFitness(),
                alive,
                total);
    }
}
