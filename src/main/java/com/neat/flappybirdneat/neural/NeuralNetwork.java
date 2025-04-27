package com.neat.flappybirdneat.neural;

import java.util.Random;

/**
 * Implementación de una red neuronal feedforward con una capa oculta.
 * Incluye funcionalidades para mutación y cruce genético.
 */
public class NeuralNetwork {
    private int inputSize;
    private int hiddenSize;
    private int outputSize;
    private double[][] weightsInputHidden;
    private double[][] weightsHiddenOutput;
    private double[] biasHidden;
    private double[] biasOutput;

    private final Random random = new Random();

    /**
     * Constructor
     * @param inputSize Número de neuronas en la capa de entrada
     * @param hiddenSize Número de neuronas en la capa oculta
     * @param outputSize Número de neuronas en la capa de salida
     */
    public NeuralNetwork(int inputSize, int hiddenSize, int outputSize) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.outputSize = outputSize;

        // Inicializar pesos con valores aleatorios entre -1 y 1
        weightsInputHidden = new double[inputSize][hiddenSize];
        weightsHiddenOutput = new double[hiddenSize][outputSize];
        biasHidden = new double[hiddenSize];
        biasOutput = new double[outputSize];

        initializeRandomWeights();
    }

    /**
     * Inicializa los pesos y bias con valores aleatorios
     */
    private void initializeRandomWeights() {
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
    public double[] feedForward(double[] inputs) {
        // Activación de la capa oculta
        double[] hiddenLayer = new double[hiddenSize];
        for (int i = 0; i < hiddenSize; i++) {
            double sum = biasHidden[i];
            for (int j = 0; j < inputSize; j++) {
                sum += inputs[j] * weightsInputHidden[j][i];
            }
            hiddenLayer[i] = sigmoid(sum);
        }

        // Activación de la capa de salida
        double[] outputs = new double[outputSize];
        for (int i = 0; i < outputSize; i++) {
            double sum = biasOutput[i];
            for (int j = 0; j < hiddenSize; j++) {
                sum += hiddenLayer[j] * weightsHiddenOutput[j][i];
            }
            outputs[i] = sigmoid(sum);
        }

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
     * Aplica mutaciones aleatorias a los pesos y bias
     * @param mutationRate Probabilidad de mutación (0-1)
     */
    public void mutate(double mutationRate) {
        // Mutar pesos de capa de entrada a capa oculta
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < hiddenSize; j++) {
                if (random.nextDouble() < mutationRate) {
                    weightsInputHidden[i][j] += random.nextGaussian() * 0.1;
                }
            }
        }

        // Mutar pesos de capa oculta a capa de salida
        for (int i = 0; i < hiddenSize; i++) {
            if (random.nextDouble() < mutationRate) {
                biasHidden[i] += random.nextGaussian() * 0.1;
            }

            for (int j = 0; j < outputSize; j++) {
                if (random.nextDouble() < mutationRate) {
                    weightsHiddenOutput[i][j] += random.nextGaussian() * 0.1;
                }
            }
        }

        // Mutar bias de capa de salida
        for (int i = 0; i < outputSize; i++) {
            if (random.nextDouble() < mutationRate) {
                biasOutput[i] += random.nextGaussian() * 0.1;
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
     * Realiza cruce entre dos redes neuronales (operador genético)
     * @param parent1 Primera red neuronal padre
     * @param parent2 Segunda red neuronal padre
     * @return Nueva red neuronal hijo
     */
    public static NeuralNetwork crossover(NeuralNetwork parent1, NeuralNetwork parent2) {
        NeuralNetwork child = new NeuralNetwork(
                parent1.inputSize,
                parent1.hiddenSize,
                parent1.outputSize
        );

        Random random = new Random();

        // Cruzar pesos de entrada a capa oculta
        for (int i = 0; i < parent1.inputSize; i++) {
            for (int j = 0; j < parent1.hiddenSize; j++) {
                if (random.nextBoolean()) {
                    child.weightsInputHidden[i][j] = parent1.weightsInputHidden[i][j];
                } else {
                    child.weightsInputHidden[i][j] = parent2.weightsInputHidden[i][j];
                }
            }
        }

        // Cruzar pesos y bias de capa oculta
        for (int i = 0; i < parent1.hiddenSize; i++) {
            if (random.nextBoolean()) {
                child.biasHidden[i] = parent1.biasHidden[i];
            } else {
                child.biasHidden[i] = parent2.biasHidden[i];
            }

            for (int j = 0; j < parent1.outputSize; j++) {
                if (random.nextBoolean()) {
                    child.weightsHiddenOutput[i][j] = parent1.weightsHiddenOutput[i][j];
                } else {
                    child.weightsHiddenOutput[i][j] = parent2.weightsHiddenOutput[i][j];
                }
            }
        }

        // Cruzar bias de capa de salida
        for (int i = 0; i < parent1.outputSize; i++) {
            if (random.nextBoolean()) {
                child.biasOutput[i] = parent1.biasOutput[i];
            } else {
                child.biasOutput[i] = parent2.biasOutput[i];
            }
        }

        return child;
    }

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
}