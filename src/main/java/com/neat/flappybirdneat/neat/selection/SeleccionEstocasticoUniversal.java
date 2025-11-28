package com.neat.flappybirdneat.neat.selection;

/**
 * Selección estocástica universal (SUS).
 * Mejora de la ruleta que reduce el sesgo usando múltiples punteros equidistantes.
 * Proporciona una selección más justa y con menor varianza.
 */
public class SeleccionEstocasticoUniversal extends Seleccion {
    @Override
    public int[] getSeleccion(Seleccionable[] list, int tamPoblacion) {
        int[] seleccion = new int[tamPoblacion];

        // Generar un valor aleatorio entre 0 y 1/tamPoblacion
        double r = this.rand.nextDouble() / tamPoblacion;

        // Para cada punto de selección
        for (int i = 0; i < tamPoblacion; i++) {
            // Calcular el punto de selección actual
            double punto = r + ((double) i / tamPoblacion);

            // Encontrar el individuo correspondiente
            int j = 0;
            while (j < tamPoblacion && punto > list[j].getAccProb()) {
                j++;
            }

            // Evitar índice fuera de rango
            if (j >= tamPoblacion) j = tamPoblacion - 1;

            seleccion[i] = list[j].getIndex();
        }

        return seleccion;
    }
}
