package com.neat.flappybirdneat.view.main;

import com.neat.flappybirdneat.neat.Population;
import com.neat.flappybirdneat.simulation.FitnessCsvExporter;
import com.neat.flappybirdneat.simulation.SimulationController;
import com.neat.flappybirdneat.view.BenchmarkWindow;
import com.neat.flappybirdneat.view.Dialogs;
import com.neat.flappybirdneat.view.GeneticOperatorsConfigWindow;
import com.neat.flappybirdneat.view.StatisticsWindow;
import java.io.File;
import java.io.PrintWriter;
import javafx.animation.AnimationTimer;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

/**
 * Top of the statistics tab: live statistics, the headless training controls (engine, number of
 * generations, start/stop, progress) and the row of actions (reset, CSV export, best-agent
 * replay, operator configuration, advanced statistics, benchmark).
 */
final class ControlPanel {

    private static final Font LABEL_FONT = Font.font("System", FontWeight.BOLD, 14);
    private static final String NEAT = "NEAT";
    private static final String FIXED_MLP = "Fixed MLP";

    private final SimulationController controller;
    private final int populationSize;
    private final int canvasWidth;
    private final int canvasHeight;
    private final LiveViewSettings settings;
    private final StatisticsWindow statisticsWindow;
    private final MainWindowActions actions;

    private final Label speciesLabel = new Label("🧬 Especies: -");
    private final VBox view;

    ControlPanel(
            SimulationController controller,
            int populationSize,
            int canvasWidth,
            int canvasHeight,
            LiveViewSettings settings,
            StatisticsWindow statisticsWindow,
            MainWindowActions actions) {
        this.controller = controller;
        this.populationSize = populationSize;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.settings = settings;
        this.statisticsWindow = statisticsWindow;
        this.actions = actions;

        HBox topControls = new HBox(20, new VBox(8, createInfoPanel()), new VBox(10, createTrainingPanel()));
        view = new VBox(10, topControls, createActionsRow());
        refreshChartsWhileRunning();
    }

    Node getView() {
        return view;
    }

    /** Shows the current number of NEAT species (no-op in Fixed MLP mode, where the count is -1). */
    void updateSpeciesCount(int speciesCount) {
        if (speciesCount >= 0) {
            speciesLabel.setText("🧬 Especies: " + speciesCount);
        }
    }

    private VBox createInfoPanel() {
        Label statsTitle = new Label("📊 Estadísticas en Tiempo Real");
        statsTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        statsTitle.setTextFill(Color.DARKBLUE);

        Label genLabel = boldLabel();
        genLabel.textProperty()
                .bind(Bindings.concat(
                        "🔄 Generación: ",
                        controller.currentGenerationProperty().asString()));

        Label bestFitnessLabel = boldLabel();
        bestFitnessLabel
                .textProperty()
                .bind(Bindings.concat("🏆 Mejor Fitness: ", Bindings.format("%.2f", controller.bestFitnessProperty())));

        Label avgFitnessLabel = boldLabel();
        avgFitnessLabel
                .textProperty()
                .bind(Bindings.concat(
                        "📈 Fitness Promedio: ", Bindings.format("%.2f", controller.averageFitnessProperty())));

        Label aliveLabel = boldLabel();
        aliveLabel
                .textProperty()
                .bind(Bindings.concat(
                        "💚 Agentes Vivos: ", controller.aliveCountProperty().asString(), " / ", populationSize));

        speciesLabel.setFont(LABEL_FONT);
        showSpeciesLabel(false);

        Label statusLabel = boldLabel();
        statusLabel
                .textProperty()
                .bind(Bindings.when(controller.runningProperty())
                        .then("⚡ Estado: SIMULACIÓN RÁPIDA EN CURSO...")
                        .otherwise("⏸ Estado: Pausado"));
        controller
                .runningProperty()
                .addListener(
                        (obs, wasRunning, running) -> statusLabel.setTextFill(running ? Color.GREEN : Color.ORANGE));

        VBox infoPanel = new VBox(
                5, statsTitle, genLabel, bestFitnessLabel, avgFitnessLabel, aliveLabel, speciesLabel, statusLabel);
        infoPanel.setPadding(new Insets(10));
        infoPanel.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 5;");
        return infoPanel;
    }

    private static Label boldLabel() {
        Label label = new Label();
        label.setFont(LABEL_FONT);
        return label;
    }

    private void showSpeciesLabel(boolean visible) {
        speciesLabel.setVisible(visible);
        speciesLabel.setManaged(visible);
    }

