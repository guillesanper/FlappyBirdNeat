package com.neat.flappybirdneat.neat.scaling;

/**
 * Factory (singleton) para crear instancias de estrategias de escalado.
 * La instancia concreta ({@link EscaladoFactoryImp}) solo se obtiene a través de {@link #getInstance()}.
 */
public abstract class EscaladoFactory {

    private static final EscaladoFactory INSTANCE = new EscaladoFactoryImp();

    public static EscaladoFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Crea una estrategia de escalado del tipo indicado.
     * @param tipo Nombre del tipo de escalado (ej. "lineal", "sigma", "boltzmann", "ninguno")
     * @param params Parámetros opcionales específicos de la estrategia
     * @return La estrategia de escalado correspondiente, o {@code null} para "ninguno"/"none"
     */
    public abstract Escalado getEscaladoStrategy(String tipo, double... params);
}
