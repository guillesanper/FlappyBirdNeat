package com.neat.flappybirdneat.benchmark;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class BenchmarkRunnerTest {

    @Test
    void runProducesOneResultPerConfigWithOnePointPerGeneration() {
        int generations = 2;
        List<BenchmarkConfig> configs = BenchmarkPresets.defaultConfigs(generations);

        List<BenchmarkResult> results = BenchmarkRunner.run(configs, 1, generations, 10, 400, 300, null);

        assertEquals(configs.size(), results.size());
        for (BenchmarkResult result : results) {
            assertEquals(generations, result.getMeanCurve().size());
            assertEquals(generations, result.getStdDevCurve().size());
            assertEquals(1, result.getSeeds());
            for (double stdDev : result.getStdDevCurve()) {
                assertTrue(stdDev >= 0.0);
            }
        }
    }

    @Test
    void sameSeedsProduceIdenticalMeanCurveAcrossRuns() {
        int generations = 2;
        List<BenchmarkConfig> configs = List.of(BenchmarkPresets.defaultConfigs(generations).get(0));

        List<BenchmarkResult> run1 = BenchmarkRunner.run(configs, 1, generations, 10, 400, 300, null);
        List<BenchmarkResult> run2 = BenchmarkRunner.run(configs, 1, generations, 10, 400, 300, null);

        for (int i = 0; i < configs.size(); i++) {
            assertEquals(run1.get(i).getMeanCurve(), run2.get(i).getMeanCurve(),
                    "La comparativa debería ser reproducible con la misma semilla: " + configs.get(i).getLabel());
        }
    }
}
