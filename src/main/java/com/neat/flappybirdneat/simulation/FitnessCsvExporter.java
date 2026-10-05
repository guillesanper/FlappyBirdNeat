package com.neat.flappybirdneat.simulation;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;

/**
 * Writes the per-generation statistics of a {@link SimulationController} as CSV: one row per
 * generation with best, average and minimum fitness, number of species (-1 outside NEAT) and
 * genetic diversity. Columns missing for a generation are left empty.
 */
public final class FitnessCsvExporter {

    static final String HEADER = "Generacion,MejorFitness,FitnessPromedio,FitnessMinimo,NumEspecies,Diversidad";

    private FitnessCsvExporter() {}

    public static void write(SimulationController controller, Writer out) {
        PrintWriter writer = new PrintWriter(out);
        writer.println(HEADER);

        List<Double> bestFitness = controller.getBestFitnessHistory();
        List<Double> avgFitness = controller.getAvgFitnessHistory();
        List<Double> minFitness = controller.getMinFitnessHistory();
        List<Integer> speciesCount = controller.getSpeciesCountHistory();
        List<Double> diversity = controller.getDiversityHistory();

        for (int i = 0; i < bestFitness.size(); i++) {
            writer.println((i + 1) + "," + bestFitness.get(i) + ","
                    + cell(avgFitness, i) + ","
                    + cell(minFitness, i) + ","
                    + cell(speciesCount, i) + ","
                    + cell(diversity, i));
        }
        writer.flush();
    }

    private static String cell(List<?> column, int row) {
        return row < column.size() ? String.valueOf(column.get(row)) : "";
    }
}
