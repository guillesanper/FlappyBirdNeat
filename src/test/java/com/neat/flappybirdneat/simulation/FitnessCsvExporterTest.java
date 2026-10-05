package com.neat.flappybirdneat.simulation;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class FitnessCsvExporterTest {

    @Test
    void writesHeaderPlusOneRowPerRecordedGeneration() {
        SimulationController controller = new SimulationController(10, 800, 600, 3L);
        controller.runningProperty().set(true);
        for (int gen = 0; gen < 2; gen++) {
            int frames = 0;
            while (!controller.updateFrame() && frames++ < 5_000) {
                // play the generation out
            }
            controller.nextGeneration();
        }

        StringWriter out = new StringWriter();
        FitnessCsvExporter.write(controller, out);
        String[] lines = out.toString().split("\\R");

        assertEquals(FitnessCsvExporter.HEADER, lines[0]);
        assertEquals(1 + controller.getBestFitnessHistory().size(), lines.length);
        for (int i = 1; i < lines.length; i++) {
            String[] cells = lines[i].split(",", -1);
            assertEquals(6, cells.length);
            assertEquals(String.valueOf(i), cells[0]);
            assertEquals(controller.getBestFitnessHistory().get(i - 1), Double.parseDouble(cells[1]));
            assertEquals("-1", cells[4], "Fixed MLP mode has no species");
        }
    }
}
