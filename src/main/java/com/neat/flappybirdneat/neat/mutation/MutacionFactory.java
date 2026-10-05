package com.neat.flappybirdneat.neat.mutation;

/**
 * Factory (singleton) para crear instancias de estrategias de mutación.
 * La instancia concreta ({@link MutacionFactoryImp}) solo se obtiene a través de {@link #getInstance()}.
 */
public abstract class MutacionFactory {

    private static final MutacionFactory INSTANCE = new MutacionFactoryImp();

    public static MutacionFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Crea una estrategia de mutación del tipo indicado.
     * @param tipo Nombre del tipo de mutación (ej. "gaussiana", "uniforme", "no_uniforme")
     * @param params Parámetros opcionales específicos de la estrategia
     * @return La estrategia de mutación correspondiente
     */
    public abstract MutacionStrategy getMutacionStrategy(String tipo, double... params);
}
