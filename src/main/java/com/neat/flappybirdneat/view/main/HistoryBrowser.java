package com.neat.flappybirdneat.view.main;

import com.neat.flappybirdneat.history.GenerationData;
import com.neat.flappybirdneat.history.HistoryManager;
import com.neat.flappybirdneat.history.RunHistory;
import com.neat.flappybirdneat.simulation.SimulationController;
import com.neat.flappybirdneat.view.Dialogs;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Browser of the recorded runs (the current one and previous ones) and their generations, any of
 * which can be replayed in the simulation tab.
 */
final class HistoryBrowser {

    private final SimulationController controller;
    private final Runnable onGenerationReplay;
    private final ComboBox<String> runComboBox = new ComboBox<>();
    private final TableView<GenerationData> generationTable = new TableView<>();

    /**
     * @param onGenerationReplay shows the simulation tab once a generation replay has started
     */
    HistoryBrowser(SimulationController controller, Runnable onGenerationReplay) {
        this.controller = controller;
        this.onGenerationReplay = onGenerationReplay;

        runComboBox.setPromptText("Seleccionar ejecución");
        runComboBox.setPrefWidth(200);
        runComboBox.setOnAction(e -> showSelectedRun());

        generationTable.setPrefHeight(160);
        TableColumn<GenerationData, Integer> genNumberCol = new TableColumn<>("Gen");
        genNumberCol.setCellValueFactory(new PropertyValueFactory<>("generationNumber"));
        TableColumn<GenerationData, Double> bestFitnessCol = new TableColumn<>("Mejor Fitness");
        bestFitnessCol.setCellValueFactory(new PropertyValueFactory<>("bestFitness"));
        TableColumn<GenerationData, Integer> aliveCountCol = new TableColumn<>("Vivos");
        aliveCountCol.setCellValueFactory(new PropertyValueFactory<>("aliveCount"));
        generationTable.getColumns().addAll(List.of(genNumberCol, bestFitnessCol, aliveCountCol));

        refreshRuns();
    }

    ComboBox<String> getRunComboBox() {
        return runComboBox;
    }

    TableView<GenerationData> getGenerationTable() {
        return generationTable;
    }

    /** Reloads the list of runs (selecting the current one) and the generations of the selection. */
    void refresh() {
        refreshRuns();
        showSelectedRun();
    }

    private void refreshRuns() {
        runComboBox.getItems().clear();
        List<RunHistory> runs = controller.getHistoryManager().getRunHistories();
        for (int i = 0; i < runs.size(); i++) {
            double bestFitness = runs.get(i).getGenerationDataList().stream()
                    .mapToDouble(GenerationData::getBestFitness)
                    .max()
                    .orElse(0.0);
            runComboBox.getItems().add("Ejecución " + (i + 1) + " (Mejor: " + String.format("%.2f", bestFitness) + ")");
        }
        runComboBox.getItems().add("Ejecución actual");
        runComboBox.getSelectionModel().select(runComboBox.getItems().size() - 1);
    }

    /** @return the run selected in the combo box (the last entry is the current run), or null. */
    private RunHistory selectedRun() {
        int selectedIndex = runComboBox.getSelectionModel().getSelectedIndex();
        HistoryManager manager = controller.getHistoryManager();
        List<RunHistory> runs = manager.getRunHistories();
        if (selectedIndex == runComboBox.getItems().size() - 1) {
            return manager.getCurrentRun();
        } else if (selectedIndex >= 0 && selectedIndex < runs.size()) {
            return runs.get(selectedIndex);
        }
        return null;
    }

    private void showSelectedRun() {
        RunHistory run = selectedRun();
        if (run == null) {
            return;
        }
        List<GenerationData> generations = run.getGenerationDataList();
        for (int i = 0; i < generations.size(); i++) {
            // Histories saved before the number was recorded lack it
            if (generations.get(i).getGenerationNumber() == 0) {
                generations.get(i).setGenerationNumber(i + 1);
            }
        }
        generationTable.setItems(FXCollections.observableArrayList(generations));
    }

    /**
     * After a training run: finds its best generation and, if the user accepts, replays it in the
     * simulation tab.
     */
    void offerBestGenerationReplay() {
        RunHistory currentRun = controller.getHistoryManager().getCurrentRun();
        if (currentRun == null || currentRun.getGenerationDataList().isEmpty()) {
            return;
        }
        List<GenerationData> generations = currentRun.getGenerationDataList();
        double bestFitness = -1;
        int bestGenerationIndex = -1;
        for (int i = 0; i < generations.size(); i++) {
            if (generations.get(i).getBestFitness() > bestFitness) {
                bestFitness = generations.get(i).getBestFitness();
                bestGenerationIndex = i;
            }
        }
        if (bestGenerationIndex < 0) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mejor Individuo Encontrado");
        alert.setHeaderText(null);
        alert.setContentText("Se encontró el mejor individuo en la generación " + (bestGenerationIndex + 1)
                + " con fitness " + String.format("%.2f", bestFitness)
                + "\n\n¿Deseas ver la simulación de este individuo?");
        ButtonType watchButton = new ButtonType("Ver Simulación");
        ButtonType cancelButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(watchButton, cancelButton);

        final int bestIndex = bestGenerationIndex;
        alert.showAndWait().ifPresent(response -> {
            if (response == watchButton) {
                runComboBox.getSelectionModel().select(runComboBox.getItems().size() - 1);
                showSelectedRun();
                if (bestIndex < generationTable.getItems().size()) {
                    generationTable.getSelectionModel().select(bestIndex);
                    generationTable.scrollTo(bestIndex);
                    playSelectedGeneration();
                }
            }
        });
    }

    /** Replays the generation selected in the table in the simulation tab, in a loop. */
    void playSelectedGeneration() {
        int selectedGenIndex = generationTable.getSelectionModel().getSelectedIndex();
        if (selectedGenIndex < 0) {
            return;
        }
        RunHistory run = selectedRun();
        if (run == null || selectedGenIndex >= run.getGenerationDataList().size()) {
            return;
        }
        if (controller.isFastMode()) {
            Dialogs.show(
                    Alert.AlertType.WARNING,
                    "Entrenamiento en curso",
                    null,
                    "Detén el entrenamiento antes de reproducir una generación.");
            return;
        }
        controller.playGeneration(run.getGenerationDataList().get(selectedGenIndex));
        onGenerationReplay.run();
    }
}
