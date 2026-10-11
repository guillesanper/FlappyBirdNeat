package com.neat.flappybirdneat.neat.selection;

import java.util.Random;

/**
 * Selección por ranking.
 * Asigna probabilidades basadas en el ranking (posición) de los individuos,
 * no en su fitness absoluto. Luego utiliza roulette para seleccionar.
 */
public class RankingSelection extends SelectionStrategy {

    private final double beta;

    public RankingSelection() {
        this.beta = 1.5;
    }

    public RankingSelection(double beta) {
        this.beta = beta;
    }

    /**
     * Probabilidad por rango de toda la población (la lista va de mejor a peor), no solo de los
     * count padres a elegir: con elitismo, {@code count < list.length}.
     */
    private void calculateProbs(Selectable[] list) {
        int n = list.length;
        double accProb = 0.0;
        for (int i = 0; i < n; ++i) {
            double probOfIth = (double) i / n;
            probOfIth *= 2 * (beta - 1);
            probOfIth = beta - probOfIth;
            probOfIth = probOfIth * ((double) 1 / n);

            list[i].setAccProb(accProb);
            list[i].setProb(probOfIth);
            accProb += probOfIth;
        }
    }

    @Override
    public int[] select(Selectable[] list, int count, Random random) {
        this.calculateProbs(list);

        // Usar roulette después de calcular probabilidades por ranking, compartiendo el generador
        // aleatorio para que el resultado siga siendo reproducible con una semilla fija.
        RouletteSelection roulette = new RouletteSelection();
        return roulette.select(list, count, random);
    }
}
