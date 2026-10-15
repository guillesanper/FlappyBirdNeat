package com.neat.flappybirdneat.view.main;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.neat.Population;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class LeaderTest {

    private static FlappyBirdAgent agent(double fitness, boolean dead) {
        FlappyBirdAgent agent = new FlappyBirdAgent(4, 8, 1, new Random(1));
        agent.setFitness(fitness);
        agent.setDead(dead);
        return agent;
    }

    @Test
    void leaderIsTheLiveAgentWithMostFitnessAndTheFirstOnTies() {
        FlappyBirdAgent dead = agent(500, true);
        FlappyBirdAgent first = agent(100, false);
        FlappyBirdAgent tied = agent(100, false);

        assertSame(first, Leader.of(new FlappyBirdAgent[] {dead, first, tied}));
        assertNull(Leader.of(new FlappyBirdAgent[] {dead}));
    }

    /**
     * Regresión: la vista destacaba population.getBestAgent(), que en el GA es una copia del mejor
     * histórico que no está entre los agentes que juegan, así que nunca se destacaba a nadie.
     */
    @Test
    void leaderIsOneOfThePlayingAgentsWhereasTheRecordedBestIsNot() {
        Population population = new Population(10, new Random(3));
        for (FlappyBirdAgent agent : population.getAgents()) agent.setFitness(10);
        population.naturalSelection();

        FlappyBirdAgent[] playing = population.getAgents();
        assertFalse(Arrays.asList(playing).contains(population.getBestAgent()));
        assertTrue(Arrays.asList(playing).contains(Leader.of(playing)));
    }
}
