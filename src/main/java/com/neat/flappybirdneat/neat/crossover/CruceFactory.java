package com.neat.flappybirdneat.neat.crossover;

/**
 * Factory para crear instancias de estrategias de cruce.
 */
public class CruceFactory {

    public static CruceStrategy getMetodoCruce(String tipo) {
        String t = tipo.toLowerCase();
        if (t.equals("uniforme") || t.equals("uniform")) {
            return new CruceUniforme();
        } else if (t.equals("punto unico") || t.equals("punto_unico") || t.equals("single_point")) {
            return new CrucePuntoUnico();
        } else if (t.equals("aritmetico") || t.equals("arithmetic")) {
            return new CruceAritmetico();
        } else {
            throw new IllegalArgumentException("Tipo de cruce desconocido: " + tipo);
        }
    }

    public static CruceStrategy getMetodoCruce(String tipo, double... params) {
        String t = tipo.toLowerCase();
        if (t.equals("aritmetico") || t.equals("arithmetic")) {
            if (params.length >= 1) {
                return new CruceAritmetico(params[0]);
            } else {
                return new CruceAritmetico();
            }
        } else {
            return getMetodoCruce(tipo);
        }
    }
}
