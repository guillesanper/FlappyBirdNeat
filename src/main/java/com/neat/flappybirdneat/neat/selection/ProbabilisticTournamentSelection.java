package com.neat.flappybirdneat.neat.selection;

import java.util.Random;

/**
 * Selección por torneo probabilístico.
 * Selecciona 3 individuos al azar, luego elige el mejor con probabilidad p
 * y el peor con probabilidad (1-p).
 */
public class ProbabilisticTournamentSelection extends SelectionStrategy {

    private final double p;

    public ProbabilisticTournamentSelection() {
        this.p = 0.6;
    }

    public ProbabilisticTournamentSelection(double p) {
        this.p = p;
    }

    private Selectable bigger(Selectable a, Selectable b, Selectable c) {
        if (a.compareTo(b) > 0 && a.compareTo(c) > 0) return a;
        if (b.compareTo(c) > 0) return b;
        return c;
    }

    private Selectable smaller(Selectable a, Selectable b, Selectable c) {
        if (a.compareTo(b) < 0 && a.compareTo(c) < 0) return a;
        if (b.compareTo(c) < 0) return b;
        return c;
    }

    @Override
    public int[] select(Selectable[] list, int count, Random random) {
        int[] selected = new int[count];

        for (int i = 0; i < count; i++) {
            selected[i] = runTournament(list, random);
        }

        return selected;
    }

    private int runTournament(Selectable[] list, Random random) {
        // Participantes de toda la población (con elitismo, count < list.length)
        int ind1 = random.nextInt(list.length);
        int ind2 = random.nextInt(list.length);
        int ind3 = random.nextInt(list.length);

        Selectable a = list[ind1];
        Selectable b = list[ind2];
        Selectable c = list[ind3];

        // El mejor individuo es seleccionado con probabilidad p
        if (random.nextDouble() <= p) return bigger(a, b, c).getIndex();
        return smaller(a, b, c).getIndex();
    }
}
