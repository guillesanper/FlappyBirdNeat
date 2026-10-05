package com.neat.flappybirdneat.neat.crossover;

/**
 * Implementación concreta de {@link CrossoverFactory}. Solo accesible a través de {@link CrossoverFactory#getInstance()}.
 */
class CrossoverFactoryImpl extends CrossoverFactory {

    @Override
    public CrossoverStrategy getCrossoverStrategy(String type, double... params) {
        String t = type.toLowerCase();
        if (t.equals("uniforme") || t.equals("uniform")) {
            return new UniformCrossover();
        } else if (t.equals("punto unico") || t.equals("punto_unico") || t.equals("single_point")) {
            return new SinglePointCrossover();
        } else if (t.equals("aritmetico") || t.equals("arithmetic")) {
            return params.length >= 1 ? new ArithmeticCrossover(params[0]) : new ArithmeticCrossover();
        } else {
            throw new IllegalArgumentException("Tipo de cruce desconocido: " + type);
        }
    }
}
