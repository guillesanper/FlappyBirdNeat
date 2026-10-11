package com.neat.flappybirdneat.neat.scaling;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;

/**
 * Escalado lineal de Goldberg: f' = a*f + b, con a y b recalculados en cada generación para que la
 * media del fitness se conserve y el mejor individuo reciba {@code C} veces la media (presión
 * selectiva). Así la selección proporcional no se estanca cuando las diferencias de fitness son
 * pequeñas respecto a su valor, ni deja que un superindividuo acapare a los padres al principio.
 * Si con esa recta algún fitness quedara negativo, se usa la recta que lleva el mínimo a 0 y
 * conserva la media (la presión es entonces la mayor posible sin valores negativos).
 */
public class LinearScaling implements ScalingStrategy {

    public static final double DEFAULT_SELECTION_PRESSURE = 2.0;

    private final double selectionPressure;

    public LinearScaling() {
        this(DEFAULT_SELECTION_PRESSURE);
    }

    /** @param selectionPressure C: cuántas veces la media recibe el mejor individuo (C >= 1) */
    public LinearScaling(double selectionPressure) {
        if (!(selectionPressure >= 1.0)) {
            throw new IllegalArgumentException("Selection pressure must be at least 1: " + selectionPressure);
        }
        this.selectionPressure = selectionPressure;
    }

    public double getSelectionPressure() {
        return selectionPressure;
    }

    @Override
    public void scaleFitness(FlappyBirdAgent[] agents) {
        if (agents.length == 0) return;

        double sum = 0;
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        for (FlappyBirdAgent agent : agents) {
            double fitness = agent.getFitness();
            sum += fitness;
            max = Math.max(max, fitness);
            min = Math.min(min, fitness);
        }
        double avg = sum / agents.length;
        // Todos iguales: no hay nada que escalar
        if (max == avg) return;

        double a;
        double b;
        double c = selectionPressure;
        if (min > (c * avg - max) / (c - 1)) {
            // La recta que lleva el máximo a C * media no deja ningún valor negativo
            a = (c - 1) * avg / (max - avg);
            b = avg * (max - c * avg) / (max - avg);
        } else {
            // Recta que lleva el mínimo a 0 y conserva la media
            a = avg / (avg - min);
            b = -min * avg / (avg - min);
        }

        for (FlappyBirdAgent agent : agents) {
            // max(0, ...) solo por redondeo: por construcción ningún valor es negativo
            agent.setFitness(Math.max(0, a * agent.getFitness() + b));
        }
    }
}
