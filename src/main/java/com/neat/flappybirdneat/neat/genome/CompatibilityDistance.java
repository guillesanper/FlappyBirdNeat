package com.neat.flappybirdneat.neat.genome;

import java.util.HashMap;
import java.util.Map;

/**
 * Distancia de compatibilidad δ entre dos genomas NEAT, usada para agruparlos en especies.
 * Sigue la fórmula de Stanley &amp; Miikkulainen (2002):
 * <pre>δ = c1·E/N + c2·D/N + c3·W̄</pre>
 * donde E son los genes "excess" (más allá del rango de innovation numbers del otro genoma),
 * D los genes "disjoint" (huecos dentro del rango común), W̄ la diferencia media de peso en los
 * genes "matching" (mismo innovation number en ambos), y N el nº de genes del genoma más grande
 * (o 1 si ambos genomas son pequeños, para no penalizar en exceso a poblaciones iniciales).
 */
public final class CompatibilityDistance {

    private static final int SMALL_GENOME_THRESHOLD = 20;

    private CompatibilityDistance() {
    }

    public static double distance(Genome genome1, Genome genome2, NeatConfig config) {
        Map<Integer, ConnectionGene> genes1 = indexByInnovation(genome1);
        Map<Integer, ConnectionGene> genes2 = indexByInnovation(genome2);

        int highestInnovation1 = maxInnovation(genes1);
        int highestInnovation2 = maxInnovation(genes2);

        int matching = 0;
        int disjoint = 0;
        int excess = 0;
        double totalWeightDifference = 0;

        for (Map.Entry<Integer, ConnectionGene> entry : genes1.entrySet()) {
            int innovation = entry.getKey();
            ConnectionGene gene2 = genes2.get(innovation);
            if (gene2 != null) {
                matching++;
                totalWeightDifference += Math.abs(entry.getValue().getWeight() - gene2.getWeight());
            } else if (innovation > highestInnovation2) {
                excess++;
            } else {
                disjoint++;
            }
        }
        for (Map.Entry<Integer, ConnectionGene> entry : genes2.entrySet()) {
            int innovation = entry.getKey();
            if (genes1.containsKey(innovation)) continue;
            if (innovation > highestInnovation1) {
                excess++;
            } else {
                disjoint++;
            }
        }

        int genomeSize = Math.max(genes1.size(), genes2.size());
        int normalizer = genomeSize < SMALL_GENOME_THRESHOLD ? 1 : genomeSize;
        double averageWeightDifference = matching > 0 ? totalWeightDifference / matching : 0;

        return (config.getExcessCoefficient() * excess) / normalizer
                + (config.getDisjointCoefficient() * disjoint) / normalizer
                + config.getWeightDifferenceCoefficient() * averageWeightDifference;
    }

    private static int maxInnovation(Map<Integer, ConnectionGene> genes) {
        int max = 0;
        for (int innovation : genes.keySet()) {
            if (innovation > max) max = innovation;
        }
        return max;
    }

    private static Map<Integer, ConnectionGene> indexByInnovation(Genome genome) {
        Map<Integer, ConnectionGene> byInnovation = new HashMap<>();
        for (ConnectionGene connection : genome.getConnections()) {
            byInnovation.put(connection.getInnovationNumber(), connection);
        }
        return byInnovation;
    }
}
