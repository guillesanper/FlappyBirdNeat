package com.neat.flappybirdneat.neat;

import java.util.Arrays;
import java.util.Random;
import com.neat.flappybirdneat.neural.NeuralNetwork;

import com.neat.flappybirdneat.neat.selection.*;
import com.neat.flappybirdneat.neat.scaling.*;
import com.neat.flappybirdneat.neat.mutation.*;
import com.neat.flappybirdneat.neat.crossover.*;

public class Population implements EvolvingPopulation {
    private FlappyBirdAgent[] agents;
    private FlappyBirdAgent bestAgent;
    private int generation;
    private double bestFitness;
    private double mutationRate;
    private double elitismRate = 0.1;
    private final Random random;

    // Operadores genéticos configurables
    private SelectionStrategy selectionStrategy;
    private ScalingStrategy scalingStrategy;
    private MutationStrategy mutationStrategy;
    private CrossoverStrategy crossoverStrategy;

    /**
     * Constructor con generador aleatorio inyectado, para reproducibilidad (tests, semillas fijas).
     * El mismo generador se propaga a los agentes iniciales y a las estrategias por defecto,
     * de modo que dos poblaciones creadas con la misma semilla evolucionan de forma idéntica.
     * @param size Tamaño de la población
     * @param random Generador aleatorio a usar
     */
    public Population(int size, Random random) {
        this.random = random;
        agents = new FlappyBirdAgent[size];
        for (int i = 0; i < size; i++) {
            agents[i] = new FlappyBirdAgent(4, 8, 1, random);
        }
        generation = 1;
        bestFitness = 0;
        mutationRate = 0.1;
        bestAgent = new FlappyBirdAgent(4, 8, 1, random);

        // Inicializar estrategias por defecto
        selectionStrategy = new RouletteSelection();
        scalingStrategy = null;
        mutationStrategy = new GaussianMutation();
        crossoverStrategy = new UniformCrossover();
    }

    @Override
    public void naturalSelection() {
        FlappyBirdAgent[] newAgents = new FlappyBirdAgent[agents.length];

        // Guardar fitness original
        double[] originalFitness = new double[agents.length];
        for (int i = 0; i < agents.length; i++) {
            originalFitness[i] = agents[i].getFitness();
        }

        // Aplicar escalado si está configurado
        if (scalingStrategy != null) {
            scalingStrategy.scaleFitness(agents);
        }

        // Elitismo
        setBestAgent();
        Arrays.sort(agents, (a1, a2) -> Double.compare(a2.getFitness(), a1.getFitness()));

        int eliteSize = (int)(agents.length * elitismRate);
        for (int i = 0; i < eliteSize; i++) {
            newAgents[i] = new FlappyBirdAgent(4, 8, 1, random);
            brainOf(newAgents[i]).setBrain(brainOf(agents[i]));
            newAgents[i].setFitness(agents[i].getFitness());
        }

        // Calcular probabilidades
        Selectable[] selectables = computeSelectionProbabilities();

        // Selección
        int[] selected = selectionStrategy.select(selectables, agents.length - eliteSize, random);

        // Cruce y mutación
        for (int i = 0; i < selected.length; i += 2) {
            int idx1 = selected[i];
            int idx2 = (i + 1 < selected.length) ? selected[i + 1] : selected[i];

            FlappyBirdAgent parent1 = agents[idx1];
            FlappyBirdAgent parent2 = agents[idx2];

            FlappyBirdAgent child1 = new FlappyBirdAgent(4, 8, 1, random);
            brainOf(child1).setBrain(crossoverStrategy.crossover(
                    brainOf(parent1), brainOf(parent2), random));
            mutationStrategy.mutate(brainOf(child1), mutationRate, random);
            newAgents[eliteSize + i] = child1;

            if (eliteSize + i + 1 < agents.length) {
                FlappyBirdAgent child2 = new FlappyBirdAgent(4, 8, 1, random);
                brainOf(child2).setBrain(crossoverStrategy.crossover(
                        brainOf(parent2), brainOf(parent1), random));
                mutationStrategy.mutate(brainOf(child2), mutationRate, random);
                newAgents[eliteSize + i + 1] = child2;
            }
        }

        // Restaurar fitness original
        for (int i = 0; i < agents.length; i++) {
            agents[i].setFitness(originalFitness[i]);
        }

        agents = newAgents;
        generation++;
        mutationStrategy.update(generation);
    }

    private Selectable[] computeSelectionProbabilities() {
        Selectable[] selectables = new Selectable[agents.length];
        double totalFitness = 0;
        for (int i = 0; i < agents.length; i++) {
            totalFitness += Math.max(0, agents[i].getFitness());
        }
        if (totalFitness == 0) totalFitness = 1.0;

        double accProb = 0;
        for (int i = 0; i < agents.length; i++) {
            double prob = Math.max(0, agents[i].getFitness()) / totalFitness;
            selectables[i] = new Selectable(i, agents[i].getFitness());
            selectables[i].setProb(prob);
            selectables[i].setAccProb(accProb);
            accProb += prob;
        }
        return selectables;
    }

    private void setBestAgent() {
        double maxFitness = 0;
        int maxIndex = 0;
        for (int i = 0; i < agents.length; i++) {
            if (agents[i].getFitness() > maxFitness) {
                maxFitness = agents[i].getFitness();
                maxIndex = i;
            }
        }
        if (maxFitness > bestFitness) {
            bestFitness = maxFitness;
            bestAgent = new FlappyBirdAgent(4, 8, 1, random);
            brainOf(bestAgent).setBrain(brainOf(agents[maxIndex]));
        }
    }

