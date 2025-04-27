package com.neat.flappybirdneat.game;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase que implementa la lógica del juego Flappy Bird.
 * Gestiona los tubos, colisiones y puntuación.
 */
public class FlappyBirdGame {
    private List<Pipe> pipes;
    private int score;
    private int frameCount;
    private static final int PIPE_SPACING = 120;

    private final int canvasWidth;
    private final int canvasHeight;

    /**
     * Constructor
     * @param canvasWidth Ancho del área de juego
     * @param canvasHeight Alto del área de juego
     */
    public FlappyBirdGame(int canvasWidth, int canvasHeight) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        pipes = new ArrayList<>();
        reset();
    }

    /**
     * Reinicia el estado del juego
     */
    public void reset() {
        pipes.clear();
        // Añadir un tubo inicial
        pipes.add(new Pipe(canvasWidth, canvasHeight));
        score = 0;
        frameCount = 0;
    }

    /**
     * Actualiza el estado del juego y los agentes
     * @param agents Los agentes controlados por redes neuronales
     */
    public void update(FlappyBirdAgent[] agents) {
        frameCount++;

        // Añadir nuevo tubo cada cierto tiempo
        if (frameCount % PIPE_SPACING == 0) {
            pipes.add(new Pipe(canvasWidth, canvasHeight));
        }

        // Actualizar tubos
        for (int i = pipes.size() - 1; i >= 0; i--) {
            Pipe pipe = pipes.get(i);
            pipe.update();

            // Eliminar tubos fuera de pantalla
            if (pipe.isOffscreen()) {
                pipes.remove(i);
                score++;
            }
        }

        // Para cada agente
        for (FlappyBirdAgent agent : agents) {
            if (!agent.isDead()) {
                // Obtener info para la red neuronal
                Pipe nextPipe = getNextPipe(agent);
                if (nextPipe != null) {
                    agent.think(
                            agent.getY(),
                            nextPipe.getX() - 50, // 50 es el ancho del pájaro
                            nextPipe.getGapY(),
                            nextPipe.getGapSize()
                    );
                } else {
                    // Si no hay tubo, usar valores predeterminados
                    agent.think(agent.getY(), canvasWidth, canvasHeight/2, 150);
                }

                // Actualizar física del agente
                agent.update();

                // Comprobar colisiones
                checkCollision(agent);
            }
        }
    }

    /**
     * Obtiene el próximo tubo al que se enfrentará el agente
     * @param agent El agente para el que se busca el próximo tubo
     * @return El próximo tubo o null si no hay ninguno
     */
    private Pipe getNextPipe(FlappyBirdAgent agent) {
        for (Pipe pipe : pipes) {
            if (pipe.getX() + pipe.getWidth() > 50) { // 50 es x del pájaro
                return pipe;
            }
        }
        return null;
    }

    /**
     * Comprueba si un agente ha colisionado con algún obstáculo
     * @param agent El agente a comprobar
     */
    private void checkCollision(FlappyBirdAgent agent) {
        // Colisión con el suelo o techo
        if (agent.getY() < 0 || agent.getY() > canvasHeight - 20) { // -20 para el suelo
            agent.setDead(true);
            return;
        }

        // Colisión con tubos
        for (Pipe pipe : pipes) {
            if (pipe.collides(50, agent.getY(), 30)) { // x, y, tamaño del pájaro
                agent.setDead(true);
                return;
            }
        }
    }

    /**
     * @return La lista de tubos activos en el juego
     */
    public List<Pipe> getPipes() {
        return pipes;
    }

    /**
     * @return La puntuación actual
     */
    public int getScore() {
        return score;
    }
}
