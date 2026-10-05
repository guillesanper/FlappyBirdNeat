package com.neat.flappybirdneat.neat.scaling;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import java.util.Arrays;

/**
 * Escalado de Boltzmann.
 * Escala el fitness usando una temperatura que decrece con el tiempo:
 * f' = exp(f/T) / media(exp(f/T))
 * La temperatura alta inicial permite mayor exploración, y al decrecer
 * aumenta la presión selectiva (explotación).
 */
public class BoltzmannScaling implements ScalingStrategy {

    private double temperature;
    private final double coolingFactor;

    public BoltzmannScaling(double initialTemperature) {
        this.temperature = initialTemperature;
        this.coolingFactor = 0.99;
    }

    public BoltzmannScaling(double initialTemperature, double coolingFactor) {
        this.temperature = initialTemperature;
        this.coolingFactor = coolingFactor;
    }

    @Override
    public void scaleFitness(FlappyBirdAgent[] agents) {
        int n = agents.length;

        // Calcular media de exp(fitness/T)
        double meanExp = Arrays.stream(agents)
                .mapToDouble(agent -> Math.exp(agent.getFitness() / temperature))
                .average()
                .orElse(1.0);

        // Evitar división por cero
        if (meanExp == 0) meanExp = 1.0;

        // Aplicar escalado de Boltzmann
        for (FlappyBirdAgent agent : agents) {
            double scaledFitness = Math.exp(agent.getFitness() / temperature) / meanExp;
            agent.setFitness(scaledFitness);
        }

        // Reducir temperatura para próxima generación
        temperature *= coolingFactor;
    }

    public double getTemperature() {
        return temperature;
    }

    public void resetTemperature(double initialTemperature) {
        this.temperature = initialTemperature;
    }
}
