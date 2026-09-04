package com.neat.flappybirdneat.neat.genome;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class NeatPopulationTest {

    private static final int POPULATION_SIZE = 30;

    private void assignFitness(NeatPopulation population, Random fitnessSource) {
        for (FlappyBirdAgent agent : population.getAgents()) {
            agent.setFitness(fitnessSource.nextDouble() * 100);
        }
    }

    @Test
    void initialPopulationHasOneGenomeAgentPerSlot() {
        NeatPopulation population = new NeatPopulation(POPULATION_SIZE, 4, 1, new Random(1), new NeatConfig());

        assertEquals(POPULATION_SIZE, population.getAgents().length);
        for (FlappyBirdAgent agent : population.getAgents()) {
            assertInstanceOf(Genome.class, agent.getBrain());
        }
    }

    @Test
    void naturalSelectionKeepsPopulationSizeConstant() {
        NeatPopulation population = new NeatPopulation(POPULATION_SIZE, 4, 1, new Random(1), new NeatConfig());
        assignFitness(population, new Random(2));

        population.naturalSelection();

        assertEquals(POPULATION_SIZE, population.getAgents().length);
        for (FlappyBirdAgent agent : population.getAgents()) {
            assertNotNull(agent);
        }
    }

    @Test
    void naturalSelectionAdvancesGenerationAndTracksBestFitness() {
        NeatPopulation population = new NeatPopulation(POPULATION_SIZE, 4, 1, new Random(1), new NeatConfig());
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
    void topologyCanGrowAcrossGenerations() {
        // Umbral de especiación muy alto para forzar una sola especie (así todos compiten y
        // mutan sin fragmentarse) y tasas de mutación estructural altas para observar crecimiento.
        NeatConfig config = new NeatConfig();
        config.setCompatibilityThreshold(1000);
        config.setAddConnectionRate(0.9);
        config.setAddNodeRate(0.9);

        NeatPopulation population = new NeatPopulation(POPULATION_SIZE, 4, 1, new Random(1), config);
        int initialConnectionCount = ((Genome) population.getAgents()[0].getBrain()).getConnections().size();

        Random fitnessSource = new Random(3);
        int maxNodesSeen = 0;
        for (int generation = 0; generation < 15; generation++) {
            assignFitness(population, fitnessSource);
            population.naturalSelection();
            for (FlappyBirdAgent agent : population.getAgents()) {
                maxNodesSeen = Math.max(maxNodesSeen, ((Genome) agent.getBrain()).getNodes().size());
            }
        }

        int initialNodeCount = 4 + 1 + 1; // inputs + bias + output
        assertTrue(maxNodesSeen > initialNodeCount,
                "Tras 15 generaciones con mutación estructural agresiva, la topología debería haber crecido");
    }

    @Test
    void speciationGroupsCompatibleGenomesTogether() {
        NeatConfig config = new NeatConfig();
        config.setCompatibilityThreshold(1000); // todo compatible: una única especie
        NeatPopulation population = new NeatPopulation(POPULATION_SIZE, 4, 1, new Random(1), config);
        assignFitness(population, new Random(2));

        population.naturalSelection();

        assertEquals(1, population.getSpeciesCount());
    }

    @Test
    void veryLowCompatibilityThresholdSplitsIntoManySpecies() {
        NeatConfig config = new NeatConfig();
        config.setCompatibilityThreshold(0.0001); // casi nada es compatible entre sí
        NeatPopulation population = new NeatPopulation(POPULATION_SIZE, 4, 1, new Random(1), config);
        assignFitness(population, new Random(2));

        population.naturalSelection();

        assertTrue(population.getSpeciesCount() > 1);
    }

    @Test
    void sameSeedProducesIdenticalEvolution() {
        NeatPopulation population1 = new NeatPopulation(POPULATION_SIZE, 4, 1, new Random(42), new NeatConfig());
        NeatPopulation population2 = new NeatPopulation(POPULATION_SIZE, 4, 1, new Random(42), new NeatConfig());

        for (int generation = 0; generation < 5; generation++) {
            assignFitness(population1, new Random(100 + generation));
            assignFitness(population2, new Random(100 + generation));

            population1.naturalSelection();
            population2.naturalSelection();

            FlappyBirdAgent[] agents1 = population1.getAgents();
            FlappyBirdAgent[] agents2 = population2.getAgents();
            assertEquals(agents1.length, agents2.length);
            for (int i = 0; i < agents1.length; i++) {
                Genome genome1 = (Genome) agents1[i].getBrain();
                Genome genome2 = (Genome) agents2[i].getBrain();
                assertEquals(genome1.getConnections().size(), genome2.getConnections().size(),
                        "Divergencia en generación " + population1.getGeneration() + ", agente " + i);
                for (int c = 0; c < genome1.getConnections().size(); c++) {
                    assertEquals(genome1.getConnections().get(c).getWeight(),
                            genome2.getConnections().get(c).getWeight(), 1e-9,
                            "Divergencia de peso en generación " + population1.getGeneration() + ", agente " + i);
                }
            }
        }
    }

    @Test
    void deepCopyIsIndependentFromOriginal() {
        NeatPopulation population = new NeatPopulation(POPULATION_SIZE, 4, 1, new Random(1), new NeatConfig());
        assignFitness(population, new Random(2));

        NeatPopulation copy = population.deepCopy();
        copy.getAgents()[0].setFitness(999999);

        assertNotEquals(999999, population.getAgents()[0].getFitness());
        assertEquals(POPULATION_SIZE, copy.getAgents().length);
    }

    @Test
    void singleAgentFactoryWrapsOneClonedAgent() {
        Genome genome = new Genome(4, 1, new Random(1), new InnovationTracker());
        FlappyBirdAgent bestAgent = new FlappyBirdAgent(genome);
        bestAgent.setFitness(123.0);

        NeatPopulation single = NeatPopulation.singleAgent(bestAgent, 4, 1, new NeatConfig());

        // singleAgent envuelve el agente tal cual (la responsabilidad de clonarlo, si hace
        // falta, es de quien lo llama); ver SimulationController.createBestAgentOnlyPopulation.
        assertEquals(1, single.getAgents().length);
        assertEquals(123.0, single.getAgents()[0].getFitness(), 1e-9);
        assertSame(bestAgent, single.getAgents()[0]);
    }

    @Test
    void diversityIsZeroForSingleAgentPopulation() {
        Genome genome = new Genome(4, 1, new Random(1), new InnovationTracker());
        FlappyBirdAgent agent = new FlappyBirdAgent(genome);
        NeatPopulation single = NeatPopulation.singleAgent(agent, 4, 1, new NeatConfig());

        assertEquals(0.0, single.diversity(), 1e-9);
    }

    @Test
    void diversityIsPositiveForRandomlyInitializedPopulation() {
        NeatPopulation population = new NeatPopulation(POPULATION_SIZE, 4, 1, new Random(1), new NeatConfig());
        assertTrue(population.diversity() > 0.0);
    }

    @Test
    void diversityStaysFiniteAndNonNegativeAsTopologyEvolves() {
        NeatConfig config = new NeatConfig();
        config.setCompatibilityThreshold(1000);
        config.setAddConnectionRate(0.9);
        config.setAddNodeRate(0.9);

        NeatPopulation population = new NeatPopulation(POPULATION_SIZE, 4, 1, new Random(1), config);
        Random fitnessSource = new Random(3);
        for (int generation = 0; generation < 15; generation++) {
            assignFitness(population, fitnessSource);
            population.naturalSelection();
            double diversity = population.diversity();
            assertTrue(diversity >= 0.0 && Double.isFinite(diversity));
        }
    }
}
