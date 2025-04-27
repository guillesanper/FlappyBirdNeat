package com.neat.flappybirdneat.view;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.game.Pipe;

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

        // Preparar entradas para la visualización
        double[] inputs = new double[4];
        double distanceToNextPipe = nextPipe != null ? nextPipe.getX() - 50 : 500;
        double heightOfNextPipe = nextPipe != null ? nextPipe.getGapY() : 300;
        double gapSize = nextPipe != null ? nextPipe.getGapSize() : 150;

        // Normalizar entradas como se hace en FlappyBirdAgent.think()
        inputs[0] = agent.getY() / 600.0;
        inputs[1] = 0.5; // No tenemos acceso directo a la velocidad, así que usamos un valor neutral
        inputs[2] = distanceToNextPipe / 800.0;
        inputs[3] = heightOfNextPipe / 600.0;

        // Calcular la salida (simulando la decisión del agente)
        double[] outputs = agent.getBrain().feedForward(inputs);

        // Dibujar la red neuronal
        NeuralNetworkVisualizer.drawNetwork(
                gc, agent.getBrain(),
                120, 70, canvasWidth - 240, canvasHeight - 100,
                inputs, outputs
        );

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