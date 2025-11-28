package com.neat.flappybirdneat.neat.selection;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Selección por truncamiento.
 * Solo los mejores individuos (top X%) pueden reproducirse.
 * Los seleccionados se repiten proporcionalmente para llenar la población.
 */
public class SeleccionTruncamiento extends Seleccion {

    private final double trunc;

    public SeleccionTruncamiento() {
        this.trunc = 0.6;
    }

    public SeleccionTruncamiento(double trunc) {
        this.trunc = trunc;
    }

    @Override
    public int[] getSeleccion(Seleccionable[] list, int tamPoblacion) {
        int[] seleccion = new int[tamPoblacion];

        // Ordenar individuos por fitness descendente
        Arrays.sort(list, Comparator.comparingDouble(Seleccionable::getFitness).reversed());

        // Determinar número de individuos seleccionables
        int numSeleccionables = (int) (list.length * this.trunc);
        numSeleccionables = Math.max(numSeleccionables, 1);

        // Calcular repeticiones
        int repeticiones = tamPoblacion / numSeleccionables;
        int resto = tamPoblacion % numSeleccionables;

        int index = 0;
        for (int i = 0; i < numSeleccionables; i++) {
            for (int j = 0; j < repeticiones; j++) {
                seleccion[index++] = list[i].getIndex();
            }
        }

        // Distribuir el resto
        for (int i = 0; i < resto; i++) {
            seleccion[index++] = list[i].getIndex();
        }

        return seleccion;
    }
}
