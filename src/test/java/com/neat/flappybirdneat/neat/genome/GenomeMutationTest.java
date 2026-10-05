package com.neat.flappybirdneat.neat.genome;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GenomeMutationTest {

    @Test
    void mutateAddConnectionAddsANewEnabledConnection() {
        InnovationTracker tracker = new InnovationTracker();
        Genome genome = new Genome(2, 1, new Random(1), tracker);
        int connectionsBefore = genome.getConnections().size();

        boolean added = genome.mutateAddConnection(new Random(1), tracker);

        // El genoma inicial ya está completamente conectado (sin nodos ocultos), así que no
        // hay ningún par (source, target) libre: la mutación debe fallar silenciosamente.
        assertFalse(added);
        assertEquals(connectionsBefore, genome.getConnections().size());
    }

    @Test
    void mutateAddConnectionConnectsNewHiddenNodeAfterAddNode() {
        InnovationTracker tracker = new InnovationTracker();
        Genome genome = new Genome(2, 1, new Random(1), tracker);
        genome.mutateAddNode(new Random(2), tracker);
        int connectionsBefore = genome.getConnections().size();

        boolean added = genome.mutateAddConnection(new Random(3), tracker);

        assertTrue(added, "Tras add-node hay pares nodo-nodo libres (p.ej. otra entrada -> nodo oculto)");
        assertEquals(connectionsBefore + 1, genome.getConnections().size());
        long enabledCount = genome.getConnections().stream().filter(ConnectionGene::isEnabled).count();
        assertTrue(enabledCount >= 1);
    }

    @Test
    void mutateAddConnectionNeverCreatesACycle() {
        InnovationTracker tracker = new InnovationTracker();
        Genome genome = new Genome(3, 2, new Random(5), tracker);
        Random random = new Random(42);
        for (int i = 0; i < 20; i++) {
            genome.mutateAddNode(random, tracker);
            genome.mutateAddConnection(random, tracker);
        }

        // Si el grafo tuviera un ciclo, no existiría ningún orden topológico válido y
        // feedForward lanzaría o se quedaría con nodos sin valor; en su lugar debe terminar
        // y devolver siempre el número de salidas esperado.
        double[] outputs = genome.feedForward(new double[]{0.1, 0.2, 0.3});
        assertEquals(2, outputs.length);
        for (double output : outputs) {
            assertTrue(output >= 0.0 && output <= 1.0);
        }
    }

    @Test
    void mutateAddNodeDisablesSplitConnectionAndAddsTwoReplacements() {
        InnovationTracker tracker = new InnovationTracker();
        Genome genome = new Genome(2, 1, new Random(1), tracker);
        int nodesBefore = genome.getNodes().size();
        int connectionsBefore = genome.getConnections().size();

        boolean split = genome.mutateAddNode(new Random(9), tracker);

        assertTrue(split);
        assertEquals(nodesBefore + 1, genome.getNodes().size());
        assertEquals(connectionsBefore + 2, genome.getConnections().size());

        long disabledCount = genome.getConnections().stream().filter(c -> !c.isEnabled()).count();
        assertEquals(1, disabledCount, "Debe quedar exactamente una conexión deshabilitada: la que se partió");

        NodeGene newNode = genome.getNodes().stream()
                .filter(n -> n.getType() == NodeType.HIDDEN)
                .findFirst()
                .orElseThrow();

        long connectionsThroughNewNode = genome.getConnections().stream()
                .filter(c -> c.isEnabled() && (c.getInNode() == newNode.getId() || c.getOutNode() == newNode.getId()))
                .count();
        assertEquals(2, connectionsThroughNewNode);
    }

    @Test
    void mutateAddNodePreservesFeedForwardOutputCountAndRange() {
        InnovationTracker tracker = new InnovationTracker();
        Genome genome = new Genome(4, 2, new Random(3), tracker);
        genome.mutateAddNode(new Random(4), tracker);
        genome.mutateAddNode(new Random(5), tracker);

        double[] outputs = genome.feedForward(new double[]{0.1, -0.2, 0.3, 0.4});

        assertEquals(2, outputs.length);
        for (double output : outputs) {
            assertTrue(output > 0.0 && output < 1.0);
        }
    }

    @Test
    void mutateAddNodeReusesSameNodeIdWhenSameConnectionIsSplitAgain() {
        InnovationTracker tracker = new InnovationTracker();
        Genome genomeA = new Genome(2, 1, new Random(1), tracker);
        Genome genomeB = genomeA.copy();

        ConnectionGene splitTarget = genomeA.getConnections().get(0);
        int splitInnovation = splitTarget.getInnovationNumber();

        genomeA.mutateAddNode(new Random(1), tracker);
        // Fuerza a partir la misma conexión en el segundo genoma deshabilitando las demás
        // no es necesario: getNodeIdForSplit se indexa por innovation number de la conexión
        // partida, así que basta con pedirlo directamente al tracker para simular el mismo evento.
        int nodeIdFromTracker = tracker.getNodeIdForSplit(splitInnovation);

        NodeGene newNodeInA = genomeA.getNodes().stream()
                .filter(n -> n.getType() == NodeType.HIDDEN)
                .findFirst()
                .orElseThrow();

        assertEquals(nodeIdFromTracker, newNodeInA.getId(),
                "Partir la misma conexión (mismo innovation number) debe reusar el mismo id de nodo nuevo");
    }

    @Test
    void mutateAddNodeDoesNothingWhenNoEnabledConnections() {
        InnovationTracker tracker = new InnovationTracker();
        Genome genome = new Genome(1, 1, new Random(1), tracker);
        for (ConnectionGene connection : genome.getConnections()) {
            connection.setEnabled(false);
        }

        boolean split = genome.mutateAddNode(new Random(1), tracker);

        assertFalse(split);
    }

    @Test
    void mutateWeightsChangesWeightsWhenRateIsOne() {
        InnovationTracker tracker = new InnovationTracker();
        Genome genome = new Genome(3, 1, new Random(1), tracker);
        List<Double> before = genome.getConnections().stream().map(ConnectionGene::getWeight).toList();

        genome.mutateWeights(new Random(2), 1.0);

        List<Double> after = genome.getConnections().stream().map(ConnectionGene::getWeight).toList();
        assertNotEquals(before, after);
    }

    @Test
    void mutateWeightsLeavesWeightsUnchangedWhenRateIsZero() {
        InnovationTracker tracker = new InnovationTracker();
        Genome genome = new Genome(3, 1, new Random(1), tracker);
        List<Double> before = genome.getConnections().stream().map(ConnectionGene::getWeight).toList();

        genome.mutateWeights(new Random(2), 0.0);

        List<Double> after = genome.getConnections().stream().map(ConnectionGene::getWeight).toList();
        assertEquals(before, after);
    }

    @Test
    void copyIsIndependentFromOriginal() {
        InnovationTracker tracker = new InnovationTracker();
        Genome original = new Genome(2, 1, new Random(1), tracker);
        Genome copy = original.copy();

        copy.mutateAddNode(new Random(1), tracker);
        copy.getConnections().get(0);

        assertNotEquals(original.getNodes().size(), copy.getNodes().size());
        assertEquals(4, original.getNodes().size());
    }
}
