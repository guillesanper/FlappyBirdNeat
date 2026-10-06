package com.neat.flappybirdneat;

import com.neat.flappybirdneat.cli.Cli;

/**
 * Entry point for the shaded (fat) jar. With no arguments it opens the JavaFX UI; with arguments
 * it runs the command line (headless training, --help). The choice is made before any JavaFX class
 * is loaded, which is also why this class must not extend {@code Application}: that lets the JavaFX
 * runtime be loaded from the classpath in the fat jar.
 */
public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            UiLauncher.launch(args);
        } else {
            System.exit(Cli.run(args, System.out, System.err));
        }
    }
}
