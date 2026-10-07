#!/usr/bin/env bash
# NEAT vs fixed-topology GA: a paired, seeded, resumable study driven through the headless CLI.
#
# Every configuration below is run once per seed, with the SAME seeds for every configuration
# (paired design: for a given seed and generation, the GA and NEAT also face the same pipes).
#
# Fixed parameters of the published study (docs/results/REPORT.md). Each one can be overridden
# through the environment, e.g. for the reduced study of the manual CI workflow:
#
#   SEEDS        30     seeds 1..SEEDS
#   GENERATIONS  200    generations per run (full runs: no --stop-on-solve)
#   POPULATION   50     agents per generation
#   MAX_FRAMES   20000  frame cap per generation; an agent that reaches it "solves" the game
#   THREADS      all cores (evaluation threads; results do not depend on it)
#   JAR          target/FlappyBirdNEAT-1.0-SNAPSHOT.jar (build it with ./mvnw package)
#   OUT_DIR      experiments/out
#
# Configurations (name: CLI arguments):
#   neat                   --engine neat
#   ga-default             --engine ga  roulette + uniform + gaussian, no scaling (the GA defaults)
#   ga-tournament          --engine ga  deterministic_tournament + uniform + gaussian
#   ga-ranking-nonuniform  --engine ga  ranking + uniform + non_uniform
#   ga-roulette-sigma      --engine ga  roulette + uniform + gaussian + sigma scaling
#
# Output, under OUT_DIR:
#   raw/<config>/seed-NN.csv   one CSV per run (the CLI's per-generation format)
#   logs/<config>/seed-NN.log  the CLI's stdout and stderr for each run
#   manifest.json              commit, JDK, CPU, cores, dates, parameters and configurations
#   timings.csv                wall-clock seconds of every run
#   packed/                    manifest, timings and one <config>.tar.gz of CSVs per configuration
#
# Resuming: rerun the same command. A run whose CSV already has a header plus GENERATIONS rows is
# skipped; anything else is rerun from scratch (each run writes to a temporary file that is only
# renamed once the run has exited cleanly with the expected number of rows). Resuming with
# different parameters is refused, so one OUT_DIR never mixes two studies.
#
# Any failed run stops the study with a non-zero exit status and the tail of its log.
#
# Usage: experiments/run_study.sh            (from the repository root or anywhere else)

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

cores() {
    nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 1
}

SEEDS="${SEEDS:-30}"
GENERATIONS="${GENERATIONS:-200}"
POPULATION="${POPULATION:-50}"
MAX_FRAMES="${MAX_FRAMES:-20000}"
THREADS="${THREADS:-$(cores)}"
JAR="${JAR:-target/FlappyBirdNEAT-1.0-SNAPSHOT.jar}"
OUT_DIR="${OUT_DIR:-experiments/out}"

CONFIGS=(
    "neat|--engine neat"
    "ga-default|--engine ga --selection roulette --crossover uniform --mutation gaussian --scaling none"
    "ga-tournament|--engine ga --selection deterministic_tournament --crossover uniform --mutation gaussian --scaling none"
    "ga-ranking-nonuniform|--engine ga --selection ranking --crossover uniform --mutation non_uniform --scaling none"
    "ga-roulette-sigma|--engine ga --selection roulette --crossover uniform --mutation gaussian --scaling sigma"
)

die() {
    echo "run_study.sh: error: $*" >&2
    exit 1
}

for name in SEEDS GENERATIONS POPULATION MAX_FRAMES THREADS; do
    [[ "${!name}" =~ ^[1-9][0-9]*$ ]] || die "$name must be a positive integer, got '${!name}'"
done
[[ -f "$JAR" ]] || die "jar not found: $JAR (build it with ./mvnw -B package -DskipTests)"
command -v java >/dev/null || die "java not found on PATH"

mkdir -p "$OUT_DIR/raw" "$OUT_DIR/logs" "$OUT_DIR/packed"

json_escape() {
    local s="$1"
    s="${s//\\/\\\\}"
    s="${s//\"/\\\"}"
    printf '%s' "$s"
}

# The parameters that define the study; resuming with different ones is refused
PARAMS_KEY="seeds=$SEEDS generations=$GENERATIONS population=$POPULATION max_frames=$MAX_FRAMES"
for config in "${CONFIGS[@]}"; do
    PARAMS_KEY+=" ${config%%|*}=[${config#*|}]"
done
if [[ -f "$OUT_DIR/params.key" ]]; then
    if [[ "$(cat "$OUT_DIR/params.key")" != "$PARAMS_KEY" ]]; then
        die "$OUT_DIR holds a study with other parameters:
  found:    $(cat "$OUT_DIR/params.key")
  expected: $PARAMS_KEY
Use another OUT_DIR or delete $OUT_DIR."
    fi
else
    printf '%s\n' "$PARAMS_KEY" >"$OUT_DIR/params.key"
fi

commit="$(git rev-parse HEAD 2>/dev/null || echo unknown)"
dirty=false
if [[ -n "$(git status --porcelain --untracked-files=no 2>/dev/null)" ]]; then dirty=true; fi
jdk="$(java -version 2>&1 | grep -v '^Picked up' | head -n 1)"
jdk_runtime="$(java -version 2>&1 | grep -v '^Picked up' | sed -n 2p)"
cpu="$(
    { grep -m1 'model name' /proc/cpuinfo 2>/dev/null | cut -d: -f2 | sed 's/^ *//'; } ||
        sysctl -n machdep.cpu.brand_string 2>/dev/null || echo unknown
)"
[[ -n "$cpu" ]] || cpu="$(sysctl -n machdep.cpu.brand_string 2>/dev/null || echo unknown)"
os="$(uname -srm)"
started="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
[[ -f "$OUT_DIR/started" ]] || echo "$started" >"$OUT_DIR/started"

