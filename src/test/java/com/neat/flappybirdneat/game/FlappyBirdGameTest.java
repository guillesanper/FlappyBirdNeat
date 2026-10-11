package com.neat.flappybirdneat.game;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import java.util.Random;
import org.junit.jupiter.api.Test;

class FlappyBirdGameTest {

    private static final float BIRD_X = 50;

    /**
     * Regresión: la puntuación subía cuando el tubo salía de la pantalla, unos 17 frames después
     * de que el pájaro lo hubiera pasado. Debe subir en el frame en que el borde trasero del tubo
     * queda por detrás del pájaro, y solo una vez por tubo.
     */
    @Test
    void scoreCountsAPipeAsSoonAsItsTrailingEdgePassesTheBird() {
        FlappyBirdGame game = new FlappyBirdGame(800, 600, new Random(1));
        FlappyBirdAgent[] noAgents = new FlappyBirdAgent[0];
        Pipe first = game.getPipes().get(0);

        while (first.getX() + first.getWidth() >= BIRD_X) {
            assertEquals(0, game.getScore(), "el pájaro aún no ha pasado el primer tubo");
            game.update(noAgents);
        }
        assertEquals(1, game.getScore(), "el primer tubo cuenta en cuanto queda por detrás del pájaro");

        while (game.getPipes().contains(first)) {
            game.update(noAgents);
        }
        assertEquals(1, game.getScore(), "un tubo que sale de la pantalla no vuelve a contar");
    }
}
