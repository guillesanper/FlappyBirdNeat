package com.neat.flappybirdneat.cli;

import com.neat.flappybirdneat.champion.Champion;
import com.neat.flappybirdneat.champion.ChampionFile;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Command-line front end: parses the arguments, runs a headless training and prints its summary,
 * or loads a champion and hands it to a {@link ChampionViewer}. Nothing in this class touches
 * JavaFX: the viewer that opens the UI is supplied by the launcher.
 */
public final class Cli {

    public static final int EXIT_OK = 0;
    public static final int EXIT_RUNTIME_ERROR = 1;
    public static final int EXIT_USAGE = 2;

    private Cli() {}

    /** Shows a champion; returns once the viewer is closed. */
    @FunctionalInterface
    public interface ChampionViewer {
        void show(Champion champion);
    }

    /** Runs without a viewer: {@code --watch} and {@code --demo} then fail with a runtime error. */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        return run(args, out, err, champion -> {
            throw new IllegalStateException("no user interface available to watch a champion");
        });
    }

    /** @return the process exit code */
    public static int run(String[] args, PrintStream out, PrintStream err, ChampionViewer viewer) {
        CliParser.Command command;
        try {
            command = CliParser.parse(args);
        } catch (UsageException e) {
            err.println("error: " + e.getMessage());
            err.println("Run with --help for usage.");
            return EXIT_USAGE;
        }

        if (command instanceof CliParser.Help) {
            out.print(CliParser.USAGE);
            return EXIT_OK;
        }

        if (command instanceof CliParser.Watch watch) {
            return watch(watch.champion(), err, viewer);
        }

        CliOptions options = ((CliParser.Run) command).options();
        HeadlessTraining.Summary summary;
        try {
            summary = HeadlessTraining.run(options);
        } catch (IOException | UncheckedIOException e) {
            err.println("error: cannot write " + options.out() + ": " + e.getMessage());
            return EXIT_RUNTIME_ERROR;
        }
        if (options.saveChampion() != null) {
            try {
                ChampionFile.save(summary.champion(), options.saveChampion());
            } catch (IOException | UncheckedIOException e) {
                err.println("error: cannot write " + options.saveChampion() + ": " + e.getMessage());
                return EXIT_RUNTIME_ERROR;
            }
        }
        printSummary(summary, out);
        return EXIT_OK;
    }

    /** Loads the champion (null: the bundled NEAT one) and shows it; a bad file is a runtime error. */
    private static int watch(Path file, PrintStream err, ChampionViewer viewer) {
        Champion champion;
        try {
            champion = file == null ? ChampionFile.loadBundled(ChampionFile.BUNDLED_NEAT) : ChampionFile.load(file);
        } catch (NoSuchFileException e) {
            err.println("error: cannot read " + file + ": no such file");
            return EXIT_RUNTIME_ERROR;
        } catch (IOException | RuntimeException e) {
            err.println("error: cannot load champion " + (file == null ? "(bundled)" : file) + ": " + e.getMessage());
            return EXIT_RUNTIME_ERROR;
        }
        try {
            viewer.show(champion);
        } catch (RuntimeException e) {
            err.println("error: cannot show the champion: " + e.getMessage());
            return EXIT_RUNTIME_ERROR;
        }
        return EXIT_OK;
    }

    private static void printSummary(HeadlessTraining.Summary summary, PrintStream out) {
        CliOptions options = summary.options();
        out.println("Training summary");
        out.printf(Locale.ROOT, "  Engine:        %s%n", options.engine());
        out.printf(Locale.ROOT, "  Seed:          %d%n", options.seed());
        out.printf(
                Locale.ROOT,
                "  Generations:   %d of %d (population %d, max %d frames per generation, %d threads)%n",
                summary.generationsRun(),
                options.generations(),
                options.population(),
                options.maxFrames(),
                options.threads());
        out.printf(
                Locale.ROOT,
                "  Best fitness:  %.0f (generation %d)%n",
                summary.bestFitness(),
                summary.bestGeneration());
        out.printf(
                Locale.ROOT,
                "  Solved:        %s%n",
                summary.solvedGeneration() > 0
                        ? "yes, at generation " + summary.solvedGeneration()
                        : "no (no agent reached " + options.maxFrames() + " frames)");
        out.printf(Locale.ROOT, "  Total time:    %.1f s%n", summary.totalMillis() / 1000.0);
        out.printf(Locale.ROOT, "  CSV:           %s%n", options.out());
        if (options.saveChampion() != null) {
            out.printf(
                    Locale.ROOT,
                    "  Champion:      %s (generation %d, fitness %.0f)%n",
                    options.saveChampion(),
                    summary.champion().metadata().generation(),
                    summary.champion().metadata().fitness());
        }
    }
}
