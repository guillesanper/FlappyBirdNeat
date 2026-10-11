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

    private static final Set<String> FLAGS = Set.of("--headless", "--stop-on-solve", "--demo", "--help", "-h");
    private static final Set<String> OPTIONS = Set.of(
            "--engine",
            "--seed",
            "--generations",
            "--population",
            "--max-frames",
            "--threads",
            "--selection",
            "--crossover",
            "--mutation",
            "--scaling",
            "--out",
            "--save-champion",
            "--watch");
    private static final Set<String> GA_OPTIONS = Set.of("--selection", "--crossover", "--mutation", "--scaling");

    static final String USAGE =
            """
            Usage: java -jar FlappyBirdNEAT.jar --headless --out FILE [options]
                   java -jar FlappyBirdNEAT.jar --watch CHAMPION.json
                   java -jar FlappyBirdNEAT.jar --demo
                   java -jar FlappyBirdNEAT.jar            (no arguments: open the JavaFX UI)

            Trains without a UI, writes one CSV row per generation to FILE and prints a summary.

            Options:
              --headless            Run without the UI (required to train from the command line)
              --out FILE            CSV file to write (required)
              --save-champion FILE  Also save the best agent of the run as a champion JSON file
              --engine neat|ga      Evolution engine (default: neat)
              --seed N              Global seed (default: random; printed in the summary)
              --generations N       Generations to run (default: %d)
              --population N        Agents per generation (default: %d)
              --max-frames N        Frame cap per generation; an agent that reaches it
                                    solves the game (default: %d)
              --threads N           Threads that evaluate each generation's agents
                                    (default: available cores, %d here)
              --stop-on-solve       Stop after the first solved generation
              -h, --help            Show this help and exit

            Watching a champion (opens the UI, takes no other options):
              --watch FILE          Replay the champion saved in FILE in a loop, with its network
              --demo                Replay the NEAT champion bundled with the application


            GA operators (only with --engine ga):
              --selection KEY       roulette (default), deterministic_tournament,
                                    probabilistic_tournament, ranking, truncation,
                                    stochastic_universal, remainder
              --crossover KEY       uniform (default), single_point, arithmetic
              --mutation KEY        gaussian (default), uniform, non_uniform
              --scaling KEY         none (default), linear, sigma, boltzmann

            Exit codes: 0 success, 1 runtime error (e.g. FILE cannot be written or a champion
            cannot be read), 2 invalid arguments.
            """.formatted(DEFAULT_GENERATIONS, DEFAULT_POPULATION, DEFAULT_MAX_FRAMES, defaultThreads());

    private CliParser() {}

    /** Result of parsing: a request for help, a headless run with validated options or a champion to watch. */
    public sealed interface Command permits Help, Run, Watch {}

    public record Help() implements Command {}

    public record Run(CliOptions options) implements Command {}

    /** @param champion the champion file to watch, or null for the bundled NEAT champion ({@code --demo}) */
    public record Watch(Path champion) implements Command {}

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
        if (values.containsKey("--watch") || values.containsKey("--demo")) return watch(values);
        if (!values.containsKey("--headless")) {
            throw new UsageException(
                    "Command-line arguments require --headless, --watch or --demo (run with no arguments for the UI)");
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
        int threads = positiveInt(values, "--threads", defaultThreads(), 1);

        String selection = operatorKey(values, "--selection", SelectionFactory.getInstance()::getSelectionStrategy);
        String crossover = operatorKey(values, "--crossover", CrossoverFactory.getInstance()::getCrossoverStrategy);
        String mutation = operatorKey(values, "--mutation", MutationFactory.getInstance()::getMutationStrategy);
        String scaling = operatorKey(values, "--scaling", ScalingFactory.getInstance()::getScalingStrategy);

        String out = values.get("--out");
        if (out.isBlank()) throw new UsageException("--out needs a file name");
        String saveChampion = values.get("--save-champion");
        if (saveChampion != null && saveChampion.isBlank()) {
            throw new UsageException("--save-champion needs a file name");
        }

        return new Run(new CliOptions(
                engine,
                seed,
                generations,
                population,
                maxFrames,
                threads,
                values.containsKey("--stop-on-solve"),
                selection,
                crossover,
                mutation,
                scaling,
                Path.of(out),
                saveChampion == null ? null : Path.of(saveChampion)));
    }

    private static Watch watch(Map<String, String> values) throws UsageException {
        String mode = values.containsKey("--watch") ? "--watch" : "--demo";
        for (String name : values.keySet()) {
            if (!name.equals(mode)) throw new UsageException(mode + " cannot be combined with " + name);
        }
        if (mode.equals("--demo")) return new Watch(null);
        String file = values.get("--watch");
        if (file.isBlank()) throw new UsageException("--watch needs a champion file");
        return new Watch(Path.of(file));
    }

    static int defaultThreads() {
        return Runtime.getRuntime().availableProcessors();
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
