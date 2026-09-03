package com.neat.flappybirdneat.neat;

import java.util.Arrays;
import java.util.Random;
import com.neat.flappybirdneat.neural.NeuralNetwork;
import com.neat.flappybirdneat.neat.selection.*;
import com.neat.flappybirdneat.neat.scaling.*;
import com.neat.flappybirdneat.neat.mutation.*;
import com.neat.flappybirdneat.neat.crossover.*;

public class Population {
    private FlappyBirdAgent[] agents;
    private FlappyBirdAgent bestAgent;
    private int generation;
    private double bestFitness;
    private double mutationRate;
    private double elitismRate = 0.1;
    private final Random random;

    // Operadores genéticos configurables
    private Seleccion seleccionStrategy;
    private Escalado escaladoStrategy;
    private MutacionStrategy mutacionStrategy;
    private CruceStrategy cruceStrategy;

    public Population(int size) {
        this(size, new Random());
    }

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
        seleccionStrategy = new SeleccionRuleta();
        escaladoStrategy = null;
        mutacionStrategy = new MutacionGaussiana();
        cruceStrategy = new CruceUniforme();
        applyRandomToStrategies();
    }

    private void applyRandomToStrategies() {
        if (seleccionStrategy != null) seleccionStrategy.setRandom(random);
        if (cruceStrategy != null) cruceStrategy.setRandom(random);
        if (mutacionStrategy != null) mutacionStrategy.setRandom(random);
    }

    public void naturalSelection() {
        FlappyBirdAgent[] newAgents = new FlappyBirdAgent[agents.length];

        // Guardar fitness original
        double[] originalFitness = new double[agents.length];
        for (int i = 0; i < agents.length; i++) {
            originalFitness[i] = agents[i].getFitness();
        }

        // Aplicar escalado si está configurado
        if (escaladoStrategy != null) {
            escaladoStrategy.escalarFitness(agents);
        }

        // Elitismo
        setBestAgent();
        Arrays.sort(agents, (a1, a2) -> Double.compare(a2.getFitness(), a1.getFitness()));

        int eliteSize = (int)(agents.length * elitismRate);
        for (int i = 0; i < eliteSize; i++) {
            newAgents[i] = new FlappyBirdAgent(4, 8, 1, random);
            newAgents[i].getBrain().setBrain(agents[i].getBrain());
            newAgents[i].setFitness(agents[i].getFitness());
        }

        // Calcular probabilidades
        Seleccionable[] seleccionables = calcularProbabilidades();

        // Selección
        int[] selected = seleccionStrategy.getSeleccion(seleccionables, agents.length - eliteSize);

        // Cruce y mutación
        for (int i = 0; i < selected.length; i += 2) {
            int idx1 = selected[i];
            int idx2 = (i + 1 < selected.length) ? selected[i + 1] : selected[i];

            FlappyBirdAgent parent1 = agents[idx1];
            FlappyBirdAgent parent2 = agents[idx2];

            FlappyBirdAgent child1 = new FlappyBirdAgent(4, 8, 1, random);
            child1.getBrain().setBrain(cruceStrategy.crossover(
                    parent1.getBrain(), parent2.getBrain()));
            mutacionStrategy.mutate(child1.getBrain(), mutationRate);
            newAgents[eliteSize + i] = child1;

            if (eliteSize + i + 1 < agents.length) {
                FlappyBirdAgent child2 = new FlappyBirdAgent(4, 8, 1, random);
                child2.getBrain().setBrain(cruceStrategy.crossover(
                        parent2.getBrain(), parent1.getBrain()));
                mutacionStrategy.mutate(child2.getBrain(), mutationRate);
                newAgents[eliteSize + i + 1] = child2;
            }
        }

        // Restaurar fitness original
        for (int i = 0; i < agents.length; i++) {
            agents[i].setFitness(originalFitness[i]);
        }

        agents = newAgents;
        generation++;
        mutacionStrategy.update(generation);
    }

    private Seleccionable[] calcularProbabilidades() {
        Seleccionable[] seleccionables = new Seleccionable[agents.length];
        double totalFitness = 0;
        for (int i = 0; i < agents.length; i++) {
            totalFitness += Math.max(0, agents[i].getFitness());
        }
        if (totalFitness == 0) totalFitness = 1.0;

        double accProb = 0;
        for (int i = 0; i < agents.length; i++) {
            double prob = Math.max(0, agents[i].getFitness()) / totalFitness;
            seleccionables[i] = new Seleccionable(i, agents[i].getFitness());
            seleccionables[i].setProb(prob);
            seleccionables[i].setAccProb(accProb);
            accProb += prob;
        }
        return seleccionables;
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
            bestAgent.getBrain().setBrain(agents[maxIndex].getBrain());
        }
    }

    // Setters para configurar operadores
    // Nota: cada setter propaga el generador aleatorio compartido de la población a la nueva
    // estrategia, para que la evolución completa siga siendo reproducible con una semilla fija.
    public void setSeleccionStrategy(Seleccion strategy) {
        this.seleccionStrategy = strategy;
        if (strategy != null) strategy.setRandom(random);
    }

    public void setEscaladoStrategy(Escalado strategy) {
        this.escaladoStrategy = strategy;
    }

    public void setMutacionStrategy(MutacionStrategy strategy) {
        this.mutacionStrategy = strategy;
        if (strategy != null) strategy.setRandom(random);
    }

    public void setCruceStrategy(CruceStrategy strategy) {
        this.cruceStrategy = strategy;
        if (strategy != null) strategy.setRandom(random);
    }

    public void setSeleccionStrategy(String tipo) {
        setSeleccionStrategy(SeleccionFactory.getInstance().getSeleccionStrategy(tipo));
    }

    public void setEscaladoStrategy(String tipo) {
        setEscaladoStrategy(EscaladoFactory.getInstance().getEscaladoStrategy(tipo));
    }

    public void setMutacionStrategy(String tipo) {
        setMutacionStrategy(MutacionFactory.getInstance().getMutacionStrategy(tipo));
    }

    public void setCruceStrategy(String tipo) {
        setCruceStrategy(CruceFactory.getInstance().getCruceStrategy(tipo));
    }

    // Getters
    public FlappyBirdAgent[] getAgents() { return agents; }
    public int getGeneration() { return generation; }
    public double getBestFitness() { return bestFitness; }
    public double getElitismRate() { return elitismRate; }
    public void setElitismRate(double elitismRate) { this.elitismRate = elitismRate; }
    public FlappyBirdAgent getBestAgent() { return bestAgent; }
    public Seleccion getSeleccionStrategy() { return seleccionStrategy; }
    public Escalado getEscaladoStrategy() { return escaladoStrategy; }
    public MutacionStrategy getMutacionStrategy() { return mutacionStrategy; }
    public CruceStrategy getCruceStrategy() { return cruceStrategy; }

    public Population deepCopy() {
        Population copy = new Population(agents.length);
        for (int i = 0; i < agents.length; i++) {
            copy.agents[i] = new FlappyBirdAgent(this.agents[i]);
        }
        copy.generation = this.generation;
        copy.bestFitness = this.bestFitness;
        copy.mutationRate = this.mutationRate;
        copy.elitismRate = this.elitismRate;
        if (this.bestAgent != null) {
            copy.bestAgent = new FlappyBirdAgent(this.bestAgent);
        }
        copy.seleccionStrategy = this.seleccionStrategy;
        copy.escaladoStrategy = this.escaladoStrategy;
        copy.mutacionStrategy = this.mutacionStrategy;
        copy.cruceStrategy = this.cruceStrategy;
        return copy;
    }
}
