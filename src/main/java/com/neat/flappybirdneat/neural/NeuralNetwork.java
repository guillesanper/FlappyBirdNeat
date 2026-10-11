package com.neat.flappybirdneat.neural;

import java.util.Random;

/**
 * Implementación de una red neuronal feedforward con una capa oculta.
 * Incluye funcionalidades para mutación y cruce genético.
 */
public class NeuralNetwork implements Brain {
    private int inputSize;
    private int hiddenSize;
    private int outputSize;
    private double[][] weightsInputHidden;
    private double[][] weightsHiddenOutput;
    private double[] biasHidden;
    private double[] biasOutput;

    // State tracking for visualization
    private double[] lastInputs;
    private double[] lastHiddenActivations;
    private double[] lastOutputs;

    /**
     * Constructor con generador aleatorio inyectado, para reproducibilidad (tests, semillas fijas).
     * @param inputSize Número de neuronas en la capa de entrada
     * @param hiddenSize Número de neuronas en la capa oculta
     * @param outputSize Número de neuronas en la capa de salida
     * @param random Generador aleatorio a usar para inicializar los pesos
     */
    public NeuralNetwork(int inputSize, int hiddenSize, int outputSize, Random random) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.outputSize = outputSize;

        // Inicializar pesos con valores aleatorios entre -1 y 1
        weightsInputHidden = new double[inputSize][hiddenSize];
        weightsHiddenOutput = new double[hiddenSize][outputSize];
        biasHidden = new double[hiddenSize];
        biasOutput = new double[outputSize];

