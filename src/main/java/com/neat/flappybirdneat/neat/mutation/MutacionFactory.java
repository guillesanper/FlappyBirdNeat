package com.neat.flappybirdneat.neat.mutation;

public class MutacionFactory {

    public static MutacionStrategy getMetodoMutacion(String tipo) {
        String t = tipo.toLowerCase();
        if (t.equals("gaussiana") || t.equals("gaussian")) {
            return new MutacionGaussiana();
        } else if (t.equals("uniforme") || t.equals("uniform")) {
            return new MutacionUniforme();
        } else if (t.equals("no uniforme") || t.equals("no_uniforme") || t.equals("nonuniform")) {
            return new MutacionNoUniforme(1000);
        } else {
            throw new IllegalArgumentException("Tipo de mutación desconocido: " + tipo);
        }
    }

    public static MutacionStrategy getMetodoMutacion(String tipo, double... params) {
        String t = tipo.toLowerCase();
        if (t.equals("gaussiana") || t.equals("gaussian")) {
            if (params.length >= 1) {
                return new MutacionGaussiana(params[0]);
            } else {
                return new MutacionGaussiana();
            }
        } else if (t.equals("no uniforme") || t.equals("no_uniforme") || t.equals("nonuniform")) {
            if (params.length >= 3) {
                return new MutacionNoUniforme(params[0], (int)params[1], params[2]);
            } else if (params.length >= 1) {
                return new MutacionNoUniforme((int)params[0]);
            } else {
                return new MutacionNoUniforme(1000);
            }
        } else {
            return getMetodoMutacion(tipo);
        }
    }
}
