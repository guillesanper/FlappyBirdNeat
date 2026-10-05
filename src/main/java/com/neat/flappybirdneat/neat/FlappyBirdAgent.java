package com.neat.flappybirdneat.neat;

import com.neat.flappybirdneat.neat.genome.Genome;
import com.neat.flappybirdneat.neural.Brain;
import com.neat.flappybirdneat.neural.NeuralNetwork;

import java.util.Random;

/**
 * Clase que representa un agente controlado por un {@link Brain} (MLP de topología fija o
 * genoma NEAT de topología evolutiva) que juega a Flappy Bird. La física y las reglas del
 * juego son agnósticas al tipo de cerebro: {@link #think} solo necesita {@code feedForward}.
 */
public class FlappyBirdAgent {
    private Brain brain;
    private double fitness;
    private boolean isDead;

    // Posición y física del pájaro
    private float y;
    private float velocity;
    private static final float GRAVITY = 0.8f;
    private static final float JUMP_FORCE = -12f;

    /**
     * Constructor
     * @param inputSize Número de entradas de la red neuronal
     * @param hiddenSize Número de neuronas en la capa oculta
     * @param outputSize Número de salidas de la red neuronal
     */
    public FlappyBirdAgent(int inputSize, int hiddenSize, int outputSize) {
        brain = new NeuralNetwork(inputSize, hiddenSize, outputSize);
        reset();
    }

    /**
     * Constructor con generador aleatorio inyectado, para reproducibilidad (tests, semillas fijas).
     * @param inputSize Número de entradas de la red neuronal
     * @param hiddenSize Número de neuronas en la capa oculta
     * @param outputSize Número de salidas de la red neuronal
     * @param random Generador aleatorio a usar para inicializar el cerebro
     */
    public FlappyBirdAgent(int inputSize, int hiddenSize, int outputSize, Random random) {
        brain = new NeuralNetwork(inputSize, hiddenSize, outputSize, random);
        reset();
    }

    /**
     * Constructor para agentes NEAT: el cerebro es un {@link Genome} de topología variable
     * en vez de la MLP densa de tamaño fijo.
     * @param brain Cerebro (genoma NEAT u otra implementación de Brain) que controla al agente
     */
    public FlappyBirdAgent(Brain brain) {
        this.brain = brain;
        reset();
    }

    /**
     * Reinicia el estado del agente
     */
    public void reset() {
        y = 300;
        velocity = 0;
        fitness = 0;
        isDead = false;
    }

    /**
     * Procesa la información del entorno y decide acciones
     * @param birdY Posición Y del pájaro
     * @param distanceToNextPipe Distancia al próximo tubo
     * @param heightOfNextPipe Altura del próximo tubo
     * @param nextPipeGapSize Tamaño del hueco del próximo tubo
     */
    public void think(float birdY, float distanceToNextPipe, float heightOfNextPipe, float nextPipeGapSize) {
        // Normalizar entradas
        double[] inputs = new double[4];
        inputs[0] = birdY / 600.0;  // Posición Y normalizada
        inputs[1] = velocity / 15.0;  // Velocidad normalizada
        inputs[2] = distanceToNextPipe / 800.0;  // Distancia normalizada
        inputs[3] = heightOfNextPipe / 600.0;  // Altura del hueco normalizada

        double[] outputs = brain.feedForward(inputs);

        // Si la salida es mayor que 0.5, el pájaro salta
        if (outputs[0] > 0.5) {
            jump();
        }
    }

    /**
     * Actualiza la física del agente
     */
    public void update() {
        fitness++;

        // Aplicar gravedad
        velocity += GRAVITY;
        y += velocity;
    }

    /**
     * Hace que el pájaro salte
     */
    public void jump() {
        velocity = JUMP_FORCE;
    }

    /**
     * @return El cerebro (MLP fija o genoma NEAT) que controla al agente
     */
    public Brain getBrain() {
        return brain;
    }

    /**
     * @return El valor de fitness del agente
     */
    public double getFitness() {
        return fitness;
    }

    /**
     * Establece el fitness del agente
     * @param fitness Nuevo valor de fitness
     */
    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    /**
     * @return Si el agente está muerto
     */
    public boolean isDead() {
        return isDead;
    }

    /**
     * Establece si el agente está muerto
     * @param dead Estado de vida del agente
     */
    public void setDead(boolean dead) {
        isDead = dead;
    }

    /**
     * @return Posición Y del agente
     */
    public float getY() {
        return y;
    }

    /**
     * @return Velocidad vertical del agente
     */
    public float getVelocity() {
        return velocity;
    }

    public FlappyBirdAgent(FlappyBirdAgent other) {
        // Deep copy del cerebro, sea cual sea su tipo concreto
        if (other.brain instanceof NeuralNetwork network) {
            this.brain = new NeuralNetwork(network);
        } else if (other.brain instanceof Genome genome) {
            this.brain = genome.copy();
        } else {
            throw new IllegalStateException("Tipo de cerebro no soportado para copia: " + other.brain.getClass());
        }

        // Copy primitive fields
        this.fitness = other.fitness;
        this.isDead = other.isDead;
        this.y = other.y;
        this.velocity = other.velocity;
    }
}