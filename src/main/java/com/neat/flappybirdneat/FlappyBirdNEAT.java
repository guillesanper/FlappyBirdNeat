package com.neat.flappybirdneat;

import com.neat.flappybirdneat.game.FlappyBirdGame;
import com.neat.flappybirdneat.game.Pipe;
import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.neat.Population;
import com.neat.flappybirdneat.simulation.SimulationController;
import com.neat.flappybirdneat.view.FlappyBirdGameUI;
import com.neat.flappybirdneat.view.NeuralNetworkWindow;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.List;
import com.neat.flappybirdneat.history.GenerationData;
import com.neat.flappybirdneat.history.HistoryManager;
import com.neat.flappybirdneat.history.RunHistory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

// Añadir estos atributos a la clase FlappyBirdNEAT


/**
 * Clase principal con interfaz de usuario mejorada para el algoritmo NEAT en Flappy Bird.
 * Incluye pestañas para estadísticas/gráficos y para simulación visual.
 */
public class FlappyBirdNEAT extends Application {
    // Configuración principal
    private static final int POPULATION_SIZE = 50;
    private static final int CANVAS_WIDTH = 800;
    private static final int CANVAS_HEIGHT = 600;

    // Controlador de simulación
    private SimulationController simulationController;

    // Variables para la interfaz gráfica
    private Canvas gameCanvas;
    private GraphicsContext gc;
    private AnimationTimer gameLoop;
    private int gameSpeed = 1;
    private boolean showAllAgents = true;

    // Ventana de visualización de la red neuronal
    private NeuralNetworkWindow networkWindow;

    // Gráficos
    private LineChart<Number, Number> fitnessChart;
    private XYChart.Series<Number, Number> bestFitnessSeries;
    private XYChart.Series<Number, Number> avgFitnessSeries;

    // UI components
    private Label genLabel;
    private Label bestFitnessLabel;
    private Label avgFitnessLabel;
    private Label aliveLabel;
    private Button stopButton;
    private ProgressBar progressBar;
    private Slider speedSlider;
    private ComboBox<String> historyComboBox;

    private TableView<GenerationData> historyTableView;
    private ComboBox<String> runHistoryComboBox;
    private ListView<String> historyListView;

    private int bestGenerationIndex = -1;


