package com.neat.flappybirdneat.neat.genome;

import com.neat.flappybirdneat.neural.Brain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Genoma NEAT: listas de node genes y connection genes que representan una red
 * de topología variable. A diferencia de la MLP densa (arrays de pesos fijos),
 * {@link #feedForward} evalúa la red recorriendo el grafo en orden topológico,
 * lo que sigue funcionando cuando las mutaciones estructurales (fase 2b) añadan
 * nodos ocultos o conexiones nuevas.
 */
public class Genome implements Brain {
    private final List<NodeGene> nodes = new ArrayList<>();
    private final List<ConnectionGene> connections = new ArrayList<>();
    private final int numInputs;
    private final int numOutputs;
    private final int biasNodeId;

    /**
     * Construye el genoma inicial mínimo de NEAT: sin nodos ocultos, cada entrada
     * (y el nodo de bias) conectados directamente a cada salida.
     */
    public Genome(int numInputs, int numOutputs, Random random, InnovationTracker tracker) {
        this.numInputs = numInputs;
        this.numOutputs = numOutputs;

        List<NodeGene> inputNodes = new ArrayList<>();
        for (int i = 0; i < numInputs; i++) {
            NodeGene node = new NodeGene(tracker.nextNodeId(), NodeType.INPUT);
            nodes.add(node);
            inputNodes.add(node);
        }

        NodeGene biasNode = new NodeGene(tracker.nextNodeId(), NodeType.BIAS);
        nodes.add(biasNode);
        this.biasNodeId = biasNode.getId();

        List<NodeGene> outputNodes = new ArrayList<>();
        for (int i = 0; i < numOutputs; i++) {
            NodeGene node = new NodeGene(tracker.nextNodeId(), NodeType.OUTPUT);
            nodes.add(node);
            outputNodes.add(node);
        }

        for (NodeGene source : inputNodes) {
            connectFullyConnected(source, outputNodes, random, tracker);
        }
        connectFullyConnected(biasNode, outputNodes, random, tracker);
    }

    private void connectFullyConnected(NodeGene source, List<NodeGene> targets, Random random, InnovationTracker tracker) {
        for (NodeGene target : targets) {
            double weight = random.nextDouble() * 2 - 1;
            int innovation = tracker.getInnovationNumber(source.getId(), target.getId());
            connections.add(new ConnectionGene(source.getId(), target.getId(), weight, true, innovation));
        }
    }

    @Override
    public double[] feedForward(double[] inputs) {
        if (inputs.length != numInputs) {
            throw new IllegalArgumentException("Se esperaban " + numInputs + " entradas, se recibieron " + inputs.length);
        }

        List<NodeGene> inputNodes = getNodesOfType(NodeType.INPUT);
        Map<Integer, Double> values = new HashMap<>();
        for (int i = 0; i < inputNodes.size(); i++) {
            values.put(inputNodes.get(i).getId(), inputs[i]);
        }
        values.put(biasNodeId, 1.0);

        for (int nodeId : topologicalOrder()) {
            if (values.containsKey(nodeId)) continue;

            double sum = 0;
            for (ConnectionGene connection : connections) {
                if (connection.isEnabled() && connection.getOutNode() == nodeId) {
                    Double sourceValue = values.get(connection.getInNode());
                    if (sourceValue != null) {
                        sum += sourceValue * connection.getWeight();
                    }
                }
            }
            values.put(nodeId, sigmoid(sum));
        }

        List<NodeGene> outputNodes = getNodesOfType(NodeType.OUTPUT);
        double[] outputs = new double[numOutputs];
        for (int i = 0; i < outputNodes.size(); i++) {
            outputs[i] = values.getOrDefault(outputNodes.get(i).getId(), 0.0);
        }
        return outputs;
    }

    /**
     * Orden topológico (Kahn) de todos los nodos según las conexiones habilitadas.
     * Los nodos INPUT/BIAS salen primero por no tener conexiones entrantes.
     * Asume un grafo acíclico: las mutaciones estructurales de la fase 2b deben
     * evitar crear ciclos al añadir conexiones.
     */
    private List<Integer> topologicalOrder() {
        Map<Integer, Integer> inDegree = new HashMap<>();
        Map<Integer, List<Integer>> adjacency = new HashMap<>();
        for (NodeGene node : nodes) {
            inDegree.put(node.getId(), 0);
            adjacency.put(node.getId(), new ArrayList<>());
        }
        for (ConnectionGene connection : connections) {
            if (!connection.isEnabled()) continue;
            adjacency.get(connection.getInNode()).add(connection.getOutNode());
            inDegree.merge(connection.getOutNode(), 1, Integer::sum);
        }

        Deque<Integer> queue = new ArrayDeque<>();
        for (Map.Entry<Integer, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) queue.add(entry.getKey());
        }

        List<Integer> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            int current = queue.poll();
            order.add(current);
            for (int next : adjacency.get(current)) {
                if (inDegree.merge(next, -1, Integer::sum) == 0) {
                    queue.add(next);
                }
            }
        }
        return order;
    }

    private List<NodeGene> getNodesOfType(NodeType type) {
        List<NodeGene> result = new ArrayList<>();
        for (NodeGene node : nodes) {
            if (node.getType() == type) result.add(node);
        }
        return result;
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    public List<NodeGene> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<ConnectionGene> getConnections() {
        return Collections.unmodifiableList(connections);
    }

    public int getNumInputs() {
        return numInputs;
    }

    public int getNumOutputs() {
        return numOutputs;
    }

    public int getBiasNodeId() {
        return biasNodeId;
    }
}
