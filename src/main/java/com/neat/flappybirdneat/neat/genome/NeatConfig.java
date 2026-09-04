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

    public int getChampionCloneMinSpeciesSize() {
        return championCloneMinSpeciesSize;
    }

    public void setChampionCloneMinSpeciesSize(int championCloneMinSpeciesSize) {
        this.championCloneMinSpeciesSize = championCloneMinSpeciesSize;
    }
}
