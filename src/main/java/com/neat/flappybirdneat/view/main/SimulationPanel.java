package com.neat.flappybirdneat.view.main;

import com.neat.flappybirdneat.neat.EvolvingPopulation;
import com.neat.flappybirdneat.simulation.SimulationController;
import com.neat.flappybirdneat.view.GameRenderer;
import com.neat.flappybirdneat.view.NeuralNetworkWindow;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
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
    private final VBox view;

    SimulationPanel(SimulationController controller, int canvasWidth, int canvasHeight, LiveViewSettings settings,
                    NeuralNetworkWindow networkWindow, HistoryBrowser historyBrowser) {
        this.controller = controller;
        this.settings = settings;
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

        Button showNetworkButton = new Button("Mostrar Red Neuronal");
        showNetworkButton.setOnAction(e -> {
            if (networkWindow.isShowing()) {
                networkWindow.close();
                showNetworkButton.setText("Mostrar Red Neuronal");
            } else {
                networkWindow.show();
                showNetworkButton.setText("Ocultar Red Neuronal");
            }
        });

        // Redraws the canvas without touching the population
        Button resetViewButton = new Button("Reiniciar Vista");
        resetViewButton.setOnAction(e -> draw());

        Button playSelectedGenButton = new Button("Reproducir generación seleccionada");
        playSelectedGenButton.setOnAction(e -> historyBrowser.playSelectedGeneration());

        Label infoLabel = new Label();
        infoLabel.textProperty().bind(Bindings.concat(
                "Generación: ", controller.currentGenerationProperty().asString(), "\n",
                "Mejor Fitness: ", Bindings.format("%.2f", controller.bestFitnessProperty())));

        Button toggleAgentsButton = new Button("Alternar vista (todos/mejor)");
        toggleAgentsButton.setOnAction(e -> {
            settings.setShowAllAgents(!settings.isShowAllAgents());
            toggleAgentsButton.setText(settings.isShowAllAgents() ? "Mostrar solo el mejor" : "Mostrar todos los agentes");
        });

        VBox controlsPanel = new VBox(10,
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

        view = new VBox(10, new HBox(15, canvas, controlsPanel));
        view.setPadding(new Insets(15));
    }

    Node getView() {
        return view;
    }

    /** Draws the current state of the live game. */
    void draw() {
        EvolvingPopulation population = controller.getPopulation();
        renderer.render(canvas.getGraphicsContext2D(), controller.getGame(), population.getAgents(),
                population.getBestAgent(),
                GameRenderer.Options.liveSimulation(settings.isShowAllAgents(), controller.isReplayMode(),
                        population.getAgents().length == 1));
    }
}
