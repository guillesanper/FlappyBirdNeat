# FlappyBirdNEAT

[![CI](https://github.com/guillesanper/FlappyBirdNeat/actions/workflows/ci.yml/badge.svg)](https://github.com/guillesanper/FlappyBirdNeat/actions/workflows/ci.yml)

A neuroevolution playground built with **Java 21** and **JavaFX 17**: a population of Flappy Bird agents, each controlled by a small neural network, learns to play through evolution. The simulation runs live in a JavaFX canvas, with two selectable engines (a fixed-topology genetic algorithm and true [NEAT](https://nn.cs.utexas.edu/downloads/papers/stanley.ec02.pdf)), configurable genetic operators, generation history/replay, live fitness/diversity/species charts, and a benchmark mode for comparing operator configurations.

<!-- TODO: add a GIF/screenshot of the simulation running, e.g. docs/screenshot.gif, and embed it here with ![demo](docs/screenshot.gif) -->

## Stack

- **Language:** Java 21
- **UI:** JavaFX 17 (Canvas-based rendering, `LineChart` for fitness curves)
- **Build:** Maven (`javafx-maven-plugin`, `maven-shade-plugin` for a fat jar)
- **Testing:** JUnit 5, run via `mvn test`; CI runs the suite on every push/PR (see badge above)

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
├── neural/                    Brain interface + NeuralNetwork (fixed-topology MLP, feed-forward, sigmoid)
├── neat/                      EvolvingPopulation (engine-agnostic) and agent (FlappyBirdAgent wraps a Brain)
│   ├── selection/             Selection strategies (roulette, ranking, tournament, ...) — GA engine
│   ├── crossover/             Crossover strategies (uniform, single-point, arithmetic) — GA engine
│   ├── mutation/               Mutation strategies (gaussian, uniform, non-uniform) — GA engine
│   ├── scaling/                Fitness scaling (linear, sigma, Boltzmann) — GA engine
│   └── genome/                 True NEAT engine: Genome, NodeGene/ConnectionGene, InnovationTracker,
│                                Species/CompatibilityDistance, NeatCrossover, NeatPopulation
├── simulation/                 SimulationController — drives the live game/training loop, switches
│                                between the Fixed MLP (Population) and NEAT (NeatPopulation) engines
├── benchmark/                  BenchmarkRunner/BenchmarkConfig/BenchmarkPresets — headless comparison
│                                of operator configurations across repeated runs, with CSV export
├── history/                    Per-generation snapshots for replay and CSV export
├── config/                     Genetic operator configuration shared across the UI
└── view/                       JavaFX windows: game canvas, operator config, network visualizer,
                                 StatisticsWindow (advanced charts), BenchmarkWindow
```

All genetic operators are implemented as interchangeable strategies (Strategy pattern) behind small factories (`SeleccionFactory`, `CruceFactory`, `MutacionFactory`, `EscaladoFactory`), so a run can mix and match selection/crossover/mutation/scaling methods from the UI without touching `Population`. The evolution engine itself (Fixed MLP GA vs. NEAT) is selectable from the same "Algoritmo" dropdown.

## Algorithms implemented

**Genetic algorithm (fixed-topology engine)**

- **Selection:** roulette wheel, ranking, deterministic/probabilistic tournament, truncation, remainder stochastic sampling, stochastic universal sampling
- **Crossover:** uniform, single-point, arithmetic
- **Mutation:** gaussian (fixed magnitude), non-uniform (magnitude decays over generations), uniform
- **Fitness scaling:** linear, sigma, Boltzmann
- **Network:** fixed-topology MLP (4 inputs → 8 hidden → 1 output), only weights and biases evolve
- Configurable operator combinations can be compared head-to-head via the benchmark mode (CSV export of results)

**NEAT** — real topology evolution: node/connection genes with global innovation numbers, structural mutations (add connection, add node), speciation via compatibility distance, and NEAT-style crossover (matching/disjoint/excess genes). Selectable as an alternative engine (`Algoritmo: NEAT`) alongside the fixed-topology GA; the network topology and visualizer grow dynamically as the population evolves.

## Known limitations / in-progress cleanup

- `view/FlappyBirdGameUI` (used for replaying a saved generation in its own window) and the inline canvas drawing in `FlappyBirdNEAT` (used for the live training loop) currently duplicate drawing logic. They serve different call sites (popup replay vs. main loop) so they haven't been merged yet; unifying them into a shared renderer is a follow-up.
- Test coverage focuses on the genetic operators, `NeuralNetwork.feedForward`/mutation, and one full `Population.naturalSelection()` generation; UI/JavaFX classes are not covered.
- Root-level `*.md` design notes are historical/development notes in Spanish, kept for reference.

## Saving and loading

Population/history snapshots are saved with the `*.neat` extension via the file choosers in the UI. Fitness histories can also be exported to CSV.

## License

MIT — see [LICENSE](LICENSE).
