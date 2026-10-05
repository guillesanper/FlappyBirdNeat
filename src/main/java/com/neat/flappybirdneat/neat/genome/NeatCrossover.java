package com.neat.flappybirdneat.neat.genome;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Crossover NEAT: alinea los connection genes de dos genomas por innovation number.
 * Los genes "matching" (mismo innovation number en ambos padres) se heredan al azar
 * de uno u otro; los genes "disjoint" (huecos dentro del rango del otro padre) y
 * "excess" (más allá del rango del otro padre) se heredan siempre del padre más apto,
 * siguiendo a Stanley &amp; Miikkulainen (2002).
 */
public final class NeatCrossover {

    private static final double INHERIT_DISABLED_CHANCE = 0.75;

    private NeatCrossover() {
    }

    /**
     * Cruza dos genomas según su fitness. Si el fitness es igual, los genes disjoint/excess
     * de ambos padres se heredan (no solo los de uno), ya que ninguno es "el más apto".
     */
    public static Genome crossover(Genome parent1, double fitness1, Genome parent2, double fitness2, Random random) {
        if (fitness1 > fitness2) return align(parent1, parent2, false, random);
        if (fitness2 > fitness1) return align(parent2, parent1, false, random);
        return align(parent1, parent2, true, random);
    }

    private static Genome align(Genome fitterParent, Genome otherParent, boolean equalFitness, Random random) {
        Map<Integer, ConnectionGene> fitterGenes = indexByInnovation(fitterParent);
        Map<Integer, ConnectionGene> otherGenes = indexByInnovation(otherParent);

        List<ConnectionGene> offspringConnections = new ArrayList<>();
        for (Map.Entry<Integer, ConnectionGene> entry : fitterGenes.entrySet()) {
            ConnectionGene fitterGene = entry.getValue();
            ConnectionGene otherGene = otherGenes.get(entry.getKey());

            if (otherGene != null) {
                offspringConnections.add(inheritMatchingGene(fitterGene, otherGene, random));
            } else {
                offspringConnections.add(copyOf(fitterGene, fitterGene.isEnabled()));
            }
        }

        if (equalFitness) {
            for (Map.Entry<Integer, ConnectionGene> entry : otherGenes.entrySet()) {
                if (!fitterGenes.containsKey(entry.getKey())) {
                    ConnectionGene otherGene = entry.getValue();
                    offspringConnections.add(copyOf(otherGene, otherGene.isEnabled()));
                }
            }
        }

        Map<Integer, NodeGene> offspringNodes = new LinkedHashMap<>();
        for (NodeGene node : fitterParent.getNodes()) {
            offspringNodes.put(node.getId(), copyOf(node));
        }
        for (ConnectionGene connection : offspringConnections) {
            offspringNodes.computeIfAbsent(connection.getInNode(), id -> copyOf(findNode(otherParent, id)));
            offspringNodes.computeIfAbsent(connection.getOutNode(), id -> copyOf(findNode(otherParent, id)));
        }

        return Genome.fromGenes(fitterParent.getNumInputs(), fitterParent.getNumOutputs(), fitterParent.getBiasNodeId(),
                new ArrayList<>(offspringNodes.values()), offspringConnections);
    }

    private static ConnectionGene inheritMatchingGene(ConnectionGene fitterGene, ConnectionGene otherGene, Random random) {
        ConnectionGene chosen = random.nextBoolean() ? fitterGene : otherGene;
        boolean enabled = chosen.isEnabled();
        if ((!fitterGene.isEnabled() || !otherGene.isEnabled()) && random.nextDouble() < INHERIT_DISABLED_CHANCE) {
            enabled = false;
        }
        return copyOf(chosen, enabled);
    }

    private static Map<Integer, ConnectionGene> indexByInnovation(Genome genome) {
        Map<Integer, ConnectionGene> byInnovation = new LinkedHashMap<>();
        for (ConnectionGene connection : genome.getConnections()) {
            byInnovation.put(connection.getInnovationNumber(), connection);
        }
        return byInnovation;
    }

    private static NodeGene findNode(Genome genome, int nodeId) {
        for (NodeGene node : genome.getNodes()) {
            if (node.getId() == nodeId) return node;
        }
        throw new IllegalStateException("Nodo " + nodeId + " referenciado por una conexión pero ausente en ambos padres");
    }

    private static ConnectionGene copyOf(ConnectionGene source, boolean enabled) {
        return new ConnectionGene(source.getInNode(), source.getOutNode(), source.getWeight(), enabled, source.getInnovationNumber());
    }

    private static NodeGene copyOf(NodeGene source) {
        return new NodeGene(source.getId(), source.getType());
    }
}
