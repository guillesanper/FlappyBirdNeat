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
import javafx.scene.layout.VBox;

/**
 * "Simulación Visual" tab: the game canvas, the live view controls (speed, which agents to show,
 * network window) and the run history browser. The game fills the space the sidebar leaves free:
 * the canvas is scaled uniformly (never stretched) and centred, while the game itself keeps
 * drawing in its logical coordinates.
 */
final class SimulationPanel {

    private static final double SIDEBAR_WIDTH = 300;

    private final SimulationController controller;
    private final LiveViewSettings settings;
    private final double worldWidth;
    private final double worldHeight;
    private final Canvas canvas;
    private final GameRenderer renderer;
    private final HBox view;
    private double scale = 1;

    SimulationPanel(
            SimulationController controller,
            int canvasWidth,
            int canvasHeight,
            LiveViewSettings settings,
            NeuralNetworkWindow networkWindow,
            HistoryBrowser historyBrowser) {
        this.controller = controller;
        this.settings = settings;
        this.worldWidth = canvasWidth;
        this.worldHeight = canvasHeight;
        this.canvas = new Canvas(canvasWidth, canvasHeight);
        this.renderer = new GameRenderer(canvasWidth, canvasHeight);

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

        Button pauseButton = new Button("⏸ Pausar");
        pauseButton.setOnAction(e -> {
            settings.setPaused(!settings.isPaused());
            pauseButton.setText(settings.isPaused() ? "▶ Continuar" : "⏸ Pausar");
            draw();
        });

        // Ends any replay (generation, best agent or champion) and puts the training back
        Button exitReplayButton = new Button("⏹ Volver al entrenamiento");
        exitReplayButton.visibleProperty().bind(controller.replayModeProperty());
        exitReplayButton.managedProperty().bind(exitReplayButton.visibleProperty());
        exitReplayButton.setOnAction(e -> {
            controller.exitReplayMode();
            draw();
        });

        // Lo que se está mostrando: un campeón, una generación grabada o el entrenamiento en vivo
        // (la generación y el mejor fitness del entrenamiento no tienen relación con un replay)
        Label infoLabel = new Label();
        infoLabel
                .textProperty()
                .bind(Bindings.createStringBinding(
                        this::describeShownAgents,
                        controller.watchedChampionProperty(),
                        controller.replayedGenerationProperty(),
                        controller.aliveCountProperty(),
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
                pauseButton,
                exitReplayButton,
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

        // The sidebar keeps its width and scrolls when the window is too short for it
        ScrollPane sidebar = new ScrollPane(controlsPanel);
        sidebar.setFitToWidth(true);
        sidebar.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebar.setMinWidth(SIDEBAR_WIDTH);
        sidebar.setPrefWidth(SIDEBAR_WIDTH);
        sidebar.setMaxWidth(SIDEBAR_WIDTH);

        // The game takes the rest; the canvas is resized to fit it and centred
        Pane gameArea = new Pane(canvas);
        gameArea.setPrefSize(canvasWidth, canvasHeight);
        gameArea.setMinSize(0, 0);
        gameArea.widthProperty().addListener((obs, oldWidth, newWidth) -> fitCanvas(gameArea));
        gameArea.heightProperty().addListener((obs, oldHeight, newHeight) -> fitCanvas(gameArea));
        HBox.setHgrow(gameArea, Priority.ALWAYS);

        view = new HBox(15, gameArea, sidebar);
        view.setPadding(new Insets(15));
    }

    private String describeShownAgents() {
        if (controller.watchedChampionProperty().get() != null) {
            return ChampionSummary.describe(controller.watchedChampionProperty().get());
        }
        if (controller.replayedGenerationProperty().get() != null) {
            return GenerationReplaySummary.describe(
                    controller.replayedGenerationProperty().get(),
                    controller.aliveCountProperty().get(),
                    controller.getPopulation().getAgents().length,
                    controller.isReplayPipesOriginal());
        }
        return String.format(
                "Generación: %d%nMejor Fitness: %.2f",
                controller.currentGenerationProperty().get(),
                controller.bestFitnessProperty().get());
    }

    /**
     * Largest uniform scale at which a {@code worldWidth} × {@code worldHeight} game fits in the
     * available space (0 when there is none).
     */
    static double fitScale(double availableWidth, double availableHeight, double worldWidth, double worldHeight) {
        double fit = Math.min(availableWidth / worldWidth, availableHeight / worldHeight);
        return fit > 0 ? fit : 0;
    }

    private void fitCanvas(Pane gameArea) {
        scale = fitScale(gameArea.getWidth(), gameArea.getHeight(), worldWidth, worldHeight);
        canvas.setWidth(worldWidth * scale);
        canvas.setHeight(worldHeight * scale);
        canvas.relocate((gameArea.getWidth() - canvas.getWidth()) / 2, (gameArea.getHeight() - canvas.getHeight()) / 2);
        draw();
    }

    Node getView() {
        return view;
    }

    /** Draws the current state of the live game, scaled to the canvas. */
    void draw() {
        EvolvingPopulation population = controller.getPopulation();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.save();
        gc.setTransform(scale, 0, 0, scale, 0, 0);
        renderer.render(
                gc,
                controller.getGame(),
                population.getAgents(),
                controller.getHighlightedAgent(),
                GameRenderer.Options.liveSimulation(
                        settings.isShowAllAgents(),
                        controller.isReplayMode(),
                        population.getAgents().length == 1,
                        controller.getReplayBanner()));
        if (settings.isPaused()) {
            renderer.renderPausedOverlay(gc);
        }
        gc.restore();
    }
}
