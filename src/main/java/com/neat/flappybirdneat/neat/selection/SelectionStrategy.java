package com.neat.flappybirdneat.neat.selection;

import java.util.Random;

/**
 * Clase base abstracta para todos los métodos de selección.
 */
public abstract class SelectionStrategy {

    /**
     * Realiza la selección de individuos.
     * @param list Array de individuos seleccionables con sus probabilidades calculadas
     * @param count Tamaño de la población
     * @param random Generador de la simulación (se pasa en cada llamada para que la estrategia no
     *               guarde estado aleatorio y pueda compartirse entre poblaciones sin acoplarlas)
     * @return Array de índices de los individuos seleccionados
     */
    public abstract int[] select(Selectable[] list, int count, Random random);
}
