package com.neat.flappybirdneat.neat.selection;

import java.util.Random;

/**
 * Selección estocástica universal (SUS).
 * Mejora de la roulette que reduce el sesgo usando múltiples punteros equidistantes.
 * Proporciona una selección más justa y con menor varianza.
 */
public class StochasticUniversalSelection extends SelectionStrategy {
    @Override
    public int[] select(Selectable[] list, int count, Random random) {
        int[] selected = new int[count];

        // Generar un valor aleatorio entre 0 y 1/tamPoblacion
        double r = random.nextDouble() / count;

        // Para cada punto de selección
        for (int i = 0; i < count; i++) {
            // Calcular el punto de selección actual
            double punto = r + ((double) i / count);

            // Encontrar el individuo correspondiente
            int j = 0;
            while (j < count && punto > list[j].getAccProb()) {
                j++;
            }

            // Evitar índice fuera de rango
            if (j >= count) j = count - 1;

            selected[i] = list[j].getIndex();
        }

        return selected;
    }
}
