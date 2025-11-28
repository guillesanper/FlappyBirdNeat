package com.neat.flappybirdneat.neat.selection;

/**
 * Selección por torneo determinista.
 * Selecciona 3 individuos al azar y elige el mejor.
 */
public class SeleccionTorneoDeterministico extends Seleccion {

    private Seleccionable bigger(Seleccionable a, Seleccionable b, Seleccionable c) {
        if (a.compareTo(b) > 0 && a.compareTo(c) > 0) return a;
        if (b.compareTo(c) > 0) return b;
        return c;
    }

    @Override
    public int[] getSeleccion(Seleccionable[] list, int tamPoblacion) {
        int[] seleccion = new int[tamPoblacion];

        for (int i = 0; i < tamPoblacion; i++) {
            // 3 individuos aleatorios
            int ind1 = this.rand.nextInt(tamPoblacion);
            int ind2 = this.rand.nextInt(tamPoblacion);
            int ind3 = rand.nextInt(tamPoblacion);

            Seleccionable a = list[ind1];
            Seleccionable b = list[ind2];
            Seleccionable c = list[ind3];
            seleccion[i] = bigger(a, b, c).getIndex();
        }

        return seleccion;
    }
}
