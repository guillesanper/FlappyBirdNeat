package com.neat.flappybirdneat.neat;

/**
 * Abstracción común de "población evolutiva" implementada tanto por {@link Population}
 * (GA de pesos sobre topología fija) como por {@link com.neat.flappybirdneat.neat.genome.NeatPopulation}
 * (NEAT real, con topología evolutiva). Permite que {@code SimulationController} y el resto del
 * motor (historial, UI de juego) operen igual sin importar qué modo esté activo.
 */
public interface EvolvingPopulation {
    FlappyBirdAgent[] getAgents();

    FlappyBirdAgent getBestAgent();

    int getGeneration();

    double getBestFitness();

    /**
     * Diversidad genética de la población actual: distancia media por pareja entre genomas
     * (compatibilidad NEAT en {@code NeatPopulation}, distancia euclídea de pesos en {@code Population}).
     * Sirve como métrica de "cuánto se parecen entre sí" los individuos de la generación.
     */
    double diversity();

    /** Evoluciona a la siguiente generación (in-place: sustituye los agentes actuales por la descendencia). */
    void naturalSelection();

    /** Copia profunda, usada para guardar snapshots de generaciones en el historial. */
    EvolvingPopulation deepCopy();
}
