package com.neat.flappybirdneat.view.main;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.simulation.SimulationController;
import com.neat.flappybirdneat.view.NeuralNetworkWindow;
import javafx.animation.AnimationTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frame loop of the main window. During headless training it only refreshes the statistics once
 * per second; otherwise it steps the live game at 60 fps times the chosen speed, draws it, moves
 * on to the next generation when every agent is dead (or restarts the best-agent replay), and
 * feeds the network window.
 */
final class GameLoop extends AnimationTimer {

    private static final Logger LOG = LoggerFactory.getLogger(GameLoop.class);
    private static final long ONE_SECOND_NANOS = 1_000_000_000L;

    private final SimulationController controller;
    private final LiveViewSettings settings;
    private final NeuralNetworkWindow networkWindow;
    private final Runnable drawFrame;
    private final Runnable onProgress;
    private long lastUpdate = 0;

    /**
     * @param drawFrame  draws the current game state
     * @param onProgress refreshes statistics and history after a generation (or periodically while training)
     */
    GameLoop(
            SimulationController controller,
            LiveViewSettings settings,
            NeuralNetworkWindow networkWindow,
            Runnable drawFrame,
            Runnable onProgress) {
        this.controller = controller;
        this.settings = settings;
        this.networkWindow = networkWindow;
        this.drawFrame = drawFrame;
        this.onProgress = onProgress;
    }

    @Override
    public void handle(long now) {
        if (controller.isFastMode()) {
            if (now - lastUpdate > ONE_SECOND_NANOS) {
                lastUpdate = now;
                onProgress.run();
            }
            return;
        }

        if (now - lastUpdate < ONE_SECOND_NANOS / (60L * settings.getGameSpeed())) {
            return;
        }
        lastUpdate = now;

        boolean allDead = controller.updateFrame();
        drawFrame.run();

        if (allDead) {
            if (controller.isReplayMode()) {
                // Replaying the best agent: restart it instead of evolving
                controller.getGame().reset();
                for (FlappyBirdAgent agent : controller.getPopulation().getAgents()) {
                    agent.reset();
                }
                LOG.info(
                        "Best agent died with fitness {}; restarting the replay",
                        String.format(
                                "%.2f",
                                controller.getPopulation().getAgents()[0].getFitness()));
            } else {
                controller.nextGeneration();
                onProgress.run();
            }
        }

        if (networkWindow.isShowing()) {
            FlappyBirdAgent bestAgent = controller.getPopulation().getBestAgent();
            if (!bestAgent.isDead()) {
                networkWindow.update(bestAgent, controller.getGame().getNextPipe(bestAgent));
            }
        }
    }
}
