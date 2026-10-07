package com.neat.flappybirdneat.neat.genome;

import com.neat.flappybirdneat.neat.EvolvingPopulation;
import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Población NEAT: evoluciona tanto pesos como topología. En cada generación agrupa los agentes
 * en especies por distancia de compatibilidad δ, aplica fitness sharing para repartir la
 * descendencia proporcionalmente entre especies, y reproduce dentro de cada especie mediante
 * {@link NeatCrossover} y las mutaciones estructurales/de peso de {@link Genome}.
 */
public class NeatPopulation implements EvolvingPopulation {
    private final int populationSize;
    private final int numInputs;
    private final int numOutputs;
    private final Random random;
    private final NeatConfig config;
    private final InnovationTracker tracker;

    private FlappyBirdAgent[] agents;
    private List<Species> species = new ArrayList<>();
    private int generation;
    private double bestFitness;
    private FlappyBirdAgent bestAgent;
    /**
     * Umbral δ efectivo de esta población. Empieza en {@link NeatConfig#getCompatibilityThreshold()}
     * y se ajusta cada generación hacia {@link NeatConfig#getTargetSpeciesCount()}. Es estado de la
     * población, no de la configuración: la configuración se comparte entre poblaciones (copias,
     * repeticiones) y ajustarla allí las acoplaría.
     */
    private double compatibilityThreshold;
    /** Nº de especies encontradas en la última especiación (incluidas las estancadas que no se reproducen). */
    private int speciesCount;

    public NeatPopulation(int populationSize, int numInputs, int numOutputs, Random random, NeatConfig config) {
        this.populationSize = populationSize;
        this.numInputs = numInputs;
        this.numOutputs = numOutputs;
        this.random = random;
        this.config = config;
        this.tracker = new InnovationTracker();

        agents = new FlappyBirdAgent[populationSize];
        for (int i = 0; i < populationSize; i++) {
            agents[i] = new FlappyBirdAgent(new Genome(numInputs, numOutputs, random, tracker));
        }
        generation = 1;
        bestFitness = 0;
        compatibilityThreshold = config.getCompatibilityThreshold();
    }

    /** Constructor usado por {@link #deepCopy()} y por réplicas de un único agente para el historial. */
    private NeatPopulation(
            int populationSize,
            int numInputs,
            int numOutputs,
            Random random,
            NeatConfig config,
            InnovationTracker tracker,
            FlappyBirdAgent[] agents,
            int generation,
            double bestFitness,
            FlappyBirdAgent bestAgent,
            double compatibilityThreshold,
            int speciesCount) {
        this.populationSize = populationSize;
        this.numInputs = numInputs;
        this.numOutputs = numOutputs;
        this.random = random;
        this.config = config;
        this.tracker = tracker;
        this.agents = agents;
        this.generation = generation;
        this.bestFitness = bestFitness;
        this.bestAgent = bestAgent;
        this.compatibilityThreshold = compatibilityThreshold;
        this.speciesCount = speciesCount;
    }

    /** Crea una población de un único agente (usada para reproducir el mejor agente histórico). */
    public static NeatPopulation singleAgent(
            FlappyBirdAgent agent, int numInputs, int numOutputs, Random random, NeatConfig config) {
        NeatPopulation single = new NeatPopulation(
                1,
                numInputs,
                numOutputs,
                random,
                config,
                new InnovationTracker(),
                new FlappyBirdAgent[] {agent},
                1,
                agent.getFitness(),
                agent,
                config.getCompatibilityThreshold(),
                0);
        return single;
    }

