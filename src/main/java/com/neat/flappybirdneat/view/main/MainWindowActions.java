package com.neat.flappybirdneat.view.main;

/** Cross-panel actions that the main window coordinates on behalf of its panels. */
interface MainWindowActions {

    /** Redraws the charts, the species counter and the advanced statistics window if it is open. */
    void refreshStatistics();

    /** Brings the "Simulación Visual" tab to the front. */
    void showSimulationTab();

    /** After a training run: finds the best generation and offers to replay it. */
    void offerBestGenerationReplay();
}
