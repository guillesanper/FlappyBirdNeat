package com.neat.flappybirdneat.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Locale;

/**
 * Command-line front end: parses the arguments, runs a headless training and prints its summary.
 * Nothing on this path touches JavaFX.
 */
public final class Cli {

    public static final int EXIT_OK = 0;
    public static final int EXIT_RUNTIME_ERROR = 1;
    public static final int EXIT_USAGE = 2;

    private Cli() {}

    /** @return the process exit code */
    public static int run(String[] args, PrintStream out, PrintStream err) {
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

        CliOptions options = ((CliParser.Run) command).options();
        try {
            printSummary(HeadlessTraining.run(options), out);
            return EXIT_OK;
        } catch (IOException | UncheckedIOException e) {
            err.println("error: cannot write " + options.out() + ": " + e.getMessage());
            return EXIT_RUNTIME_ERROR;
        }
    }

    private static void printSummary(HeadlessTraining.Summary summary, PrintStream out) {
        CliOptions options = summary.options();
        out.println("Training summary");
        out.printf(Locale.ROOT, "  Engine:        %s%n", options.engine());
        out.printf(Locale.ROOT, "  Seed:          %d%n", options.seed());
        out.printf(
                Locale.ROOT,
                "  Generations:   %d of %d (population %d, max %d frames per generation)%n",
                summary.generationsRun(),
                options.generations(),
                options.population(),
                options.maxFrames());
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
    }
}