    private VBox createTrainingPanel() {
        Label fastSimLabel = new Label("⚡ Simulación Rápida (Modo Headless)");
        fastSimLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        fastSimLabel.setTextFill(Color.DARKGREEN);

        Label fastSimDescription = new Label("Entrena rápidamente sin visualización");
        fastSimDescription.setFont(Font.font("System", 12));
        fastSimDescription.setTextFill(Color.GRAY);

        // Engine selector: fixed-topology MLP (configurable operators) vs NEAT
        Label modeLabel = new Label("Algoritmo:");
        modeLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        ComboBox<String> modeComboBox = new ComboBox<>();
        modeComboBox.getItems().addAll(FIXED_MLP, NEAT);
        modeComboBox.setValue(FIXED_MLP);
        modeComboBox.disableProperty().bind(controller.runningProperty());
        modeComboBox.setOnAction(e -> {
            boolean neatSelected = NEAT.equals(modeComboBox.getValue());
            controller.setMode(neatSelected ? SimulationController.Mode.NEAT : SimulationController.Mode.FIXED_MLP);
            showSpeciesLabel(neatSelected);
            controller.resetSimulation();
            actions.refreshStatistics();
        });
        HBox modeBox = new HBox(10, modeLabel, modeComboBox);
        modeBox.setAlignment(Pos.CENTER_LEFT);

        Label genToRunLabel = new Label("Generaciones:");
        genToRunLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        TextField genToRunField = new TextField("50");
        genToRunField.setPrefWidth(80);
        genToRunField.setStyle("-fx-font-size: 14px;");
        HBox genInputBox = new HBox(10, genToRunLabel, genToRunField);
        for (String preset : new String[] {"10", "50", "100", "500"}) {
            Button quickButton = new Button(preset);
            quickButton.setOnAction(e -> genToRunField.setText(preset));
            genInputBox.getChildren().add(quickButton);
        }
        genInputBox.setAlignment(Pos.CENTER_LEFT);

        Button runButton = new Button("▶ Iniciar Entrenamiento");
        runButton.setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        Button stopButton = new Button("⏹ Detener");
        stopButton.setStyle(
                "-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        stopButton.setDisable(true);
        controller.runningProperty().addListener((obs, wasRunning, running) -> {
            stopButton.setDisable(!running);
            runButton.setDisable(running);
        });
        HBox buttonBox = new HBox(10, runButton, stopButton);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(350);
        progressBar.setStyle("-fx-accent: #4CAF50;");
        Label progressLabel = new Label("Progreso:");
        progressLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        runButton.setOnAction(e -> startTraining(genToRunField.getText(), progressBar));
        stopButton.setOnAction(e -> {
            controller.stopSimulation();
            progressBar.progressProperty().unbind();
        });

        VBox fastSimPanel = new VBox(
                10, fastSimLabel, fastSimDescription, modeBox, genInputBox, buttonBox, progressLabel, progressBar);
        fastSimPanel.setPadding(new Insets(10));
        fastSimPanel.setStyle(
                "-fx-background-color: #e8f4f8; -fx-background-radius: 5; -fx-border-color: #4CAF50; -fx-border-radius: 5; -fx-border-width: 2;");
        return fastSimPanel;
    }

    private void startTraining(String generationsText, ProgressBar progressBar) {
        int generations;
        try {
            generations = Integer.parseInt(generationsText.trim());
        } catch (NumberFormatException ex) {
            new Alert(Alert.AlertType.ERROR, "Por favor, introduce un número válido de generaciones.").showAndWait();
            return;
        }
        if (generations <= 0) {
            return;
        }

        // Start from a fresh run (the operator configuration is kept by the controller)
        controller.resetSimulation();
        actions.refreshStatistics();

        progressBar.progressProperty().unbind();
        progressBar.setProgress(0);
        final int startGeneration = controller.currentGenerationProperty().getValue();

        controller.runFastSimulation(generations);

        progressBar
                .progressProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> {
                            int current = controller.currentGenerationProperty().getValue();
                            return Math.min(1.0, (double) (current - startGeneration) / generations);
                        },
                        controller.currentGenerationProperty()));

