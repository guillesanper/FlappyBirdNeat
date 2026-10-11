package com.neat.flappybirdneat.champion;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.cli.Cli;
import com.neat.flappybirdneat.simulation.EngineType;
import com.neat.flappybirdneat.simulation.TrainingEngine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** The champions in champions/ (see its README), as packaged in the jar. */
class BundledChampionsTest {

    private static final int FRAME_CAP = 20_000;

    @TempDir
    Path tempDir;

    @ParameterizedTest
    @ValueSource(strings = {ChampionFile.BUNDLED_NEAT, ChampionFile.BUNDLED_MLP})
    void bundledChampionReachesTheFrameCapOnFixedPipeSeeds(String name) {
        Champion champion = ChampionFile.loadBundled(name);

        assertEquals(FRAME_CAP, champion.metadata().fitness());
        // Its own generation's pipes, then sequences it never trained on
        long ownPipes = TrainingEngine.pipeSeed(
                champion.metadata().seed(), champion.metadata().generation());
        assertEquals(FRAME_CAP, ChampionRun.play(champion, ownPipes, FRAME_CAP));
        for (long pipeSeed : new long[] {0, 1, 42, 2024, -7}) {
            assertEquals(FRAME_CAP, ChampionRun.play(champion, pipeSeed, FRAME_CAP), "pipe seed " + pipeSeed);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "neat-champion.json, --engine neat --seed 3 --generations 200",
        "mlp-champion.json, --engine ga --selection deterministic_tournament --seed 2 --generations 250"
    })
    void documentedCommandReproducesTheBundledNetwork(String name, String trainingArgs) throws IOException {
        Path retrained = tempDir.resolve(name);
        List<String> args = new ArrayList<>(List.of("--headless", "--stop-on-solve", "--threads", "2"));
        args.addAll(List.of(trainingArgs.split(" ")));
        args.addAll(List.of("--out", tempDir.resolve("run.csv").toString(), "--save-champion", retrained.toString()));
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int exit = Cli.run(
                args.toArray(String[]::new),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        assertEquals(Cli.EXIT_OK, exit, err.toString(StandardCharsets.UTF_8));

        Champion bundled = ChampionFile.load(Path.of("champions", name));
        Champion fresh = ChampionFile.load(retrained);
        String stale = name + " is stale: regenerate it as champions/README.md explains";
        assertEquals(bundled.engine(), fresh.engine(), stale);
        assertEquals(bundled.metadata().generation(), fresh.metadata().generation(), stale);
        assertEquals(bundled.metadata().training(), fresh.metadata().training(), stale);
        assertEquals(networkJson(bundled), networkJson(fresh), stale);
    }

    /** The "network" part of the champion's JSON (everything but the metadata). */
    private static String networkJson(Champion champion) throws IOException {
        Champion withoutMetadata =
                new Champion(champion.engine(), champion.brain(), new ChampionMetadata(0, 0, 0, "", "", Map.of()));
        StringWriter writer = new StringWriter();
        ChampionFile.write(withoutMetadata, writer);
        return writer.toString();
    }

    @ParameterizedTest
    @ValueSource(strings = {ChampionFile.BUNDLED_NEAT, ChampionFile.BUNDLED_MLP})
    void bundledChampionsHaveTheExpectedEngine(String name) {
        EngineType expected = name.startsWith("neat") ? EngineType.NEAT : EngineType.GA;
        assertEquals(expected, ChampionFile.loadBundled(name).engine());
    }
}
