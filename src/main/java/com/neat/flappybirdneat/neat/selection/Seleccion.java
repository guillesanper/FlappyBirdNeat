package com.neat.flappybirdneat.neat.selection;

import java.util.Random;

/**
 * Clase base abstracta para todos los métodos de selección.
 */
public abstract class Seleccion {
    protected Random rand;

    public Seleccion() {
        this.rand = new Random();
    }

    /**
     * Realiza la selección de individuos.
     * @param list Array de individuos seleccionables con sus probabilidades calculadas
     * @param tamPoblacion Tamaño de la población
     * @return Array de índices de los individuos seleccionados
     */
    public abstract int[] getSeleccion(Seleccionable[] list, int tamPoblacion);
}
