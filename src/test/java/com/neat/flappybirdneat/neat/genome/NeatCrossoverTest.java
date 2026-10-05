package com.neat.flappybirdneat.neat.genome;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class NeatCrossoverTest {

    @Test
    void offspringInheritsAllGenesFromFitterParentWhenOtherHasNoExtraStructure() {
        InnovationTracker tracker = new InnovationTracker();
        Genome fitter = new Genome(2, 1, new Random(1), tracker);
        Genome other = fitter.copy();

        Genome offspring = NeatCrossover.crossover(fitter, 10.0, other, 5.0, new Random(1));

        assertEquals(fitter.getNodes().size(), offspring.getNodes().size());
        assertEquals(fitter.getConnections().size(), offspring.getConnections().size());
    }

    @Test
    void excessAndDisjointGenesComeFromTheFitterParentWhenFitnessDiffers() {
        InnovationTracker tracker = new InnovationTracker();
        Genome fitter = new Genome(2, 1, new Random(1), tracker);
        Genome lessFit = fitter.copy();

        // El padre más apto gana estructura (add-node) que el otro no tiene: esas conexiones
        // son "excess"/"disjoint" y deben heredarse siempre de él.
        fitter.mutateAddNode(new Random(2), tracker);

        Genome offspring = NeatCrossover.crossover(fitter, 10.0, lessFit, 1.0, new Random(1));

        assertEquals(fitter.getNodes().size(), offspring.getNodes().size());
        assertEquals(fitter.getConnections().size(), offspring.getConnections().size());
    }

    @Test
    void lessFitParentsExtraStructureIsNeverInheritedWhenFitnessDiffers() {
        InnovationTracker tracker = new InnovationTracker();
        Genome fitter = new Genome(2, 1, new Random(1), tracker);
        Genome lessFit = fitter.copy();

        lessFit.mutateAddNode(new Random(3), tracker);

        Genome offspring = NeatCrossover.crossover(fitter, 10.0, lessFit, 1.0, new Random(1));

        assertEquals(fitter.getNodes().size(), offspring.getNodes().size());
        assertEquals(fitter.getConnections().size(), offspring.getConnections().size());
    }

    @Test
    void equalFitnessInheritsExtraStructureFromBothParents() {
        InnovationTracker tracker = new InnovationTracker();
        Genome parent1 = new Genome(2, 1, new Random(1), tracker);
        Genome parent2 = parent1.copy();

        parent1.mutateAddNode(new Random(2), tracker);
        parent2.mutateAddNode(new Random(3), tracker);

        Genome offspring = NeatCrossover.crossover(parent1, 5.0, parent2, 5.0, new Random(1));

        // Ambos padres partieron una conexión distinta (semillas distintas): si el crossover
        // realmente incorpora la estructura de ambos cuando el fitness empata, el hijo debe
        // tener más nodos/conexiones que cualquiera de los dos padres por separado.
        assertTrue(offspring.getNodes().size() >= parent1.getNodes().size());
        assertTrue(offspring.getNodes().size() >= parent2.getNodes().size());
    }

    @Test
    void offspringFeedForwardProducesValidOutputs() {
        InnovationTracker tracker = new InnovationTracker();
        Genome parent1 = new Genome(3, 2, new Random(1), tracker);
        Genome parent2 = parent1.copy();
        parent1.mutateAddNode(new Random(2), tracker);
        parent1.mutateWeights(new Random(4), 1.0);
        parent2.mutateWeights(new Random(5), 1.0);

        Genome offspring = NeatCrossover.crossover(parent1, 8.0, parent2, 3.0, new Random(6));

        double[] outputs = offspring.feedForward(new double[] {0.2, -0.4, 0.6});
        assertEquals(2, outputs.length);
        for (double output : outputs) {
            assertTrue(output > 0.0 && output < 1.0);
        }
    }

    @Test
    void matchingGenesAreInheritedFromEitherParentAtRandom() {
        InnovationTracker tracker = new InnovationTracker();
        Genome parent1 = new Genome(2, 1, new Random(1), tracker);
        Genome parent2 = parent1.copy();
        parent1.mutateWeights(new Random(1), 1.0);
        parent2.mutateWeights(new Random(2), 1.0);

        Genome offspring = NeatCrossover.crossover(parent1, 5.0, parent2, 5.0, new Random(3));

        Map<Integer, Double> parent1WeightsByInnovation = byInnovation(parent1, ConnectionGene::getWeight);
        Map<Integer, Double> parent2WeightsByInnovation = byInnovation(parent2, ConnectionGene::getWeight);

        for (ConnectionGene gene : offspring.getConnections()) {
            double weight = gene.getWeight();
            boolean matchesParent1 = weight == parent1WeightsByInnovation.get(gene.getInnovationNumber());
            boolean matchesParent2 = weight == parent2WeightsByInnovation.get(gene.getInnovationNumber());
            assertTrue(
                    matchesParent1 || matchesParent2,
                    "Cada gen matching del hijo debe venir literalmente de uno de los dos padres");
        }
    }

    @Test
    void equalFitnessCrossoverOfIndependentlyCreatedGenomesKeepsInputCount() {
        // Regresión: cada genoma inicial reservaba ids de nodo propios, así que dos genomas
        // creados por separado no compartían ningún gen y un crossover con empate heredaba las
        // entradas de ambos padres (8 nodos INPUT para 4 entradas -> ArrayIndexOutOfBounds).
        InnovationTracker tracker = new InnovationTracker();
        Genome parent1 = new Genome(4, 1, new Random(1), tracker);
        Genome parent2 = new Genome(4, 1, new Random(2), tracker);

        Genome offspring = NeatCrossover.crossover(parent1, 5.0, parent2, 5.0, new Random(3));

        long inputNodes = offspring.getNodes().stream()
                .filter(n -> n.getType() == NodeType.INPUT)
                .count();
        assertEquals(4, inputNodes);
        assertEquals(parent1.getConnections().size(), offspring.getConnections().size());
        assertDoesNotThrow(() -> offspring.feedForward(new double[] {0.1, 0.2, 0.3, 0.4}));
    }

    private Map<Integer, Double> byInnovation(Genome genome, Function<ConnectionGene, Double> extractor) {
        return genome.getConnections().stream()
                .collect(java.util.stream.Collectors.toMap(ConnectionGene::getInnovationNumber, extractor));
    }
}
