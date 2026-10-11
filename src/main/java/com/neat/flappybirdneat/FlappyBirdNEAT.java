package com.neat.flappybirdneat;

import com.neat.flappybirdneat.champion.Champion;
import com.neat.flappybirdneat.simulation.SimulationController;
import com.neat.flappybirdneat.view.main.MainWindow;
import java.util.concurrent.ThreadLocalRandom;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JavaFX entry point: creates the simulation (seeded for reproducibility) and opens the main window.
 */
public class FlappyBirdNEAT extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(FlappyBirdNEAT.class);

    private static final int POPULATION_SIZE = 50;
    private static final int CANVAS_WIDTH = 800;
    private static final int CANVAS_HEIGHT = 600;

    // Set by the launcher before Application.launch, which creates this class itself
    private static volatile Champion championToWatch;

    /** Makes the next start open the UI replaying {@code champion}. */
    static void watchOnStart(Champion champion) {
        championToWatch = champion;
    }

    @Override
    public void start(Stage primaryStage) {
        // Global seed: -Dseed=N reproduces a run; otherwise a fresh one on every launch
        long seed = Long.getLong("seed", ThreadLocalRandom.current().nextLong());
        LOG.info("Simulation seed: {} (relaunch with -Dseed={} to reproduce this run)", seed, seed);

        SimulationController controller = new SimulationController(POPULATION_SIZE, CANVAS_WIDTH, CANVAS_HEIGHT, seed);
        MainWindow window = new MainWindow(controller, POPULATION_SIZE, CANVAS_WIDTH, CANVAS_HEIGHT);
        window.show(primaryStage);
        if (championToWatch != null) {
            window.watchChampion(championToWatch);
        }
    }
}
