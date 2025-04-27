package com.neat.flappybirdneat;

import com.neat.flappybirdneat.game.FlappyBirdGame;
import com.neat.flappybirdneat.game.Pipe;
import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.neat.Population;
import com.neat.flappybirdneat.view.NeuralNetworkWindow;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
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
import javafx.stage.Stage;

/**
 * Clase principal que integra el algoritmo evolutivo de redes neuronales
 * con JavaFX para visualizar el aprendizaje de los agentes en Flappy Bird.
 */
public class FlappyBirdNEAT extends Application {
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

    // Variables para la interfaz gráfica
    private Canvas canvas;
    private GraphicsContext gc;
    private Label generationLabel;
    private Label aliveLabel;
    private Label scoreLabel;
    private Label bestFitnessLabel;
    private Label speedLabel;
    private AnimationTimer gameLoop;

    // Ventana de visualización de la red neuronal
    private NeuralNetworkWindow networkWindow;
    private boolean showNeuralNetwork = false;

    /**
     * Método principal para iniciar la aplicación JavaFX
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Inicializar población y juego
        population = new Population(POPULATION_SIZE);
        game = new FlappyBirdGame(CANVAS_WIDTH, CANVAS_HEIGHT);

        // Inicializar ventana de red neuronal
        networkWindow = new NeuralNetworkWindow(600, 400);

        // Configurar la interfaz gráfica
        BorderPane root = new BorderPane();

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
                showAllAgentsCheckbox,
                autoRestartCheckbox,
                showNetworkButton
        );

        root.setRight(infoPanel);

        Scene scene = new Scene(root, CANVAS_WIDTH + 200, CANVAS_HEIGHT);
        primaryStage.setTitle("Flappy Bird NEAT");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Manejar el cierre de la ventana principal
        primaryStage.setOnCloseRequest(e -> {
            if (networkWindow.isShowing()) {
                networkWindow.close();
            }
        });

        // Iniciar el bucle del juego
        startGameLoop();
    }

    /**
     * Inicia el bucle principal del juego
     */
    private void startGameLoop() {
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
                        if (currentGeneration >= 50 && autoRestartOnExtinction) {
                            // Reiniciar después de 50 generaciones para evitar estancamiento
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
}