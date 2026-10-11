package com.neat.flappybirdneat.view.main;

import com.neat.flappybirdneat.champion.Champion;
import com.neat.flappybirdneat.neat.genome.ConnectionGene;
import com.neat.flappybirdneat.neat.genome.Genome;
import com.neat.flappybirdneat.neat.genome.NodeType;
import com.neat.flappybirdneat.neural.NeuralNetwork;
import java.util.Locale;

/**
 * Texto de la barra lateral mientras se ve un campeón: sustituye a la generación y el mejor fitness
 * del entrenamiento en vivo, que no tienen relación con el agente que se está mostrando. Tres líneas
 * como máximo: el panel lateral no tiene altura para más.
 */
final class ChampionSummary {

    private ChampionSummary() {}

    static String describe(Champion champion) {
        return String.format(
                Locale.ROOT,
                "Campeón %s · semilla %d%nGeneración %d · fitness %.0f%nRed: %s",
                champion.brain() instanceof Genome ? "NEAT" : "MLP",
                champion.metadata().seed(),
                champion.metadata().generation(),
                champion.metadata().fitness(),
                network(champion));
    }

    private static String network(Champion champion) {
        if (champion.brain() instanceof Genome genome) {
            long hidden = genome.getNodes().stream()
                    .filter(node -> node.getType() == NodeType.HIDDEN)
                    .count();
            long enabled = genome.getConnections().stream()
                    .filter(ConnectionGene::isEnabled)
                    .count();
            return String.format(
                    Locale.ROOT,
                    "%d ocultos, %d/%d conexiones",
                    hidden,
                    enabled,
                    genome.getConnections().size());
        }
        if (champion.brain() instanceof NeuralNetwork network) {
            return String.format(
                    Locale.ROOT,
                    "MLP %d-%d-%d",
                    network.getInputSize(),
                    network.getHiddenSize(),
                    network.getOutputSize());
        }
        return champion.brain().getClass().getSimpleName();
    }
}
