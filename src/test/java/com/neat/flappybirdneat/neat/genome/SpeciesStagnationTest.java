package com.neat.flappybirdneat.neat.genome;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Species stagnation (NEAT "dropoff age") and the species elitism that protects the best ones. */
class SpeciesStagnationTest {

    private static final InnovationTracker TRACKER = new InnovationTracker();

    private static Species speciesWithChampion(double fitness) {
        Genome genome = new Genome(4, 1, new Random(1), TRACKER);
        Species species = new Species(genome);
        FlappyBirdAgent agent = new FlappyBirdAgent(genome);
        agent.setFitness(fitness);
        species.addMember(agent);
        return species;
    }

    /** Runs {@code generations} generations in which the species' champion scores {@code fitness}. */
    private static Species age(Species species, double fitness, int generations) {
        Species current = species;
        for (int i = 0; i < generations; i++) {
            current.updateStagnation();
            Species next = current.nextGeneration();
            FlappyBirdAgent agent = new FlappyBirdAgent(current.getRepresentative());
            agent.setFitness(fitness);
            next.addMember(agent);
            current = next;
        }
        return current;
    }

    @Test
    void improvementResetsTheStagnationCounter() {
        Species species = age(speciesWithChampion(10), 10, 5);
        assertEquals(4, species.getGenerationsWithoutImprovement(), "first update sets the best, then 4 flat");

        species.getMembers().get(0).setFitness(11);
        species.updateStagnation();

        assertEquals(0, species.getGenerationsWithoutImprovement());
        assertEquals(11, species.getBestFitnessEver());
    }

    @Test
    void nextGenerationKeepsHistoryButNotMembers() {
        Species species = age(speciesWithChampion(10), 10, 3);
        species.updateStagnation();
        Species next = species.nextGeneration();

        assertTrue(next.getMembers().isEmpty());
        assertSame(species.getRepresentative(), next.getRepresentative());
        assertEquals(species.getBestFitnessEver(), next.getBestFitnessEver());
        assertEquals(species.getGenerationsWithoutImprovement(), next.getGenerationsWithoutImprovement());
    }

    @Test
    void stagnantSpeciesStopReproducingUnlessTheyAreAmongTheBest() {
        NeatConfig config = new NeatConfig();
        config.setStagnationLimit(15);
        config.setSpeciesElitism(2);

        Species bestStagnant = age(speciesWithChampion(500), 500, 30);
        Species secondStagnant = age(speciesWithChampion(400), 400, 30);
        Species thirdStagnant = age(speciesWithChampion(300), 300, 30);
        Species improving = age(speciesWithChampion(100), 100, 1);
        bestStagnant.updateStagnation();
        secondStagnant.updateStagnation();
        thirdStagnant.updateStagnation();
        improving.updateStagnation();

        List<Species> reproducing = NeatPopulation.reproducingSpecies(
                List.of(bestStagnant, secondStagnant, thirdStagnant, improving), config);

        assertEquals(List.of(bestStagnant, secondStagnant, improving), reproducing);
    }
}
