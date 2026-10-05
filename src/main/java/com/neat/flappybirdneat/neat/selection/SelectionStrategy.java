package com.neat.flappybirdneat.neat.selection;

import java.util.Random;

/**
 * Clase base abstracta para todos los métodos de selección.
 */
public abstract class SelectionStrategy {
    protected Random rand;

    public SelectionStrategy() {
        this.rand = new Random();
    }

    /**
     * Sustituye el generador aleatorio, para reproducibilidad (tests, semillas fijas).
     * @param rand Generador aleatorio a usar
     */
    public void setRandom(Random rand) {
        this.rand = rand;
    }

    /**
     * Realiza la selección de individuos.
     * @param list Array de individuos seleccionables con sus probabilidades calculadas
     * @param count Tamaño de la población
     * @return Array de índices de los individuos seleccionados
     */
    public abstract int[] select(Selectable[] list, int count);
}
