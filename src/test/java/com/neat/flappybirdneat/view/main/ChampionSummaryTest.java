package com.neat.flappybirdneat.view.main;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.champion.ChampionFile;
import org.junit.jupiter.api.Test;

/** Texto de la barra lateral mientras se ve un campeón (los valores salen de champions/README.md). */
class ChampionSummaryTest {

    @Test
    void describesTheBundledNeatChampion() {
        String text = ChampionSummary.describe(ChampionFile.loadBundled(ChampionFile.BUNDLED_NEAT));

        assertTrue(text.startsWith("Campeón NEAT · semilla 3"), text);
        assertTrue(text.contains("Generación 39 · fitness 20000"), text);
        assertTrue(text.contains("Red: 5 ocultos, 9/15 conexiones"), text);
        assertEquals(3, text.lines().count(), "el panel lateral solo tiene sitio para tres líneas");
    }

    @Test
    void describesTheBundledMlpChampion() {
        String text = ChampionSummary.describe(ChampionFile.loadBundled(ChampionFile.BUNDLED_MLP));

        assertTrue(text.startsWith("Campeón MLP · semilla 2"), text);
        assertTrue(text.contains("Generación 13"), text);
        assertTrue(text.contains("Red: MLP 4-8-1"), text);
    }
}
