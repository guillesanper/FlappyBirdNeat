package com.neat.flappybirdneat.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** End-to-end runs of the command line, in process. */
class CliTest {

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    private int run(String... args) {
        return Cli.run(
                args,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
    }

    private String out() {
        return stdout.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return stderr.toString(StandardCharsets.UTF_8);
    }

    @Test
    void neatRunWritesOneRowPerGenerationAndASummary() throws IOException {
        Path csv = tempDir.resolve("neat.csv");

        int exit = run(
                "--headless",
                "--engine",
                "neat",
                "--seed",
                "42",
                "--generations",
                "4",
                "--population",
                "15",
                "--max-frames",
                "3000",
                "--out",
                csv.toString());

        assertEquals(Cli.EXIT_OK, exit, err());
        List<String> lines = Files.readAllLines(csv);
        assertEquals(TrainingCsvWriter.HEADER, lines.get(0));
        assertEquals(5, lines.size(), "header plus one row per generation");
        for (int i = 1; i < lines.size(); i++) {
            String[] cells = lines.get(i).split(",", -1);
            assertEquals(11, cells.length, lines.get(i));
            assertEquals(Integer.toString(i), cells[0]);
            assertTrue(Integer.parseInt(cells[4]) >= 1, "NEAT reports its species");
            assertTrue(Double.parseDouble(cells[8]) >= 6.0, "at least the 6 initial nodes");
            assertTrue(Double.parseDouble(cells[9]) > 0, "enabled connections");
            assertTrue(cells[10].equals("true") || cells[10].equals("false"));
        }
        assertTrue(out().contains("Seed:          42"), out());
        assertTrue(out().contains("Engine:        NEAT"), out());
        assertTrue(out().contains("Generations:   4 of 4"), out());
    }

    @Test
    void gaRunLeavesTheNeatOnlyColumnsEmpty() throws IOException {
        Path csv = tempDir.resolve("ga.csv");

        int exit = run(
                "--headless",
                "--engine",
                "ga",
                "--seed",
                "1",
                "--generations",
                "2",
                "--population",
                "10",
                "--selection",
                "deterministic_tournament",
                "--mutation",
                "non_uniform",
                "--scaling",
                "linear",
                "--out",
                csv.toString());

        assertEquals(Cli.EXIT_OK, exit, err());
        for (String row : Files.readAllLines(csv).subList(1, 3)) {
            String[] cells = row.split(",", -1);
            assertEquals("", cells[4], "species");
            assertEquals("", cells[8], "nodes");
            assertEquals("", cells[9], "connections");
        }
    }

    @Test
    void sameSeedGivesTheSameCsvExceptForWallTime() throws IOException {
        for (String engine : List.of("neat", "ga")) {
            Path first = tempDir.resolve(engine + "-1.csv");
            Path second = tempDir.resolve(engine + "-2.csv");
            String[] args = {
                "--headless",
                "--engine",
                engine,
                "--seed",
                "99",
                "--generations",
                "5",
                "--population",
                "20",
                "--out",
                ""
            };

            args[args.length - 1] = first.toString();
            assertEquals(Cli.EXIT_OK, run(args), err());
            args[args.length - 1] = second.toString();
            assertEquals(Cli.EXIT_OK, run(args), err());

            assertEquals(withoutWallTime(first), withoutWallTime(second), engine);
        }
    }

    @Test
    void threadCountDoesNotChangeTheCsv() throws IOException {
        for (String engine : List.of("neat", "ga")) {
            Path single = tempDir.resolve(engine + "-1-thread.csv");
            Path many = tempDir.resolve(engine + "-4-threads.csv");

            assertEquals(
                    Cli.EXIT_OK,
                    run(
                            "--headless",
                            "--engine",
                            engine,
                            "--seed",
                            "8",
                            "--generations",
                            "6",
                            "--population",
                            "30",
                            "--threads",
                            "1",
                            "--out",
                            single.toString()),
                    err());
            assertEquals(
                    Cli.EXIT_OK,
                    run(
                            "--headless",
                            "--engine",
                            engine,
                            "--seed",
                            "8",
                            "--generations",
                            "6",
                            "--population",
                            "30",
                            "--threads",
                            "4",
                            "--out",
                            many.toString()),
                    err());

            assertEquals(withoutWallTime(single), withoutWallTime(many), engine);
        }
    }

    @Test
    void differentSeedsGiveDifferentCsvs() throws IOException {
        Path first = tempDir.resolve("seed1.csv");
        Path second = tempDir.resolve("seed2.csv");

        run("--headless", "--seed", "1", "--generations", "3", "--population", "20", "--out", first.toString());
        run("--headless", "--seed", "2", "--generations", "3", "--population", "20", "--out", second.toString());

        assertNotEquals(withoutWallTime(first), withoutWallTime(second));
    }

    @Test
    void stopOnSolveEndsAtTheFirstSolvedGeneration() throws IOException {
        Path csv = tempDir.resolve("solved.csv");

        // With a 1-frame cap the very first generation is solved
        int exit = run(
                "--headless",
                "--seed",
                "3",
                "--generations",
                "10",
                "--population",
                "10",
                "--max-frames",
                "1",
                "--stop-on-solve",
                "--out",
                csv.toString());

        assertEquals(Cli.EXIT_OK, exit, err());
        List<String> lines = Files.readAllLines(csv);
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).endsWith(",true"));
        assertTrue(out().contains("Solved:        yes, at generation 1"), out());
        assertTrue(out().contains("Generations:   1 of 10"), out());
    }

    @Test
    void helpPrintsUsageAndSucceeds() {
        assertEquals(Cli.EXIT_OK, run("--help"));
        assertTrue(out().contains("Usage: java -jar FlappyBirdNEAT.jar --headless"), out());
        assertTrue(out().contains("--max-frames N"), out());
    }

    @Test
    void invalidArgumentsFailWithAClearMessage() {
        assertEquals(Cli.EXIT_USAGE, run("--headless", "--out", "x.csv", "--population", "zero"));
        assertTrue(err().contains("error: --population expects an integer, got 'zero'"), err());
        assertTrue(err().contains("--help"), err());
        assertEquals("", out());
    }

    @Test
    void unwritableOutputIsARuntimeError() {
        Path csv = tempDir.resolve("missing-dir").resolve("out.csv");

        int exit = run("--headless", "--generations", "1", "--population", "5", "--out", csv.toString());

        assertEquals(Cli.EXIT_RUNTIME_ERROR, exit);
        assertTrue(err().contains("cannot write"), err());
    }

    /** CSV rows with the wall_ms column (index 7) blanked out. */
    static List<String> withoutWallTime(Path csv) throws IOException {
        return Files.readAllLines(csv).stream()
                .map(line -> {
                    String[] cells = line.split(",", -1);
                    cells[7] = "";
                    return String.join(",", Arrays.asList(cells));
                })
                .toList();
    }
}
