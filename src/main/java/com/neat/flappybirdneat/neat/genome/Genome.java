package com.neat.flappybirdneat.neat.genome;

import com.neat.flappybirdneat.neural.Brain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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

    /**
     * Construye un genoma directamente a partir de listas de genes ya resueltas
     * (usado por {@link #copy()} y por el crossover NEAT para ensamblar el genoma hijo).
     */
    private Genome(int numInputs, int numOutputs, int biasNodeId, List<NodeGene> nodes, List<ConnectionGene> connections) {
        this.numInputs = numInputs;
        this.numOutputs = numOutputs;
        this.biasNodeId = biasNodeId;
        this.nodes.addAll(nodes);
        this.connections.addAll(connections);
    }

    static Genome fromGenes(int numInputs, int numOutputs, int biasNodeId, List<NodeGene> nodes, List<ConnectionGene> connections) {
        return new Genome(numInputs, numOutputs, biasNodeId, nodes, connections);
    }

    /** Copia profunda: nodos y conexiones son objetos nuevos, no compartidos con el original. */
    public Genome copy() {
        List<NodeGene> nodeCopies = new ArrayList<>();
        for (NodeGene node : nodes) {
            nodeCopies.add(new NodeGene(node.getId(), node.getType()));
        }
        List<ConnectionGene> connectionCopies = new ArrayList<>();
        for (ConnectionGene connection : connections) {
            connectionCopies.add(new ConnectionGene(connection.getInNode(), connection.getOutNode(),
                    connection.getWeight(), connection.isEnabled(), connection.getInnovationNumber()));
        }
        return new Genome(numInputs, numOutputs, biasNodeId, nodeCopies, connectionCopies);
    }

    /**
     * Mutación estructural add-connection: añade una conexión nueva entre dos nodos
     * no conectados todavía, evitando ciclos (el grafo debe seguir siendo acíclico para
     * que {@link #feedForward} funcione por orden topológico). Si la red ya está
     * completamente conectada (o toda conexión posible crearía un ciclo), no hace nada.
     *
     * @return true si se añadió una conexión nueva
     */
    public boolean mutateAddConnection(Random random, InnovationTracker tracker) {
        List<int[]> candidates = new ArrayList<>();
        for (NodeGene source : nodes) {
            if (source.getType() == NodeType.OUTPUT) continue;
            for (NodeGene target : nodes) {
                if (target.getType() == NodeType.INPUT || target.getType() == NodeType.BIAS) continue;
                if (source.getId() == target.getId()) continue;
                if (connectionExists(source.getId(), target.getId())) continue;
                candidates.add(new int[]{source.getId(), target.getId()});
            }
        }
        Collections.shuffle(candidates, random);

        for (int[] candidate : candidates) {
            int sourceId = candidate[0];
            int targetId = candidate[1];
            if (canReach(targetId, sourceId)) continue; // añadir source->target crearía un ciclo

            double weight = random.nextDouble() * 2 - 1;
            int innovation = tracker.getInnovationNumber(sourceId, targetId);
            connections.add(new ConnectionGene(sourceId, targetId, weight, true, innovation));
            return true;
        }
        return false;
    }

    /**
     * Mutación estructural add-node: elige una conexión habilitada al azar, la deshabilita
     * (no se elimina, para preservar el historial evolutivo) y la sustituye por dos conexiones
     * nuevas a través de un nodo oculto: in -> nuevoNodo (peso 1.0) y nuevoNodo -> out (con el
     * peso original), de forma que el efecto inmediato de la red no cambie.
     *
     * @return true si se partió una conexión (false si no había ninguna conexión habilitada)
     */
    public boolean mutateAddNode(Random random, InnovationTracker tracker) {
        List<ConnectionGene> enabledConnections = new ArrayList<>();
        for (ConnectionGene connection : connections) {
            if (connection.isEnabled()) enabledConnections.add(connection);
        }
        if (enabledConnections.isEmpty()) return false;

        ConnectionGene toSplit = enabledConnections.get(random.nextInt(enabledConnections.size()));
        toSplit.setEnabled(false);

        int newNodeId = tracker.getNodeIdForSplit(toSplit.getInnovationNumber());
        nodes.add(new NodeGene(newNodeId, NodeType.HIDDEN));

        int innovationIn = tracker.getInnovationNumber(toSplit.getInNode(), newNodeId);
        int innovationOut = tracker.getInnovationNumber(newNodeId, toSplit.getOutNode());
        connections.add(new ConnectionGene(toSplit.getInNode(), newNodeId, 1.0, true, innovationIn));
        connections.add(new ConnectionGene(newNodeId, toSplit.getOutNode(), toSplit.getWeight(), true, innovationOut));
        return true;
    }

    private static final double NEW_RANDOM_WEIGHT_CHANCE = 0.1;
    private static final double WEIGHT_PERTURBATION_STD_DEV = 0.5;

    /**
     * Perturbación de pesos: cada conexión muta con probabilidad {@code weightMutationRate}.
     * Al mutar, con un 10% de probabilidad se le asigna un peso nuevo al azar en [-1, 1]
     * (equivalente a una re-inicialización) y con un 90% se perturba con ruido gaussiano
     * sobre el peso actual (equivalente en espíritu a {@code MutacionGaussiana} pero aplicado
     * gen a gen, ya que el genoma no tiene arrays de pesos fijos).
     */
    public void mutateWeights(Random random, double weightMutationRate) {
        for (ConnectionGene connection : connections) {
            if (random.nextDouble() >= weightMutationRate) continue;

            if (random.nextDouble() < NEW_RANDOM_WEIGHT_CHANCE) {
                connection.setWeight(random.nextDouble() * 2 - 1);
            } else {
                connection.setWeight(connection.getWeight() + random.nextGaussian() * WEIGHT_PERTURBATION_STD_DEV);
            }
        }
    }

    private boolean connectionExists(int inNode, int outNode) {
        for (ConnectionGene connection : connections) {
            if (connection.getInNode() == inNode && connection.getOutNode() == outNode) return true;
        }
        return false;
    }

    /** true si existe un camino dirigido de {@code from} a {@code to} (recorriendo todas las conexiones, habilitadas o no). */
    private boolean canReach(int from, int to) {
        if (from == to) return true;
        Deque<Integer> stack = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        stack.push(from);
        while (!stack.isEmpty()) {
            int current = stack.pop();
            if (!visited.add(current)) continue;
            if (current == to) return true;
            for (ConnectionGene connection : connections) {
                if (connection.getInNode() == current) stack.push(connection.getOutNode());
            }
        }
        return false;
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
