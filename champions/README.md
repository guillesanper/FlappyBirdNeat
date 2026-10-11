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
pipe seeds 0-999 (`ChampionRun.play`), not only on the pipes it was trained on. That is why these
two were picked: GA champions tend to overfit to the pipes of their generation. Of the champions of
GA tournament seeds 1 to 5, only seed 2's survived all of 100 new sequences (the others 0, 19, 20
and 51), while those of NEAT seeds 2 and 3 survived 94 and 100. A small sample, not a study.

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
