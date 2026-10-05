package com.neat.flappybirdneat.neat.selection;

import java.util.Random;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Selección por truncamiento.
 * Solo los mejores individuos (top X%) pueden reproducirse.
 * Los seleccionados se repiten proporcionalmente para llenar la población.
 */
public class TruncationSelection extends SelectionStrategy {

    private final double trunc;

    public TruncationSelection() {
        this.trunc = 0.6;
    }

    public TruncationSelection(double trunc) {
        this.trunc = trunc;
    }

    @Override
    public int[] select(Selectable[] list, int count, Random random) {
        int[] selected = new int[count];

        // Ordenar individuos por fitness descendente
        Arrays.sort(list, Comparator.comparingDouble(Selectable::getFitness).reversed());

        // Determinar número de individuos seleccionables
        int numCandidates = (int) (list.length * this.trunc);
        numCandidates = Math.max(numCandidates, 1);

        // Calcular repeticiones
        int repetitions = count / numCandidates;
        int remainder = count % numCandidates;

        int index = 0;
        for (int i = 0; i < numCandidates; i++) {
            for (int j = 0; j < repetitions; j++) {
                selected[index++] = list[i].getIndex();
            }
        }

        // Distribuir el resto
        for (int i = 0; i < remainder; i++) {
            selected[index++] = list[i].getIndex();
        }

        return selected;
    }
}
