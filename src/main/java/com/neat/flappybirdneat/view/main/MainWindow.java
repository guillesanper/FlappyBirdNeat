package com.neat.flappybirdneat.view.main;

import com.neat.flappybirdneat.simulation.SimulationController;
import com.neat.flappybirdneat.view.NeuralNetworkWindow;
import com.neat.flappybirdneat.view.StatisticsWindow;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Main application window: a statistics/control tab and a live simulation tab. It builds the
 * panels, wires them together and owns the game loop.
 */
public final class MainWindow implements MainWindowActions {

    private static final int SIDEBAR_WIDTH = 200;

    private final SimulationController controller;
    private final NeuralNetworkWindow networkWindow = new NeuralNetworkWindow(600, 400);
    private final StatisticsWindow statisticsWindow = new StatisticsWindow(800, 600);
    private final TabPane tabPane = new TabPane();
    private final ControlPanel controlPanel;
    private final FitnessCharts charts;
    private final HistoryBrowser historyBrowser;
    private final GameLoop gameLoop;
    private final int canvasWidth;
    private final int canvasHeight;

    public MainWindow(SimulationController controller, int populationSize, int canvasWidth, int canvasHeight) {
        this.controller = controller;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        LiveViewSettings settings = new LiveViewSettings();

        controlPanel = new ControlPanel(
                controller, populationSize, canvasWidth, canvasHeight, settings, statisticsWindow, this);
        charts = new FitnessCharts();
        refreshStatistics();
        VBox statsPanel = new VBox(10, controlPanel.getView(), charts.getView());
        statsPanel.setPadding(new Insets(15));

        historyBrowser = new HistoryBrowser(controller, this::pauseGameLoop, this::resumeGameLoop);
        SimulationPanel simulationPanel =
                new SimulationPanel(controller, canvasWidth, canvasHeight, settings, networkWindow, historyBrowser);

        gameLoop = new GameLoop(controller, settings, networkWindow, simulationPanel::draw, () -> {
            refreshStatistics();
            historyBrowser.refresh();
        });

        Tab statsTab = new Tab("Estadísticas y Control", statsPanel);
        statsTab.setClosable(false);
        Tab simulationTab = new Tab("Simulación Visual", simulationPanel.getView());
        simulationTab.setClosable(false);
        tabPane.getTabs().addAll(statsTab, simulationTab);
    }

    public void show(Stage stage) {
        stage.setTitle("Flappy Bird NEAT - Evolución Gráfica");
        stage.setScene(new Scene(tabPane, canvasWidth + SIDEBAR_WIDTH, canvasHeight));
        stage.show();
        gameLoop.start();

        stage.setOnCloseRequest(e -> {
            if (networkWindow.isShowing()) {
                networkWindow.close();
            }
            if (statisticsWindow.isShowing()) {
                statisticsWindow.close();
            }
            gameLoop.stop();
            controller.stopSimulation();
        });
    }

    @Override
    public void refreshStatistics() {
        controlPanel.updateSpeciesCount(controller.getSpeciesCount());
        charts.update(controller);
        if (statisticsWindow.isShowing()) {
            statisticsWindow.update(controller);
        }
    }

    @Override
    public void showSimulationTab() {
        tabPane.getSelectionModel().select(1);
    }

    @Override
    public void offerBestGenerationReplay() {
        historyBrowser.offerBestGenerationReplay(this::showSimulationTab);
    }

    private void pauseGameLoop() {
        gameLoop.stop();
    }

    private void resumeGameLoop() {
        gameLoop.start();
    }
}
