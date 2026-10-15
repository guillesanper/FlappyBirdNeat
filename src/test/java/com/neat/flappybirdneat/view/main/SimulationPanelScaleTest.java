package com.neat.flappybirdneat.view.main;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Escala del juego (800 x 600 lógicos) dentro del espacio que deja la ventana. */
class SimulationPanelScaleTest {

    @Test
    void fitsTheGameWithoutDistortingIt() {
        assertEquals(1.0, SimulationPanel.fitScale(800, 600, 800, 600), 1e-12);
        assertEquals(2.0, SimulationPanel.fitScale(1600, 1200, 800, 600), 1e-12);
        // Ventana panorámica: manda la altura
        assertEquals(1.5, SimulationPanel.fitScale(2400, 900, 800, 600), 1e-12);
        // Ventana estrecha: manda el ancho
        assertEquals(0.5, SimulationPanel.fitScale(400, 900, 800, 600), 1e-12);
    }

    @Test
    void noSpaceMeansNoScaleInsteadOfDividingByZero() {
        assertEquals(0.0, SimulationPanel.fitScale(0, 600, 800, 600));
        assertEquals(0.0, SimulationPanel.fitScale(800, -1, 800, 600));
    }
}
