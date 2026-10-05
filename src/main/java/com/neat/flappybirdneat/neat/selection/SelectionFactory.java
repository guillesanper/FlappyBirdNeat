package com.neat.flappybirdneat.neat.selection;

import java.util.Random;

/**
 * Factory (singleton) para crear instancias de estrategias de selección.
 * La instancia concreta ({@link SelectionFactoryImpl}) solo se obtiene a través de {@link #getInstance()}.
 */
public abstract class SelectionFactory {

    private static final SelectionFactory INSTANCE = new SelectionFactoryImpl();

    public static SelectionFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Crea una estrategia de selección del tipo indicado.
     * @param type Nombre del tipo de selección (ej. "roulette", "torneo_deterministico", "ranking"...)
     * @param params Parámetros opcionales específicos de la estrategia (ej. beta para ranking)
     * @return La estrategia de selección correspondiente
     */
    public abstract SelectionStrategy getSelectionStrategy(String type, double... params);
}
