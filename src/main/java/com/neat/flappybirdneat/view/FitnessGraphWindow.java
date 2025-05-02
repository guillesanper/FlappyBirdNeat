package com.neat.flappybirdneat.view;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Ventana que muestra una gráfica de la evolución del fitness a lo largo de las generaciones.
 */
public class FitnessGraphWindow {
    private Stage stage;
    private Canvas canvas;
    private GraphicsContext gc;

    private final int canvasWidth;
    private final int canvasHeight;

    // Listas para almacenar el historial de fitness
    private List<Double> bestFitnessHistory;
    private List<Double> currentBestFitnessHistory;
    private List<Double> averageFitnessHistory;

    // Valores máximos y mínimos para escalar la gráfica
    private double maxFitness = 100;
    private double minFitness = 0;

    // Máximo de generaciones a mostrar en la ventana
    private final int MAX_GENERATIONS_DISPLAYED = 50;

    /**
     * Constructor
     * @param width Ancho de la ventana
     * @param height Alto de la ventana
     */
    public FitnessGraphWindow(int width, int height) {
        stage = new Stage();
        stage.setTitle("Evolución del Fitness");

        this.canvasWidth = width;
        this.canvasHeight = height;

        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();

        BorderPane root = new BorderPane();
        root.setCenter(canvas);

        Scene scene = new Scene(root, width, height);
        stage.setScene(scene);

        // Inicializar las listas
        bestFitnessHistory = new ArrayList<>();
        currentBestFitnessHistory = new ArrayList<>();
        averageFitnessHistory = new ArrayList<>();
    }

    /**
     * Muestra la ventana
     */
    public void show() {
        stage.show();
        draw();
    }

    /**
     * Actualiza los datos de fitness para la generación actual
     * @param bestFitness El mejor fitness global
     * @param currentBestFitness El mejor fitness de la generación actual
     * @param averageFitness El fitness promedio de la generación actual
     */
    public void updateData(double bestFitness, double currentBestFitness, double averageFitness) {
        // Añadir los nuevos valores
        bestFitnessHistory.add(bestFitness);
        currentBestFitnessHistory.add(currentBestFitness);
        averageFitnessHistory.add(averageFitness);

        // Limitar el número de generaciones mostradas
        if (bestFitnessHistory.size() > MAX_GENERATIONS_DISPLAYED) {
            bestFitnessHistory.remove(0);
            currentBestFitnessHistory.remove(0);
            averageFitnessHistory.remove(0);
        }

        // Actualizar valores máximos y mínimos
        maxFitness = bestFitness * 1.1; // Añadir un 10% de margen
        minFitness = 0; // Comenzar desde 0

        // Redibujar la gráfica
        draw();
    }

    /**
     * Dibuja la gráfica con los datos actuales
     */
    private void draw() {
        // Limpiar el canvas
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvasWidth, canvasHeight);

