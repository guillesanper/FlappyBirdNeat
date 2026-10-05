package com.neat.flappybirdneat.view;

import com.neat.flappybirdneat.game.FlappyBirdGame;
import com.neat.flappybirdneat.game.Pipe;
import com.neat.flappybirdneat.neat.EvolvingPopulation;
import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.neat.Population;
import java.util.Random;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Componente principal que integra el algoritmo evolutivo de redes neuronales
 * con JavaFX para visualizar el aprendizaje de los agentes en Flappy Bird.
 */
public class FlappyBirdGameUI {
    private static final Logger LOG = LoggerFactory.getLogger(FlappyBirdGameUI.class);

    // Configuración principal
    private static final int POPULATION_SIZE = 50;
    private static final int CANVAS_WIDTH = 800;
    private static final int CANVAS_HEIGHT = 600;

    // Variables del juego
    private EvolvingPopulation population;
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
    private final GameRenderer renderer = new GameRenderer(CANVAS_WIDTH, CANVAS_HEIGHT);
    private Label generationLabel;
    private Label aliveLabel;
    private Label scoreLabel;
    private Label bestFitnessLabel;
    private Label speedLabel;
    private AnimationTimer gameLoop;
    private Stage primaryStage;

    // Generador de la repetición (derivado de la semilla global por quien abre la ventana)
    private Random random;

    // Ventana de visualización de la red neuronal
    private NeuralNetworkWindow networkWindow;
    private boolean showNeuralNetwork = false;

    public FlappyBirdGameUI() {
        // El constructor vacío no inicializa nada, se hará mediante prepareStage
        networkWindow = null; // Se inicializará bajo demanda
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
    public void startGameLoop(EvolvingPopulation population) {
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
                    // Actualizar juego
                    game.update(population.getAgents());

                    // Actualizar visualización de red neuronal si está activa
                    if (showNeuralNetwork && networkWindow.isShowing()) {
                        // Encontrar el mejor agente ACTIVO (que está realmente jugando)
                        FlappyBirdAgent bestActiveAgent = null;
                        double bestActiveFitness = -1;
                        for (FlappyBirdAgent agent : population.getAgents()) {
                            if (!agent.isDead() && agent.getFitness() > bestActiveFitness) {
                                bestActiveFitness = agent.getFitness();
                                bestActiveAgent = agent;
                            }
                        }

                        // Si hay un agente vivo, visualizar su red neuronal
                        if (bestActiveAgent != null) {
                            Pipe nextPipe = game.getNextPipe(bestActiveAgent);
                            networkWindow.update(bestActiveAgent, nextPipe);
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
                    renderer.renderPausedOverlay(gc);
                }
            }
        };
        gameLoop.start();
    }

    /**
     * Prepara el escenario para visualizar una población específica
     */
    public void prepareStage(Stage stage, EvolvingPopulation population, int generationNumber, Random random) {
        this.primaryStage = stage;
        this.random = random;

        // Copia independiente de la población guardada: la repetición evoluciona con su propio
        // generador y no modifica ni el historial ni la ejecución en curso
        this.population = population.deepCopy(random);
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
        game = new FlappyBirdGame(CANVAS_WIDTH, CANVAS_HEIGHT, random);

        // Reiniciar los agentes para la visualización
        for (FlappyBirdAgent agent : this.population.getAgents()) {
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
        startGameLoop(this.population);
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
        infoPanel
                .getChildren()
                .addAll(
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
                        showNetworkButton);

        return infoPanel;
    }

    private void drawGame() {
        renderer.render(
                gc,
                game,
                population.getAgents(),
                population.getBestAgent(),
                GameRenderer.Options.generationReplay(showAllAgents));
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
        LOG.debug("Replay generation {} - best fitness {}", currentGeneration, population.getBestFitness());
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
        population = new Population(POPULATION_SIZE, random);
        game.reset();
        currentGeneration = 1;

        // Si estaba pausado, reanudar
        gamePaused = false;
    }

    // Getter para el estado de la simulación
    public boolean isRunning() {
        return !gamePaused;
    }
}
