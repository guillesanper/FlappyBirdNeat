package com.neat.flappybirdneat.simulation;

/**
 * Statistics of one played generation, taken before the population evolves.
 *
 * @param generation      1-based number of the generation that was played
 * @param best            best fitness (frames survived) in the generation
 * @param mean            mean fitness
 * @param min             worst fitness
 * @param alive           agents still alive when the generation stopped (non-zero only if it hit the frame cap)
 * @param frames          frames the generation lasted (the longest-lived agent's fitness, capped)
 * @param species         number of NEAT species (see {@link TrainingEngine#runGeneration}), or -1 for the GA
 * @param diversity       mean pairwise genetic distance (see {@link com.neat.flappybirdneat.neat.EvolvingPopulation#diversity()})
 * @param meanNodes       mean node genes per NEAT genome (inputs, bias and outputs included), or NaN for the GA
 * @param meanConnections mean enabled connection genes per NEAT genome, or NaN for the GA
 */
public record GenerationStats(
        int generation,
        double best,
        double mean,
        double min,
        int alive,
        int frames,
        int species,
        double diversity,
        double meanNodes,
        double meanConnections) {

    /** @return a copy of these statistics with another species count */
    public GenerationStats withSpecies(int speciesCount) {
        return new GenerationStats(
                generation, best, mean, min, alive, frames, speciesCount, diversity, meanNodes, meanConnections);
    }

    /** @return true if some agent survived {@code maxFrames} frames, i.e. it played the whole capped generation */
    public boolean solved(int maxFrames) {
        return best >= maxFrames;
    }
}
