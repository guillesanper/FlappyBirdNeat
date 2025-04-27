package com.neat.flappybirdneat.view;

import com.neat.flappybirdneat.neural.NeuralNetwork;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Clase que dibuja una representación visual de una red neuronal.
 */
public class NeuralNetworkVisualizer {

    /**
     * Dibuja una red neuronal en un contexto gráfico.
     *
     * @param gc El contexto gráfico donde dibujar
     * @param network La red neuronal a visualizar
     * @param x Posición X donde empezar a dibujar
     * @param y Posición Y donde empezar a dibujar
     * @param width Ancho total de la visualización
     * @param height Alto total de la visualización
     * @param inputs Valores actuales de entrada
     * @param outputs Valores actuales de salida
     */
    public static void drawNetwork(GraphicsContext gc, NeuralNetwork network,
                                   double x, double y, double width, double height,
                                   double[] inputs, double[] outputs) {
        // Asumimos que la red tiene una estructura 4-8-1 (entrada-oculta-salida)
        int inputSize = inputs.length;
        int hiddenSize = 8; // Asumimos 8 neuronas en la capa oculta
        int outputSize = outputs.length;

        double nodeRadius = 15;
        double layerSpacing = width / 3;

        // Calcular espaciado vertical para cada capa
        double inputSpacing = height / (inputSize + 1);
        double hiddenSpacing = height / (hiddenSize + 1);
        double outputSpacing = height / (outputSize + 1);

        // Dibujar etiquetas de las capas
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("System", FontWeight.BOLD, 14));
        gc.fillText("Entradas", x + 10, y - 10);
        gc.fillText("Capa Oculta", x + layerSpacing - 30, y - 10);
        gc.fillText("Salidas", x + 2 * layerSpacing - 20, y - 10);

        // Dibujar neuronas de entrada
        for (int i = 0; i < inputSize; i++) {
            double nodeY = y + (i + 1) * inputSpacing;

            // Color basado en el valor de activación
            // Azul para valores negativos, rojo para positivos
            double inputVal = inputs[i];
            Color nodeColor = getColorFromValue(inputVal);

            gc.setFill(nodeColor);
            gc.fillOval(x - nodeRadius, nodeY - nodeRadius, 2 * nodeRadius, 2 * nodeRadius);

            gc.setFill(Color.WHITE);
            gc.fillText(String.format("%.2f", inputVal), x - 10, nodeY + 5);

            // Etiquetas para las entradas
            gc.setFill(Color.BLACK);
            String inputLabel;
            switch(i) {
                case 0: inputLabel = "Altura"; break;
                case 1: inputLabel = "Velocidad"; break;
                case 2: inputLabel = "Dist. Tubo"; break;
                case 3: inputLabel = "Alt. Hueco"; break;
                default: inputLabel = "Input " + i;
            }
            gc.fillText(inputLabel, x - 100, nodeY + 5);
        }

        // Dibujar neuronas ocultas
        for (int i = 0; i < hiddenSize; i++) {
            double nodeY = y + (i + 1) * hiddenSpacing;

            // Las neuronas ocultas se dibujan en gris ya que no conocemos su valor
            gc.setFill(Color.GRAY);
            gc.fillOval(x + layerSpacing - nodeRadius, nodeY - nodeRadius,
                    2 * nodeRadius, 2 * nodeRadius);

            // Dibujar conexiones desde capa de entrada
            gc.setStroke(Color.LIGHTGRAY);
            for (int j = 0; j < inputSize; j++) {
                double inputY = y + (j + 1) * inputSpacing;
                gc.strokeLine(x + nodeRadius, inputY, x + layerSpacing - nodeRadius, nodeY);
            }
        }

        // Dibujar neuronas de salida
        for (int i = 0; i < outputSize; i++) {
            double nodeY = y + (i + 1) * outputSpacing;

            // Color basado en el valor de activación
            double outputVal = outputs[i];
            Color nodeColor = getColorFromValue(outputVal);

            gc.setFill(nodeColor);
            gc.fillOval(x + 2 * layerSpacing - nodeRadius, nodeY - nodeRadius,
                    2 * nodeRadius, 2 * nodeRadius);

            gc.setFill(Color.WHITE);
            gc.fillText(String.format("%.2f", outputVal), x + 2 * layerSpacing - 10, nodeY + 5);

            // Dibujar conexiones desde capa oculta
            gc.setStroke(Color.LIGHTGRAY);
            for (int j = 0; j < hiddenSize; j++) {
                double hiddenY = y + (j + 1) * hiddenSpacing;
                gc.strokeLine(x + layerSpacing + nodeRadius, hiddenY,
                        x + 2 * layerSpacing - nodeRadius, nodeY);
            }

            // Etiqueta para la salida
            gc.setFill(Color.BLACK);
            String outputLabel = outputVal > 0.5 ? "SALTAR" : "NO SALTAR";
            gc.fillText(outputLabel, x + 2 * layerSpacing + 30, nodeY + 5);
        }
    }

    /**
     * Devuelve un color basado en el valor, de -1 (azul) a 1 (rojo)
     */
    private static Color getColorFromValue(double value) {
        // Valores entre 0 y 1
        double normalizedValue = Math.max(0, Math.min(1, value));

        // Interpolación entre azul (0) y rojo (1)
        return new Color(normalizedValue, 0, 1 - normalizedValue, 0.8);
    }
}