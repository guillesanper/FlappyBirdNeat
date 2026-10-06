package com.neat.flappybirdneat.jmh;

import com.neat.flappybirdneat.config.GeneticOperatorsConfig;
import com.neat.flappybirdneat.simulation.EngineType;
import com.neat.flappybirdneat.simulation.TrainingEngine;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Generations per second of a training run, by engine, population size and evaluation threads.
 *
 * <p>Each invocation trains a fresh, seeded engine for {@value #GENERATIONS} generations (playing
 * and evolving), so every invocation does exactly the same work and the score, thanks to
 * {@link OperationsPerInvocation}, reads directly as generations per second. The sequential part
 * (statistics, selection, reproduction) is included on purpose: it is what bounds the speed-up.
 *
 * <p>Run with {@code ./mvnw -P jmh test-compile exec:exec}; pass JMH options with
 * {@code -Djmh.args="..."} (for example {@code -Djmh.args="-p threads=1,4 -rf json"}).
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(1)
public class GenerationThroughputBenchmark {

    static final int GENERATIONS = 30;
    private static final int MAX_FRAMES = 5_000;
    private static final long SEED = 42L;

    @Param({"GA", "NEAT"})
    public EngineType engine;

    @Param({"50", "500"})
    public int population;

    @Param({"1", "2", "4", "8"})
    public int threads;

    @Benchmark
    @OperationsPerInvocation(GENERATIONS)
    public void train(Blackhole blackhole) {
        try (TrainingEngine training = new TrainingEngine(population, 800, 600, SEED)) {
            training.setThreads(threads);
            training.reset(engine, new GeneticOperatorsConfig());
            for (int i = 0; i < GENERATIONS; i++) {
                blackhole.consume(training.runGeneration(MAX_FRAMES));
            }
        }
    }
}