    @Override
    public void naturalSelection() {
        updateBestAgent();

        speciate();
        for (Species s : species) {
            s.updateStagnation();
        }
        speciesCount = species.size();
        adjustCompatibilityThreshold(speciesCount);
        species = reproducingSpecies(species, config);

        if (species.isEmpty()) {
            // No debería ocurrir (siempre hay al menos un agente), pero por seguridad no hacemos nada.
            generation++;
            return;
        }

        double totalAdjustedFitness = 0;
        for (Species s : species) {
            totalAdjustedFitness += s.totalAdjustedFitness();
        }

        List<FlappyBirdAgent> nextGenAgents = new ArrayList<>(populationSize);
        int[] offspringCounts = allocateOffspring(totalAdjustedFitness);

        for (int i = 0; i < species.size(); i++) {
            Species s = species.get(i);
            int offspring = offspringCounts[i];
            if (offspring <= 0) continue;

            List<FlappyBirdAgent> parents = s.survivors(config.getSurvivalThreshold());

            if (s.size() >= config.getChampionCloneMinSpeciesSize()) {
                nextGenAgents.add(new FlappyBirdAgent(s.champion()));
                offspring--;
            }

            for (int j = 0; j < offspring; j++) {
                nextGenAgents.add(reproduce(parents));
            }
        }

        // Ajuste de redondeo: completar o recortar hasta el tamaño exacto de población.
        while (nextGenAgents.size() < populationSize) {
            Species s = species.get(random.nextInt(species.size()));
            nextGenAgents.add(reproduce(s.survivors(config.getSurvivalThreshold())));
        }
        while (nextGenAgents.size() > populationSize) {
            nextGenAgents.remove(nextGenAgents.size() - 1);
        }

        agents = nextGenAgents.toArray(new FlappyBirdAgent[0]);
        generation++;
    }

    /**
     * Umbral dinámico: si hay menos especies que el objetivo, baja δ (más fácil fundar especies); si
     * hay más, lo sube. Se aplica a la especiación de la generación siguiente.
     */
    private void adjustCompatibilityThreshold(int speciesCount) {
        if (speciesCount < config.getTargetSpeciesCount()) {
            compatibilityThreshold = Math.max(
                    config.getMinCompatibilityThreshold(),
                    compatibilityThreshold - config.getCompatibilityThresholdStep());
        } else if (speciesCount > config.getTargetSpeciesCount()) {
            compatibilityThreshold += config.getCompatibilityThresholdStep();
        }
    }

    /**
     * Especies que se reproducen esta generación: todas salvo las estancadas (sin mejorar en
     * {@link NeatConfig#getStagnationLimit()} generaciones), excepto las
     * {@link NeatConfig#getSpeciesElitism()} de mayor fitness histórico, que siempre se conservan.
     * Mantiene el orden original de la lista.
     */
    static List<Species> reproducingSpecies(List<Species> candidates, NeatConfig config) {
        List<Species> byBest = new ArrayList<>(candidates);
        byBest.sort((a, b) -> Double.compare(b.getBestFitnessEver(), a.getBestFitnessEver()));
        List<Species> protectedSpecies = byBest.subList(0, Math.min(config.getSpeciesElitism(), byBest.size()));

        List<Species> result = new ArrayList<>();
        for (Species s : candidates) {
            boolean stagnant = s.getGenerationsWithoutImprovement() >= config.getStagnationLimit();
            if (!stagnant || protectedSpecies.contains(s)) {
                result.add(s);
            }
        }
        return result;
    }

    private int[] allocateOffspring(double totalAdjustedFitness) {
        int[] counts = new int[species.size()];
        if (totalAdjustedFitness <= 0) {
            // Sin señal de fitness: repartir a partes iguales.
            int base = populationSize / species.size();
            for (int i = 0; i < counts.length; i++) counts[i] = base;
            return counts;
        }
        for (int i = 0; i < species.size(); i++) {
            double share = species.get(i).totalAdjustedFitness() / totalAdjustedFitness;
            counts[i] = (int) Math.round(share * populationSize);
        }
        return counts;
    }

    private FlappyBirdAgent reproduce(List<FlappyBirdAgent> parents) {
        FlappyBirdAgent parent1 = parents.get(random.nextInt(parents.size()));
        FlappyBirdAgent parent2 = parents.get(random.nextInt(parents.size()));

        Genome childGenome = NeatCrossover.crossover(
                genomeOf(parent1), parent1.getFitness(), genomeOf(parent2), parent2.getFitness(), random);

        childGenome.mutateWeights(random, config.getWeightMutationRate());
        if (random.nextDouble() < config.getAddConnectionRate()) {
            childGenome.mutateAddConnection(random, tracker);
        }
        if (random.nextDouble() < config.getAddNodeRate()) {
            childGenome.mutateAddNode(random, tracker);
        }

        return new FlappyBirdAgent(childGenome);
    }