        // Once this run finishes, offer to replay its best generation (one-shot listener)
        controller.runningProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean wasRunning, Boolean running) {
                if (wasRunning && !running) {
                    actions.offerBestGenerationReplay();
                    controller.runningProperty().removeListener(this);
                }
            }
        });
    }

    /** While a simulation is running, refreshes the charts once per second. */
    private void refreshChartsWhileRunning() {
        controller.runningProperty().addListener((obs, wasRunning, running) -> {
            if (!running) {
                return;
            }
            new AnimationTimer() {
                private long lastUpdate = 0;

                @Override
                public void handle(long now) {
                    if (now - lastUpdate > 1_000_000_000) {
                        actions.refreshStatistics();
                        lastUpdate = now;
                        if (!controller.runningProperty().get()) {
                            stop();
                        }
                    }
                }
            }.start();
        });
    }

    private HBox createActionsRow() {
        Button resetButton = new Button("Reiniciar Simulación");
        resetButton.setOnAction(e -> {
            controller.resetSimulation();
            actions.refreshStatistics();
        });

        Button exportDataButton = new Button("Exportar Datos a CSV");
        exportDataButton.setOnAction(e -> exportSimulationData());

        Button playBestButton = new Button("▶ Ver Mejor Individuo");
        playBestButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        playBestButton.setOnAction(e -> playBestAgent());

        Button configOperatorsButton = new Button("⚙ Configurar Operadores Genéticos");
        configOperatorsButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        configOperatorsButton.setOnAction(e -> openOperatorsConfig());

        Button statsWindowButton = new Button("📊 Panel de Estadísticas Avanzado");
        statsWindowButton.setOnAction(e -> {
            statisticsWindow.update(controller);
            statisticsWindow.show();
        });

        Button benchmarkButton = new Button("🔬 Comparar Operadores (Benchmark)");
        benchmarkButton.setOnAction(e -> new BenchmarkWindow(populationSize, canvasWidth, canvasHeight).show());

        HBox actionsRow = new HBox(
                20,
                resetButton,
                exportDataButton,
                playBestButton,
                configOperatorsButton,
                statsWindowButton,
                benchmarkButton);
        actionsRow.setPadding(new Insets(10, 0, 10, 0));
        actionsRow.setAlignment(Pos.CENTER_LEFT);
        actionsRow.setStyle(
                "-fx-border-color: transparent transparent lightgray transparent; -fx-border-width: 0 0 1 0;");
        return actionsRow;
    }

    private void playBestAgent() {
        if (controller.getHistoryManager().getBestGeneration() == null) {
            Dialogs.show(
                    Alert.AlertType.WARNING,
                    "Sin datos",
                    "No hay datos disponibles",
                    "Ejecuta primero una simulación para encontrar el mejor individuo.");
            return;
        }
        controller.stopSimulation();

        double bestFitnessEver = controller.getHistoryManager().getBestFitnessEver();
        if (bestFitnessEver >= SimulationController.getOptimalFitnessThreshold()) {
            Dialogs.show(
                    Alert.AlertType.INFORMATION,
                    "Reproducir Mejor Individuo",
                    "🎯 ¡AGENTE ÓPTIMO ENCONTRADO! 🎯",
                    "Fitness alcanzado: " + String.format("%.2f", bestFitnessEver) + "\n\n"
                            + "¡Este agente ha alcanzado el umbral óptimo!\n"
                            + "Es probablemente el mejor agente posible.\n\n"
                            + "Cambia a la pestaña 'Simulación Visual' para verlo en acción.\n"
                            + "El agente aparecerá marcado en rojo con un borde dorado brillante.");
        } else {
            Dialogs.show(
                    Alert.AlertType.INFORMATION,
                    "Reproducir Mejor Individuo",
                    "Mejor fitness encontrado: " + String.format("%.2f", bestFitnessEver),
                    "Se reproducirá el mejor agente encontrado.\n\n"
                            + "Cambia a la pestaña 'Simulación Visual' para verlo en acción.\n"
                            + "El agente aparecerá marcado en rojo con un borde dorado.");
        }

        controller.playBestAgentOnly();
        actions.showSimulationTab();
        settings.setShowAllAgents(false); // Only the best agent is on screen
    }

    private void openOperatorsConfig() {
        if (!(controller.getPopulation() instanceof Population fixedPopulation)) {
            Dialogs.show(
                    Alert.AlertType.INFORMATION,
                    "No disponible en modo NEAT",
                    null,
                    "Los operadores genéticos configurables (cruce, selección, mutación, escalado) "
                            + "solo aplican en modo 'Fixed MLP'. En modo NEAT, la topología y los pesos evolucionan "
                            + "mediante las reglas propias de NEAT.");
            return;
        }
        // Make sure the live population carries the saved configuration before editing it
        controller.getOperatorsConfig().applyTo(fixedPopulation);
        new GeneticOperatorsConfigWindow(fixedPopulation, controller).show();
    }

    private void exportSimulationData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar datos de simulación");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        File file = fileChooser.showSaveDialog(null);
        if (file == null) {
            return;
        }
        try (PrintWriter writer = new PrintWriter(file)) {
            FitnessCsvExporter.write(controller, writer);
        } catch (Exception e) {
            Dialogs.show(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Error al exportar datos",
                    "Se produjo un error: " + e.getMessage());
            return;
        }
        Dialogs.show(
                Alert.AlertType.INFORMATION, "Exportación Completa", null, "Los datos se han exportado correctamente.");
    }
}
