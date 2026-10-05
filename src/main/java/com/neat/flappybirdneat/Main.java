package com.neat.flappybirdneat;

import javafx.application.Application;

/**
 * Entry point for the shaded (fat) jar. It must not extend {@link Application}
 * so the JavaFX runtime can be loaded from the classpath, which is why the
 * application class is passed explicitly instead of relying on caller detection.
 */
public class Main {
    public static void main(String[] args) {
        Application.launch(FlappyBirdNEAT.class, args);
    }
}
