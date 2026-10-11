package com.neat.flappybirdneat.neat.selection;

import java.util.Random;

/**
 * Selección por restos (muestreo estocástico del resto).
 * Cada individuo recibe tantas copias como la parte entera de su número esperado de copias
 * ({@code prob * count}); las plazas que quedan se sortean por ruleta sobre las partes
 * fraccionarias, de modo que el número esperado de copias de cada individuo se respeta.
 */
public class RemainderSelection extends SelectionStrategy {
    @Override
    public int[] select(Selectable[] list, int count, Random random) {
        int[] selected = new int[count];
        if (count == 0 || list.length == 0) return selected;

        // Recorre TODA la lista: con elitismo, count < list.length, y los peores individuos
        // también tienen un número esperado de copias (y una parte fraccionaria) propio.
        double total = 0;
        for (Selectable s : list) total += s.getProb();
        if (total <= 0) {
            // Sin señal de fitness: elegir uniformemente para no bloquear la selección.
            for (int i = 0; i < count; i++) {
                selected[i] = list[random.nextInt(list.length)].getIndex();
            }
            return selected;
        }

        int filled = 0;
        double[] fractions = new double[list.length];
        double fractionTotal = 0;
        for (int i = 0; i < list.length; i++) {
            double expected = list[i].getProb() / total * count;
            int copies = Math.min((int) Math.floor(expected), count - filled);
            for (int c = 0; c < copies; c++) {
                selected[filled++] = list[i].getIndex();
            }
            fractions[i] = expected - Math.floor(expected);
            fractionTotal += fractions[i];
        }

        // Plazas restantes: ruleta sobre las partes fraccionarias, compartiendo el generador
        // aleatorio para que el resultado siga siendo reproducible con una semilla fija.
        while (filled < count) {
            int chosen = list.length - 1;
            if (fractionTotal > 0) {
                double x = random.nextDouble() * fractionTotal;
                double acc = 0;
                for (int i = 0; i < list.length; i++) {
                    acc += fractions[i];
                    if (x < acc) {
                        chosen = i;
                        break;
                    }
                }
            } else {
                chosen = random.nextInt(list.length);
            }
            selected[filled++] = list[chosen].getIndex();
        }
        return selected;
    }
}
