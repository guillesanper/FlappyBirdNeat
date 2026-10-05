package com.neat.flappybirdneat.neat.selection;

import java.util.Random;

/**
 * Selección por roulette.
 * Cada individuo tiene una probabilidad de ser seleccionado proporcional a su fitness.
 */
public class RouletteSelection extends SelectionStrategy {
    @Override
    public int[] select(Selectable[] list, int count, Random random) {
        int[] selected = new int[count];
        // Suma total de probabilidades acumuladas (normalmente 1.0, salvo si el fitness total
        // de la población es 0, en cuyo caso todas las prob quedan a 0 y no hay señal que seguir).
        double total = list.length == 0 ? 0 : list[list.length - 1].getAccProb() + list[list.length - 1].getProb();

        for (int selectedSoFar = 0; selectedSoFar < count; selectedSoFar++) {
            if (total <= 0) {
                // Sin señal de fitness: elegir uniformemente para no bloquear la selección.
                selected[selectedSoFar] = list[random.nextInt(list.length)].getIndex();
                continue;
            }

            // Recorre TODA la lista (no solo los primeros tamPoblacion elementos: cuando hay
            // elitismo, tamPoblacion < list.length y los individuos con mayor probabilidad
            // acumulada pueden quedar fuera del rango si no se comprueban todos).
            double x = random.nextDouble() * total;
            int chosen = list.length - 1;
            for (int i = 0; i < list.length; i++) {
                if (x < list[i].getAccProb() + list[i].getProb()) {
                    chosen = i;
                    break;
                }
            }
            selected[selectedSoFar] = list[chosen].getIndex();
        }
        return selected;
    }
}
