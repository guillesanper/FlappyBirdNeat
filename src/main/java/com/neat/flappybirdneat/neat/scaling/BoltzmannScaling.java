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
        if (agents.length == 0) return;
        // exp(f / T) se desborda a Infinity con fitness altos (hasta 80000) o con T ya enfriada,
        // y el cociente da NaN. Restar el máximo antes de exponenciar no cambia el resultado,
        // porque exp(-max / T) aparece en el numerador y en la media y se cancela.
        double max = Arrays.stream(agents)
                .mapToDouble(FlappyBirdAgent::getFitness)
                .max()
                .orElse(0);
        double meanExp = Arrays.stream(agents)
                .mapToDouble(agent -> Math.exp((agent.getFitness() - max) / temperature))
                .average()
                .orElse(1.0);
        for (FlappyBirdAgent agent : agents) {
            agent.setFitness(Math.exp((agent.getFitness() - max) / temperature) / meanExp);
        }
        temperature *= coolingFactor;
    }

    public double getTemperature() {
        return temperature;
    }

    public void resetTemperature(double initialTemperature) {
        this.temperature = initialTemperature;
    }
}
