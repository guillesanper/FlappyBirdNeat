package com.neat.flappybirdneat.view;

import javafx.scene.control.Alert;

/** Small helpers for the modal message dialogs used across the UI. */
public final class Dialogs {

    private Dialogs() {}

    /** Shows a modal alert and waits for it to be closed. A {@code null} header hides the header area. */
    public static void show(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
