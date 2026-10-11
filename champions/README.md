# Champions

Trained agents bundled with the application, in the portable champion format (versioned JSON,
see [`ChampionFile`](../src/main/java/com/neat/flappybirdneat/champion/ChampionFile.java)). They are
packaged inside the jar and the installers: **Ver campeón** in the UI and `--demo` on the command
line replay `neat-champion.json`.

| File | Engine | Seed | Solved at | Network | Pipe sequences survived to the cap |
|---|---|---|---|---|---|
| [`neat-champion.json`](neat-champion.json) | NEAT | 3 | generation 39 | 4 inputs + bias, 5 hidden, 1 output; 9 of 15 connections enabled | 1000 / 1000 |
| [`mlp-champion.json`](mlp-champion.json) | GA (fixed 4-8-1 MLP, deterministic tournament) | 2 | generation 13 | 4-8-1, 49 weights and biases | 1000 / 1000 |

Both reached the 20,000-frame cap in training. The last column replays each champion alone on
pipe seeds 0-999 (`ChampionRun.play`), not only on the pipes it was trained on: GA champions often
overfit to their generation's pipes (of the first five GA tournament champions tried, only this one
survived every sequence; the others managed 0-51 of 100), while the NEAT champions tried generalized.

## Regenerating them

Training is deterministic (same seed, same result whatever the thread count), so these commands
reproduce the networks exactly; only `metadata.commit` changes. Build the jar first with
`./mvnw package`.

```bash
java -jar target/FlappyBirdNEAT.jar --headless --engine neat --seed 3 --generations 200 \
  --stop-on-solve --out neat-champion.csv --save-champion champions/neat-champion.json

java -jar target/FlappyBirdNEAT.jar --headless --engine ga --selection deterministic_tournament \
  --seed 2 --generations 250 --stop-on-solve --out mlp-champion.csv --save-champion champions/mlp-champion.json
```

`BundledChampionsTest` retrains both and fails if a change to the engines makes the bundled files
stale; rerun the commands above when that is intended.

## Watching one

```bash
java -jar target/FlappyBirdNEAT.jar --demo                              # the bundled NEAT champion
java -jar target/FlappyBirdNEAT.jar --watch champions/mlp-champion.json # any champion file
```
