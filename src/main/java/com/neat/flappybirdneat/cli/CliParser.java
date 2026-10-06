package com.neat.flappybirdneat.cli;

import com.neat.flappybirdneat.neat.crossover.CrossoverFactory;
import com.neat.flappybirdneat.neat.mutation.MutationFactory;
import com.neat.flappybirdneat.neat.scaling.ScalingFactory;
import com.neat.flappybirdneat.neat.selection.SelectionFactory;
import com.neat.flappybirdneat.simulation.EngineType;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Parses the headless command line. Options take their value as the next argument
 * ({@code --seed 42}) or inline ({@code --seed=42}); every problem is reported as a
 * {@link UsageException} with a message meant for the user.
 */
public final class CliParser {

    static final int DEFAULT_GENERATIONS = 100;
    static final int DEFAULT_POPULATION = 50;
    static final int DEFAULT_MAX_FRAMES = 20_000;

    private static final Set<String> FLAGS = Set.of("--headless", "--stop-on-solve", "--help", "-h");
    private static final Set<String> OPTIONS = Set.of(
            "--engine",
            "--seed",
            "--generations",
            "--population",
            "--max-frames",
            "--selection",
            "--crossover",
            "--mutation",
            "--scaling",
            "--out");
    private static final Set<String> GA_OPTIONS = Set.of("--selection", "--crossover", "--mutation", "--scaling");

    static final String USAGE = """
            Usage: java -jar FlappyBirdNEAT.jar --headless --out FILE [options]
                   java -jar FlappyBirdNEAT.jar            (no arguments: open the JavaFX UI)

            Trains without a UI, writes one CSV row per generation to FILE and prints a summary.

            Options:
              --headless            Run without the UI (required to train from the command line)
              --out FILE            CSV file to write (required)
              --engine neat|ga      Evolution engine (default: neat)
              --seed N              Global seed (default: random; printed in the summary)
              --generations N       Generations to run (default: %d)
              --population N        Agents per generation (default: %d)
              --max-frames N        Frame cap per generation; an agent that reaches it
                                    solves the game (default: %d)
              --stop-on-solve       Stop after the first solved generation
              -h, --help            Show this help and exit

            GA operators (only with --engine ga):
              --selection KEY       roulette (default), deterministic_tournament,
                                    probabilistic_tournament, ranking, truncation,
                                    stochastic_universal, remainder
              --crossover KEY       uniform (default), single_point, arithmetic
              --mutation KEY        gaussian (default), uniform, non_uniform
              --scaling KEY         none (default), linear, sigma, boltzmann

            Exit codes: 0 success, 1 runtime error (e.g. FILE cannot be written), 2 invalid arguments.
            """.formatted(DEFAULT_GENERATIONS, DEFAULT_POPULATION, DEFAULT_MAX_FRAMES);

    private CliParser() {}

    /** Result of parsing: either a request for help or a run with validated options. */
    public sealed interface Command permits Help, Run {}

    public record Help() implements Command {}

    public record Run(CliOptions options) implements Command {}

    public static Command parse(String[] args) throws UsageException {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            String name = arg;
            String value = null;
            int equals = arg.indexOf('=');
            if (arg.startsWith("--") && equals > 0) {
                name = arg.substring(0, equals);
                value = arg.substring(equals + 1);
            }

            if (FLAGS.contains(name)) {
                if (value != null) throw new UsageException(name + " does not take a value");
            } else if (OPTIONS.contains(name)) {
                if (value == null) {
                    if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                        throw new UsageException("Missing value for " + name);
                    }
                    value = args[++i];
                }
            } else if (arg.startsWith("-")) {
                throw new UsageException("Unknown option: " + arg);
            } else {
                throw new UsageException("Unexpected argument: " + arg);
            }

            if (name.equals("-h")) name = "--help";
            if (values.containsKey(name)) throw new UsageException(name + " given more than once");
            values.put(name, value == null ? "" : value);
        }

        if (values.containsKey("--help")) return new Help();
        if (!values.containsKey("--headless")) {
            throw new UsageException("Command-line arguments require --headless (run with no arguments for the UI)");
        }
        if (!values.containsKey("--out")) throw new UsageException("Missing required option --out FILE");

        EngineType engine = EngineType.NEAT;
        if (values.containsKey("--engine")) {
            try {
                engine = EngineType.fromName(values.get("--engine"));
            } catch (IllegalArgumentException e) {
                throw new UsageException(e.getMessage());
            }
        }
        if (engine != EngineType.GA) {
            for (String gaOption : GA_OPTIONS) {
                if (values.containsKey(gaOption)) {
                    throw new UsageException(gaOption + " only applies to --engine ga");
                }
            }
        }

        long seed = values.containsKey("--seed")
                ? parseLong("--seed", values.get("--seed"))
                : ThreadLocalRandom.current().nextLong();
        int generations = positiveInt(values, "--generations", DEFAULT_GENERATIONS, 1);
        int population = positiveInt(values, "--population", DEFAULT_POPULATION, 2);
        int maxFrames = positiveInt(values, "--max-frames", DEFAULT_MAX_FRAMES, 1);

        String selection = operatorKey(values, "--selection", SelectionFactory.getInstance()::getSelectionStrategy);
        String crossover = operatorKey(values, "--crossover", CrossoverFactory.getInstance()::getCrossoverStrategy);
        String mutation = operatorKey(values, "--mutation", MutationFactory.getInstance()::getMutationStrategy);
        String scaling = operatorKey(values, "--scaling", ScalingFactory.getInstance()::getScalingStrategy);

        String out = values.get("--out");
        if (out.isBlank()) throw new UsageException("--out needs a file name");

        return new Run(new CliOptions(
                engine,
                seed,
                generations,
                population,
                maxFrames,
                values.containsKey("--stop-on-solve"),
                selection,
                crossover,
                mutation,
                scaling,
                Path.of(out)));
    }

    private static long parseLong(String name, String value) throws UsageException {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new UsageException(name + " expects an integer, got '" + value + "'");
        }
    }

    private static int positiveInt(Map<String, String> values, String name, int defaultValue, int min)
            throws UsageException {
        if (!values.containsKey(name)) return defaultValue;
        String value = values.get(name);
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new UsageException(name + " expects an integer, got '" + value + "'");
        }
        if (parsed < min) throw new UsageException(name + " must be at least " + min + ", got " + parsed);
        return parsed;
    }

    /** Validates an operator key against its factory (which accepts Spanish and English keys). */
    private static String operatorKey(Map<String, String> values, String name, Consumer<String> factory)
            throws UsageException {
        if (!values.containsKey(name)) return null;
        String key = values.get(name);
        try {
            factory.accept(key);
        } catch (IllegalArgumentException e) {
            throw new UsageException("Unknown value for " + name + ": '" + key + "' (see --help)");
        }
        return key;
    }
}
