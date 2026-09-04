package com.neat.flappybirdneat.benchmark;

import java.util.List;

/**
 * Resultado agregado de un benchmark para una {@link BenchmarkConfig}: la curva media de
 * "mejor fitness por generación" a través de todas las semillas ejecutadas, junto a la
 * desviación estándar por generación (usada para dibujar una banda de intervalo de confianza).
 */
public class BenchmarkResult {
    private final String label;
    private final List<Double> meanCurve;
    private final List<Double> stdDevCurve;
    private final int seeds;

    public BenchmarkResult(String label, List<Double> meanCurve, List<Double> stdDevCurve, int seeds) {
        this.label = label;
        this.meanCurve = meanCurve;
        this.stdDevCurve = stdDevCurve;
        this.seeds = seeds;
    }

    public String getLabel() { return label; }
    public List<Double> getMeanCurve() { return meanCurve; }
    public List<Double> getStdDevCurve() { return stdDevCurve; }
    public int getSeeds() { return seeds; }
}
