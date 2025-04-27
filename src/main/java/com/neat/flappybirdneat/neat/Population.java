package com.neat.flappybirdneat.neat;

import java.util.Arrays;
import com.neat.flappybirdneat.neural.NeuralNetwork;

public class Population {
    private FlappyBirdAgent[] agents;
    private FlappyBirdAgent bestAgent;
    private int generation;
    private double bestFitness;
    private double mutationRate;

    public Population(int size) {
        agents = new FlappyBirdAgent[size];
        for (int i = 0; i < size; i++) {
            // 4 entradas: posición Y, velocidad, distancia al tubo, altura del tubo
            // 1 salida: saltar o no
            agents[i] = new FlappyBirdAgent(4, 8, 1);
        }
        generation = 1;
        bestFitness = 0;
        mutationRate = 0.1;

        // Inicializar el mejor agente
        bestAgent = new FlappyBirdAgent(4, 8, 1);
    }

    public void naturalSelection() {
        FlappyBirdAgent[] newAgents = new FlappyBirdAgent[agents.length];

        // Elitismo: el mejor agente pasa directamente
        setBestAgent();
        newAgents[0] = new FlappyBirdAgent(4, 8, 1);
        newAgents[0].getBrain().setBrain(bestAgent.getBrain());

        // Selección, cruce y mutación para el resto
        for (int i = 1; i < agents.length; i++) {
            // Selección por ruleta
            FlappyBirdAgent parent1 = selectParent();
            FlappyBirdAgent parent2 = selectParent();

            // Cruzar padres
            FlappyBirdAgent child = new FlappyBirdAgent(4, 8, 1);
            child.getBrain().setBrain(NeuralNetwork.crossover(
                    parent1.getBrain(), parent2.getBrain()));

            // Mutar hijo
            child.getBrain().mutate(mutationRate);

            newAgents[i] = child;
        }

        agents = newAgents;
        generation++;
    }

    private FlappyBirdAgent selectParent() {
        // Implementar selección por ruleta
        double totalFitness = Arrays.stream(agents)
                .mapToDouble(FlappyBirdAgent::getFitness)
                .sum();

        double randomValue = Math.random() * totalFitness;
        double sum = 0;

        for (FlappyBirdAgent agent : agents) {
            sum += agent.getFitness();
            if (sum > randomValue) {
                return agent;
            }
        }

        // Si algo sale mal, devuelve el primer agente
        return agents[0];
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
            bestAgent = new FlappyBirdAgent(4, 8, 1);
            bestAgent.getBrain().setBrain(agents[maxIndex].getBrain());
        }
    }

    // Getters
    public FlappyBirdAgent[] getAgents() {
        return agents;
    }

    public int getGeneration() {
        return generation;
    }

    public double getBestFitness() {
        return bestFitness;
    }

    /**
     * @return El mejor agente encontrado hasta ahora
     */
    public FlappyBirdAgent getBestAgent() {
        return bestAgent;
    }
}