    @Override
    public void start(Stage primaryStage) {
        // Inicializar controlador de simulación
        simulationController = new SimulationController(POPULATION_SIZE, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Inicializar ventana de red neuronal
        networkWindow = new NeuralNetworkWindow(600, 400);

        // Crear la interfaz con pestañas
        TabPane tabPane = new TabPane();

        // Pestaña de estadísticas
        Tab statsTab = new Tab("Estadísticas y Control");
        statsTab.setClosable(false);
        statsTab.setContent(createStatsPanel());

        // Pestaña de simulación
        Tab simulationTab = new Tab("Simulación Visual");
        simulationTab.setClosable(false);
        simulationTab.setContent(createSimulationPanel());

        tabPane.getTabs().addAll(statsTab, simulationTab);

        // Crear la escena principal
        Scene scene = new Scene(tabPane, CANVAS_WIDTH + 200, CANVAS_HEIGHT);
        primaryStage.setTitle("Flappy Bird NEAT - Evolución Gráfica");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Iniciar el bucle del juego
        startGameLoop();

        // Manejar el cierre de la ventana principal
        primaryStage.setOnCloseRequest(e -> {
            if (networkWindow.isShowing()) {
                networkWindow.close();
            }
            if (gameLoop != null) {
                gameLoop.stop();
            }
            simulationController.stopSimulation();
        });
    }

    /**
     * Crea el panel de estadísticas con gráficos y controles
     */
    private VBox createStatsPanel() {
        VBox statsPanel = new VBox(10);
        statsPanel.setPadding(new Insets(15));

        // Sección superior con información y controles principales
        HBox topControls = new HBox(20);
        VBox infoBox = new VBox(5);
        VBox controlBox = new VBox(10);

        // Etiquetas de información
        genLabel = new Label();
        genLabel.textProperty().bind(Bindings.concat("Generación: ",
                simulationController.currentGenerationProperty().asString()));

        bestFitnessLabel = new Label();
        bestFitnessLabel.textProperty().bind(Bindings.concat("Mejor Fitness: ",
                Bindings.format("%.2f", simulationController.bestFitnessProperty())));

        avgFitnessLabel = new Label();
        avgFitnessLabel.textProperty().bind(Bindings.concat("Fitness Promedio: ",
                Bindings.format("%.2f", simulationController.averageFitnessProperty())));

        aliveLabel = new Label();
        aliveLabel.textProperty().bind(Bindings.concat("Agentes Vivos: ",
                simulationController.aliveCountProperty().asString(), "/", POPULATION_SIZE));

        // Fuentes y estilos
        Font labelFont = Font.font("System", FontWeight.BOLD, 14);
        genLabel.setFont(labelFont);
        bestFitnessLabel.setFont(labelFont);
        avgFitnessLabel.setFont(labelFont);
        aliveLabel.setFont(labelFont);

        infoBox.getChildren().addAll(genLabel, bestFitnessLabel, avgFitnessLabel, aliveLabel);

        // Controles de simulación rápida
        Label fastSimLabel = new Label("Simulación Rápida");
        fastSimLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        HBox genInputBox = new HBox(10);
        Label genToRunLabel = new Label("Generaciones a ejecutar:");
        TextField genToRunField = new TextField("10");
        genToRunField.setPrefWidth(60);
        genInputBox.getChildren().addAll(genToRunLabel, genToRunField);
        genInputBox.setAlignment(Pos.CENTER_LEFT);

        HBox buttonBox = new HBox(10);
        Button runButton = new Button("Ejecutar Simulación Rápida");
        stopButton = new Button("Detener");
        stopButton.setDisable(true);

        // Vincular estado del botón de parada
        simulationController.runningProperty().addListener((obs, oldVal, newVal) -> {
            stopButton.setDisable(!newVal);
            runButton.setDisable(newVal);
        });

        buttonBox.getChildren().addAll(runButton, stopButton);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        // Barra de progreso
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);

        controlBox.getChildren().addAll(fastSimLabel, genInputBox, buttonBox, progressBar);

        // Acciones de los botones
        runButton.setOnAction(e -> {
            try {
                int generations = Integer.parseInt(genToRunField.getText().trim());
                if (generations > 0) {
                    simulationController.runFastSimulation(generations);
                    progressBar.setProgress(0);
                    // Actualizar la barra de progreso
                    progressBar.progressProperty().bind(
                            Bindings.divide(
                                    Bindings.subtract(
                                            simulationController.currentGenerationProperty(),
                                            simulationController.currentGenerationProperty().getValue()
                                    ),
                                    generations
                            )
                    );

                    // Añade un listener para cuando termine la simulación rápida
                    simulationController.runningProperty().addListener(new javafx.beans.value.ChangeListener<Boolean>() {
                        @Override
                        public void changed(javafx.beans.value.ObservableValue<? extends Boolean> observable,
                                            Boolean wasRunning, Boolean isRunning) {
                            if (wasRunning && !isRunning) {
                                // La simulación ha terminado
                                findBestGenerationAndShow();
                                // Eliminar este listener para evitar que se active en futuras simulaciones
                                simulationController.runningProperty().removeListener(this);
                            }
                        }
                    });
                }
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Por favor, introduce un número válido de generaciones.");
                alert.showAndWait();
            }
        });

        stopButton.setOnAction(e -> {
            simulationController.stopSimulation();
            progressBar.progressProperty().unbind();
        });

        topControls.getChildren().addAll(infoBox, controlBox);

        // Gráfico de evolución del fitness
        createFitnessChart();

        // Actualizar automáticamente el gráfico cuando la simulación rápida esté corriendo
        simulationController.runningProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) { // Si está corriendo, programar actualizaciones periódicas
                new AnimationTimer() {
                    private long lastUpdate = 0;

                    @Override
                    public void handle(long now) {
                        if (now - lastUpdate > 1_000_000_000) { // Actualizar cada segundo
                            updateChart();
                            lastUpdate = now;

                            // Detener si la simulación ha terminado
                            if (!simulationController.runningProperty().get()) {
                                this.stop();
                            }
                        }
                    }
                }.start();
            }
        });

        // Opciones adicionales
        HBox additionalControls = new HBox(20);

        Button resetButton = new Button("Reiniciar Simulación");
        resetButton.setOnAction(e -> {
            simulationController.resetSimulation();
            updateChart();
        });

        Button exportDataButton = new Button("Exportar Datos");
        exportDataButton.setOnAction(e -> {
            exportSimulationData();
        });

        Button toggleAgentsButton = new Button("Alternar vista (todos/mejor)");
        toggleAgentsButton.setOnAction(e -> {
            showAllAgents = !showAllAgents;
            if (showAllAgents) {
                toggleAgentsButton.setText("Mostrar solo el mejor");
            } else {
                toggleAgentsButton.setText("Mostrar todos los agentes");
            }
        });

        additionalControls.getChildren().addAll(resetButton, exportDataButton);
        additionalControls.setPadding(new Insets(10, 0, 0, 0));
        additionalControls.setAlignment(Pos.CENTER_LEFT);

        // Organizar todo el panel
        statsPanel.getChildren().addAll(topControls, fitnessChart, additionalControls);

        return statsPanel;
    }

    private void findBestGenerationAndShow() {
        HistoryManager manager = simulationController.getHistoryManager();
        RunHistory currentRun = manager.getCurrentRun();

        if (currentRun != null && !currentRun.getGenerationDataList().isEmpty()) {
            // Buscar la generación con el mejor fitness
            double bestFitness = -1;
            int bestGenIdx = -1;

            List<GenerationData> generations = currentRun.getGenerationDataList();
            for (int i = 0; i < generations.size(); i++) {
                GenerationData data = generations.get(i);
                if (data.getBestFitness() > bestFitness) {
                    bestFitness = data.getBestFitness();
                    bestGenIdx = i;
                }
            }

            if (bestGenIdx >= 0) {
                bestGenerationIndex = bestGenIdx;

                // Mostrar un mensaje informativo
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Mejor Individuo Encontrado");
                alert.setHeaderText(null);
                alert.setContentText("Se encontró el mejor individuo en la generación " +
                        (bestGenIdx + 1) + " con fitness " +
                        String.format("%.2f", bestFitness) +
                        "\n\n¿Deseas ver la simulación de este individuo?");

                // Añadir botones personalizados
                ButtonType verSimulacionBtn = new ButtonType("Ver Simulación");
                ButtonType cancelarBtn = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(verSimulacionBtn, cancelarBtn);

                alert.showAndWait().ifPresent(response -> {
                    if (response == verSimulacionBtn) {
                        // Cambiar a la pestaña de simulación y mostrar el mejor individuo
                        switchToSimulationAndShowBest();
                    }
                });
            }
        }
    }

    private void switchToSimulationAndShowBest() {
        // Cambiar a la pestaña de simulación (asumiendo que es el índice 1)
        TabPane tabPane = (TabPane) gameCanvas.getScene().getRoot();
        tabPane.getSelectionModel().select(1);

        // Asegurarse de que se seleccione la ejecución actual
        runHistoryComboBox.getSelectionModel().select(runHistoryComboBox.getItems().size() - 1);
        updateHistoryDetails();

        // Seleccionar la generación con el mejor individuo
        if (bestGenerationIndex >= 0 && bestGenerationIndex < historyTableView.getItems().size()) {
            historyTableView.getSelectionModel().select(bestGenerationIndex);
            historyTableView.scrollTo(bestGenerationIndex);

            // Reproducir esta generación automáticamente
            playSelectedGeneration();
        }
    }

    /**
     * Exporta los datos de la simulación a un archivo CSV
     */
    private void exportSimulationData() {
        // Implementación básica de la exportación
        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Guardar datos de simulación");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            javafx.stage.FileChooser.ExtensionFilter extFilter =
                    new javafx.stage.FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv");
            fileChooser.getExtensionFilters().add(extFilter);

            java.io.File file = fileChooser.showSaveDialog(null);

            if (file != null) {
                java.io.PrintWriter writer = new java.io.PrintWriter(file);
                writer.println("Generacion,MejorFitness,FitnessPromedio");

                List<Double> bestFitness = simulationController.getBestFitnessHistory();
                List<Double> avgFitness = simulationController.getAvgFitnessHistory();

                for (int i = 0; i < bestFitness.size(); i++) {
                    writer.println((i+1) + "," + bestFitness.get(i) + "," +
                            (i < avgFitness.size() ? avgFitness.get(i) : ""));
                }

                writer.close();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Exportación Completa");
                alert.setHeaderText(null);
                alert.setContentText("Los datos se han exportado correctamente.");
                alert.showAndWait();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error al exportar datos");
            alert.setContentText("Se produjo un error: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Crea el gráfico de evolución del fitness
     */
    private void createFitnessChart() {
        // Definir ejes
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Generación");
        xAxis.setAnimated(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Fitness");
        yAxis.setAnimated(false);

        // Crear gráfico
        fitnessChart = new LineChart<>(xAxis, yAxis);
        fitnessChart.setTitle("Evolución del Fitness a lo largo de las Generaciones");
        fitnessChart.setAnimated(false);
        fitnessChart.setCreateSymbols(false);

        // Series para mejores y promedio
        bestFitnessSeries = new XYChart.Series<>();
        bestFitnessSeries.setName("Mejor Fitness");

        avgFitnessSeries = new XYChart.Series<>();
        avgFitnessSeries.setName("Fitness Promedio");

        fitnessChart.getData().addAll(bestFitnessSeries, avgFitnessSeries);

        // Aplicar estilos a las líneas
        bestFitnessSeries.getNode().setStyle("-fx-stroke: red; -fx-stroke-width: 2px;");
        avgFitnessSeries.getNode().setStyle("-fx-stroke: blue; -fx-stroke-width: 1.5px;");

        // Inicializar con datos actuales
        updateChart();

        // Hacer que el gráfico se expanda al máximo disponible
        VBox.setVgrow(fitnessChart, Priority.ALWAYS);
    }

    /**
     * Actualiza el gráfico con los datos actuales
     */
    private void updateChart() {
        bestFitnessSeries.getData().clear();
        avgFitnessSeries.getData().clear();

        for (int i = 0; i < simulationController.getBestFitnessHistory().size(); i++) {
            bestFitnessSeries.getData().add(
                    new XYChart.Data<>(i, simulationController.getBestFitnessHistory().get(i)));

            if (i < simulationController.getAvgFitnessHistory().size()) {
                avgFitnessSeries.getData().add(
                        new XYChart.Data<>(i, simulationController.getAvgFitnessHistory().get(i)));
            }
        }
    }

    private VBox createSimulationPanel() {
        VBox simulationPanel = new VBox(10);
        simulationPanel.setPadding(new Insets(15));

        // Canvas para el juego
        gameCanvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = gameCanvas.getGraphicsContext2D();

        // Controles laterales
        HBox gameControlsBox = new HBox(15);

        // Panel de velocidad
        VBox speedBox = new VBox(5);
        Label speedLabel = new Label("Velocidad de Simulación:");
        speedSlider = new Slider(1, 10, 1);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(1);
        speedSlider.setMinorTickCount(0);
        speedSlider.setBlockIncrement(1);
        speedSlider.setSnapToTicks(true);

        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            gameSpeed = newVal.intValue();
        });

        speedBox.getChildren().addAll(speedLabel, speedSlider);

        // Checkbox para mostrar todos los agentes
        CheckBox showAllCheckbox = new CheckBox("Mostrar todos los agentes");
        showAllCheckbox.setSelected(showAllAgents);
        showAllCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            showAllAgents = newVal;
        });

        // Botón para mostrar red neuronal
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

        // Botón para reiniciar la vista de simulación
        Button resetViewButton = new Button("Reiniciar Vista");
        resetViewButton.setOnAction(e -> {
            // Reinicia solo la visualización sin afectar la población
            drawGame();
        });

        // Selector de historial de ejecuciones
        runHistoryComboBox = new ComboBox<>();
        runHistoryComboBox.setPromptText("Seleccionar ejecución");
        runHistoryComboBox.setPrefWidth(200);
        runHistoryComboBox.setOnAction(e -> {
            updateHistoryDetails();
        });

        // Actualizar el selector con las ejecuciones disponibles
        updateRunHistoryComboBox();

        // Botón para reproducir la generación seleccionada
        Button playSelectedGenButton = new Button("Reproducir generación seleccionada");
        playSelectedGenButton.setOnAction(e -> {
            playSelectedGeneration();
        });

        // Tabla para mostrar detalles de las generaciones
        historyTableView = new TableView<>();
        historyTableView.setPrefHeight(200);

        TableColumn<GenerationData, Integer> genNumberCol = new TableColumn<>("Gen");
        genNumberCol.setCellValueFactory(new PropertyValueFactory<>("generationNumber"));

        TableColumn<GenerationData, Double> bestFitnessCol = new TableColumn<>("Mejor Fitness");
        bestFitnessCol.setCellValueFactory(new PropertyValueFactory<>("bestFitness"));

        TableColumn<GenerationData, Integer> aliveCountCol = new TableColumn<>("Vivos");
        aliveCountCol.setCellValueFactory(new PropertyValueFactory<>("aliveCount"));

        historyTableView.getColumns().addAll(genNumberCol, bestFitnessCol, aliveCountCol);

        // Lista para historial resumido
        historyListView = new ListView<>();
        historyListView.setPrefHeight(200);

        // Mostrar la generación seleccionada en la lista
        historyListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int index = historyListView.getSelectionModel().getSelectedIndex();
                // Seleccionar la misma fila en la tabla
                historyTableView.getSelectionModel().select(index);
                historyTableView.scrollTo(index);
            }
        });

        // Mostrar la generación seleccionada en la tabla
        historyTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int index = historyTableView.getSelectionModel().getSelectedIndex();
                // Seleccionar la misma fila en la lista
                historyListView.getSelectionModel().select(index);
                historyListView.scrollTo(index);
            }
        });

        // Organizar controles
        VBox controlsPanel = new VBox(10);
        controlsPanel.setPadding(new Insets(10));
        controlsPanel.getChildren().addAll(
                speedBox,
                showAllCheckbox,
                showNetworkButton,
                resetViewButton,
                new Separator(),
                new Label("Historial de Ejecuciones:"),
                runHistoryComboBox,
                historyTableView,
                playSelectedGenButton
        );

        Button toggleAgentsButton = new Button("Alternar vista (todos/mejor)");
        toggleAgentsButton.setOnAction(e -> {
            showAllAgents = !showAllAgents;
            if (showAllAgents) {
                toggleAgentsButton.setText("Mostrar solo el mejor");
            } else {
                toggleAgentsButton.setText("Mostrar todos los agentes");
            }
        });

        // Añadir información de la generación actual en la vista de simulación
        Label infoLabel = new Label();
        infoLabel.textProperty().bind(Bindings.concat(
                "Generación: ", simulationController.currentGenerationProperty().asString(), "\n",
                "Mejor Fitness: ", Bindings.format("%.2f", simulationController.bestFitnessProperty())
        ));
        controlsPanel.getChildren().add(infoLabel);
        controlsPanel.getChildren().add(toggleAgentsButton);

        gameControlsBox.getChildren().addAll(gameCanvas, controlsPanel);
        simulationPanel.getChildren().add(gameControlsBox);

        return simulationPanel;
    }

    private void updateRunHistoryComboBox() {
        runHistoryComboBox.getItems().clear();

        HistoryManager manager = simulationController.getHistoryManager();
        List<RunHistory> runs = manager.getRunHistories();

        if (runs.isEmpty()) {
            runHistoryComboBox.getItems().add("Ejecución actual");
        } else {
            for (int i = 0; i < runs.size(); i++) {
                RunHistory run = runs.get(i);
                double bestFitness = run.getGenerationDataList()
                        .stream()
                        .mapToDouble(GenerationData::getBestFitness)
                        .max()
                        .orElse(0.0);;
                runHistoryComboBox.getItems().add("Ejecución " + (i+1) + " (Mejor: " + String.format("%.2f", bestFitness) + ")");
            }
            runHistoryComboBox.getItems().add("Ejecución actual");
        }

        // Seleccionar la ejecución actual por defecto
        runHistoryComboBox.getSelectionModel().select(runHistoryComboBox.getItems().size() - 1);
    }

    // Añadir método para actualizar detalles del historial seleccionado
    private void updateHistoryDetails() {
        int selectedIndex = runHistoryComboBox.getSelectionModel().getSelectedIndex();
        HistoryManager manager = simulationController.getHistoryManager();
        List<RunHistory> runs = manager.getRunHistories();

        ObservableList<GenerationData> generationData = FXCollections.observableArrayList();
        RunHistory selectedRun;

        if (selectedIndex == runHistoryComboBox.getItems().size() - 1) {
            // Ejecución actual seleccionada
            selectedRun = manager.getCurrentRun();
        } else if (selectedIndex >= 0 && selectedIndex < runs.size()) {
            // Ejecución histórica seleccionada
            selectedRun = runs.get(selectedIndex);
        } else {
            return;
        }

        // Llenar la tabla con datos de generaciones
        List<GenerationData> generations = selectedRun.getGenerationDataList();
        for (int i = 0; i < generations.size(); i++) {
            GenerationData data = generations.get(i);
            // Asegurarnos de que tenga un número de generación (por compatibilidad)
            if (data.getGenerationNumber() == 0) {
                data.setGenerationNumber(i + 1);
            }
            generationData.add(data);
        }

        historyTableView.setItems(generationData);

        // También actualizar la lista resumida
        ObservableList<String> summaryItems = FXCollections.observableArrayList();
        for (int i = 0; i < generations.size(); i++) {
            GenerationData data = generations.get(i);
            summaryItems.add("Gen " + (i+1) + ": Fitness " + String.format("%.2f", data.getBestFitness()));
        }
        historyListView.setItems(summaryItems);
    }

    // Método para reproducir una generación seleccionada
    // Modificaciones a FlappyBirdNEAT.java - método playSelectedGeneration
    private void playSelectedGeneration() {
        int selectedRunIndex = runHistoryComboBox.getSelectionModel().getSelectedIndex();
        int selectedGenIndex = historyTableView.getSelectionModel().getSelectedIndex();

        if (selectedGenIndex < 0) return;

        HistoryManager manager = simulationController.getHistoryManager();
        List<RunHistory> runs = manager.getRunHistories();
        RunHistory selectedRun;

        if (selectedRunIndex == runHistoryComboBox.getItems().size() - 1) {
            // Ejecución actual seleccionada
            selectedRun = manager.getCurrentRun();
        } else if (selectedRunIndex >= 0 && selectedRunIndex < runs.size()) {
            // Ejecución histórica seleccionada
            selectedRun = runs.get(selectedRunIndex);
        } else {
            return;
        }

        List<GenerationData> generations = selectedRun.getGenerationDataList();
        if (selectedGenIndex < generations.size()) {
            GenerationData selectedGen = generations.get(selectedGenIndex);

            // Pausar la ejecución actual
            if (gameLoop != null) {
                gameLoop.stop();
            }

            // Cargar la población seleccionada
            Population selectedPopulation = selectedGen.getSavedPopulation();

            // Crear una nueva ventana con FlappyBirdGameUI para la visualización
            try {
                // Crear un nuevo Stage para la simulación visual
                Stage simulationStage = new Stage();
                simulationStage.setTitle("Simulación de Generación " + (selectedGenIndex + 1));

                // Inicializar FlappyBirdGameUI con la población seleccionada
                FlappyBirdGameUI gameUI = new FlappyBirdGameUI();

                // Configuramos el escenario para su inicialización
                gameUI.prepareStage(simulationStage, selectedPopulation, selectedGenIndex + 1);

                // Mostrar la ventana
                simulationStage.show();

                // Mostrar un mensaje informativo
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Reproducción Histórica");
                alert.setHeaderText(null);
                alert.setContentText("Reproduciendo generación " + (selectedGenIndex + 1) +
                        " con fitness " + String.format("%.2f", selectedGen.getBestFitness()) +
                        "\n\nSe abrirá una nueva ventana con la simulación visual.");
                alert.showAndWait();

            } catch (Exception e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Error al cargar la simulación");
                alert.setContentText("No se pudo iniciar la simulación visual: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }



    /**
     * Inicia el bucle del juego para la visualización
     */
    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                // En modo rápido, solo actualizar la gráfica periódicamente
                if (simulationController.isFastMode()) {
                    if (now - lastUpdate > 1_000_000_000) { // Actualizar gráfica cada segundo
                        lastUpdate = now;
                        updateChart();
                        updateRunHistoryComboBox(); // Actualizar el selector de historiales
                        updateHistoryDetails();    // Actualizar detalles del historial
                    }
                    return;
                }

                // Control de velocidad para simulación visual
                if (now - lastUpdate < 1_000_000_000 / (60 * gameSpeed)) {
                    return;
                }
                lastUpdate = now;

                // Actualizar simulación visual
                boolean allDead = simulationController.updateFrame();

                // Dibujar el estado actual
                drawGame();

                // Si todos están muertos, pasar a siguiente generación
                if (allDead) {
                    simulationController.nextGeneration();
                    updateChart();
                    updateRunHistoryComboBox(); // Actualizar el selector de historiales
                    updateHistoryDetails();    // Actualizar detalles del historial
                }

                // Actualizar ventana de red neuronal si está activa
                if (networkWindow.isShowing()) {
                    FlappyBirdAgent bestAgent = simulationController.getPopulation().getBestAgent();
                    if (!bestAgent.isDead()) {
                        Pipe nextPipe = getNextPipe(bestAgent);
                        networkWindow.update(bestAgent, nextPipe);
                    }
                }
            }
        };
        gameLoop.start();
    }

    /**
     * Obtiene el próximo tubo para un agente
     */
    private Pipe getNextPipe(FlappyBirdAgent agent) {
        FlappyBirdGame game = simulationController.getGame();
        for (Pipe pipe : game.getPipes()) {
            if (pipe.getX() + pipe.getWidth() > 50) { // 50 es x del pájaro
                return pipe;
            }
        }
        return null;
    }

    /**
     * Dibuja el estado actual del juego
     */
    /**
     * Versión mejorada de drawGame() con estilo visual similar a FlappyBirdGameUI
     */
    private void drawGame() {
        FlappyBirdGame game = simulationController.getGame();
        Population population = simulationController.getPopulation();

        // Dibujar fondo
        gc.setFill(Color.SKYBLUE);
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Dibujar nubes decorativas
        gc.setFill(Color.WHITE);
        gc.setFill(Color.WHITE);
        gc.fillOval(100, 100, 80, 40);
        gc.fillOval(300, 150, 100, 50);
        gc.fillOval(600, 80, 120, 60);

        // Dibujar tubos
        for (Pipe pipe : game.getPipes()) {
            // Tubo superior
            gc.setFill(Color.GREEN);
            gc.fillRect(pipe.getX(), 0, pipe.getWidth(), pipe.getGapY() - pipe.getGapSize()/2);

            // Borde del tubo superior
            gc.setFill(Color.DARKGREEN);
            gc.fillRect(pipe.getX() - 3, pipe.getGapY() - pipe.getGapSize()/2 - 20,
                    pipe.getWidth() + 6, 20);

            // Tubo inferior
            gc.setFill(Color.GREEN);
            gc.fillRect(pipe.getX(), pipe.getGapY() + pipe.getGapSize()/2,
                    pipe.getWidth(), CANVAS_HEIGHT - (pipe.getGapY() + pipe.getGapSize()/2));

            // Borde del tubo inferior
            gc.setFill(Color.DARKGREEN);
            gc.fillRect(pipe.getX() - 3, pipe.getGapY() + pipe.getGapSize()/2,
                    pipe.getWidth() + 6, 20);
        }

        // Dibujar pájaros (agentes)
        FlappyBirdAgent bestOverall = population.getBestAgent();

        for (FlappyBirdAgent agent : population.getAgents()) {
            if (!agent.isDead()) {
                if (agent == bestOverall) {
                    // El mejor agente de todas las generaciones se dibuja en rojo
                    gc.setFill(Color.RED);
                    gc.fillOval(50, agent.getY(), 30, 30);

                    // Ojo del pájaro
                    gc.setFill(Color.WHITE);
                    gc.fillOval(65, agent.getY() + 8, 8, 8);
                    gc.setFill(Color.BLACK);
                    gc.fillOval(67, agent.getY() + 10, 4, 4);

                    // Pico
                    gc.setFill(Color.ORANGE);
                    gc.fillPolygon(
                            new double[] {80, 90, 80},
                            new double[] {agent.getY() + 15, agent.getY() + 18, agent.getY() + 21},
                            3
                    );

                    // Indicador visual de que es el mejor agente
                    gc.setStroke(Color.GOLD);
                    gc.setLineWidth(2);
                    gc.strokeOval(45, agent.getY() - 5, 40, 40);
                } else if (showAllAgents) {
                    // El resto en amarillo, semitransparente para ver mejor
                    gc.setFill(new Color(1, 1, 0, 0.3));
                    gc.fillOval(50, agent.getY(), 30, 30);
                }
            }
        }

        // Dibujar suelo
        gc.setFill(Color.SADDLEBROWN);
        gc.fillRect(0, CANVAS_HEIGHT - 20, CANVAS_WIDTH, 20);

        // Textura del suelo
        gc.setFill(Color.SANDYBROWN);
        for (int i = 0; i < CANVAS_WIDTH; i += 30) {
            gc.fillRect(i, CANVAS_HEIGHT - 20, 15, 5);
        }

        // Mostrar puntuación en pantalla con estilo de FlappyBirdGameUI
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        gc.setFont(Font.font("System", FontWeight.BOLD, 30));
        String scoreText = String.valueOf(game.getScore());
        gc.fillText(scoreText, CANVAS_WIDTH/2 - 15, 50);
        gc.strokeText(scoreText, CANVAS_WIDTH/2 - 15, 50);

        // Si estamos en modo reproducción de mejor agente, indicarlo
        if (!showAllAgents) {
            gc.setFill(new Color(0, 0, 0, 0.7));
            gc.fillRect(10, CANVAS_HEIGHT - 60, 300, 30);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 16));
            gc.fillText("Mostrando al mejor agente", 20, CANVAS_HEIGHT - 40);
        }
    }
}