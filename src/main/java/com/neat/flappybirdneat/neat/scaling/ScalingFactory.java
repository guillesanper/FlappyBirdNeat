package com.neat.flappybirdneat.neat.scaling;

/**
 * Factory (singleton) para crear instancias de estrategias de escalado.
 * La instancia concreta ({@link ScalingFactoryImpl}) solo se obtiene a través de {@link #getInstance()}.
 */
public abstract class ScalingFactory {

    private static final ScalingFactory INSTANCE = new ScalingFactoryImpl();

    public static ScalingFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Crea una estrategia de escalado del tipo indicado.
     * @param type Nombre del tipo de escalado (ej. "lineal", "sigma", "boltzmann", "ninguno")
     * @param params Parámetros opcionales específicos de la estrategia
     * @return La estrategia de escalado correspondiente, o {@code null} para "ninguno"/"none"
     */
    public abstract ScalingStrategy getScalingStrategy(String type, double... params);
}
