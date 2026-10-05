package com.neat.flappybirdneat.neat.scaling;

/**
 * Implementación concreta de {@link ScalingFactory}. Solo accesible a través de {@link ScalingFactory#getInstance()}.
 */
class ScalingFactoryImpl extends ScalingFactory {

    @Override
    public ScalingStrategy getScalingStrategy(String type, double... params) {
        String t = type.toLowerCase();
        if (t.equals("lineal") || t.equals("linear")) {
            return params.length >= 2 ? new LinearScaling(params[0], params[1]) : new LinearScaling();
        } else if (t.equals("sigma")) {
            return new SigmaScaling();
        } else if (t.equals("boltzmann")) {
            if (params.length >= 2) {
                return new BoltzmannScaling(params[0], params[1]);
            } else if (params.length >= 1) {
                return new BoltzmannScaling(params[0]);
            }
            return new BoltzmannScaling(100.0);
        } else if (t.equals("ninguno") || t.equals("none")) {
            return null;
        } else {
            throw new IllegalArgumentException("Tipo de escalado desconocido: " + type);
        }
    }
}
