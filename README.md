# FlappyBirdNEAT

A neuroevolution playground built with **Java 21** and **JavaFX 17**: a population of Flappy Bird agents, each controlled by a small neural network, learns to play through a genetic algorithm. The simulation runs live in a JavaFX canvas, with configurable genetic operators, generation history/replay, and fitness charts.

> Despite the project name, the current engine evolves the **weights** of a fixed-topology network (a classic genetic algorithm), not the network topology itself. True [NEAT](https://nn.cs.utexas.edu/downloads/papers/stanley.ec02.pdf) (topology + weight evolution, speciation) is on the roadmap — see [Algorithms implemented](#algorithms-implemented) below.

<!-- TODO: add a GIF/screenshot of the simulation running, e.g. docs/screenshot.gif, and embed it here with ![demo](docs/screenshot.gif) -->

## Stack

- **Language:** Java 21
- **UI:** JavaFX 17 (Canvas-based rendering, `LineChart` for fitness curves)
- **Build:** Maven (`javafx-maven-plugin`, `maven-shade-plugin` for a fat jar)
- **Testing:** JUnit 5 (dependency present; test suite is a work in progress — see roadmap)

## Getting started

Requirements: JDK 21+ and Maven.

```bash
# Run the app
mvn javafx:run

# Build an executable fat jar (target/FlappyBirdNEAT-1.0-SNAPSHOT.jar)
mvn clean package
```

## Architecture

```
com.neat.flappybirdneat
├── Main / FlappyBirdNEAT      Application entry point and main JavaFX window
├── game/                      Game rules: bird physics, pipes, collisions
├── neural/                    NeuralNetwork — fixed-topology MLP (feed-forward, sigmoid)
├── neat/                      Population and agent (FlappyBirdAgent wraps a NeuralNetwork)
│   ├── selection/             Selection strategies (roulette, ranking, tournament, ...)
│   ├── crossover/             Crossover strategies (uniform, single-point, arithmetic)
│   ├── mutation/               Mutation strategies (gaussian, uniform, non-uniform)
│   └── scaling/                Fitness scaling (linear, sigma, Boltzmann)
├── simulation/                 SimulationController — drives the live game/training loop
├── history/                    Per-generation snapshots for replay and CSV export
├── config/                     Genetic operator configuration shared across the UI
└── view/                       JavaFX windows: game canvas, operator config, network visualizer
```

All genetic operators are implemented as interchangeable strategies (Strategy pattern) behind small factories (`SeleccionFactory`, `CruceFactory`, `MutacionFactory`, `EscaladoFactory`), so a run can mix and match selection/crossover/mutation/scaling methods from the UI without touching `Population`.

## Algorithms implemented

**Genetic algorithm (current default engine)**

- **Selection:** roulette wheel, ranking, deterministic/probabilistic tournament, truncation, remainder stochastic sampling, stochastic universal sampling
- **Crossover:** uniform, single-point, arithmetic
- **Mutation:** gaussian (fixed magnitude), non-uniform (magnitude decays over generations), uniform
- **Fitness scaling:** linear, sigma, Boltzmann
- **Network:** fixed-topology MLP (4 inputs → 8 hidden → 1 output), only weights and biases evolve

**NEAT (planned)** — real topology evolution (node/connection genes, innovation numbers, structural mutations, speciation) as an alternative, selectable engine alongside the fixed-topology GA. Not implemented yet.

## Known limitations / in-progress cleanup

- `view/FlappyBirdGameUI` (used for replaying a saved generation in its own window) and the inline canvas drawing in `FlappyBirdNEAT` (used for the live training loop) currently duplicate drawing logic. They serve different call sites (popup replay vs. main loop) so they haven't been merged yet; unifying them into a shared renderer is a follow-up.
- No automated tests or CI yet (planned).
- Root-level `*.md` design notes are historical/development notes in Spanish, kept for reference.

## Saving and loading

Population/history snapshots are saved with the `*.neat` extension via the file choosers in the UI. Fitness histories can also be exported to CSV.

## License

MIT — see [LICENSE](LICENSE).
