package com.neat.flappybirdneat.neat.genome;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GenomeTest {

    @Test
    void initialGenomeHasNoHiddenNodesAndIsFullyConnected() {
        int numInputs = 4;
        int numOutputs = 1;
        Genome genome = new Genome(numInputs, numOutputs, new Random(1), new InnovationTracker());

        assertEquals(numInputs + 1 + numOutputs, genome.getNodes().size(),
                "Debe haber inputs + 1 nodo de bias + outputs, sin nodos ocultos");
        assertEquals((numInputs + 1) * numOutputs, genome.getConnections().size(),
                "Cada entrada y el bias deben conectar con cada salida");

        long hiddenNodes = genome.getNodes().stream().filter(n -> n.getType() == NodeType.HIDDEN).count();
        assertEquals(0, hiddenNodes);

        for (ConnectionGene connection : genome.getConnections()) {
            assertTrue(connection.isEnabled());
        }
    }

    @Test
    void feedForwardReturnsOneOutputPerOutputNodeInSigmoidRange() {
        Genome genome = new Genome(4, 2, new Random(1), new InnovationTracker());

        double[] outputs = genome.feedForward(new double[]{0.5, -0.3, 0.1, 0.9});

        assertEquals(2, outputs.length);
        for (double output : outputs) {
            assertTrue(output > 0.0 && output < 1.0, "La sigmoide siempre produce valores en (0,1): " + output);
        }
    }

    @Test
    void feedForwardRejectsWrongInputSize() {
        Genome genome = new Genome(4, 1, new Random(1), new InnovationTracker());

        assertThrows(IllegalArgumentException.class, () -> genome.feedForward(new double[]{1.0, 2.0}));
    }

    @Test
    void feedForwardMatchesManualComputationOfInitialGenome() {
        // Replica exactamente la secuencia de llamadas a random.nextDouble() que hace el
        // constructor de Genome (una por conexión, en orden: input0->out, input1->out, bias->out),
        // para verificar que feedForward calcula sum(w_i * x_i) + bias y aplica sigmoid.
        Random expectedRandom = new Random(7);
        double weightInput0 = expectedRandom.nextDouble() * 2 - 1;
        double weightInput1 = expectedRandom.nextDouble() * 2 - 1;
        double weightBias = expectedRandom.nextDouble() * 2 - 1;

        Genome genome = new Genome(2, 1, new Random(7), new InnovationTracker());

        double x0 = 0.4;
        double x1 = -0.7;
        double sum = x0 * weightInput0 + x1 * weightInput1 + 1.0 * weightBias;
        double expectedOutput = 1.0 / (1.0 + Math.exp(-sum));

        double[] outputs = genome.feedForward(new double[]{x0, x1});

        assertEquals(1, outputs.length);
        assertEquals(expectedOutput, outputs[0], 1e-9);
    }

    @Test
    void sharedInnovationTrackerAssignsSameInnovationForSameConnectionPair() {
        InnovationTracker tracker = new InnovationTracker();

        // Con numInputs=2: input0=0, input1=1, bias=2, output=3.
        Genome genomeA = new Genome(2, 1, new Random(1), tracker);

        // Volver a pedir el innovation number del mismo par (input0 -> output) debe devolver
        // el mismo valor que se asignó al construir el genoma, sin importar qué más haya
        // ocurrido con el tracker entre medias.
        int reusedInnovation = tracker.getInnovationNumber(0, 3);
        int originalInnovation = genomeA.getConnections().stream()
                .filter(c -> c.getInNode() == 0 && c.getOutNode() == 3)
                .findFirst()
                .orElseThrow()
                .getInnovationNumber();

        assertEquals(originalInnovation, reusedInnovation);
    }

    @Test
    void computeNodeLayersPlacesInputsAndBiasAtZeroAndOutputsRightAfter() {
        Genome genome = new Genome(2, 1, new Random(1), new InnovationTracker());

        Map<Integer, Integer> layers = genome.computeNodeLayers();

        for (NodeGene node : genome.getNodes()) {
            if (node.getType() == NodeType.INPUT || node.getType() == NodeType.BIAS) {
                assertEquals(0, layers.get(node.getId()), "Inputs/bias deben estar en la columna 0");
            } else if (node.getType() == NodeType.OUTPUT) {
                assertEquals(1, layers.get(node.getId()), "Sin nodos ocultos, la salida va justo tras la columna 0");
            }
        }
    }

    @Test
    void computeNodeLayersPlacesHiddenNodeBetweenInputAndOutputAfterAddNodeMutation() {
        InnovationTracker tracker = new InnovationTracker();
        Genome genome = new Genome(2, 1, new Random(1), tracker);
        assertTrue(genome.mutateAddNode(new Random(1), tracker), "Debe haber al menos una conexión que partir");

        Map<Integer, Integer> layers = genome.computeNodeLayers();
        NodeGene hidden = genome.getNodes().stream()
                .filter(n -> n.getType() == NodeType.HIDDEN).findFirst().orElseThrow();
        NodeGene output = genome.getNodes().stream()
                .filter(n -> n.getType() == NodeType.OUTPUT).findFirst().orElseThrow();

        assertEquals(1, layers.get(hidden.getId()), "El nodo oculto va después de la columna de inputs");
        assertEquals(2, layers.get(output.getId()), "La salida siempre queda tras el oculto más profundo");
    }

    @Test
    void feedForwardStoresLastInputsOutputsAndActivationsForVisualization() {
        Genome genome = new Genome(2, 1, new Random(1), new InnovationTracker());
        assertNull(genome.getLastInputs(), "Antes de evaluar no hay estado que dibujar");
        assertTrue(genome.getLastActivations().isEmpty());

        double[] inputs = {0.4, -0.7};
        double[] outputs = genome.feedForward(inputs);

        assertArrayEquals(inputs, genome.getLastInputs());
        assertArrayEquals(outputs, genome.getLastOutputs());
        assertEquals(genome.getNodes().size(), genome.getLastActivations().size(),
                "Debe registrarse la activación de todos los nodos, no solo las salidas");
    }
}
