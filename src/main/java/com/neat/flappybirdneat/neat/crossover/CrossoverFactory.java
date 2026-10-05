package com.neat.flappybirdneat.neat.crossover;

/**
 * Factory (singleton) para crear instancias de estrategias de cruce.
 * La instancia concreta ({@link CrossoverFactoryImpl}) solo se obtiene a través de {@link #getInstance()}.
 */
public abstract class CrossoverFactory {

    private static final CrossoverFactory INSTANCE = new CrossoverFactoryImpl();

    public static CrossoverFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Crea una estrategia de cruce del tipo indicado.
     * @param type Nombre del tipo de cruce (ej. "uniforme", "punto_unico", "aritmetico")
     * @param params Parámetros opcionales específicos de la estrategia (ej. alpha para aritmético)
     * @return La estrategia de cruce correspondiente
     */
    public abstract CrossoverStrategy getCrossoverStrategy(String type, double... params);
}
