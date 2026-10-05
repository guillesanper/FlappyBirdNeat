package com.neat.flappybirdneat.game;

import java.io.Serializable;
import java.util.Random;

/**
 * Clase que representa un obstáculo (tubo) en el juego Flappy Bird.
 * Define la posición, tamaño y comportamiento de los tubos.
 */
public class Pipe implements Serializable {
    private static final long serialVersionUID = 1L;

    private float x;
    private float gapY;
    private float gapSize;
    private float width;
    private static final float SPEED = 3;

    private transient Random random;

    /**
     * Constructor
     * @param x Posición inicial en el eje X
     * @param canvasHeight Altura del área de juego
     */
    public Pipe(float x, int canvasHeight) {
        this(x, canvasHeight, new Random());
    }

    /**
     * Constructor con generador aleatorio inyectado, para reproducibilidad (posición del hueco
     * determinista con una semilla fija).
     * @param x Posición inicial en el eje X
     * @param canvasHeight Altura del área de juego
     * @param random Generador aleatorio a usar para la posición del hueco
     */
    public Pipe(float x, int canvasHeight, Random random) {
        this.random = random;
        this.x = x;
        this.width = 80;
        this.gapSize = 150;
        this.gapY = (float) (random.nextDouble() * (canvasHeight - 200 - gapSize) + 100);
    }

    /**
     * Método llamado después de la deserialización para reinicializar campos transient
     */
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.random = new Random();
    }

    /**
     * Actualiza la posición del tubo
     */
    public void update() {
        x -= SPEED;
    }

    /**
     * Comprueba si el tubo está fuera de la pantalla
     * @return true si está fuera de la pantalla
     */
    public boolean isOffscreen() {
        return x + width < 0;
    }

    /**
     * Comprueba si un pájaro colisiona con este tubo
     * @param birdX Posición X del pájaro
     * @param birdY Posición Y del pájaro
     * @param birdSize Tamaño del pájaro
     * @return true si hay colisión
     */
    public boolean collides(float birdX, float birdY, float birdSize) {
        // Si el pájaro está a la derecha o izquierda del tubo, no hay colisión
        if (birdX + birdSize < x || birdX > x + width) {
            return false;
        }

        // Si el pájaro está dentro del hueco, no hay colisión
        if (birdY > gapY - gapSize/2 && birdY + birdSize < gapY + gapSize/2) {
            return false;
        }

        // En cualquier otro caso, hay colisión
        return true;
    }

    /**
     * @return Posición X del tubo
     */
    public float getX() {
        return x;
    }

    /**
     * @return Posición Y del centro del hueco
     */
    public float getGapY() {
        return gapY;
    }

    /**
     * @return Ancho del tubo
     */
    public float getWidth() {
        return width;
    }

    /**
     * @return Tamaño del hueco entre tubos
     */
    public float getGapSize() {
        return gapSize;
    }
}