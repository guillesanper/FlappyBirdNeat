package com.neat.flappybirdneat.neat;

import com.neat.flappybirdneat.neural.NeuralNetwork;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PopulationTest {

    private static final int POPULATION_SIZE = 20;

    private void assignFitness(Population population, Random fitnessSource) {
        for (FlappyBirdAgent agent : population.getAgents()) {
            agent.setFitness(fitnessSource.nextDouble() * 100);
        }
    }

    @Test
    void naturalSelectionKeepsPopulationSizeConstant() {
        Population population = new Population(POPULATION_SIZE, new Random(1));
        assignFitness(population, new Random(2));

        population.naturalSelection();

        assertEquals(POPULATION_SIZE, population.getAgents().length);
        for (FlappyBirdAgent agent : population.getAgents()) {
            assertNotNull(agent);
        }
    }

    @Test
    void naturalSelectionAdvancesGenerationAndPreservesBestFitness() {
        Population population = new Population(POPULATION_SIZE, new Random(1));
        assignFitness(population, new Random(2));

        double bestBefore = 0;
        for (FlappyBirdAgent agent : population.getAgents()) {
            bestBefore = Math.max(bestBefore, agent.getFitness());
        }

        int generationBefore = population.getGeneration();
        population.naturalSelection();

        assertEquals(generationBefore + 1, population.getGeneration());
        assertEquals(bestBefore, population.getBestFitness(), 1e-9);
        assertNotNull(population.getBestAgent());
    }

    @Test
    void elitismCarriesBestBrainIntoNextGeneration() {
        Population population = new Population(POPULATION_SIZE, new Random(1));
        assignFitness(population, new Random(2));

        FlappyBirdAgent[] before = population.getAgents();
        double bestFitness = 0;
        double[][] bestWeights = null;
        for (FlappyBirdAgent agent : before) {
            if (agent.getFitness() > bestFitness) {
                bestFitness = agent.getFitness();
                bestWeights = ((NeuralNetwork) agent.getBrain()).getWeightsInputHidden();
            }
        }

        population.naturalSelection();

        FlappyBirdAgent eliteSurvivor = population.getAgents()[0];
        assertArrayEquals(bestWeights, ((NeuralNetwork) eliteSurvivor.getBrain()).getWeightsInputHidden());
        assertEquals(bestFitness, eliteSurvivor.getFitness(), 1e-9);
    }

    @Test
    void sameSeedProducesIdenticalEvolution() {
        Population population1 = new Population(POPULATION_SIZE, new Random(42));
        Population population2 = new Population(POPULATION_SIZE, new Random(42));

        for (int generation = 0; generation < 5; generation++) {
            assignFitness(population1, new Random(100 + generation));
            assignFitness(population2, new Random(100 + generation));

            population1.naturalSelection();
            population2.naturalSelection();

            FlappyBirdAgent[] agents1 = population1.getAgents();
            FlappyBirdAgent[] agents2 = population2.getAgents();
            assertEquals(agents1.length, agents2.length);
            for (int i = 0; i < agents1.length; i++) {
                NeuralNetwork brain1 = (NeuralNetwork) agents1[i].getBrain();
                NeuralNetwork brain2 = (NeuralNetwork) agents2[i].getBrain();
                assertArrayEquals(
                        brain1.getWeightsInputHidden(),
                        brain2.getWeightsInputHidden(),
                        "Divergencia en generación " + population1.getGeneration() + ", agente " + i);
                assertArrayEquals(
                        brain1.getWeightsHiddenOutput(),
                        brain2.getWeightsHiddenOutput(),
                        "Divergencia en generación " + population1.getGeneration() + ", agente " + i);
            }
        }
    }

    @Test
    void diversityIsZeroForSingleAgentPopulation() {
        Population population = new Population(1, new Random(1));
        assertEquals(0.0, population.diversity(), 1e-9);
    }

    @Test
    void diversityIsPositiveForRandomlyInitializedPopulation() {
        Population population = new Population(POPULATION_SIZE, new Random(1));
        assertTrue(population.diversity() > 0.0);
    }

    @Test
    void diversityDropsWhenAllAgentsShareTheSameBrain() {
        Population population = new Population(POPULATION_SIZE, new Random(1));
        NeuralNetwork sharedBrain = (NeuralNetwork) population.getAgents()[0].getBrain();
        for (FlappyBirdAgent agent : population.getAgents()) {
            ((NeuralNetwork) agent.getBrain()).setBrain(sharedBrain);
        }

        assertEquals(0.0, population.diversity(), 1e-9);
    }
}
