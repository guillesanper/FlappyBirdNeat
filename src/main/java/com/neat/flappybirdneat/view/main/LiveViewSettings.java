package com.neat.flappybirdneat.view.main;

/**
 * Display settings of the live simulation, shared by the panels that change them and the game
 * loop and renderer that read them.
 */
final class LiveViewSettings {
    private int gameSpeed = 1;
    private boolean showAllAgents = true;
    private boolean paused = false;

    int getGameSpeed() {
        return gameSpeed;
    }

    void setGameSpeed(int gameSpeed) {
        this.gameSpeed = gameSpeed;
    }

    boolean isShowAllAgents() {
        return showAllAgents;
    }

    void setShowAllAgents(boolean showAllAgents) {
        this.showAllAgents = showAllAgents;
    }

    /** @return true while the game is frozen: the loop keeps drawing but does not advance it */
    boolean isPaused() {
        return paused;
    }

    void setPaused(boolean paused) {
        this.paused = paused;
    }
}
