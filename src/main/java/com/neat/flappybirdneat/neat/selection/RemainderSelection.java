package com.neat.flappybirdneat.neat.selection;

import java.util.Random;

/**
 * Selección por restos.
 * Asigna copias de individuos según su fitness esperado.
 * Los restos se completan con torneo determinista.
 */
public class RemainderSelection extends SelectionStrategy {
    @Override
    public int[] select(Selectable[] list, int count, Random random) {
        int[] selected = new int[count];

        int filled = 0;

        // Asignar copias según fitness esperado
        for (int i = 0; i < count; i++) {
            if (filled == count) break;
            long copies = Math.round(list[i].getProb() * count);

            if(copies < count - filled) {
                for (int j = 0; j < copies; j++) {
                    selected[filled++] = list[i].getIndex();
                }
            }
        }

        // Completar con torneo determinista si quedan espacios, compartiendo el generador
        // aleatorio para que el resultado siga siendo reproducible con una semilla fija.
        if (filled != count) {
            DeterministicTournamentSelection tournament = new DeterministicTournamentSelection();
            int[] newSelection = tournament.select(list, count - filled, random);

            if (count - filled >= 0)
                System.arraycopy(newSelection, 0, selected, filled, count - filled);
        }

        return selected;
    }
}
