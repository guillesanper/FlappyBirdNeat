package com.neat.flappybirdneat.view;

import com.neat.flappybirdneat.game.FlappyBirdGame;
import com.neat.flappybirdneat.game.Pipe;
import com.neat.flappybirdneat.history.GenerationData;
import com.neat.flappybirdneat.history.HistoryManager;
import com.neat.flappybirdneat.history.RunHistory;
import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.neat.Population;
import com.neat.flappybirdneat.simulation.SimulationController;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.CheckBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Componente principal que integra el algoritmo evolutivo de redes neuronales
 * con JavaFX para visualizar el aprendizaje de los agentes en Flappy Bird.
 */
public class FlappyBirdGameUI {
    // Configuración principal
    private static final int POPULATION_SIZE = 50;
    private static final int CANVAS_WIDTH = 800;
    private static final int CANVAS_HEIGHT = 600;

    // Variables del juego
    private Population population;
    private FlappyBirdGame game;
    private int currentGeneration = 1;
    private boolean gamePaused = false;
    private int gameSpeed = 1; // Velocidad normal
    private boolean showAllAgents = true;
    private boolean autoRestartOnExtinction = true;
    private boolean loopSimulation = true;
    private int maxGenerations = 50;

    // Variables para la interfaz gráfica
    private Canvas canvas;
    private GraphicsContext gc;
    private Label generationLabel;
    private Label aliveLabel;
    private Label scoreLabel;
    private Label bestFitnessLabel;
    private Label speedLabel;
    private AnimationTimer gameLoop;
    private Stage primaryStage;
    private BorderPane root;

    // Controlador de simulación
    private SimulationController simulationController;

    // Ventana de visualización de la red neuronal
    private NeuralNetworkWindow networkWindow;
    private boolean showNeuralNetwork = false;

    public FlappyBirdGameUI() {
        // El constructor vacío no inicializa nada, se hará mediante prepareStage
        networkWindow = null;  // Se inicializará bajo demanda
    }

    /**
     * Inicializa y configura la UI del juego
     * @param stage La ventana principal donde se mostrará el juego
     * @return La escena configurada
     */
    public Scene initialize(Stage stage) {
        this.primaryStage = stage;

        // Inicializar población y juego
        population = new Population(POPULATION_SIZE);
        game = new FlappyBirdGame(CANVAS_WIDTH, CANVAS_HEIGHT);

        // Inicializar controlador de simulación
        simulationController = new SimulationController(POPULATION_SIZE, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Inicializar ventana de red neuronal
        networkWindow = new NeuralNetworkWindow(600, 400);

        // Configurar la interfaz gráfica
        root = new BorderPane();

        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D();

        root.setCenter(canvas);

        // Panel de información
        generationLabel = new Label("Generación: 1");
        generationLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        aliveLabel = new Label("Vivos: " + POPULATION_SIZE);
        scoreLabel = new Label("Puntuación: 0");
        bestFitnessLabel = new Label("Mejor Fitness: 0");
        speedLabel = new Label("Velocidad: 1x");

        // Botones de control
        Button pauseButton = new Button("Pausar / Continuar");
        pauseButton.setOnAction(e -> togglePause());

        // Control de velocidad
        Label speedSliderLabel = new Label("Velocidad de simulación:");
        Slider speedSlider = new Slider(1, 10, 1);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(1);
        speedSlider.setBlockIncrement(1);
        speedSlider.setSnapToTicks(true);
        speedSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            gameSpeed = newValue.intValue();
            speedLabel.setText("Velocidad: " + gameSpeed + "x");
        });

        // Botón para la siguiente generación
        Button nextGenButton = new Button("Siguiente generación");
        nextGenButton.setOnAction(e -> {
            // Solo permitir saltar a la siguiente generación si está pausado
            if (gamePaused) {
                nextGeneration();
            }
        });

        // Botón para reiniciar simulación
        Button resetButton = new Button("Reiniciar simulación");
        resetButton.setOnAction(e -> resetSimulation());

        // Botón para ejecutar simulación rápida
        Button fastSimulationButton = new Button("Ejecutar 20 generaciones rápido");
        fastSimulationButton.setOnAction(e -> {
            stopGameLoop();
            simulationController.runFastSimulation(20);
        });

