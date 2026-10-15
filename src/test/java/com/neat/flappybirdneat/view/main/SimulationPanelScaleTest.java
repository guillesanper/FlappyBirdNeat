package com.neat.flappybirdneat.view.main;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** The game scales uniformly to the space the sidebar leaves free. */
class SimulationPanelScaleTest {

    private static double fit(double width, double height) {
        return SimulationPanel.fitScale(width, height, 800, 600);
    }

    @Test
    void theLogicalSizeIsScaleOne() {
        assertEquals(1.0, fit(800, 600));
    }

    @Test
    void twiceTheLogicalSizeIsScaleTwo() {
        assertEquals(2.0, fit(1600, 1200));
    }

    @Test
    void aWideAreaIsLimitedByItsHeight() {
        assertEquals(1.5, fit(2400, 900));
        assertEquals(0.5, fit(1000, 300));
    }

    @Test
    void aTallAreaIsLimitedByItsWidth() {
        assertEquals(0.5, fit(400, 1000));
    }

    @Test
    void noSpaceGivesScaleZero() {
        assertEquals(0.0, fit(0, 0));
        assertEquals(0.0, fit(0, 600));
        assertEquals(0.0, fit(800, -5));
    }
}
