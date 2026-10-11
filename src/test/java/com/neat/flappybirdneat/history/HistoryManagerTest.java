package com.neat.flappybirdneat.history;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.game.Pipe;
import com.neat.flappybirdneat.neat.Population;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class HistoryManagerTest {

    @Test
    void addGenerationDataStoresAllMetricsAndTracksBestEver() {
        HistoryManager historyManager = new HistoryManager();
        Population population = new Population(10, new Random(1));
        List<Pipe> pipes = new ArrayList<>();

        historyManager.addGenerationData(1, 50.0, 20.0, 5.0, 8, -1, 1.5, population, pipes);
        historyManager.addGenerationData(2, 90.0, 40.0, 10.0, 9, -1, 2.5, population, pipes);

        assertEquals(2, historyManager.getCurrentRun().getGenerations());

        GenerationData best = historyManager.getBestGeneration();
        assertEquals(90.0, best.getBestFitness(), 1e-9);
        assertEquals(40.0, best.getAvgFitness(), 1e-9);
        assertEquals(10.0, best.getMinFitness(), 1e-9);
        assertEquals(-1, best.getSpeciesCount());
        assertEquals(2.5, best.getDiversity(), 1e-9);
        assertEquals(90.0, historyManager.getBestFitnessEver(), 1e-9);
    }

    @Test
    void startNewRunArchivesPreviousRunAndResetsCurrent() {
        HistoryManager historyManager = new HistoryManager();
        Population population = new Population(5, new Random(1));
        List<Pipe> pipes = new ArrayList<>();

        historyManager.addGenerationData(1, 10.0, 5.0, 1.0, 5, -1, 0.5, population, pipes);
        historyManager.startNewRun();

        assertEquals(1, historyManager.getRunHistories().size());
        assertEquals(0, historyManager.getCurrentRun().getGenerations());
    }

    @Test
    void generationsKeepTheirNumberWhenOldOnesArePruned() {
        HistoryManager historyManager = new HistoryManager();
        Population population = new Population(2, new Random(1));
        List<Pipe> pipes = new ArrayList<>();

        for (int generation = 1; generation <= 503; generation++) {
            historyManager.addGenerationData(generation, generation, 0, 0, 0, -1, 0, population, pipes);
        }

        List<GenerationData> kept = historyManager.getCurrentRun().getGenerationDataList();
        assertEquals(500, kept.size());
        assertEquals(4, kept.get(0).getGenerationNumber(), "generations 1-3 were pruned");
        assertEquals(503, historyManager.getBestGeneration().getGenerationNumber());
    }
}
