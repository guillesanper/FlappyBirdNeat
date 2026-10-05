package com.neat.flappybirdneat.neat.crossover;

/**
 * Factory (singleton) para crear instancias de estrategias de cruce.
 * La instancia concreta ({@link CruceFactoryImp}) solo se obtiene a través de {@link #getInstance()}.
 */
public abstract class CruceFactory {

    private static final CruceFactory INSTANCE = new CruceFactoryImp();

    public static CruceFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Crea una estrategia de cruce del tipo indicado.
     * @param tipo Nombre del tipo de cruce (ej. "uniforme", "punto_unico", "aritmetico")
     * @param params Parámetros opcionales específicos de la estrategia (ej. alpha para aritmético)
     * @return La estrategia de cruce correspondiente
     */
    public abstract CruceStrategy getCruceStrategy(String tipo, double... params);
}
