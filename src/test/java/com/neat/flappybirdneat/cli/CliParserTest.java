package com.neat.flappybirdneat.cli;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.simulation.EngineType;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CliParserTest {

    private static CliOptions parseRun(String... args) throws UsageException {
        CliParser.Command command = CliParser.parse(args);
        return assertInstanceOf(CliParser.Run.class, command).options();
    }

    private static String parseError(String... args) {
        return assertThrows(UsageException.class, () -> CliParser.parse(args)).getMessage();
    }

    @Test
    void parsesEveryOption() throws UsageException {
        CliOptions options = parseRun(
                "--headless",
                "--engine",
                "ga",
                "--seed",
                "-42",
                "--generations",
                "200",
                "--population",
                "30",
                "--max-frames",
                "15000",
                "--threads",
                "3",
                "--stop-on-solve",
                "--selection",
                "ranking",
                "--crossover",
                "single_point",
                "--mutation",
                "non_uniform",
                "--scaling",
                "sigma",
                "--out",
                "results.csv",
                "--save-champion",
                "champ.json");

        assertEquals(
                new CliOptions(
                        EngineType.GA,
                        -42L,
                        200,
                        30,
                        15000,
                        3,
                        true,
                        "ranking",
                        "single_point",
                        "non_uniform",
                        "sigma",
                        Path.of("results.csv"),
                        Path.of("champ.json")),
                options);
    }

    @Test
    void appliesDefaults() throws UsageException {
        CliOptions options = parseRun("--headless", "--out", "out.csv");

        assertEquals(EngineType.NEAT, options.engine());
        assertEquals(CliParser.DEFAULT_GENERATIONS, options.generations());
        assertEquals(CliParser.DEFAULT_POPULATION, options.population());
        assertEquals(CliParser.DEFAULT_MAX_FRAMES, options.maxFrames());
        assertEquals(Runtime.getRuntime().availableProcessors(), options.threads());
        assertFalse(options.stopOnSolve());
        assertNull(options.selection());
        assertNull(options.scaling());
    }

    @Test
    void acceptsInlineValuesAndCaseInsensitiveEngine() throws UsageException {
        CliOptions options = parseRun("--headless", "--engine=NEAT", "--seed=7", "--out=a.csv");

        assertEquals(EngineType.NEAT, options.engine());
        assertEquals(7L, options.seed());
        assertEquals(Path.of("a.csv"), options.out());
    }

    @Test
    void acceptsSpanishOperatorKeys() throws UsageException {
        CliOptions options = parseRun("--headless", "--engine", "ga", "--selection", "ruleta", "--out", "a.csv");

        assertEquals("ruleta", options.selection());
    }

    @Test
    void helpWinsOverEverythingElse() throws UsageException {
        assertInstanceOf(CliParser.Help.class, CliParser.parse(new String[] {"--help"}));
        assertInstanceOf(CliParser.Help.class, CliParser.parse(new String[] {"--headless", "-h"}));
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "--headless --out a.csv --bogus | Unknown option: --bogus",
                "--headless --out a.csv extra | Unexpected argument: extra",
                "--engine neat --out a.csv | require --headless",
                "--headless | Missing required option --out",
                "--headless --out | Missing value for --out",
                "--headless --seed --out a.csv | Missing value for --seed",
                "--headless --out a.csv --engine cma | Unknown engine 'cma'",
                "--headless --out a.csv --seed abc | --seed expects an integer, got 'abc'",
                "--headless --out a.csv --generations 0 | --generations must be at least 1, got 0",
                "--headless --out a.csv --generations 2.5 | --generations expects an integer",
                "--headless --out a.csv --population 1 | --population must be at least 2",
                "--headless --out a.csv --max-frames -5 | --max-frames must be at least 1",
                "--headless --out a.csv --threads 0 | --threads must be at least 1, got 0",
                "--headless --out a.csv --selection ranking | --selection only applies to --engine ga",
                "--headless --out a.csv --engine ga --mutation wild | Unknown value for --mutation: 'wild'",
                "--headless --out a.csv --seed 1 --seed 2 | --seed given more than once",
                "--headless=yes --out a.csv | --headless does not take a value",
                "--headless --out a.csv --save-champion= | --save-champion needs a file name",
                "--watch champ.json --headless | --watch cannot be combined with --headless",
                "--demo --seed 3 | --demo cannot be combined with --seed",
                "--demo --watch champ.json | --watch cannot be combined with --demo",
                "--watch= | --watch needs a champion file",
                "--watch | Missing value for --watch",
                "--demo=yes | --demo does not take a value",
            })
    void rejectsInvalidArguments(String args, String expectedMessage) {
        String message = parseError(args.split(" "));
        assertTrue(
                message.contains(expectedMessage), () -> "'" + message + "' should mention '" + expectedMessage + "'");
    }

    @Test
    void parsesWatchAndDemo() throws UsageException {
        assertEquals(
                new CliParser.Watch(Path.of("champ.json")), CliParser.parse(new String[] {"--watch", "champ.json"}));
        assertEquals(new CliParser.Watch(null), CliParser.parse(new String[] {"--demo"}));
        assertNull(parseRun("--headless", "--out", "a.csv").saveChampion());
    }
}
