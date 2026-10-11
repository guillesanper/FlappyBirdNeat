package com.neat.flappybirdneat.cli;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.champion.Champion;
import com.neat.flappybirdneat.champion.ChampionFile;
import com.neat.flappybirdneat.champion.ChampionRun;
import com.neat.flappybirdneat.simulation.EngineType;
import com.neat.flappybirdneat.simulation.TrainingEngine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** --save-champion, --watch and --demo, in process (the viewer is a stand-in for the UI). */
class CliChampionTest {

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    private final List<Champion> shown = new ArrayList<>();

    private int run(String... args) {
        return Cli.run(
                args,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8),
                shown::add);
    }

    private String err() {
        return stderr.toString(StandardCharsets.UTF_8);
    }

    private Path train(String engine, long seed, Path champion) {
        int exit = run(
                "--headless",
                "--engine",
                engine,
                "--seed",
                Long.toString(seed),
                "--generations",
                "6",
                "--population",
                "20",
                "--max-frames",
                "4000",
                "--threads",
                "2",
                "--out",
                tempDir.resolve(engine + ".csv").toString(),
                "--save-champion",
                champion.toString());
        assertEquals(Cli.EXIT_OK, exit, err());
        return champion;
    }

    @ParameterizedTest
    @ValueSource(strings = {"neat", "ga"})
    void savedChampionReplaysItsRecordOnItsGenerationsPipes(String engine) throws IOException {
        Champion champion = ChampionFile.load(train(engine, 31, tempDir.resolve("champ.json")));

        assertEquals(EngineType.fromName(engine), champion.engine());
        assertEquals(31, champion.metadata().seed());
        assertEquals(4000L, champion.metadata().training().get("maxFrames"));
        assertEquals(6L, champion.metadata().training().get("generations"));
        assertTrue(champion.metadata().fitness() > 0);
        // Same brain, same pipes, same start: it survives exactly as long as it did in training
        long pipes = TrainingEngine.pipeSeed(31, champion.metadata().generation());
        assertEquals(champion.metadata().fitness(), ChampionRun.play(champion, pipes, 4000));

        String out = stdout.toString(StandardCharsets.UTF_8);
        assertTrue(out.contains("Champion:      " + tempDir.resolve("champ.json")), out);
        assertTrue(out.contains("(generation " + champion.metadata().generation() + ", fitness "), out);
    }

    @Test
    void theChampionIsTheBestAgentOfTheSummary() throws IOException {
        Champion champion = ChampionFile.load(train("neat", 8, tempDir.resolve("c.json")));
        String out = stdout.toString(StandardCharsets.UTF_8);
        assertTrue(
                out.contains(String.format(
                        "Best fitness:  %.0f (generation %d)",
                        champion.metadata().fitness(), champion.metadata().generation())),
                out);
    }

    @Test
    void unwritableChampionIsARuntimeError() {
        Path champion = tempDir.resolve("missing-dir").resolve("champ.json");
        int exit = run(
                "--headless",
                "--generations",
                "1",
                "--population",
                "5",
                "--out",
                tempDir.resolve("ok.csv").toString(),
                "--save-champion",
                champion.toString());

        assertEquals(Cli.EXIT_RUNTIME_ERROR, exit);
        assertTrue(err().contains("cannot write " + champion), err());
    }

    @Test
    void watchLoadsTheChampionAndShowsIt() throws IOException {
        Path file = train("neat", 12, tempDir.resolve("watch.json"));
        Champion saved = ChampionFile.load(file);

        assertEquals(Cli.EXIT_OK, run("--watch", file.toString()));

        assertEquals(1, shown.size());
        assertEquals(saved.metadata(), shown.get(0).metadata());
    }

    @Test
    void watchingAMissingOrInvalidFileFailsBeforeOpeningTheUi() throws IOException {
        Path missing = tempDir.resolve("nope.json");
        assertEquals(Cli.EXIT_RUNTIME_ERROR, run("--watch", missing.toString()));
        assertTrue(err().contains("cannot read " + missing + ": no such file"), err());

        Path future = tempDir.resolve("future.json");
        String json = Files.readString(train("neat", 3, tempDir.resolve("v1.json")));
        Files.writeString(future, json.replace("\"version\": 1", "\"version\": 7"));
        assertEquals(Cli.EXIT_RUNTIME_ERROR, run("--watch", future.toString()));
        assertTrue(err().contains("Unsupported champion file version 7"), err());

        assertEquals(List.of(), shown);
    }

    @Test
    void withoutAViewerWatchingIsARuntimeError() throws IOException {
        Path file = train("ga", 2, tempDir.resolve("ga.json"));
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exit = Cli.run(
                new String[] {"--watch", file.toString()},
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));

        assertEquals(Cli.EXIT_RUNTIME_ERROR, exit);
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("no user interface"));
    }

    @Test
    void demoShowsTheBundledNeatChampion() {
        assertEquals(Cli.EXIT_OK, run("--demo"));

        assertEquals(1, shown.size());
        assertEquals(EngineType.NEAT, shown.get(0).engine());
        assertEquals(
                ChampionFile.loadBundled(ChampionFile.BUNDLED_NEAT).metadata(),
                shown.get(0).metadata());
    }
}
