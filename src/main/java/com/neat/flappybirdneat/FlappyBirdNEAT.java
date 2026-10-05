package com.neat.flappybirdneat;

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

    @Override
    public void start(Stage primaryStage) {
        // Global seed: -Dseed=N reproduces a run; otherwise a fresh one on every launch
        long seed = Long.getLong("seed", ThreadLocalRandom.current().nextLong());
        LOG.info("Simulation seed: {} (relaunch with -Dseed={} to reproduce this run)", seed, seed);

        SimulationController controller = new SimulationController(POPULATION_SIZE, CANVAS_WIDTH, CANVAS_HEIGHT, seed);
        new MainWindow(controller, POPULATION_SIZE, CANVAS_WIDTH, CANVAS_HEIGHT).show(primaryStage);
    }
}
