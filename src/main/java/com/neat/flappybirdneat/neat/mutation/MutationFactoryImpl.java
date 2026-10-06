package com.neat.flappybirdneat.neat.mutation;

/**
 * Implementación concreta de {@link MutationFactory}. Solo accesible a través de {@link MutationFactory#getInstance()}.
 */
class MutationFactoryImpl extends MutationFactory {

    @Override
    public MutationStrategy getMutationStrategy(String type, double... params) {
        String t = type.toLowerCase();
        if (t.equals("gaussiana") || t.equals("gaussian")) {
            return params.length >= 1 ? new GaussianMutation(params[0]) : new GaussianMutation();
        } else if (t.equals("uniforme") || t.equals("uniform")) {
            return new UniformMutation();
        } else if (t.equals("no uniforme")
                || t.equals("no_uniforme")
                || t.equals("non_uniform")
                || t.equals("nonuniform")) {
            if (params.length >= 3) {
                return new NonUniformMutation(params[0], (int) params[1], params[2]);
            } else if (params.length >= 1) {
                return new NonUniformMutation((int) params[0]);
            }
            return new NonUniformMutation(1000);
        } else {
            throw new IllegalArgumentException("Tipo de mutación desconocido: " + type);
        }
    }
}
