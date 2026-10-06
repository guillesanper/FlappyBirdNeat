package com.neat.flappybirdneat;

import javafx.application.Application;

/**
 * Starts the JavaFX UI. Kept apart from {@link Main} so that the command-line path never loads a
 * JavaFX class.
 */
final class UiLauncher {

    private UiLauncher() {}

    static void launch(String[] args) {
        // The application class is passed explicitly: Main does not extend Application, so the
        // caller-detecting Application.launch(String...) would not find it in the fat jar.
        Application.launch(FlappyBirdNEAT.class, args);
    }
}
