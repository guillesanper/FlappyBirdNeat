package com.neat.flappybirdneat.champion;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.neat.genome.Genome;
import com.neat.flappybirdneat.neural.Brain;
import com.neat.flappybirdneat.neural.NeuralNetwork;
import com.neat.flappybirdneat.simulation.EngineType;
import java.util.Objects;

/**
 * A trained agent's brain plus where it came from, as stored in a champion file (see
 * {@link ChampionFile}). The brain is a {@link Genome} for NEAT and a {@link NeuralNetwork} for the
 * GA's fixed MLP.
 */
public record Champion(EngineType engine, Brain brain, ChampionMetadata metadata) {

    public Champion {
        Objects.requireNonNull(engine, "engine");
        Objects.requireNonNull(metadata, "metadata");
        boolean matches = engine == EngineType.NEAT ? brain instanceof Genome : brain instanceof NeuralNetwork;
        if (!matches) {
            throw new IllegalArgumentException("A " + engine + " champion cannot have a "
                    + (brain == null ? "null" : brain.getClass().getSimpleName()) + " brain");
        }
    }

    /** Champion with a copy of {@code agent}'s brain, so later evolution cannot change it. */
    public static Champion of(FlappyBirdAgent agent, ChampionMetadata metadata) {
        Brain brain = new FlappyBirdAgent(agent).getBrain();
        EngineType engine = brain instanceof Genome ? EngineType.NEAT : EngineType.GA;
        return new Champion(engine, brain, metadata);
    }

    /** @return a fresh agent driven by a copy of this champion's brain, ready to play */
    public FlappyBirdAgent newAgent() {
        // The copy constructor deep-copies the brain of a freshly reset agent
        return new FlappyBirdAgent(new FlappyBirdAgent(brain));
    }
}