    /**
     * Los agentes de esta población siempre llevan una {@link NeuralNetwork} como cerebro
     * (construidos con {@code new FlappyBirdAgent(4, 8, 1, random)}), así que el cast es seguro.
     */
    private static NeuralNetwork brainOf(FlappyBirdAgent agent) {
        return (NeuralNetwork) agent.getBrain();
    }

    // Setters para configurar operadores. Las estrategias no guardan generador: la población les
    // pasa el suyo en cada llamada, así que pueden compartirse entre poblaciones sin acoplarlas.
    public void setSelectionStrategy(SelectionStrategy strategy) {
        this.selectionStrategy = strategy;
    }

    public void setScalingStrategy(ScalingStrategy strategy) {
        this.scalingStrategy = strategy;
    }

    public void setMutationStrategy(MutationStrategy strategy) {
        this.mutationStrategy = strategy;
    }

    public void setCrossoverStrategy(CrossoverStrategy strategy) {
        this.crossoverStrategy = strategy;
    }

    public void setSelectionStrategy(String tipo) {
        setSelectionStrategy(SelectionFactory.getInstance().getSelectionStrategy(tipo));
    }

    public void setScalingStrategy(String tipo) {
        setScalingStrategy(ScalingFactory.getInstance().getScalingStrategy(tipo));
    }

    public void setMutationStrategy(String tipo) {
        setMutationStrategy(MutationFactory.getInstance().getMutationStrategy(tipo));
    }

    public void setCrossoverStrategy(String tipo) {
        setCrossoverStrategy(CrossoverFactory.getInstance().getCrossoverStrategy(tipo));
    }

    /**
     * Diversidad genética: distancia euclídea media por pareja entre los vectores de pesos
     * (aplanando pesos y bias de entrada-oculta y oculta-salida) de una muestra de la población.
     * Se limita el nº de parejas comparadas para no degradar el rendimiento en poblaciones grandes.
     */
    @Override
    public double diversity() {
        int n = agents.length;
        if (n < 2) return 0;
        int sampleSize = Math.min(n, 30);

        double totalDistance = 0;
        int pairs = 0;
        double[][] vectors = new double[sampleSize][];
        for (int i = 0; i < sampleSize; i++) {
            vectors[i] = flattenWeights(brainOf(agents[i]));
        }
        for (int i = 0; i < sampleSize; i++) {
            for (int j = i + 1; j < sampleSize; j++) {
                double sumSquares = 0;
                for (int k = 0; k < vectors[i].length; k++) {
                    double diff = vectors[i][k] - vectors[j][k];
                    sumSquares += diff * diff;
                }
                totalDistance += Math.sqrt(sumSquares);
                pairs++;
            }
        }
        return pairs > 0 ? totalDistance / pairs : 0;
    }

    private static double[] flattenWeights(NeuralNetwork brain) {
        double[][] wih = brain.getWeightsInputHidden();
        double[][] who = brain.getWeightsHiddenOutput();
        double[] bh = brain.getBiasHidden();
        double[] bo = brain.getBiasOutput();

        double[] vector = new double[wih.length * wih[0].length + who.length * who[0].length + bh.length + bo.length];
        int idx = 0;
        for (double[] row : wih) for (double w : row) vector[idx++] = w;
        for (double[] row : who) for (double w : row) vector[idx++] = w;
        for (double b : bh) vector[idx++] = b;
        for (double b : bo) vector[idx++] = b;
        return vector;
    }

    // Getters
    @Override
    public FlappyBirdAgent[] getAgents() { return agents; }
    @Override
    public int getGeneration() { return generation; }
    @Override
    public double getBestFitness() { return bestFitness; }
    public double getElitismRate() { return elitismRate; }
    public void setElitismRate(double elitismRate) { this.elitismRate = elitismRate; }
    @Override
    public FlappyBirdAgent getBestAgent() { return bestAgent; }
    public SelectionStrategy getSelectionStrategy() { return selectionStrategy; }
    public ScalingStrategy getScalingStrategy() { return scalingStrategy; }
    public MutationStrategy getMutationStrategy() { return mutationStrategy; }
    public CrossoverStrategy getCrossoverStrategy() { return crossoverStrategy; }

    /** Constructor de copia: no consume aleatoriedad del original. */
    private Population(Population other, Random random) {
        this.random = random;
        this.agents = new FlappyBirdAgent[other.agents.length];
        for (int i = 0; i < other.agents.length; i++) {
            this.agents[i] = new FlappyBirdAgent(other.agents[i]);
        }
    }

    @Override
    public Population deepCopy() {
        return deepCopy(random);
    }

    @Override
    public Population deepCopy(Random random) {
        Population copy = new Population(this, random);
        copy.generation = this.generation;
        copy.bestFitness = this.bestFitness;
        copy.mutationRate = this.mutationRate;
        copy.elitismRate = this.elitismRate;
        if (this.bestAgent != null) {
            copy.bestAgent = new FlappyBirdAgent(this.bestAgent);
        }
        copy.selectionStrategy = this.selectionStrategy;
        copy.scalingStrategy = this.scalingStrategy;
        copy.mutationStrategy = this.mutationStrategy;
        copy.crossoverStrategy = this.crossoverStrategy;
        return copy;
    }
}
