package com.neat.flappybirdneat.neat.selection;

/**
 * Factory (singleton) para crear instancias de estrategias de selección.
 * La instancia concreta ({@link SeleccionFactoryImp}) solo se obtiene a través de {@link #getInstance()}.
 */
public abstract class SeleccionFactory {

    private static final SeleccionFactory INSTANCE = new SeleccionFactoryImp();

    public static SeleccionFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Crea una estrategia de selección del tipo indicado.
     * @param tipo Nombre del tipo de selección (ej. "ruleta", "torneo_deterministico", "ranking"...)
     * @param params Parámetros opcionales específicos de la estrategia (ej. beta para ranking)
     * @return La estrategia de selección correspondiente
     */
    public abstract Seleccion getSeleccionStrategy(String tipo, double... params);
}
