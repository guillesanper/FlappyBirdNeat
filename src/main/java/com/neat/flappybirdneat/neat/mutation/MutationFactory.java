package com.neat.flappybirdneat.neat.mutation;

/**
 * Factory (singleton) para crear instancias de estrategias de mutación.
 * La instancia concreta ({@link MutationFactoryImpl}) solo se obtiene a través de {@link #getInstance()}.
 */
public abstract class MutationFactory {

    private static final MutationFactory INSTANCE = new MutationFactoryImpl();

    public static MutationFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Crea una estrategia de mutación del tipo indicado.
     * @param type Nombre del tipo de mutación (ej. "gaussiana", "uniforme", "no_uniforme")
     * @param params Parámetros opcionales específicos de la estrategia
     * @return La estrategia de mutación correspondiente
     */
    public abstract MutationStrategy getMutationStrategy(String type, double... params);
}
