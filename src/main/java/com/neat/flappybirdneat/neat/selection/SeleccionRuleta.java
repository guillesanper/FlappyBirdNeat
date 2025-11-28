package com.neat.flappybirdneat.neat.selection;

/**
 * Selección por ruleta.
 * Cada individuo tiene una probabilidad de ser seleccionado proporcional a su fitness.
 */
public class SeleccionRuleta extends Seleccion {
    @Override
    public int[] getSeleccion(Seleccionable[] list, int tamPoblacion) {
        int[] seleccion = new int[tamPoblacion];

        int seleccionados = 0;
        double x;
        while (seleccionados < tamPoblacion) {
            x = this.rand.nextDouble();
            for(int i = 0; i < tamPoblacion; i++) {
                if(list[i].getAccProb() >= x) {
                    seleccion[seleccionados] = list[i].getIndex();
                    seleccionados++;
                    break;
                }
            }
        }
        return seleccion;
    }
}
