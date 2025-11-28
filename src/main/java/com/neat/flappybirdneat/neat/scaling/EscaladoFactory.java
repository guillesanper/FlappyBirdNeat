package com.neat.flappybirdneat.neat.scaling;

public class EscaladoFactory {

    public static Escalado getMetodoEscalado(String tipo) {
        String t = tipo.toLowerCase();
        if (t.equals("lineal")) {
            return new EscaladoLineal();
        } else if (t.equals("sigma")) {
            return new EscaladoSigma();
        } else if (t.equals("boltzmann")) {
            return new EscaladoBoltzmann(100.0);
        } else if (t.equals("ninguno") || t.equals("none")) {
            return null;
        } else {
            throw new IllegalArgumentException("Tipo de escalado desconocido: " + tipo);
        }
    }

    public static Escalado getMetodoEscalado(String tipo, double... params) {
        String t = tipo.toLowerCase();
        if (t.equals("lineal")) {
            if (params.length >= 2) {
                return new EscaladoLineal(params[0], params[1]);
            } else {
                return new EscaladoLineal();
            }
        } else if (t.equals("boltzmann")) {
            if (params.length >= 2) {
                return new EscaladoBoltzmann(params[0], params[1]);
            } else if (params.length >= 1) {
                return new EscaladoBoltzmann(params[0]);
            } else {
                return new EscaladoBoltzmann(100.0);
            }
        } else {
            return getMetodoEscalado(tipo);
        }
    }
}
