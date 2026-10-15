package com.neat.flappybirdneat.view.main;

import com.neat.flappybirdneat.champion.Champion;
import com.neat.flappybirdneat.simulation.SimulationController;
import com.neat.flappybirdneat.view.Dialogs;
import com.neat.flappybirdneat.view.NeuralNetworkWindow;
import com.neat.flappybirdneat.view.StatisticsWindow;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Main application window: a statistics/control tab and a live simulation tab. It builds the
 * panels, wires them together and owns the game loop.
 */
public final class MainWindow implements MainWindowActions {

    // Below this the game would be tiny next to the fixed-width sidebar
    private static final double MIN_WIDTH = 800;
    private static final double MIN_HEIGHT = 500;

    private final Node simulationView;
    private final SimulationController controller;
    private final NeuralNetworkWindow networkWindow = new NeuralNetworkWindow(600, 400);
    private final StatisticsWindow statisticsWindow = new StatisticsWindow(800, 600);
    private final TabPane tabPane = new TabPane();
    private final ControlPanel controlPanel;
    private final FitnessCharts charts;
    private final HistoryBrowser historyBrowser;
    private final GameLoop gameLoop;
    private final LiveViewSettings settings = new LiveViewSettings();
    private Stage stage;

    public MainWindow(SimulationController controller, int populationSize, int canvasWidth, int canvasHeight) {
        this.controller = controller;

        controlPanel = new ControlPanel(
                controller, populationSize, canvasWidth, canvasHeight, settings, statisticsWindow, this);
        charts = new FitnessCharts();
        refreshStatistics();
        VBox statsPanel = new VBox(10, controlPanel.getView(), charts.getView());
        statsPanel.setPadding(new Insets(15));

        historyBrowser = new HistoryBrowser(controller, () -> {
            settings.setShowAllAgents(true); // A generation is replayed with all its agents
            showSimulationTab();
        });
        SimulationPanel simulationPanel =
                new SimulationPanel(controller, canvasWidth, canvasHeight, settings, networkWindow, historyBrowser);

        gameLoop = new GameLoop(controller, settings, networkWindow, simulationPanel::draw, () -> {
            refreshStatistics();
            historyBrowser.refresh();
        });

        Tab statsTab = new Tab("Estadísticas y Control", statsPanel);
        statsTab.setClosable(false);
        simulationView = simulationPanel.getView();
        Tab simulationTab = new Tab("Simulación Visual", simulationView);
        simulationTab.setClosable(false);
        tabPane.getTabs().addAll(statsTab, simulationTab);
    }

    public void show(Stage stage) {
        this.stage = stage;
        stage.setTitle("Flappy Bird NEAT - Evolución Gráfica");
        // La ventana se abre al tamaño del panel de simulación (juego a escala 1) más la cabecera de
        // pestañas: el tamaño preferido de las pestañas lo marca la de estadísticas, mucho mayor. Luego
        // se puede redimensionar: el juego escala para ocupar el espacio libre.
        Scene scene = new Scene(tabPane);
        tabPane.applyCss();
        tabPane.layout();
        Node header = tabPane.lookup(".tab-header-area");
        double headerHeight = header == null ? 0 : header.prefHeight(-1);
        tabPane.setPrefSize(simulationView.prefWidth(-1), simulationView.prefHeight(-1) + headerHeight);
        stage.setScene(scene);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
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
        // The game loop refreshes them once per second while training: a shorter training, or its
        // last generations, would otherwise be missing from the history
        refreshStatistics();
        historyBrowser.refresh();
        historyBrowser.offerBestGenerationReplay();
    }

    @Override
    public void watchChampion(Champion champion) {
        if (controller.isFastMode()) {
            Dialogs.show(
                    Alert.AlertType.WARNING,
                    "Entrenamiento en curso",
                    null,
                    "Detén el entrenamiento antes de ver un campeón.");
            return;
        }
        controller.watchChampion(champion);
        settings.setShowAllAgents(false);
        showSimulationTab();
        if (!networkWindow.isShowing()) {
            networkWindow.showNextTo(stage);
        }
    }
}
