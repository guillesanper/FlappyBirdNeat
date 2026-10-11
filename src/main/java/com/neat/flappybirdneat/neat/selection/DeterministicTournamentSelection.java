package com.neat.flappybirdneat.neat.selection;

import java.util.Random;

/**
 * Selección por torneo determinista.
 * Selecciona 3 individuos al azar y elige el mejor.
 */
public class DeterministicTournamentSelection extends SelectionStrategy {

    private Selectable bigger(Selectable a, Selectable b, Selectable c) {
        if (a.compareTo(b) > 0 && a.compareTo(c) > 0) return a;
        if (b.compareTo(c) > 0) return b;
        return c;
    }

    @Override
    public int[] select(Selectable[] list, int count, Random random) {
        int[] selected = new int[count];

        for (int i = 0; i < count; i++) {
            // 3 individuos aleatorios de toda la población: count es el nº de padres a elegir y,
            // con elitismo, es menor que list.length (sortear en [0, count) excluiría a los peores)
            int ind1 = random.nextInt(list.length);
            int ind2 = random.nextInt(list.length);
            int ind3 = random.nextInt(list.length);

            Selectable a = list[ind1];
            Selectable b = list[ind2];
            Selectable c = list[ind3];
            selected[i] = bigger(a, b, c).getIndex();
        }

        return selected;
    }
}