        // Si no hay datos, mostrar un mensaje
        if (bestFitnessHistory.isEmpty()) {
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("System", FontWeight.NORMAL, 16));
            gc.fillText("Esperando datos...", 50, 50);
            return;
        }

        // Márgenes para la gráfica
        int marginLeft = 60;
        int marginRight = 20;
        int marginTop = 50;
        int marginBottom = 50;

        // Área disponible para la gráfica
        int graphWidth = canvasWidth - marginLeft - marginRight;
        int graphHeight = canvasHeight - marginTop - marginBottom;

        // Dibujar ejes
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeLine(marginLeft, marginTop, marginLeft, canvasHeight - marginBottom);
        gc.strokeLine(marginLeft, canvasHeight - marginBottom, canvasWidth - marginRight, canvasHeight - marginBottom);

        // Dibujar título
        gc.setFill(Color.DARKBLUE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 18));
        gc.fillText("Evolución del Fitness", marginLeft, 30);

        // Etiquetas de los ejes
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("System", FontWeight.NORMAL, 14));
        gc.fillText("Fitness", 15, marginTop + graphHeight / 2);
        gc.fillText("Generación", marginLeft + graphWidth / 2 - 30, canvasHeight - 15);

        // Dibujar escala en eje Y
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("System", FontWeight.NORMAL, 12));
        int numYTicks = 5;
        for (int i = 0; i <= numYTicks; i++) {
            double yValue = minFitness + (maxFitness - minFitness) * i / numYTicks;
            double y = canvasHeight - marginBottom - (graphHeight * i / numYTicks);
            gc.fillText(String.format("%.0f", yValue), marginLeft - 35, y + 5);

            // Líneas guía horizontales
            gc.setStroke(Color.LIGHTGRAY);
            gc.setLineWidth(1);
            gc.strokeLine(marginLeft, y, canvasWidth - marginRight, y);
        }

        // Dibujar escala en eje X
        int numXTicks = Math.min(10, bestFitnessHistory.size());
        for (int i = 0; i <= numXTicks; i++) {
            int genIndex = bestFitnessHistory.size() - 1 - (bestFitnessHistory.size() - 1) * (numXTicks - i) / numXTicks;
            int generation = bestFitnessHistory.size() - (bestFitnessHistory.size() - genIndex);
            double x = marginLeft + (graphWidth * i / numXTicks);
            gc.setFill(Color.BLACK);
            gc.fillText(String.valueOf(generation), x - 5, canvasHeight - marginBottom + 20);

            // Líneas guía verticales
            gc.setStroke(Color.LIGHTGRAY);
            gc.setLineWidth(1);
            gc.strokeLine(x, marginTop, x, canvasHeight - marginBottom);
        }

        // Función para mapear valores de fitness a coordenadas Y
        double yScale = graphHeight / (maxFitness - minFitness);

        // Dibujar líneas de datos
        if (bestFitnessHistory.size() > 1) {
            drawDataLine(bestFitnessHistory, Color.RED, marginLeft, marginBottom, graphWidth, yScale);
            drawDataLine(currentBestFitnessHistory, Color.GREEN, marginLeft, marginBottom, graphWidth, yScale);
            drawDataLine(averageFitnessHistory, Color.BLUE, marginLeft, marginBottom, graphWidth, yScale);
        } else {
            // Si solo hay un punto, dibujar puntos
            double x = marginLeft;
            double y = canvasHeight - marginBottom - (bestFitnessHistory.get(0) - minFitness) * yScale;
            gc.setFill(Color.RED);
            gc.fillOval(x - 3, y - 3, 6, 6);

            y = canvasHeight - marginBottom - (currentBestFitnessHistory.get(0) - minFitness) * yScale;
            gc.setFill(Color.GREEN);
            gc.fillOval(x - 3, y - 3, 6, 6);

            y = canvasHeight - marginBottom - (averageFitnessHistory.get(0) - minFitness) * yScale;
            gc.setFill(Color.BLUE);
            gc.fillOval(x - 3, y - 3, 6, 6);
        }

        // Leyenda
        int legendX = marginLeft + 20;
        int legendY = marginTop + 20;

        // Mejor fitness global
        gc.setFill(Color.RED);
        gc.fillRect(legendX, legendY, 15, 15);
        gc.setFill(Color.BLACK);
        gc.fillText("Mejor Global", legendX + 25, legendY + 12);

        // Mejor fitness generacional
        gc.setFill(Color.GREEN);
        gc.fillRect(legendX, legendY + 25, 15, 15);
        gc.setFill(Color.BLACK);
        gc.fillText("Mejor Generacional", legendX + 25, legendY + 37);

        // Fitness promedio
        gc.setFill(Color.BLUE);
        gc.fillRect(legendX, legendY + 50, 15, 15);
        gc.setFill(Color.BLACK);
        gc.fillText("Promedio", legendX + 25, legendY + 62);
    }

    /**
     * Dibuja una línea de datos en la gráfica
     * @param dataPoints Lista de puntos de datos
     * @param color Color de la línea
     * @param marginLeft Margen izquierdo
     * @param marginBottom Margen inferior
     * @param graphWidth Ancho de la gráfica
     * @param yScale Escala vertical
     */
    private void drawDataLine(List<Double> dataPoints, Color color, int marginLeft, int marginBottom, int graphWidth, double yScale) {
        gc.setStroke(color);
        gc.setLineWidth(2);

        double xStep = (double) graphWidth / (dataPoints.size() - 1);

        for (int i = 0; i < dataPoints.size() - 1; i++) {
            double x1 = marginLeft + i * xStep;
            double y1 = canvasHeight - marginBottom - (dataPoints.get(i) - minFitness) * yScale;
            double x2 = marginLeft + (i + 1) * xStep;
            double y2 = canvasHeight - marginBottom - (dataPoints.get(i + 1) - minFitness) * yScale;

            gc.strokeLine(x1, y1, x2, y2);
        }

        // Dibujar puntos en los datos
        gc.setFill(color);
        for (int i = 0; i < dataPoints.size(); i++) {
            double x = marginLeft + i * xStep;
            double y = canvasHeight - marginBottom - (dataPoints.get(i) - minFitness) * yScale;
            gc.fillOval(x - 3, y - 3, 6, 6);
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