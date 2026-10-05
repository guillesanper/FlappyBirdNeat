package com.neat.flappybirdneat.neat.selection;

/**
 * Selección por ruleta.
 * Cada individuo tiene una probabilidad de ser seleccionado proporcional a su fitness.
 */
public class SeleccionRuleta extends Seleccion {
    @Override
    public int[] getSeleccion(Seleccionable[] list, int tamPoblacion) {
        int[] seleccion = new int[tamPoblacion];
        // Suma total de probabilidades acumuladas (normalmente 1.0, salvo si el fitness total
        // de la población es 0, en cuyo caso todas las prob quedan a 0 y no hay señal que seguir).
        double total = list.length == 0 ? 0 : list[list.length - 1].getAccProb() + list[list.length - 1].getProb();

        for (int seleccionados = 0; seleccionados < tamPoblacion; seleccionados++) {
            if (total <= 0) {
                // Sin señal de fitness: elegir uniformemente para no bloquear la selección.
                seleccion[seleccionados] = list[this.rand.nextInt(list.length)].getIndex();
                continue;
            }

            // Recorre TODA la lista (no solo los primeros tamPoblacion elementos: cuando hay
            // elitismo, tamPoblacion < list.length y los individuos con mayor probabilidad
            // acumulada pueden quedar fuera del rango si no se comprueban todos).
            double x = this.rand.nextDouble() * total;
            int chosen = list.length - 1;
            for (int i = 0; i < list.length; i++) {
                if (x < list[i].getAccProb() + list[i].getProb()) {
                    chosen = i;
                    break;
                }
            }
            seleccion[seleccionados] = list[chosen].getIndex();
        }
        return seleccion;
    }
}
