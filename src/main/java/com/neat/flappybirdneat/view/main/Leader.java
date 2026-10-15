package com.neat.flappybirdneat.view.main;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;

/**
 * El agente que la vista destaca y cuya red muestra la ventana de la red neuronal: el vivo con más
 * fitness (el primero del array en caso de empate). No sirve {@code population.getBestAgent()}: en
 * el GA y en NEAT es una copia del mejor histórico que no juega, así que en el entrenamiento en vivo
 * no se destacaba a nadie y la red mostraba valores congelados.
 */
final class Leader {

    private Leader() {}

    /** @return el líder, o null si no queda ninguno vivo */
    static FlappyBirdAgent of(FlappyBirdAgent[] agents) {
        FlappyBirdAgent leader = null;
        for (FlappyBirdAgent agent : agents) {
            if (!agent.isDead() && (leader == null || agent.getFitness() > leader.getFitness())) {
                leader = agent;
            }
        }
        return leader;
    }
}
