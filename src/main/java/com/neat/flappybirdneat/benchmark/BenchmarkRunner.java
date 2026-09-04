package com.neat.flappybirdneat.benchmark;

import com.neat.flappybirdneat.game.FlappyBirdGame;
import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.neat.Population;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Ejecuta el modo "Fixed MLP" varias veces (distintas semillas) para cada {@link BenchmarkConfig},
 * en modo headless, y agrega las curvas de "mejor fitness por generación" en media ± desviación
 * estándar. Es la base del modo benchmark/comparativa de operadores: permite comparar de forma
 * cuantitativa distintas combinaciones de selección/cruce/mutación/escalado con la misma semilla
 * reproducible (ver {@link Population#Population(int, Random)}).
 */
public final class BenchmarkRunner {

    private BenchmarkRunner() {
    }

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(int completedRuns, int totalRuns, String message);
    }

    public static List<BenchmarkResult> run(List<BenchmarkConfig> configs, int seeds, int generations,
                                              int populationSize, int canvasWidth, int canvasHeight,
                                              ProgressListener listener) {
        int totalRuns = configs.size() * seeds;
        int completed = 0;
        List<BenchmarkResult> results = new ArrayList<>();

        for (BenchmarkConfig config : configs) {
            List<List<Double>> curves = new ArrayList<>();
            for (int seed = 0; seed < seeds; seed++) {
                curves.add(runSingle(config, seed, generations, populationSize, canvasWidth, canvasHeight));
                completed++;
                if (listener != null) {
                    listener.onProgress(completed, totalRuns, config.getLabel() + " — semilla " + seed);
                }
            }
            results.add(aggregate(config.getLabel(), curves, seeds));
        }
        return results;
    }

    /** Límite de frames por generación: evita que un individuo que aprende a "flotar" indefinidamente
     *  cuelgue el benchmark (no hay usuario que pueda pulsar "Detener" en un run headless por lotes). */
    private static final int MAX_STEPS_PER_GENERATION = 5000;

    private static List<Double> runSingle(BenchmarkConfig config, int seed, int generations,
                                           int populationSize, int canvasWidth, int canvasHeight) {
        Random random = new Random(seed);
        Population population = new Population(populationSize, random);
        population.setSeleccionStrategy(config.newSeleccion());
        population.setEscaladoStrategy(config.newEscalado());
        population.setMutacionStrategy(config.newMutacion());
        population.setCruceStrategy(config.newCruce());

        FlappyBirdGame game = new FlappyBirdGame(canvasWidth, canvasHeight);
        List<Double> curve = new ArrayList<>(generations);

        for (int gen = 0; gen < generations; gen++) {
            boolean allDead = false;
            int steps = 0;
            while (!allDead && steps < MAX_STEPS_PER_GENERATION) {
                game.update(population.getAgents());
                allDead = true;
                for (FlappyBirdAgent agent : population.getAgents()) {
                    if (!agent.isDead()) {
                        allDead = false;
                        break;
                    }
                }
                steps++;
            }

            double bestFitnessThisGen = Double.NEGATIVE_INFINITY;
            for (FlappyBirdAgent agent : population.getAgents()) {
                bestFitnessThisGen = Math.max(bestFitnessThisGen, agent.getFitness());
            }
            curve.add(bestFitnessThisGen);

            population.naturalSelection();
            game.reset();
            for (FlappyBirdAgent agent : population.getAgents()) {
                agent.reset();
            }
        }
        return curve;
    }

    private static BenchmarkResult aggregate(String label, List<List<Double>> curves, int seeds) {
        int generations = curves.get(0).size();
        List<Double> mean = new ArrayList<>(generations);
        List<Double> stdDev = new ArrayList<>(generations);

        for (int g = 0; g < generations; g++) {
            double sum = 0;
            for (List<Double> curve : curves) sum += curve.get(g);
            double meanAtGen = sum / curves.size();

            double variance = 0;
            for (List<Double> curve : curves) variance += Math.pow(curve.get(g) - meanAtGen, 2);
            variance /= curves.size();

            mean.add(meanAtGen);
            stdDev.add(Math.sqrt(variance));
        }
        return new BenchmarkResult(label, mean, stdDev, seeds);
    }
}
