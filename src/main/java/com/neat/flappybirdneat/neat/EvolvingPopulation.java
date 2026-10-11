package com.neat.flappybirdneat.neat;

import java.util.Random;

/**
 * Abstracción común de "población evolutiva" implementada tanto por {@link Population}
 * (GA de pesos sobre topología fija) como por {@link com.neat.flappybirdneat.neat.genome.NeatPopulation}
 * (NEAT real, con topología evolutiva). Permite que {@code SimulationController} y el resto del
 * motor (historial, UI de juego) operen igual sin importar qué modo esté activo.
 */
public interface EvolvingPopulation {
    FlappyBirdAgent[] getAgents();

    /**
     * Best agent of the generations already evolved: it is updated by {@link #naturalSelection},
     * so it does not include the generation being played. For that, see {@link #fittestAgent()}.
     */
    FlappyBirdAgent getBestAgent();

    /** @return the agent with the highest fitness in the current generation (the first one on ties) */
    default FlappyBirdAgent fittestAgent() {
        FlappyBirdAgent fittest = null;
        for (FlappyBirdAgent agent : getAgents()) {
            if (fittest == null || agent.getFitness() > fittest.getFitness()) fittest = agent;
        }
        return fittest;
    }

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

    /**
     * Copia profunda, usada para guardar snapshots de generaciones en el historial. Comparte el
     * generador aleatorio (y en NEAT el registro de innovaciones) con el original, así que no debe
     * evolucionarse: para eso está {@link #deepCopy(Random)}.
     */
    EvolvingPopulation deepCopy();

    /**
     * Copia profunda independiente que evoluciona con su propio generador (y, en NEAT, con su
     * propio registro de innovaciones), de modo que evolucionarla no altera la ejecución original.
     * La usan las repeticiones visuales de generaciones guardadas.
     */
    EvolvingPopulation deepCopy(Random random);
}
