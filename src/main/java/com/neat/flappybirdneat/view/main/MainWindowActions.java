package com.neat.flappybirdneat.view.main;

import com.neat.flappybirdneat.champion.Champion;

/** Cross-panel actions that the main window coordinates on behalf of its panels. */
interface MainWindowActions {

    /** Redraws the charts, the species counter and the advanced statistics window if it is open. */
    void refreshStatistics();

    /** Brings the "Simulación Visual" tab to the front. */
    void showSimulationTab();

    /** After a training run: finds the best generation and offers to replay it. */
    void offerBestGenerationReplay();

    /** Replays {@code champion} alone and in a loop, with its network window open. */
    void watchChampion(Champion champion);
}
