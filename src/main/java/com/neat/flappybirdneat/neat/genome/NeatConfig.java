package com.neat.flappybirdneat.neat.genome;

/**
 * Hiperparámetros de NEAT (Stanley &amp; Miikkulainen, 2002): coeficientes de la distancia de
 * compatibilidad (c1/c2/c3), umbral de especiación δ, tasas de mutación y política de
 * reproducción por especie. Los valores por defecto son los recomendados en el paper original,
 * ajustados a una población pequeña/mediana como la de este proyecto.
 */
public class NeatConfig {
    /** Peso de los genes "excess" en la distancia de compatibilidad. */
    private double excessCoefficient = 1.0;
    /** Peso de los genes "disjoint" en la distancia de compatibilidad. */
    private double disjointCoefficient = 1.0;
    /** Peso de la diferencia media de pesos en genes "matching". */
    private double weightDifferenceCoefficient = 0.4;
    /** Umbral δ: dos genomas con distancia menor pertenecen a la misma especie. */
    private double compatibilityThreshold = 3.0;

    /** Probabilidad, por conexión, de perturbar su peso al reproducirse. */
    private double weightMutationRate = 0.8;
    /** Probabilidad, por hijo, de intentar una mutación estructural add-connection. */
    private double addConnectionRate = 0.05;
    /** Probabilidad, por hijo, de intentar una mutación estructural add-node. */
    private double addNodeRate = 0.03;

    /** Fracción (por especie, ordenada de mejor a peor) que sobrevive como candidata a reproducirse. */
    private double survivalThreshold = 0.2;
    /** Tamaño mínimo de especie para que su campeón pase sin cambios (elitismo por especie). */
    private int championCloneMinSpeciesSize = 5;

    /**
     * Nº de especies objetivo. El umbral δ efectivo de cada población se ajusta generación a
     * generación hacia este objetivo (umbral dinámico del NEAT original de Stanley): sin él, un δ
     * fijo no separa la población inicial (todos los genomas comparten topología) y, tras un
     * colapso, nunca vuelve a separarla. {@code compatibilityThreshold} es el valor inicial.
     */
    private int targetSpeciesCount = 5;
    /** Cuánto se sube o baja δ en cada generación cuando el nº de especies no es el objetivo. */
    private double compatibilityThresholdStep = 0.3;
    /** Valor mínimo de δ al ajustarlo. */
    private double minCompatibilityThreshold = 1.0;

    /**
     * Generaciones sin mejorar su mejor fitness tras las que una especie deja de reproducirse
     * (estancamiento, "dropoff age" del NEAT original). Libera la descendencia que acaparaba.
     */
    private int stagnationLimit = 15;
    /** Nº de mejores especies (por mejor fitness) protegidas del estancamiento. */
    private int speciesElitism = 2;

    public double getExcessCoefficient() {
        return excessCoefficient;
    }

    public void setExcessCoefficient(double excessCoefficient) {
        this.excessCoefficient = excessCoefficient;
    }

    public double getDisjointCoefficient() {
        return disjointCoefficient;
    }

    public void setDisjointCoefficient(double disjointCoefficient) {
        this.disjointCoefficient = disjointCoefficient;
    }

    public double getWeightDifferenceCoefficient() {
        return weightDifferenceCoefficient;
    }

    public void setWeightDifferenceCoefficient(double weightDifferenceCoefficient) {
        this.weightDifferenceCoefficient = weightDifferenceCoefficient;
    }

    public double getCompatibilityThreshold() {
        return compatibilityThreshold;
    }

    public void setCompatibilityThreshold(double compatibilityThreshold) {
        this.compatibilityThreshold = compatibilityThreshold;
    }

    public double getWeightMutationRate() {
        return weightMutationRate;
    }

    public void setWeightMutationRate(double weightMutationRate) {
        this.weightMutationRate = weightMutationRate;
    }

    public double getAddConnectionRate() {
        return addConnectionRate;
    }

    public void setAddConnectionRate(double addConnectionRate) {
        this.addConnectionRate = addConnectionRate;
    }

    public double getAddNodeRate() {
        return addNodeRate;
    }

    public void setAddNodeRate(double addNodeRate) {
        this.addNodeRate = addNodeRate;
    }

    public double getSurvivalThreshold() {
        return survivalThreshold;
    }

    public void setSurvivalThreshold(double survivalThreshold) {
        this.survivalThreshold = survivalThreshold;
    }

    public int getTargetSpeciesCount() {
        return targetSpeciesCount;
    }

    public void setTargetSpeciesCount(int targetSpeciesCount) {
        this.targetSpeciesCount = targetSpeciesCount;
    }

    public double getCompatibilityThresholdStep() {
        return compatibilityThresholdStep;
    }

    public void setCompatibilityThresholdStep(double compatibilityThresholdStep) {
        this.compatibilityThresholdStep = compatibilityThresholdStep;
    }

    public double getMinCompatibilityThreshold() {
        return minCompatibilityThreshold;
    }

    public void setMinCompatibilityThreshold(double minCompatibilityThreshold) {
        this.minCompatibilityThreshold = minCompatibilityThreshold;
    }

    public int getStagnationLimit() {
        return stagnationLimit;
    }

    public void setStagnationLimit(int stagnationLimit) {
        this.stagnationLimit = stagnationLimit;
    }

    public int getSpeciesElitism() {
        return speciesElitism;
    }

    public void setSpeciesElitism(int speciesElitism) {
        this.speciesElitism = speciesElitism;
    }

    public int getChampionCloneMinSpeciesSize() {
        return championCloneMinSpeciesSize;
    }

    public void setChampionCloneMinSpeciesSize(int championCloneMinSpeciesSize) {
        this.championCloneMinSpeciesSize = championCloneMinSpeciesSize;
    }
}
