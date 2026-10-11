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
        if (count == 0 || list.length == 0) return selected;

        // Suma de las probabilidades de TODA la lista: con elitismo, count < list.length, así que
        // los punteros se reparten sobre la población entera y no solo sobre los count primeros.
        Selectable last = list[list.length - 1];
        double total = last.getAccProb() + last.getProb();
        if (total <= 0) {
            // Sin señal de fitness: elegir uniformemente para no bloquear la selección.
            for (int i = 0; i < count; i++) {
                selected[i] = list[random.nextInt(list.length)].getIndex();
            }
            return selected;
        }

        // count punteros equidistantes, el primero en [0, step)
        double step = total / count;
        double start = random.nextDouble() * step;

        // El individuo j ocupa [accProb, accProb + prob): se elige aquel cuyo intervalo contiene el
        // puntero. Los punteros son crecientes, así que basta con un único recorrido de la lista.
        int j = 0;
        for (int i = 0; i < count; i++) {
            double pointer = start + i * step;
            while (j < list.length - 1 && pointer >= list[j].getAccProb() + list[j].getProb()) {
                j++;
            }
            selected[i] = list[j].getIndex();
        }
        return selected;
    }
}