TIMINGS="$OUT_DIR/timings.csv"
[[ -f "$TIMINGS" ]] || echo "config,seed,seconds" >"$TIMINGS"

csv_complete() {
    # Header plus exactly one row per generation (full runs never stop early)
    [[ -f "$1" ]] && [[ "$(wc -l <"$1" | tr -d ' ')" -eq $((GENERATIONS + 1)) ]]
}

now_ns() {
    # Portable sub-second clock (BSD date has no %N)
    if date +%s%N | grep -q 'N$'; then echo "$(date +%s)000000000"; else date +%s%N; fi
}

total=$((SEEDS * ${#CONFIGS[@]}))
done_runs=0
skipped=0
echo "Study: $SEEDS seeds x ${#CONFIGS[@]} configurations, $GENERATIONS generations," \
    "population $POPULATION, max $MAX_FRAMES frames, $THREADS threads -> $OUT_DIR"

for config in "${CONFIGS[@]}"; do
    name="${config%%|*}"
    read -r -a args <<<"${config#*|}"
    mkdir -p "$OUT_DIR/raw/$name" "$OUT_DIR/logs/$name"
    for seed in $(seq 1 "$SEEDS"); do
        done_runs=$((done_runs + 1))
        csv="$OUT_DIR/raw/$name/$(printf 'seed-%02d.csv' "$seed")"
        log="$OUT_DIR/logs/$name/$(printf 'seed-%02d.log' "$seed")"
        if csv_complete "$csv"; then
            skipped=$((skipped + 1))
            continue
        fi
        tmp="$csv.partial"
        rm -f "$tmp"
        start_ns="$(now_ns)"
        if ! java -jar "$JAR" --headless "${args[@]}" --seed "$seed" --generations "$GENERATIONS" \
            --population "$POPULATION" --max-frames "$MAX_FRAMES" --threads "$THREADS" \
            --out "$tmp" >"$log" 2>&1; then
            tail -n 20 "$log" >&2
            die "run $name seed $seed failed (log: $log)"
        fi
        if ! csv_complete "$tmp"; then
            die "run $name seed $seed wrote $(($(wc -l <"$tmp") - 1)) rows, expected $GENERATIONS ($tmp)"
        fi
        mv "$tmp" "$csv"
        seconds="$(awk -v a="$start_ns" -v b="$(now_ns)" 'BEGIN { printf "%.3f", (b - a) / 1e9 }')"
        echo "$name,$seed,$seconds" >>"$TIMINGS"
        printf '[%3d/%d] %-22s seed %2d  %7.1f s  %s\n' "$done_runs" "$total" "$name" "$seed" "$seconds" \
            "$(grep -m1 'Solved:' "$log" | sed 's/^ *//')"
    done
done

finished="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
# A run that was rerun (e.g. after an interrupted attempt) counts once: its last timing wins
awk -F, 'NR == 1 { print; next } { last[$1 "," $2] = $0; if (!seen[$1 "," $2]++) order[++n] = $1 "," $2 }
    END { for (i = 1; i <= n; i++) print last[order[i]] }' "$TIMINGS" >"$OUT_DIR/packed/timings.csv"
total_seconds="$(awk -F, 'NR > 1 { s += $3 } END { printf "%.1f", s }' "$OUT_DIR/packed/timings.csv")"

{
    echo "{"
    echo "  \"commit\": \"$commit\","
    echo "  \"working_tree_dirty\": $dirty,"
    echo "  \"jdk\": \"$(json_escape "$jdk")\","
    echo "  \"jdk_runtime\": \"$(json_escape "$jdk_runtime")\","
    echo "  \"cpu\": \"$(json_escape "$cpu")\","
    echo "  \"cores\": $(cores),"
    echo "  \"os\": \"$(json_escape "$os")\","
    echo "  \"started_utc\": \"$(cat "$OUT_DIR/started")\","
    echo "  \"finished_utc\": \"$finished\","
    echo "  \"total_run_seconds\": $total_seconds,"
    echo "  \"parameters\": {"
    echo "    \"seeds\": $SEEDS,"
    echo "    \"seed_list\": \"1..$SEEDS\","
    echo "    \"generations\": $GENERATIONS,"
    echo "    \"population\": $POPULATION,"
    echo "    \"max_frames\": $MAX_FRAMES,"
    echo "    \"threads\": $THREADS,"
    echo "    \"stop_on_solve\": false"
    echo "  },"
    echo "  \"command\": \"java -jar $JAR --headless <config args> --seed <seed> --generations $GENERATIONS --population $POPULATION --max-frames $MAX_FRAMES --threads $THREADS --out <csv>\","
    echo "  \"configs\": {"
    i=0
    for config in "${CONFIGS[@]}"; do
        i=$((i + 1))
        sep=","
        [[ $i -eq ${#CONFIGS[@]} ]] && sep=""
        echo "    \"${config%%|*}\": \"$(json_escape "${config#*|}")\"$sep"
    done
    echo "  }"
    echo "}"
} >"$OUT_DIR/manifest.json"

# One archive per configuration, plus the manifest and the timings, ready to publish
for config in "${CONFIGS[@]}"; do
    name="${config%%|*}"
    tar -czf "$OUT_DIR/packed/$name.tar.gz" -C "$OUT_DIR/raw" "$name"
done
cp "$OUT_DIR/manifest.json" "$OUT_DIR/packed/"

echo "Done: $((total - skipped)) runs executed, $skipped already complete; $total_seconds s of runs in total."
echo "Manifest: $OUT_DIR/manifest.json; archives: $OUT_DIR/packed/"
