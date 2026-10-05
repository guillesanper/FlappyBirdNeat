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
            // 3 individuos aleatorios
            int ind1 = random.nextInt(count);
            int ind2 = random.nextInt(count);
            int ind3 = random.nextInt(count);

            Selectable a = list[ind1];
            Selectable b = list[ind2];
            Selectable c = list[ind3];
            selected[i] = bigger(a, b, c).getIndex();
        }

        return selected;
    }
}
