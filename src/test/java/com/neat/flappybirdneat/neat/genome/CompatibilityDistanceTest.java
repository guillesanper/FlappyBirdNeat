package com.neat.flappybirdneat.neat.genome;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CompatibilityDistanceTest {

    @Test
    void identicalGenomesHaveZeroDistance() {
        InnovationTracker tracker = new InnovationTracker();
        Genome genome = new Genome(2, 1, new Random(1), tracker);
        Genome copy = genome.copy();

        assertEquals(0.0, CompatibilityDistance.distance(genome, copy, new NeatConfig()), 1e-9);
    }

    @Test
    void distanceIsSymmetric() {
        InnovationTracker tracker = new InnovationTracker();
        Genome genome = new Genome(2, 1, new Random(1), tracker);
        Genome mutated = genome.copy();
        mutated.mutateAddNode(new Random(2), tracker);

        NeatConfig config = new NeatConfig();
        double forward = CompatibilityDistance.distance(genome, mutated, config);
        double backward = CompatibilityDistance.distance(mutated, genome, config);

        assertEquals(forward, backward, 1e-9);
    }

    @Test
    void weightDifferenceContributesProportionallyToItsCoefficient() {
        InnovationTracker tracker = new InnovationTracker();
        Genome genome = new Genome(2, 1, new Random(1), tracker);
        Genome perturbed = genome.copy();

        // Perturbar el peso de una única conexión (de las 3: input0, input1, bias -> output).
        ConnectionGene toPerturb = perturbed.getConnections().get(0);
        toPerturb.setWeight(toPerturb.getWeight() + 3.0);

        NeatConfig config = new NeatConfig();
        double expectedAverageWeightDiff = 3.0 / genome.getConnections().size();
        double expectedDistance = config.getWeightDifferenceCoefficient() * expectedAverageWeightDiff;

        assertEquals(expectedDistance, CompatibilityDistance.distance(genome, perturbed, config), 1e-9);
    }

    @Test
    void structuralDivergenceContributesExcessGenesWeightedByCoefficient() {
        InnovationTracker tracker = new InnovationTracker();
        Genome genome = new Genome(2, 1, new Random(1), tracker);
        Genome evolved = genome.copy();

        // add-node parte una conexión existente en dos nuevas, con innovation numbers más allá
        // del rango de "genome" (que no ha mutado): ambas cuentan como "excess", no "disjoint".
        assertTrue(evolved.mutateAddNode(new Random(2), tracker));

        NeatConfig config = new NeatConfig();
        double distance = CompatibilityDistance.distance(genome, evolved, config);

        // Genoma pequeño (<20 genes): el normalizador N es 1, así que la distancia es
        // exactamente c1 * (nº de genes excess) más la contribución (nula) de pesos.
        assertEquals(config.getExcessCoefficient() * 2, distance, 1e-9);
    }

    @Test
    void zeroedCoefficientsIgnoreTheirContribution() {
        InnovationTracker tracker = new InnovationTracker();
        Genome genome = new Genome(2, 1, new Random(1), tracker);
        Genome evolved = genome.copy();
        evolved.mutateAddNode(new Random(2), tracker);

        NeatConfig config = new NeatConfig();
        config.setExcessCoefficient(0);
        config.setDisjointCoefficient(0);
        config.setWeightDifferenceCoefficient(0);

        assertEquals(0.0, CompatibilityDistance.distance(genome, evolved, config), 1e-9);
    }
}
