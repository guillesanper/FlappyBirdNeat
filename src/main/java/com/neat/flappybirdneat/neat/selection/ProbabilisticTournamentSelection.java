package com.neat.flappybirdneat.neat.selection;

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
    public int[] select(Selectable[] list, int count) {
        int[] selected = new int[count];

        for (int i = 0; i < count; i++) {
            selected[i] = runTournament(list, count);
        }

        return selected;
    }

    private int runTournament(Selectable[] list, int count) {
        int ind1 = this.rand.nextInt(count);
        int ind2 = this.rand.nextInt(count);
        int ind3 = this.rand.nextInt(count);

        Selectable a = list[ind1];
        Selectable b = list[ind2];
        Selectable c = list[ind3];

        // El mejor individuo es seleccionado con probabilidad p
        if (this.rand.nextDouble() <= p) return bigger(a, b, c).getIndex();
        return smaller(a, b, c).getIndex();
    }
}