        // Control para el bucle de reproducción
        CheckBox loopCheckbox = new CheckBox("Reproducir en bucle");
        loopCheckbox.setSelected(loopSimulation);
        loopCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            loopSimulation = newVal;
        });

        // Slider para configurar el máximo de generaciones
        Label maxGenLabel = new Label("Máximo de generaciones: " + maxGenerations);
        Slider maxGenSlider = new Slider(10, 200, maxGenerations);
        maxGenSlider.setShowTickMarks(true);
        maxGenSlider.setShowTickLabels(true);
        maxGenSlider.setMajorTickUnit(50);
        maxGenSlider.setBlockIncrement(10);
        maxGenSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            maxGenerations = newVal.intValue();
            maxGenLabel.setText("Máximo de generaciones: " + maxGenerations);
        });

        // Botón para mostrar el mejor individuo histórico
        Button showBestButton = new Button("Mostrar mejor individuo");
        showBestButton.setOnAction(e -> {
            stopGameLoop();
            simulationController.playBestHistoricalGeneration();
            startGameLoop(simulationController.getPopulation());
        });

        // Botones para guardar y cargar historial
        Button saveHistoryButton = new Button("Guardar historial");
        saveHistoryButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar historial");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Archivos NEAT", "*.neat")
            );
            File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) {
                simulationController.getHistoryManager().saveToFile(file.getAbsolutePath());
            }
        });

        Button loadHistoryButton = new Button("Cargar historial");
        loadHistoryButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Cargar historial");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Archivos NEAT", "*.neat")
            );
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                simulationController.getHistoryManager().loadFromFile(file.getAbsolutePath());
                // Mostrar información del historial cargado
                HistoryManager hm = simulationController.getHistoryManager();
                System.out.println("Historial cargado. Mejor fitness: " + hm.getBestFitnessEver());
            }
        });

        // Checkbox para mostrar todos los agentes o solo el mejor
        CheckBox showAllAgentsCheckbox = new CheckBox("Mostrar todos los agentes");
        showAllAgentsCheckbox.setSelected(showAllAgents);
        showAllAgentsCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            showAllAgents = newVal;
        });

        // Checkbox para reinicio automático al extinguirse
        CheckBox autoRestartCheckbox = new CheckBox("Reinicio automático");
        autoRestartCheckbox.setSelected(autoRestartOnExtinction);
        autoRestartCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            autoRestartOnExtinction = newVal;
        });

        // Botón para mostrar/ocultar ventana de red neuronal
        Button showNetworkButton = new Button("Mostrar Red Neuronal");
        showNetworkButton.setOnAction(e -> {
            showNeuralNetwork = !showNeuralNetwork;
            if (showNeuralNetwork) {
                networkWindow.show();
                showNetworkButton.setText("Ocultar Red Neuronal");
            } else {
                networkWindow.close();
                showNetworkButton.setText("Mostrar Red Neuronal");
            }
        });

        VBox infoPanel = new VBox(10);
        infoPanel.setPadding(new Insets(10));
        infoPanel.getChildren().addAll(
                generationLabel,
                aliveLabel,
                scoreLabel,
                bestFitnessLabel,
                speedLabel,
                pauseButton,
                speedSliderLabel,
                speedSlider,
                nextGenButton,
                resetButton,
                fastSimulationButton,
                showBestButton,
                loopCheckbox,
                maxGenLabel,
                maxGenSlider,
                saveHistoryButton,
                loadHistoryButton,
                showAllAgentsCheckbox,
                autoRestartCheckbox,
                showNetworkButton
        );

        root.setRight(infoPanel);

        Scene scene = new Scene(root, CANVAS_WIDTH + 200, CANVAS_HEIGHT);
        primaryStage.setTitle("Flappy Bird NEAT");

        // Manejar el cierre de la ventana principal
        primaryStage.setOnCloseRequest(e -> {
            if (networkWindow.isShowing()) {
                networkWindow.close();
            }
            stopGameLoop();
        });

        // Iniciar el bucle del juego
        startGameLoop(population);

        return scene;
    }

    /**
     * Detiene el bucle del juego actual
     */
    private void stopGameLoop() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
    }

    /**
     * Inicia el bucle principal del juego
     */
    public void startGameLoop(Population population) {
        // Detener el bucle existente si hay uno
        stopGameLoop();

        // Asignar la población a usar
        this.population = population;

        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                // Control de velocidad de simulación
                if (now - lastUpdate < 1_000_000_000 / (60 * gameSpeed)) {
                    return;
                }
                lastUpdate = now;

                if (!gamePaused) {
                    // Limpiar pantalla
                    gc.setFill(Color.SKYBLUE);
                    gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

                    // Actualizar juego
                    game.update(population.getAgents());

                    // Actualizar visualización de red neuronal si está activa
                    if (showNeuralNetwork && networkWindow.isShowing()) {
                        FlappyBirdAgent bestAgent = population.getBestAgent();
                        if (!bestAgent.isDead()) {
                            Pipe nextPipe = getNextPipe(bestAgent);
                            networkWindow.update(bestAgent, nextPipe);
                        }
                    }

                    // Dibujar escena
                    drawGame();

                    // Actualizar información
                    updateInfo();

                    // Comprobar si todos los agentes están muertos
                    boolean allDead = true;
                    for (FlappyBirdAgent agent : population.getAgents()) {
                        if (!agent.isDead()) {
                            allDead = false;
                            break;
                        }
                    }

                    // Si todos están muertos, pasar a la siguiente generación o reiniciar
                    if (allDead) {
                        if (currentGeneration >= maxGenerations && loopSimulation) {
                            // Reiniciar después de alcanzar el máximo de generaciones para iniciar bucle
                            resetSimulation();
                        } else {
                            nextGeneration();
                        }
                    }
                } else {
                    // Si está pausado, seguir dibujando la escena estática
                    drawGame();
                    drawPausedOverlay();
                }
            }
        };
        gameLoop.start();
    }

    /**
     * Obtiene el próximo tubo al que se enfrentará el agente
     * (Replicado de FlappyBirdGame para obtener la info para la visualización)
     */
    private Pipe getNextPipe(FlappyBirdAgent agent) {
        for (Pipe pipe : game.getPipes()) {
            if (pipe.getX() + pipe.getWidth() > 50) { // 50 es x del pájaro
                return pipe;
            }
        }
        return null;
    }

    /**
     * Dibuja un overlay cuando el juego está pausado
     */
    private void drawPausedOverlay() {
        gc.setFill(new Color(0, 0, 0, 0.3)); // Color semitransparente
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 30));
        gc.fillText("SIMULACIÓN PAUSADA", CANVAS_WIDTH/2 - 150, CANVAS_HEIGHT/2);
    }

    /**
     * Prepara el escenario para visualizar una población específica
     */
    public void prepareStage(Stage stage, Population population, int generationNumber) {
        this.primaryStage = stage;

        // Guardar la población que queremos visualizar
        this.population = population;
        this.currentGeneration = generationNumber;

        // Configurar la interfaz gráfica
        BorderPane root = new BorderPane();

        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D();

        root.setCenter(canvas);

        // Configurar el panel de información (similiar al start pero con menos controles)
        VBox infoPanel = createSimulationInfoPanel();
        root.setRight(infoPanel);

        Scene scene = new Scene(root, CANVAS_WIDTH + 200, CANVAS_HEIGHT);
        stage.setTitle("Flappy Bird NEAT - Generación " + currentGeneration);
        stage.setScene(scene);

        // Reiniciar el juego para la visualización
        game = new FlappyBirdGame(CANVAS_WIDTH, CANVAS_HEIGHT);

        // Reiniciar los agentes para la visualización
        for (FlappyBirdAgent agent : population.getAgents()) {
            agent.reset();
        }

        // Manejar el cierre de la ventana
        stage.setOnCloseRequest(e -> {
            if (networkWindow != null && networkWindow.isShowing()) {
                networkWindow.close();
            }
            if (gameLoop != null) {
                gameLoop.stop();
            }
        });

        // Iniciar el bucle de juego
        startGameLoop(population);
    }

    private VBox createSimulationInfoPanel() {
        // Panel de información simplificado para la visualización de generación específica
        generationLabel = new Label("Generación: " + currentGeneration);
        generationLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        aliveLabel = new Label("Vivos: " + population.getAgents().length);
        scoreLabel = new Label("Puntuación: 0");
        bestFitnessLabel = new Label("Mejor Fitness: " + String.format("%.2f", population.getBestFitness()));
        speedLabel = new Label("Velocidad: 1x");

        // Botones de control
        Button pauseButton = new Button("Pausar / Continuar");
        pauseButton.setOnAction(e -> togglePause());

        // Control de velocidad
        Label speedSliderLabel = new Label("Velocidad de simulación:");
        Slider speedSlider = new Slider(1, 10, 1);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(1);
        speedSlider.setBlockIncrement(1);
        speedSlider.setSnapToTicks(true);
        speedSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            gameSpeed = newValue.intValue();
            speedLabel.setText("Velocidad: " + gameSpeed + "x");
        });

        // Control para el bucle de reproducción
        CheckBox loopCheckbox = new CheckBox("Reproducir en bucle");
        loopCheckbox.setSelected(loopSimulation);
        loopCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            loopSimulation = newVal;
        });

        // Slider para configurar el máximo de generaciones
        Label maxGenLabel = new Label("Máximo de generaciones: " + maxGenerations);
        Slider maxGenSlider = new Slider(10, 200, maxGenerations);
        maxGenSlider.setShowTickMarks(true);
        maxGenSlider.setShowTickLabels(true);
        maxGenSlider.setMajorTickUnit(50);
        maxGenSlider.setBlockIncrement(10);
        maxGenSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            maxGenerations = newVal.intValue();
            maxGenLabel.setText("Máximo de generaciones: " + maxGenerations);
        });

        // Checkbox para mostrar todos los agentes o solo el mejor
        CheckBox showAllAgentsCheckbox = new CheckBox("Mostrar todos los agentes");
        showAllAgentsCheckbox.setSelected(showAllAgents);
        showAllAgentsCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            showAllAgents = newVal;
        });

        // Botón para mostrar/ocultar ventana de red neuronal
        Button showNetworkButton = new Button("Mostrar Red Neuronal");
        showNetworkButton.setOnAction(e -> {
            showNeuralNetwork = !showNeuralNetwork;
            if (showNeuralNetwork) {
                if (networkWindow == null) {
                    networkWindow = new NeuralNetworkWindow(600, 400);
                }
                networkWindow.show();
                showNetworkButton.setText("Ocultar Red Neuronal");
            } else {
                if (networkWindow != null) {
                    networkWindow.close();
                }
                showNetworkButton.setText("Mostrar Red Neuronal");
            }
        });

        VBox infoPanel = new VBox(10);
        infoPanel.setPadding(new Insets(10));
        infoPanel.getChildren().addAll(
                generationLabel,
                aliveLabel,
                scoreLabel,
                bestFitnessLabel,
                speedLabel,
                pauseButton,
                speedSliderLabel,
                speedSlider,
                loopCheckbox,
                maxGenLabel,
                maxGenSlider,
                showAllAgentsCheckbox,
                showNetworkButton
        );

        return infoPanel;
    }

    /**
     * Dibuja el estado actual del juego
     */
    private void drawGame() {
        // Dibujar fondo
        gc.setFill(Color.SKYBLUE);
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Dibujar nubes decorativas
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

        // Mostrar puntuación en pantalla
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        gc.setFont(Font.font("System", FontWeight.BOLD, 30));
        String scoreText = String.valueOf(game.getScore());
        gc.fillText(scoreText, CANVAS_WIDTH/2 - 15, 50);
        gc.strokeText(scoreText, CANVAS_WIDTH/2 - 15, 50);
    }

    /**
     * Actualiza las etiquetas de información
     */
    private void updateInfo() {
        int aliveCount = 0;
        for (FlappyBirdAgent agent : population.getAgents()) {
            if (!agent.isDead()) {
                aliveCount++;
            }
        }

        generationLabel.setText("Generación: " + currentGeneration);
        aliveLabel.setText("Vivos: " + aliveCount + "/" + POPULATION_SIZE);
        scoreLabel.setText("Puntuación: " + game.getScore());
        bestFitnessLabel.setText("Mejor Fitness: " + String.format("%.2f", population.getBestFitness()));
    }

    /**
     * Evoluciona a la siguiente generación
     */
    private void nextGeneration() {
        // Evolucionar población
        population.naturalSelection();

        // Reiniciar juego
        game.reset();

        // Reiniciar agentes
        for (FlappyBirdAgent agent : population.getAgents()) {
            agent.reset();
        }

        currentGeneration++;
        System.out.println("Generación " + currentGeneration +
                " - Mejor Fitness: " + population.getBestFitness());
    }

    /**
     * Cambia el estado de pausa/ejecución del juego
     */
    private void togglePause() {
        gamePaused = !gamePaused;
    }

    /**
     * Reinicia toda la simulación
     */
    private void resetSimulation() {
        population = new Population(POPULATION_SIZE);
        game.reset();
        currentGeneration = 1;

        // Si estaba pausado, reanudar
        gamePaused = false;
    }

    // Getter para el controlador de simulación
    public SimulationController getSimulationController() {
        return simulationController;
    }

    // Getter para el estado de la simulación
    public boolean isRunning() {
        return !gamePaused;
    }
}