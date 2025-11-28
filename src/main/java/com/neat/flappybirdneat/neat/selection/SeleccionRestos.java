package com.neat.flappybirdneat.neat.selection;

/**
 * Selección por restos.
 * Asigna copias de individuos según su fitness esperado.
 * Los restos se completan con torneo determinista.
 */
public class SeleccionRestos extends Seleccion {
    @Override
    public int[] getSeleccion(Seleccionable[] list, int tamPoblacion) {
        int[] seleccion = new int[tamPoblacion];

        int metidos = 0;

        // Asignar copias según fitness esperado
        for (int i = 0; i < tamPoblacion; i++) {
            if (metidos == tamPoblacion) break;
            long apariciones = Math.round(list[i].getProb() * tamPoblacion);

            if(apariciones < tamPoblacion - metidos) {
                for (int j = 0; j < apariciones; j++) {
                    seleccion[metidos++] = list[i].getIndex();
                }
            }
        }

        // Completar con torneo determinista si quedan espacios
        if (metidos != tamPoblacion) {
            int[] nuevaSeleccion = new SeleccionTorneoDeterministico()
                    .getSeleccion(list, tamPoblacion - metidos);

            if (tamPoblacion - metidos >= 0)
                System.arraycopy(nuevaSeleccion, 0, seleccion, metidos, tamPoblacion - metidos);
        }

        return seleccion;
    }
}
