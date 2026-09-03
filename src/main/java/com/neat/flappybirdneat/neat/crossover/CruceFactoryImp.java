package com.neat.flappybirdneat.neat.crossover;

/**
 * Implementación concreta de {@link CruceFactory}. Solo accesible a través de {@link CruceFactory#getInstance()}.
 */
class CruceFactoryImp extends CruceFactory {

    @Override
    public CruceStrategy getCruceStrategy(String tipo, double... params) {
        String t = tipo.toLowerCase();
        if (t.equals("uniforme") || t.equals("uniform")) {
            return new CruceUniforme();
        } else if (t.equals("punto unico") || t.equals("punto_unico") || t.equals("single_point")) {
            return new CrucePuntoUnico();
        } else if (t.equals("aritmetico") || t.equals("arithmetic")) {
            return params.length >= 1 ? new CruceAritmetico(params[0]) : new CruceAritmetico();
        } else {
            throw new IllegalArgumentException("Tipo de cruce desconocido: " + tipo);
        }
    }
}
