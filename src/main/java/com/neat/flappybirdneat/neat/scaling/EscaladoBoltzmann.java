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
public class EscaladoBoltzmann implements Escalado {

    private double temperatura;
    private final double factorEnfriamiento;

    public EscaladoBoltzmann(double temperaturaInicial) {
        this.temperatura = temperaturaInicial;
        this.factorEnfriamiento = 0.99;
    }

    public EscaladoBoltzmann(double temperaturaInicial, double factorEnfriamiento) {
        this.temperatura = temperaturaInicial;
        this.factorEnfriamiento = factorEnfriamiento;
    }

    @Override
    public void escalarFitness(FlappyBirdAgent[] poblacion) {
        int n = poblacion.length;

        // Calcular media de exp(fitness/T)
        double meanExp = Arrays.stream(poblacion)
                .mapToDouble(agent -> Math.exp(agent.getFitness() / temperatura))
                .average()
                .orElse(1.0);

        // Evitar división por cero
        if (meanExp == 0) meanExp = 1.0;

        // Aplicar escalado de Boltzmann
        for (FlappyBirdAgent agent : poblacion) {
            double scaledFitness = Math.exp(agent.getFitness() / temperatura) / meanExp;
            agent.setFitness(scaledFitness);
        }

        // Reducir temperatura para próxima generación
        temperatura *= factorEnfriamiento;
    }

    public double getTemperatura() {
        return temperatura;
    }

    public void resetTemperatura(double temperaturaInicial) {
        this.temperatura = temperaturaInicial;
    }
}
