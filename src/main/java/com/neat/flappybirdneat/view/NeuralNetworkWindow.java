package com.neat.flappybirdneat.view;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.neat.genome.Genome;
import com.neat.flappybirdneat.game.Pipe;
import com.neat.flappybirdneat.neural.NeuralNetwork;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Ventana que muestra la visualización de la red neuronal del mejor agente.
 */
public class NeuralNetworkWindow {
    private Stage stage;
    private Canvas canvas;
    private GraphicsContext gc;

    private FlappyBirdAgent agent;
    private Pipe nextPipe;
    private final int canvasWidth;
    private final int canvasHeight;

    /**
     * Constructor
     * @param width Ancho de la ventana
     * @param height Alto de la ventana
     */
    public NeuralNetworkWindow(int width, int height) {
        stage = new Stage();
        stage.setTitle("Visualización de Red Neuronal");

        this.canvasWidth = width;
        this.canvasHeight = height;

        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();

        BorderPane root = new BorderPane();
        root.setCenter(canvas);

        Scene scene = new Scene(root, width, height);
        stage.setScene(scene);
    }

    /**
     * Muestra la ventana
     */
    public void show() {
        stage.show();
    }

    /**
     * Actualiza la información del mejor agente y el próximo tubo
     * @param agent El agente a visualizar
     * @param nextPipe El próximo tubo que enfrentará el agente
     */
    public void update(FlappyBirdAgent agent, Pipe nextPipe) {
        this.agent = agent;
        this.nextPipe = nextPipe;

        // Redibujar la visualización
        draw();
    }

    /**
     * Dibuja la red neuronal con la información actual
     */
    private void draw() {
        // Limpiar el canvas
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvasWidth, canvasHeight);

        if (agent == null) {
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("System", FontWeight.NORMAL, 16));
            gc.fillText("Esperando datos del agente...", 50, 50);
            return;
        }

        // Título
        gc.setFill(Color.DARKBLUE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 18));
        gc.fillText("Red Neuronal del Mejor Agente", 20, 30);

        // Obtener los inputs y outputs que el cerebro realmente usó en su última decisión
        // (almacenados en el state tracking de la MLP o del genoma NEAT).
        double[] inputs;
        double[] outputs;
        if (agent.getBrain() instanceof NeuralNetwork network) {
            inputs = network.getLastInputs();
            outputs = network.getLastOutputs();
        } else if (agent.getBrain() instanceof Genome genome) {
            inputs = genome.getLastInputs();
            outputs = genome.getLastOutputs();
        } else {
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("System", FontWeight.NORMAL, 14));
            gc.fillText("Visualización no disponible para este tipo de cerebro.", 20, 60);
            return;
        }

        // Si no hay datos (primera ejecución), usar valores por defecto
        if (inputs == null || outputs == null) {
            inputs = new double[]{0.5, 0.5, 0.5, 0.5};
            outputs = new double[]{0.5};
        }

        // Información adicional para mostrar al usuario
        double distanceToNextPipe = nextPipe != null ? nextPipe.getX() - 50 : 500;
        double heightOfNextPipe = nextPipe != null ? nextPipe.getGapY() : 300;
        double gapSize = nextPipe != null ? nextPipe.getGapSize() : 150;

        // Dibujar la red: MLP fija o grafo NEAT de topología variable
        if (agent.getBrain() instanceof NeuralNetwork network) {
            NeuralNetworkVisualizer.drawNetwork(
                    gc, network,
                    120, 70, canvasWidth - 240, canvasHeight - 100,
                    inputs, outputs
            );
        } else if (agent.getBrain() instanceof Genome genome) {
            NeuralNetworkVisualizer.drawGenome(
                    gc, genome,
                    120, 70, canvasWidth - 240, canvasHeight - 100,
                    inputs, outputs
            );
        }

        // Información adicional
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("System", FontWeight.NORMAL, 14));
        gc.fillText("Estado actual:", 20, canvasHeight - 60);
        gc.fillText("Posición Y: " + String.format("%.2f", agent.getY()), 40, canvasHeight - 40);

        if (nextPipe != null) {
            gc.fillText("Distancia al próximo tubo: " + String.format("%.2f", distanceToNextPipe), 40, canvasHeight - 20);
            gc.fillText("Altura del hueco: " + String.format("%.2f", heightOfNextPipe), 300, canvasHeight - 40);
            gc.fillText("Tamaño del hueco: " + String.format("%.2f", gapSize), 300, canvasHeight - 20);
        }
    }

    /**
     * Cierra la ventana
     */
    public void close() {
        stage.close();
    }

    /**
     * @return true si la ventana está visible
     */
    public boolean isShowing() {
        return stage.isShowing();
    }
}