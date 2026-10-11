package com.neat.flappybirdneat.neat.genome;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GenomeRestoreTest {

    /** Minimal genome: inputs 0 and 1, bias 2, output 3. */
    private static List<NodeGene> minimalNodes() {
        return new ArrayList<>(List.of(
                new NodeGene(0, NodeType.INPUT),
                new NodeGene(1, NodeType.INPUT),
                new NodeGene(2, NodeType.BIAS),
                new NodeGene(3, NodeType.OUTPUT)));
    }

    private static List<ConnectionGene> minimalConnections() {
        return new ArrayList<>(List.of(
                new ConnectionGene(0, 3, 0.5, true, 0),
                new ConnectionGene(1, 3, -0.5, true, 1),
                new ConnectionGene(2, 3, 0.1, true, 2)));
    }

    private static String rejection(List<NodeGene> nodes, List<ConnectionGene> connections) {
        return assertThrows(IllegalArgumentException.class, () -> Genome.restore(2, 1, 2, nodes, connections))
                .getMessage();
    }

    @Test
    void restoredGenomeOfAnEvolvedNetworkComputesTheSameOutputs() {
        Random random = new Random(5);
        InnovationTracker tracker = new InnovationTracker();
        Genome original = new Genome(4, 1, random, tracker);
        for (int i = 0; i < 12; i++) {
            original.mutateAddNode(random, tracker);
            original.mutateAddConnection(random, tracker);
            original.mutateWeights(random, 0.8);
        }

        Genome restored =
                Genome.restore(4, 1, original.getBiasNodeId(), original.getNodes(), original.getConnections());

        assertNotSame(
                original.getConnections().get(0), restored.getConnections().get(0));
        for (int i = 0; i < 100; i++) {
            double[] input = {random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble()};
            assertArrayEquals(original.feedForward(input), restored.feedForward(input));
        }
    }

    @Test
    void consistentGenesAreAccepted() {
        Genome genome = Genome.restore(2, 1, 2, minimalNodes(), minimalConnections());
        assertEquals(1, genome.feedForward(new double[] {1, 1}).length);
    }

    @Test
    void wrongNumberOfInputsOrOutputsIsRejected() {
        List<NodeGene> nodes = minimalNodes();
        nodes.remove(1);
        List<ConnectionGene> connections = minimalConnections();
        connections.remove(1);
        assertTrue(rejection(nodes, connections).contains("input nodes"));

        List<NodeGene> twoOutputs = minimalNodes();
        twoOutputs.add(new NodeGene(4, NodeType.OUTPUT));
        assertTrue(rejection(twoOutputs, minimalConnections()).contains("output nodes"));
    }

    @Test
    void aMissingBiasIsRejected() {
        List<NodeGene> nodes = minimalNodes();
        nodes.set(2, new NodeGene(2, NodeType.HIDDEN));
        assertTrue(rejection(nodes, minimalConnections()).contains("bias"));
    }

    @Test
    void repeatedIdsAreRejected() {
        List<NodeGene> nodes = minimalNodes();
        nodes.add(new NodeGene(3, NodeType.HIDDEN));
        assertTrue(rejection(nodes, minimalConnections()).contains("Repeated node id"));

        List<ConnectionGene> connections = minimalConnections();
        connections.add(new ConnectionGene(0, 3, 1, false, 7));
        assertTrue(rejection(minimalNodes(), connections).contains("Repeated connection"));

        List<NodeGene> withHidden = minimalNodes();
        withHidden.add(new NodeGene(4, NodeType.HIDDEN));
        List<ConnectionGene> sameInnovation = minimalConnections();
        sameInnovation.add(new ConnectionGene(0, 4, 1, true, 0));
        assertTrue(rejection(withHidden, sameInnovation).contains("Repeated innovation"));
    }

    @Test
    void connectionsToUnknownNodesOrIntoInputsAreRejected() {
        List<ConnectionGene> unknown = minimalConnections();
        unknown.add(new ConnectionGene(0, 9, 1, true, 5));
        assertTrue(rejection(minimalNodes(), unknown).contains("unknown node"));

        List<ConnectionGene> intoInput = minimalConnections();
        intoInput.add(new ConnectionGene(3, 0, 1, true, 5));
        assertTrue(rejection(minimalNodes(), intoInput).contains("goes from"));
    }

    @Test
    void cyclesAreRejectedEvenThroughDisabledConnections() {
        List<NodeGene> nodes = minimalNodes();
        nodes.add(new NodeGene(4, NodeType.HIDDEN));
        nodes.add(new NodeGene(5, NodeType.HIDDEN));
        List<ConnectionGene> connections = minimalConnections();
        connections.add(new ConnectionGene(0, 4, 1, true, 3));
        connections.add(new ConnectionGene(4, 5, 1, true, 4));
        connections.add(new ConnectionGene(5, 4, 1, false, 5));

        assertTrue(rejection(nodes, connections).contains("cycle"));
    }

    @Test
    void nonFiniteWeightsAreRejected() {
        List<ConnectionGene> connections = minimalConnections();
        connections.set(0, new ConnectionGene(0, 3, Double.NaN, true, 0));
        assertTrue(rejection(minimalNodes(), connections).contains("non-finite"));
    }
}
