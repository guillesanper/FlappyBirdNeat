package com.neat.flappybirdneat.neat.mutation;

/**
 * Implementación concreta de {@link MutacionFactory}. Solo accesible a través de {@link MutacionFactory#getInstance()}.
 */
class MutacionFactoryImp extends MutacionFactory {

    @Override
    public MutacionStrategy getMutacionStrategy(String tipo, double... params) {
        String t = tipo.toLowerCase();
        if (t.equals("gaussiana") || t.equals("gaussian")) {
            return params.length >= 1 ? new MutacionGaussiana(params[0]) : new MutacionGaussiana();
        } else if (t.equals("uniforme") || t.equals("uniform")) {
            return new MutacionUniforme();
        } else if (t.equals("no uniforme") || t.equals("no_uniforme") || t.equals("nonuniform")) {
            if (params.length >= 3) {
                return new MutacionNoUniforme(params[0], (int) params[1], params[2]);
            } else if (params.length >= 1) {
                return new MutacionNoUniforme((int) params[0]);
            }
            return new MutacionNoUniforme(1000);
        } else {
            throw new IllegalArgumentException("Tipo de mutación desconocido: " + tipo);
        }
    }
}
