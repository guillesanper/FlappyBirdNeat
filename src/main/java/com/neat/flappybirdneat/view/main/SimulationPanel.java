package com.neat.flappybirdneat.view.main;

import com.neat.flappybirdneat.neat.EvolvingPopulation;
import com.neat.flappybirdneat.simulation.SimulationController;
import com.neat.flappybirdneat.view.GameRenderer;
import com.neat.flappybirdneat.view.NeuralNetworkWindow;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * "Simulación Visual" tab: the game canvas, the live view controls (speed, which agents to show,
 * network window) and the run history browser.
 */
final class SimulationPanel {

    private final SimulationController controller;
    private final LiveViewSettings settings;
    private final Canvas canvas;
    private final GameRenderer renderer;
    private final HBox view;
    // El juego se dibuja siempre en coordenadas lógicas (canvasWidth x canvasHeight) y se escala
    // para llenar el espacio disponible sin deformarse
    private final double logicalWidth;
    private final double logicalHeight;
    private double scale = 1.0;

    SimulationPanel(
            SimulationController controller,
            int canvasWidth,
            int canvasHeight,
            LiveViewSettings settings,
            NeuralNetworkWindow networkWindow,
            HistoryBrowser historyBrowser) {
        this.controller = controller;
        this.settings = settings;
        this.canvas = new Canvas(canvasWidth, canvasHeight);
        this.renderer = new GameRenderer(canvasWidth, canvasHeight);
        this.logicalWidth = canvasWidth;
        this.logicalHeight = canvasHeight;

        Label speedLabel = new Label("Velocidad de Simulación:");
        Slider speedSlider = new Slider(1, 10, 1);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(1);
        speedSlider.setMinorTickCount(0);
        speedSlider.setBlockIncrement(1);
        speedSlider.setSnapToTicks(true);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> settings.setGameSpeed(newVal.intValue()));
        VBox speedBox = new VBox(5, speedLabel, speedSlider);

        CheckBox showAllCheckbox = new CheckBox("Mostrar todos los agentes");
        showAllCheckbox.setSelected(settings.isShowAllAgents());
        showAllCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> settings.setShowAllAgents(newVal));

        // The window can also be opened by the champion viewer: the label follows its state
        Button showNetworkButton = new Button();
        showNetworkButton
                .textProperty()
                .bind(Bindings.when(networkWindow.showingProperty())
                        .then("Ocultar Red Neuronal")
                        .otherwise("Mostrar Red Neuronal"));
        showNetworkButton.setOnAction(e -> {
            if (networkWindow.isShowing()) {
                networkWindow.close();
            } else {
                networkWindow.show();
            }
        });

        // Redraws the canvas without touching the population
        Button resetViewButton = new Button("Reiniciar Vista");
        resetViewButton.setOnAction(e -> draw());

        Button playSelectedGenButton = new Button("Reproducir generación seleccionada");
        playSelectedGenButton.setOnAction(e -> historyBrowser.playSelectedGeneration());

        // Mientras se ve un campeón, sus datos; si no, la generación y el mejor fitness del
        // entrenamiento en vivo (antes se mostraban también con un campeón, sin relación con él)
        Label infoLabel = new Label();
        infoLabel
                .textProperty()
                .bind(Bindings.createStringBinding(
                        () -> controller.watchedChampionProperty().get() != null
                                ? ChampionSummary.describe(
                                        controller.watchedChampionProperty().get())
                                : String.format(
                                        "Generación: %d%nMejor Fitness: %.2f",
                                        controller.currentGenerationProperty().get(),
                                        controller.bestFitnessProperty().get()),
                        controller.watchedChampionProperty(),
                        controller.currentGenerationProperty(),
                        controller.bestFitnessProperty()));

        Button toggleAgentsButton = new Button("Alternar vista (todos/mejor)");
        toggleAgentsButton.setOnAction(e -> {
            settings.setShowAllAgents(!settings.isShowAllAgents());
            toggleAgentsButton.setText(
                    settings.isShowAllAgents() ? "Mostrar solo el mejor" : "Mostrar todos los agentes");
        });

        VBox controlsPanel = new VBox(
                10,
                speedBox,
                showAllCheckbox,
                showNetworkButton,
                resetViewButton,
                new Separator(),
                new Label("Historial de Ejecuciones:"),
                historyBrowser.getRunComboBox(),
                historyBrowser.getGenerationTable(),
                playSelectedGenButton,
                infoLabel,
                toggleAgentsButton);
        controlsPanel.setPadding(new Insets(10));

        // Zona del juego: crece con la ventana; el canvas se escala y se centra en ella. Su tamaño
        // preferido es el lógico, así que la ventana abre con el juego a escala 1.
        Pane gameArea = new Pane(canvas);
        gameArea.setMinSize(0, 0);
        gameArea.setPrefSize(logicalWidth, logicalHeight);
        gameArea.widthProperty().addListener((obs, oldVal, newVal) -> fitCanvas(gameArea));
        gameArea.heightProperty().addListener((obs, oldVal, newVal) -> fitCanvas(gameArea));
        HBox.setHgrow(gameArea, Priority.ALWAYS);

        // Barra lateral de ancho fijo; con la ventana baja se desplaza en vez de cortarse
        ScrollPane sidebar = new ScrollPane(controlsPanel);
        sidebar.setFitToWidth(true);
        sidebar.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebar.setMinWidth(Region.USE_PREF_SIZE);
        sidebar.setStyle("-fx-background-color: transparent;");

        view = new HBox(15, gameArea, sidebar);
        view.setPadding(new Insets(15));
    }

    /**
     * Escala que hace caber el juego de tamaño lógico en el espacio disponible sin deformarlo.
     *
     * @return 0 si no hay espacio
     */
    static double fitScale(double availableWidth, double availableHeight, double logicalWidth, double logicalHeight) {
        if (availableWidth <= 0 || availableHeight <= 0) return 0;
        return Math.min(availableWidth / logicalWidth, availableHeight / logicalHeight);
    }

    private void fitCanvas(Pane gameArea) {
        scale = fitScale(gameArea.getWidth(), gameArea.getHeight(), logicalWidth, logicalHeight);
        canvas.setWidth(logicalWidth * scale);
        canvas.setHeight(logicalHeight * scale);
        canvas.relocate((gameArea.getWidth() - canvas.getWidth()) / 2, (gameArea.getHeight() - canvas.getHeight()) / 2);
        draw();
    }

    Node getView() {
        return view;
    }

    /** Draws the current state of the live game. */
    void draw() {
        if (scale <= 0) return;
        EvolvingPopulation population = controller.getPopulation();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.save();
        gc.setTransform(scale, 0, 0, scale, 0, 0);
        renderer.render(
                gc,
                controller.getGame(),
                population.getAgents(),
                Leader.of(population.getAgents()),
                GameRenderer.Options.liveSimulation(
                        settings.isShowAllAgents(),
                        controller.isReplayMode(),
                        population.getAgents().length == 1,
                        controller.getReplayBanner()));
        gc.restore();
    }
}
