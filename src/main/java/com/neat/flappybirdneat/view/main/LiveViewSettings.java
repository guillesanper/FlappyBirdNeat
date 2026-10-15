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

    /** Pausa la simulación visible (entrenamiento en vivo o replay); el entrenamiento rápido no. */
    boolean isPaused() {
        return paused;
    }

    void setPaused(boolean paused) {
        this.paused = paused;
    }
}
