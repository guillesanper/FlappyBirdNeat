package com.neat.flappybirdneat.view;

import com.neat.flappybirdneat.game.FlappyBirdGame;
import com.neat.flappybirdneat.game.Pipe;
import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Draws a game frame (sky, pipes, birds, ground and score) on a canvas. Shared by the main
 * window's live simulation and by the generation replay window, which only differ in the
 * overlays described by {@link Options}.
 */
public final class GameRenderer {

    private static final double BIRD_X = 50;
    private static final double BIRD_SIZE = 30;
    private static final double GROUND_HEIGHT = 20;
    private static final Color AGENT_FILL = new Color(1, 1, 0, 0.3);
    private static final Color HALO_GLOW = new Color(1, 0.84, 0, 0.5);
    private static final Color OVERLAY = new Color(0, 0, 0, 0.7);

    /**
     * What to draw on top of the plain game scene.
     *
     * @param showAllAgents   draw the non-best agents (semi-transparent), not just the best one
     * @param highlightAll    draw every live agent as "the best" (replay of a single agent)
     * @param bestHalo        surround the highlighted bird with a golden halo
     * @param replayBanner    text of the replay banner shown at the top, or null for none
     * @param bestOnlyNotice  show the "showing the best agent" notice when other agents are hidden
     */
    public record Options(
            boolean showAllAgents,
            boolean highlightAll,
            boolean bestHalo,
            String replayBanner,
            boolean bestOnlyNotice) {

        /**
         * Main window: live training, or the replay of the best agent ever found or of a champion.
         *
         * @param replayBanner banner shown while replaying (ignored outside replay mode)
         */
        public static Options liveSimulation(
                boolean showAllAgents, boolean replayMode, boolean singleAgent, String replayBanner) {
            return new Options(showAllAgents, replayMode || singleAgent, true, replayMode ? replayBanner : null, true);
        }

        /** Replay window of a saved generation. */
        public static Options generationReplay(boolean showAllAgents) {
            return new Options(showAllAgents, false, false, null, false);
        }
    }

    private final double width;
    private final double height;

    public GameRenderer(double width, double height) {
        this.width = width;
        this.height = height;
    }

    /**
     * @param best the agent drawn in red (the population's best agent so far)
     */
    public void render(
            GraphicsContext gc, FlappyBirdGame game, FlappyBirdAgent[] agents, FlappyBirdAgent best, Options options) {
        drawSky(gc);
        for (Pipe pipe : game.getPipes()) {
            drawPipe(gc, pipe);
        }
        for (FlappyBirdAgent agent : agents) {
            if (agent.isDead()) {
                continue;
            }
            if (agent == best || options.highlightAll()) {
                drawBestBird(gc, agent.getY(), options.bestHalo());
            } else if (options.showAllAgents()) {
                gc.setFill(AGENT_FILL);
                gc.fillOval(BIRD_X, agent.getY(), BIRD_SIZE, BIRD_SIZE);
            }
        }
        if (options.replayBanner() != null) {
            Font font = Font.font("System", FontWeight.BOLD, 20);
            Text measure = new Text(options.replayBanner());
            measure.setFont(font);
            gc.setFill(OVERLAY);
            gc.fillRect(10, 10, measure.getLayoutBounds().getWidth() + 20, 40);
            gc.setFill(Color.GOLD);
            gc.setFont(font);
            gc.fillText(options.replayBanner(), 20, 35);
        }
        drawGround(gc);
        drawScore(gc, game.getScore());
        if (options.bestOnlyNotice() && !options.showAllAgents()) {
            gc.setFill(OVERLAY);
            gc.fillRect(10, height - 60, 300, 30);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", FontWeight.BOLD, 16));
            gc.fillText("Mostrando al mejor agente", 20, height - 40);
        }
    }

    /** Darkens the frame and writes "paused" on top (replay window). */
    public void renderPausedOverlay(GraphicsContext gc) {
        gc.setFill(new Color(0, 0, 0, 0.3));
        gc.fillRect(0, 0, width, height);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 30));
        gc.fillText("SIMULACIÓN PAUSADA", width / 2 - 150, height / 2);
    }

    private void drawSky(GraphicsContext gc) {
        gc.setFill(Color.SKYBLUE);
        gc.fillRect(0, 0, width, height);
        gc.setFill(Color.WHITE);
        gc.fillOval(100, 100, 80, 40);
        gc.fillOval(300, 150, 100, 50);
        gc.fillOval(600, 80, 120, 60);
    }

    private void drawPipe(GraphicsContext gc, Pipe pipe) {
        double gapTop = pipe.getGapY() - pipe.getGapSize() / 2;
        double gapBottom = pipe.getGapY() + pipe.getGapSize() / 2;

        gc.setFill(Color.GREEN);
        gc.fillRect(pipe.getX(), 0, pipe.getWidth(), gapTop);
        gc.setFill(Color.DARKGREEN);
        gc.fillRect(pipe.getX() - 3, gapTop - 20, pipe.getWidth() + 6, 20);

        gc.setFill(Color.GREEN);
        gc.fillRect(pipe.getX(), gapBottom, pipe.getWidth(), height - gapBottom);
        gc.setFill(Color.DARKGREEN);
        gc.fillRect(pipe.getX() - 3, gapBottom, pipe.getWidth() + 6, 20);
    }

    private void drawBestBird(GraphicsContext gc, double y, boolean halo) {
        gc.setFill(Color.RED);
        gc.fillOval(BIRD_X, y, BIRD_SIZE, BIRD_SIZE);

        // Eye
        gc.setFill(Color.WHITE);
        gc.fillOval(65, y + 8, 8, 8);
        gc.setFill(Color.BLACK);
        gc.fillOval(67, y + 10, 4, 4);

        // Beak
        gc.setFill(Color.ORANGE);
        gc.fillPolygon(new double[] {80, 90, 80}, new double[] {y + 15, y + 18, y + 21}, 3);

        if (halo) {
            gc.setStroke(Color.GOLD);
            gc.setLineWidth(3);
            gc.strokeOval(45, y - 5, 40, 40);
            gc.setStroke(HALO_GLOW);
            gc.setLineWidth(6);
            gc.strokeOval(42, y - 8, 46, 46);
        }
    }

    private void drawGround(GraphicsContext gc) {
        gc.setFill(Color.SADDLEBROWN);
        gc.fillRect(0, height - GROUND_HEIGHT, width, GROUND_HEIGHT);
        gc.setFill(Color.SANDYBROWN);
        for (int x = 0; x < width; x += 30) {
            gc.fillRect(x, height - GROUND_HEIGHT, 15, 5);
        }
    }

    private void drawScore(GraphicsContext gc, int score) {
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        gc.setFont(Font.font("System", FontWeight.BOLD, 30));
        String text = String.valueOf(score);
        gc.fillText(text, width / 2 - 15, 50);
        gc.strokeText(text, width / 2 - 15, 50);
    }
}