    /**
     * Agrupa los agentes actuales en especies. Reutiliza los representantes de la generación
     * anterior cuando un agente sigue siendo compatible con alguno (para que las especies
     * mantengan identidad entre generaciones); si no encaja en ninguna, funda una especie nueva.
     */
    private void speciate() {
        List<Species> newSpecies = new ArrayList<>();
        for (Species previous : species) {
            newSpecies.add(previous.nextGeneration());
        }

        for (FlappyBirdAgent agent : agents) {
            Genome genome = genomeOf(agent);
            Species match = null;
            for (Species s : newSpecies) {
                if (CompatibilityDistance.distance(genome, s.getRepresentative(), config) < compatibilityThreshold) {
                    match = s;
                    break;
                }
            }
            if (match == null) {
                match = new Species(genome);
                newSpecies.add(match);
            }
            match.addMember(agent);
        }

        newSpecies.removeIf(s -> s.getMembers().isEmpty());
        for (Species s : newSpecies) {
            s.setRepresentative(
                    genomeOf(s.getMembers().get(random.nextInt(s.getMembers().size()))));
        }
        species = newSpecies;
    }

    private void updateBestAgent() {
        for (FlappyBirdAgent agent : agents) {
            if (agent.getFitness() > bestFitness) {
                bestFitness = agent.getFitness();
                bestAgent = new FlappyBirdAgent(agent);
            }
        }
    }

    private static Genome genomeOf(FlappyBirdAgent agent) {
        return (Genome) agent.getBrain();
    }

    @Override
    public FlappyBirdAgent[] getAgents() {
        return agents;
    }

    @Override
    public FlappyBirdAgent getBestAgent() {
        return bestAgent;
    }

    @Override
    public int getGeneration() {
        return generation;
    }

    @Override
    public double getBestFitness() {
        return bestFitness;
    }

    /** Nº de especies encontradas en la última especiación (0 antes de la primera generación). */
    public int getSpeciesCount() {
        return speciesCount;
    }

    /** Umbral δ efectivo actual (ajustado dinámicamente; ver {@link NeatConfig#getTargetSpeciesCount()}). */
    public double getCompatibilityThreshold() {
        return compatibilityThreshold;
    }

    /**
     * Diversidad genética: distancia de compatibilidad δ media por pareja entre genomas de una
     * muestra de la población. A diferencia del {@link Population} de topología fija, aquí la
     * distancia ya tiene en cuenta diferencias estructurales (genes excess/disjoint), no solo pesos.
     */
    @Override
    public double diversity() {
        int n = agents.length;
        if (n < 2) return 0;
        int sampleSize = Math.min(n, 30);

        double total = 0;
        int pairs = 0;
        for (int i = 0; i < sampleSize; i++) {
            Genome genomeI = genomeOf(agents[i]);
            for (int j = i + 1; j < sampleSize; j++) {
                total += CompatibilityDistance.distance(genomeI, genomeOf(agents[j]), config);
                pairs++;
            }
        }
        return pairs > 0 ? total / pairs : 0;
    }

    @Override
    public NeatPopulation deepCopy() {
        return copy(random, tracker);
    }

    @Override
    public NeatPopulation deepCopy(Random random) {
        return copy(random, new InnovationTracker(tracker));
    }

    private NeatPopulation copy(Random random, InnovationTracker tracker) {
        FlappyBirdAgent[] copiedAgents = new FlappyBirdAgent[agents.length];
        for (int i = 0; i < agents.length; i++) {
            copiedAgents[i] = new FlappyBirdAgent(agents[i]);
        }
        FlappyBirdAgent copiedBest = bestAgent != null ? new FlappyBirdAgent(bestAgent) : null;
        return new NeatPopulation(
                populationSize,
                numInputs,
                numOutputs,
                random,
                config,
                tracker,
                copiedAgents,
                generation,
                bestFitness,
                copiedBest,
                compatibilityThreshold,
                speciesCount);
    }
}
