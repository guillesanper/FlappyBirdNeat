package com.neat.flappybirdneat.view.main;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.history.GenerationData;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Texto de la barra lateral mientras se reproduce una generación grabada. */
class GenerationReplaySummaryTest {

    private static GenerationData generation(int number, double bestFitness) {
        GenerationData data = new GenerationData(bestFitness, 10, 1, 0, -1, 0, null, List.of());
        data.setGenerationNumber(number);
        return data;
    }

    @Test
    void describesTheReplayedGenerationInThreeLines() {
        String text = GenerationReplaySummary.describe(generation(7, 1234), 12, 50, true);

        assertEquals(
                String.join(
                        System.lineSeparator(),
                        "Replay · generación 7",
                        "Fitness en el entrenamiento: 1234",
                        "Vivos: 12/50"),
                text);
    }

    @Test
    void warnsWhenThePipesAreNotTheOriginalOnes() {
        String text = GenerationReplaySummary.describe(generation(3, 90), 50, 50, false);

        assertTrue(text.startsWith("Replay · generación 3 (otros tubos)"), text);
        assertEquals(3, text.lines().count());
    }
}