        initializeRandomWeights(random);
    }

    /**
     * Rebuilds a network from saved weights (e.g. a champion file). The arrays are copied.
     *
     * @param weightsInputHidden  [inputs][hidden] weights
     * @param weightsHiddenOutput [hidden][outputs] weights
     * @throws IllegalArgumentException if the shapes do not match or a value is not finite
     */
    public static NeuralNetwork fromWeights(
            double[][] weightsInputHidden, double[][] weightsHiddenOutput, double[] biasHidden, double[] biasOutput) {
        int inputs = weightsInputHidden.length;
        int hidden = biasHidden.length;
        int outputs = biasOutput.length;
        if (inputs == 0 || hidden == 0 || outputs == 0) {
            throw new IllegalArgumentException("Every layer needs at least one neuron");
        }
        requireShape("weightsInputHidden", weightsInputHidden, inputs, hidden);
        requireShape("weightsHiddenOutput", weightsHiddenOutput, hidden, outputs);
        requireFinite("biasHidden", biasHidden);
        requireFinite("biasOutput", biasOutput);

        NeuralNetwork network = new NeuralNetwork(inputs, hidden, outputs);
        for (int i = 0; i < inputs; i++) {
            network.weightsInputHidden[i] = weightsInputHidden[i].clone();
        }
        for (int i = 0; i < hidden; i++) {
            network.weightsHiddenOutput[i] = weightsHiddenOutput[i].clone();
        }
        network.biasHidden = biasHidden.clone();
        network.biasOutput = biasOutput.clone();
        return network;
    }

    /** Network with zero weights, filled in by {@link #fromWeights}. */
    private NeuralNetwork(int inputSize, int hiddenSize, int outputSize) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.outputSize = outputSize;
        weightsInputHidden = new double[inputSize][hiddenSize];
        weightsHiddenOutput = new double[hiddenSize][outputSize];
        biasHidden = new double[hiddenSize];
        biasOutput = new double[outputSize];
    }

    private static void requireShape(String name, double[][] matrix, int rows, int columns) {
        if (matrix.length != rows) {
            throw new IllegalArgumentException(name + " has " + matrix.length + " rows, expected " + rows);
        }
        for (double[] row : matrix) {
            if (row == null || row.length != columns) {
                throw new IllegalArgumentException(name + " rows must have " + columns + " values");
            }
            requireFinite(name, row);
        }
    }

    private static void requireFinite(String name, double[] values) {
        for (double value : values) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException(name + " contains a non-finite value: " + value);
            }
        }
    }

    /**
     * Inicializa los pesos y bias con valores aleatorios
     */
    private void initializeRandomWeights(Random random) {
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < hiddenSize; j++) {
                weightsInputHidden[i][j] = random.nextDouble() * 2 - 1;
            }
        }

        for (int i = 0; i < hiddenSize; i++) {
            biasHidden[i] = random.nextDouble() * 2 - 1;
            for (int j = 0; j < outputSize; j++) {
                weightsHiddenOutput[i][j] = random.nextDouble() * 2 - 1;
            }
        }

        for (int i = 0; i < outputSize; i++) {
            biasOutput[i] = random.nextDouble() * 2 - 1;
        }
    }

    /**
     * Propagación hacia adelante (feedforward)
     * @param inputs Valores de entrada
     * @return Valores de salida
     */
    @Override
    public double[] feedForward(double[] inputs) {
        // Store input state for visualization
        lastInputs = inputs.clone();

        // Activación de la capa oculta
        double[] hiddenLayer = new double[hiddenSize];
        for (int i = 0; i < hiddenSize; i++) {
            double sum = biasHidden[i];
            for (int j = 0; j < inputSize; j++) {
                sum += inputs[j] * weightsInputHidden[j][i];
            }
            hiddenLayer[i] = sigmoid(sum);
        }

        // Store hidden layer state for visualization
        lastHiddenActivations = hiddenLayer.clone();

        // Activación de la capa de salida
        double[] outputs = new double[outputSize];
        for (int i = 0; i < outputSize; i++) {
            double sum = biasOutput[i];
            for (int j = 0; j < hiddenSize; j++) {
                sum += hiddenLayer[j] * weightsHiddenOutput[j][i];
            }
            outputs[i] = sigmoid(sum);
        }

        // Store output state for visualization
        lastOutputs = outputs.clone();

        return outputs;
    }

    /**
     * Función de activación sigmoid
     * @param x Entrada
     * @return Valor sigmoid (entre 0 y 1)
     */
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    /**
     * Aplica mutaciones aleatorias a los pesos y bias con magnitud configurable.
     * @param mutationRate Probabilidad de mutación (0-1)
     * @param magnitude Desviación estándar del ruido gaussiano aplicado
     * @param random Generador de la simulación
     */
    public void mutate(double mutationRate, double magnitude, Random random) {
        // Mutar pesos de capa de entrada a capa oculta
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < hiddenSize; j++) {
                if (random.nextDouble() < mutationRate) {
                    weightsInputHidden[i][j] += random.nextGaussian() * magnitude;
                }
            }
        }

        // Mutar pesos de capa oculta a capa de salida
        for (int i = 0; i < hiddenSize; i++) {
            if (random.nextDouble() < mutationRate) {
                biasHidden[i] += random.nextGaussian() * magnitude;
            }

            for (int j = 0; j < outputSize; j++) {
                if (random.nextDouble() < mutationRate) {
                    weightsHiddenOutput[i][j] += random.nextGaussian() * magnitude;
                }
            }
        }

        // Mutar bias de capa de salida
        for (int i = 0; i < outputSize; i++) {
            if (random.nextDouble() < mutationRate) {
                biasOutput[i] += random.nextGaussian() * magnitude;
            }
        }
    }

    /**
     * Copia los pesos y bias de otra red neuronal
     * @param other Red neuronal de origen
     */
    public void setBrain(NeuralNetwork other) {
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < hiddenSize; j++) {
                this.weightsInputHidden[i][j] = other.weightsInputHidden[i][j];
            }
        }

        for (int i = 0; i < hiddenSize; i++) {
            this.biasHidden[i] = other.biasHidden[i];
            for (int j = 0; j < outputSize; j++) {
                this.weightsHiddenOutput[i][j] = other.weightsHiddenOutput[i][j];
            }
        }

        for (int i = 0; i < outputSize; i++) {
            this.biasOutput[i] = other.biasOutput[i];
        }
    }

    /**
     * Constructor de copia profunda.
     */
    public NeuralNetwork(NeuralNetwork other) {
        this.inputSize = other.inputSize;
        this.hiddenSize = other.hiddenSize;
        this.outputSize = other.outputSize;

        // Deep copy of weights and biases
        this.weightsInputHidden = new double[other.weightsInputHidden.length][];
        for (int i = 0; i < other.weightsInputHidden.length; i++) {
            this.weightsInputHidden[i] = other.weightsInputHidden[i].clone();
        }

        this.weightsHiddenOutput = new double[other.weightsHiddenOutput.length][];
        for (int i = 0; i < other.weightsHiddenOutput.length; i++) {
            this.weightsHiddenOutput[i] = other.weightsHiddenOutput[i].clone();
        }

        this.biasHidden = other.biasHidden.clone();
        this.biasOutput = other.biasOutput.clone();
    }

    // Getters para acceder a la estructura de la red
    public int getInputSize() {
        return inputSize;
    }

    public int getHiddenSize() {
        return hiddenSize;
    }

    public int getOutputSize() {
        return outputSize;
    }

    // Getters y setters para pesos y bias (para estrategias de cruce)
    public double[][] getWeightsInputHidden() {
        return weightsInputHidden;
    }

    public double[][] getWeightsHiddenOutput() {
        return weightsHiddenOutput;
    }

    public double[] getBiasHidden() {
        return biasHidden;
    }

    public double[] getBiasOutput() {
        return biasOutput;
    }

    public void setWeightsInputHidden(double[][] weights) {
        this.weightsInputHidden = weights;
    }

    public void setWeightsHiddenOutput(double[][] weights) {
        this.weightsHiddenOutput = weights;
    }

    public void setBiasHidden(double[] bias) {
        this.biasHidden = bias;
    }

    public void setBiasOutput(double[] bias) {
        this.biasOutput = bias;
    }

    // Getters for visualization (returns last computation state)
    public double[] getLastInputs() {
        return lastInputs;
    }

    public double[] getLastHiddenActivations() {
        return lastHiddenActivations;
    }

    public double[] getLastOutputs() {
        return lastOutputs;
    }
}
