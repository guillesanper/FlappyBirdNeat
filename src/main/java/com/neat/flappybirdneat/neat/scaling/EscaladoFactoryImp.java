package com.neat.flappybirdneat.neat.scaling;

/**
 * Implementación concreta de {@link EscaladoFactory}. Solo accesible a través de {@link EscaladoFactory#getInstance()}.
 */
class EscaladoFactoryImp extends EscaladoFactory {

    @Override
    public Escalado getEscaladoStrategy(String tipo, double... params) {
        String t = tipo.toLowerCase();
        if (t.equals("lineal")) {
            return params.length >= 2 ? new EscaladoLineal(params[0], params[1]) : new EscaladoLineal();
        } else if (t.equals("sigma")) {
            return new EscaladoSigma();
        } else if (t.equals("boltzmann")) {
            if (params.length >= 2) {
                return new EscaladoBoltzmann(params[0], params[1]);
            } else if (params.length >= 1) {
                return new EscaladoBoltzmann(params[0]);
            }
            return new EscaladoBoltzmann(100.0);
        } else if (t.equals("ninguno") || t.equals("none")) {
            return null;
        } else {
            throw new IllegalArgumentException("Tipo de escalado desconocido: " + tipo);
        }
    }
}
