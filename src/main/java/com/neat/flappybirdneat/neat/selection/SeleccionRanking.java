package com.neat.flappybirdneat.neat.selection;

/**
 * Selección por ranking.
 * Asigna probabilidades basadas en el ranking (posición) de los individuos,
 * no en su fitness absoluto. Luego utiliza ruleta para seleccionar.
 */
public class SeleccionRanking extends Seleccion {

    private final double beta;

    public SeleccionRanking() {
        this.beta = 1.5;
    }

    public SeleccionRanking(double beta) {
        this.beta = beta;
    }

    private void calculateProbs(Seleccionable[] list, int tamPoblacion) {
        double accProb = 0.0;
        for (int i = 0; i < tamPoblacion; ++i) {
            double probOfIth = (double) i / tamPoblacion;
            probOfIth *= 2 * (beta - 1);
            probOfIth = beta - probOfIth;
            probOfIth = probOfIth * ((double) 1 / tamPoblacion);

            list[i].setAccProb(accProb);
            list[i].setProb(probOfIth);
            accProb += probOfIth;
        }
    }

    @Override
    public int[] getSeleccion(Seleccionable[] list, int tamPoblacion) {
        this.calculateProbs(list, tamPoblacion);

        // Usar ruleta después de calcular probabilidades por ranking
        return new SeleccionRuleta().getSeleccion(list, tamPoblacion);
    }
}
