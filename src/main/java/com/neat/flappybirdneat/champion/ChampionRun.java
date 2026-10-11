package com.neat.flappybirdneat.champion;

import com.neat.flappybirdneat.game.FlappyBirdGame;
import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import java.util.Random;

/** Plays a champion alone, without a UI, on a pipe sequence fixed by a seed. */
public final class ChampionRun {

    /** Same playfield as the UI and the headless training. */
    static final int CANVAS_WIDTH = 800;

    static final int CANVAS_HEIGHT = 600;

    private ChampionRun() {}

    /** @return the frames the champion survived, at most {@code maxFrames} */
    public static int play(Champion champion, long pipeSeed, int maxFrames) {
        FlappyBirdAgent agent = champion.newAgent();
        FlappyBirdGame game = new FlappyBirdGame(CANVAS_WIDTH, CANVAS_HEIGHT, new Random(pipeSeed));
        FlappyBirdAgent[] single = {agent};
        int frames = 0;
        while (!agent.isDead() && frames < maxFrames) {
            game.update(single);
            frames++;
        }
        return frames;
    }
}
