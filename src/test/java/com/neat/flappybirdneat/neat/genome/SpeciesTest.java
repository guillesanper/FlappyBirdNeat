package com.neat.flappybirdneat.neat.genome;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SpeciesTest {

    private FlappyBirdAgent agentWithFitness(double fitness) {
        Genome genome = new Genome(2, 1, new Random(1), new InnovationTracker());
        FlappyBirdAgent agent = new FlappyBirdAgent(genome);
        agent.setFitness(fitness);
        return agent;
    }

    @Test
    void totalAdjustedFitnessDividesEachMemberFitnessBySpeciesSize() {
        Species species = new Species(new Genome(2, 1, new Random(1), new InnovationTracker()));
        species.addMember(agentWithFitness(10));
        species.addMember(agentWithFitness(20));

        // Fitness sharing: (10/2) + (20/2) = 15, no 30.
        assertEquals(15.0, species.totalAdjustedFitness(), 1e-9);
    }

    @Test
    void emptySpeciesHasZeroAdjustedFitness() {
        Species species = new Species(new Genome(2, 1, new Random(1), new InnovationTracker()));

        assertEquals(0.0, species.totalAdjustedFitness(), 1e-9);
    }

    @Test
    void championIsTheMemberWithHighestFitness() {
        Species species = new Species(new Genome(2, 1, new Random(1), new InnovationTracker()));
        FlappyBirdAgent low = agentWithFitness(5);
        FlappyBirdAgent high = agentWithFitness(50);
        FlappyBirdAgent mid = agentWithFitness(25);
        species.addMember(low);
        species.addMember(high);
        species.addMember(mid);

        assertSame(high, species.champion());
    }

    @Test
    void survivorsKeepsOnlyTopFractionRoundedUp() {
        Species species = new Species(new Genome(2, 1, new Random(1), new InnovationTracker()));
        for (double fitness : new double[]{10, 40, 20, 30, 5}) {
            species.addMember(agentWithFitness(fitness));
        }

        // 5 miembros * 0.4 = 2 supervivientes: los de fitness 40 y 30.
        List<FlappyBirdAgent> survivors = species.survivors(0.4);

        assertEquals(2, survivors.size());
        assertEquals(40.0, survivors.get(0).getFitness(), 1e-9);
        assertEquals(30.0, survivors.get(1).getFitness(), 1e-9);
    }

    @Test
    void survivorsAlwaysKeepsAtLeastOneMember() {
        Species species = new Species(new Genome(2, 1, new Random(1), new InnovationTracker()));
        species.addMember(agentWithFitness(1));
        species.addMember(agentWithFitness(2));
        species.addMember(agentWithFitness(3));

        List<FlappyBirdAgent> survivors = species.survivors(0.01);

        assertEquals(1, survivors.size());
        assertEquals(3.0, survivors.get(0).getFitness(), 1e-9);
    }
}
