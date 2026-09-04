package com.neat.flappybirdneat.view;

import com.neat.flappybirdneat.neat.genome.ConnectionGene;
import com.neat.flappybirdneat.neat.genome.Genome;
import com.neat.flappybirdneat.neat.genome.NodeGene;
import com.neat.flappybirdneat.neat.genome.NodeType;
import com.neat.flappybirdneat.neural.NeuralNetwork;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        // Network structure: 4-8-1 (input-hidden-output)
        int inputSize = inputs.length;
        int hiddenSize = network.getHiddenSize();
        int outputSize = outputs.length;

        double nodeRadius = 15;
        double layerSpacing = width / 3;

        // Calculate vertical spacing for each layer
        double inputSpacing = height / (inputSize + 1);
        double hiddenSpacing = height / (hiddenSize + 1);
        double outputSpacing = height / (outputSize + 1);

        // Get network state and weights
        double[] hiddenActivations = network.getLastHiddenActivations();
        double[][] weightsIH = network.getWeightsInputHidden();
        double[][] weightsHO = network.getWeightsHiddenOutput();

        // === PHASE 1: Draw layer labels ===
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("System", FontWeight.BOLD, 14));
        gc.fillText("Entradas", x + 10, y - 10);
        gc.fillText("Capa Oculta", x + layerSpacing - 30, y - 10);
        gc.fillText("Salidas", x + 2 * layerSpacing - 20, y - 10);

        // === PHASE 2: Draw all connections (so they appear behind neurons) ===

        // Draw input→hidden connections
        for (int i = 0; i < hiddenSize; i++) {
            double hiddenY = y + (i + 1) * hiddenSpacing;

            for (int j = 0; j < inputSize; j++) {
                double inputY = y + (j + 1) * inputSpacing;

                // Get weight for this connection
                double weight = (weightsIH != null && j < weightsIH.length && i < weightsIH[j].length)
                        ? weightsIH[j][i]
                        : 0.0;

                // Color and thickness based on weight
                Color connectionColor = getWeightColor(weight);
                double lineWidth = getWeightThickness(weight);

                gc.setStroke(connectionColor);
                gc.setLineWidth(lineWidth);
                gc.strokeLine(x + nodeRadius, inputY, x + layerSpacing - nodeRadius, hiddenY);
            }
        }

        // Draw hidden→output connections
        for (int i = 0; i < outputSize; i++) {
            double outputY = y + (i + 1) * outputSpacing;

            for (int j = 0; j < hiddenSize; j++) {
                double hiddenY = y + (j + 1) * hiddenSpacing;

                // Get weight for this connection
                double weight = (weightsHO != null && j < weightsHO.length && i < weightsHO[j].length)
                        ? weightsHO[j][i]
                        : 0.0;

                // Color and thickness based on weight
                Color connectionColor = getWeightColor(weight);
                double lineWidth = getWeightThickness(weight);

                gc.setStroke(connectionColor);
                gc.setLineWidth(lineWidth);
                gc.strokeLine(x + layerSpacing + nodeRadius, hiddenY,
                        x + 2 * layerSpacing - nodeRadius, outputY);
            }
        }

        // Reset line width to default
        gc.setLineWidth(1.0);

        // === PHASE 3: Draw all neurons (on top of connections) ===

        // Draw input neurons
        for (int i = 0; i < inputSize; i++) {
            double nodeY = y + (i + 1) * inputSpacing;

            // Color based on activation value
            double inputVal = inputs[i];
            Color nodeColor = getColorFromValue(inputVal);

            gc.setFill(nodeColor);
            gc.fillOval(x - nodeRadius, nodeY - nodeRadius, 2 * nodeRadius, 2 * nodeRadius);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 10));
            gc.fillText(String.format("%.2f", inputVal), x - 10, nodeY + 3);

            // Labels for inputs
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("System", FontWeight.NORMAL, 12));
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

        // Draw hidden neurons
        for (int i = 0; i < hiddenSize; i++) {
            double nodeY = y + (i + 1) * hiddenSpacing;

            // Use actual activation value for color
            double activation = (hiddenActivations != null && i < hiddenActivations.length)
                    ? hiddenActivations[i]
                    : 0.5; // Default to neutral if not available
            Color nodeColor = getColorFromValue(activation);

            gc.setFill(nodeColor);
            gc.fillOval(x + layerSpacing - nodeRadius, nodeY - nodeRadius,
                    2 * nodeRadius, 2 * nodeRadius);

            // Display activation value
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 10));
            gc.fillText(String.format("%.2f", activation),
                    x + layerSpacing - 10, nodeY + 3);
        }

        // Draw output neurons
        for (int i = 0; i < outputSize; i++) {
            double nodeY = y + (i + 1) * outputSpacing;

            // Color based on activation value
            double outputVal = outputs[i];
            Color nodeColor = getColorFromValue(outputVal);

            gc.setFill(nodeColor);
            gc.fillOval(x + 2 * layerSpacing - nodeRadius, nodeY - nodeRadius,
                    2 * nodeRadius, 2 * nodeRadius);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 10));
            gc.fillText(String.format("%.2f", outputVal), x + 2 * layerSpacing - 10, nodeY + 3);

            // Label for output
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("System", FontWeight.BOLD, 14));
            String outputLabel = outputVal > 0.5 ? "SALTAR" : "NO SALTAR";
            gc.fillText(outputLabel, x + 2 * layerSpacing + 30, nodeY + 5);
        }
    }

    /**
     * Dibuja un genoma NEAT de topología variable: a diferencia de {@link #drawNetwork}, el
     * número de columnas y de nodos por columna no es fijo, así que primero se calcula un
     * layout (columna por nodo vía {@link Genome#computeNodeLayers()}, posición vertical por
     * orden dentro de la columna) y luego se dibuja igual que la MLP fija: conexiones detrás,
     * nodos encima, coloreados por su última activación.
     *
     * @param gc El contexto gráfico donde dibujar
     * @param genome El genoma NEAT a visualizar
     * @param x Posición X donde empezar a dibujar
     * @param y Posición Y donde empezar a dibujar
     * @param width Ancho total de la visualización
     * @param height Alto total de la visualización
     * @param inputs Valores actuales de entrada
     * @param outputs Valores actuales de salida
     */
    public static void drawGenome(GraphicsContext gc, Genome genome,
                                   double x, double y, double width, double height,
                                   double[] inputs, double[] outputs) {
        double nodeRadius = 15;

        Map<Integer, Integer> nodeLayers = genome.computeNodeLayers();
        int maxLayer = 0;
        for (int l : nodeLayers.values()) maxLayer = Math.max(maxLayer, l);
        double layerSpacing = maxLayer > 0 ? width / maxLayer : width;

        // Agrupar nodos por columna, ordenados por id (orden de creación) para un layout estable
        // entre redibujados sucesivos.
        Map<Integer, List<NodeGene>> nodesByLayer = new HashMap<>();
        for (NodeGene node : genome.getNodes()) {
            int layer = nodeLayers.getOrDefault(node.getId(), 0);
            nodesByLayer.computeIfAbsent(layer, k -> new ArrayList<>()).add(node);
        }
        for (List<NodeGene> column : nodesByLayer.values()) {
            column.sort(Comparator.comparingInt(NodeGene::getId));
        }

        Map<Integer, double[]> position = new HashMap<>();
        for (Map.Entry<Integer, List<NodeGene>> entry : nodesByLayer.entrySet()) {
            int layer = entry.getKey();
            List<NodeGene> column = entry.getValue();
            double columnX = x + layer * layerSpacing;
            double verticalSpacing = height / (column.size() + 1);
            for (int i = 0; i < column.size(); i++) {
                double nodeY = y + (i + 1) * verticalSpacing;
                position.put(column.get(i).getId(), new double[]{columnX, nodeY});
            }
        }

        // === Etiquetas de capas ===
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("System", FontWeight.BOLD, 14));
        gc.fillText("Entradas", x + 10, y - 10);
        gc.fillText("Salidas", x + maxLayer * layerSpacing - 20, y - 10);
        if (maxLayer > 1) {
            gc.fillText("Ocultos", x + layerSpacing - 25, y - 10);
        }

        // === Conexiones (detrás de los nodos) ===
        for (ConnectionGene connection : genome.getConnections()) {
            double[] from = position.get(connection.getInNode());
            double[] to = position.get(connection.getOutNode());
            if (from == null || to == null) continue;

            if (!connection.isEnabled()) {
                gc.setStroke(new Color(0.6, 0.6, 0.6, 0.25));
                gc.setLineWidth(0.75);
            } else {
                gc.setStroke(getWeightColor(connection.getWeight()));
                gc.setLineWidth(getWeightThickness(connection.getWeight()));
            }
            gc.strokeLine(from[0] + nodeRadius, from[1], to[0] - nodeRadius, to[1]);
        }
        gc.setLineWidth(1.0);

        // === Nodos (encima de las conexiones) ===
        Map<Integer, Double> activations = genome.getLastActivations();
        int inputIndex = 0;
        String[] inputLabels = {"Altura", "Velocidad", "Dist. Tubo", "Alt. Hueco"};
        int outputIndex = 0;
        List<NodeGene> sortedNodes = new ArrayList<>(genome.getNodes());
        sortedNodes.sort(Comparator.comparingInt(NodeGene::getId));

        for (NodeGene node : sortedNodes) {
            double[] pos = position.get(node.getId());
            if (pos == null) continue;
            double nodeX = pos[0];
            double nodeY = pos[1];

            double activation;
            String valueLabel;
            switch (node.getType()) {
                case INPUT -> {
                    activation = inputIndex < inputs.length ? inputs[inputIndex] : 0.5;
                    valueLabel = String.format("%.2f", activation);
                }
                case BIAS -> {
                    activation = 1.0;
                    valueLabel = "1.0";
                }
                case OUTPUT -> {
                    activation = outputIndex < outputs.length ? outputs[outputIndex] : 0.5;
                    valueLabel = String.format("%.2f", activation);
                }
                default -> {
                    activation = activations.getOrDefault(node.getId(), 0.5);
                    valueLabel = String.format("%.2f", activation);
                }
            }

            Color nodeColor = node.getType() == NodeType.BIAS
                    ? new Color(0.4, 0.4, 0.4, 0.9)
                    : getColorFromValue(activation);
            gc.setFill(nodeColor);
            gc.fillOval(nodeX - nodeRadius, nodeY - nodeRadius, 2 * nodeRadius, 2 * nodeRadius);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 10));
            gc.fillText(valueLabel, nodeX - 10, nodeY + 3);

            if (node.getType() == NodeType.INPUT) {
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font("System", FontWeight.NORMAL, 12));
                String label = inputIndex < inputLabels.length ? inputLabels[inputIndex] : "Input " + inputIndex;
                gc.fillText(label, nodeX - 100, nodeY + 5);
                inputIndex++;
            } else if (node.getType() == NodeType.BIAS) {
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font("System", FontWeight.NORMAL, 12));
                gc.fillText("Bias", nodeX - 100, nodeY + 5);
            } else if (node.getType() == NodeType.OUTPUT) {
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font("System", FontWeight.BOLD, 14));
                String outputLabel = activation > 0.5 ? "SALTAR" : "NO SALTAR";
                gc.fillText(outputLabel, nodeX + 30, nodeY + 5);
                outputIndex++;
            }
        }
    }

    /**
     * Devuelve un color basado en el valor de activación del neurón
     * Low activation (0-0.5): Dark blue → Purple
     * High activation (0.5-1.0): Purple → Bright red
     */
    private static Color getColorFromValue(double value) {
        // Valores entre 0 y 1 (sigmoid outputs)
        double normalizedValue = Math.max(0, Math.min(1, value));

        if (normalizedValue < 0.5) {
            // Blue to purple (0 to 0.5)
            double t = normalizedValue * 2.0; // 0 to 1
            return new Color(t * 0.5, 0, 0.5 + t * 0.5, 0.9);
        } else {
            // Purple to red (0.5 to 1.0)
            double t = (normalizedValue - 0.5) * 2.0; // 0 to 1
            return new Color(0.5 + t * 0.5, 0, 1.0 - t * 0.5, 0.9);
        }
    }

    /**
     * Returns a color for connection weight visualization
     * Strong positive weights = bright green (excitatory)
     * Weak weights = light gray
     * Strong negative weights = bright red (inhibitory)
     */
    private static Color getWeightColor(double weight) {
        double absWeight = Math.abs(weight);

        if (absWeight < 0.1) {
            // Very weak connections - nearly invisible
            return new Color(0.8, 0.8, 0.8, 0.2);
        } else if (weight > 0) {
            // Positive weights - green (excitatory)
            double intensity = Math.min(1.0, absWeight / 3.0);
            return new Color(0, intensity, 0, 0.6);
        } else {
            // Negative weights - red (inhibitory)
            double intensity = Math.min(1.0, absWeight / 3.0);
            return new Color(intensity, 0, 0, 0.6);
        }
    }

    /**
     * Returns line thickness based on weight magnitude
     * Stronger connections = thicker lines
     */
    private static double getWeightThickness(double weight) {
        double absWeight = Math.abs(weight);

        if (absWeight < 0.1) {
            return 0.5; // Nearly invisible
        } else if (absWeight < 0.5) {
            return 1.0; // Weak
        } else if (absWeight < 1.5) {
            return 2.0; // Medium
        } else {
            return 3.5; // Strong
        }
    }
}