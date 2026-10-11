package com.neat.flappybirdneat;

import com.neat.flappybirdneat.champion.Champion;
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

    /** Opens the UI replaying {@code champion}; returns when the UI is closed. */
    static void watch(Champion champion) {
        FlappyBirdNEAT.watchOnStart(champion);
        Application.launch(FlappyBirdNEAT.class);
    }
}
