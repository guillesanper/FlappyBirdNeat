package com.neat.flappybirdneat.view.main;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GenerationReplayInfoTest {

    @Test
    void describesTheReplayedGenerationInThreeLines() {
        String text = SimulationPanel.generationReplayInfo(26, 719.0, 12, 50);

        assertEquals(
                "Replay · generación 26\nMejor fitness en el entrenamiento: 719\nVivos: 12/50",
                text.replace(System.lineSeparator(), "\n"));
    }
}
