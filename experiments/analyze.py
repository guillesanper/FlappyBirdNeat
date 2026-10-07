#!/usr/bin/env python3
"""Analysis of the NEAT vs GA study written by experiments/run_study.sh.

Reads the per-run CSVs (either the published archives in docs/results/data or a raw study
directory), then writes figures, a Markdown summary and a JSON file of key numbers, and prints a
summary table:

  a) best and mean fitness per generation, mean over seeds with 95% bootstrap confidence bands;
  b) success rate (share of seeds solved) with Wilson 95% intervals, and generations to solve
     (median and IQR, with unsolved seeds treated as right-censored at the last generation);
  c) NEAT against every GA: Mann-Whitney U with Cliff's delta on the final fitness, a paired
     Wilcoxon signed-rank test on the shared seeds, an exact McNemar test on success and a paired
     sign test on generations to solve, each family corrected with Holm's method;
  d) NEAT only: number of species and topology size (node and enabled connection genes).

Usage:
  python experiments/analyze.py                         # published data -> docs/results
  python experiments/analyze.py --input experiments/out --output experiments/out/analysis
"""

from __future__ import annotations

import argparse
import io
import json
import math
import sys
import tarfile
from dataclasses import dataclass
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import numpy as np  # noqa: E402
import pandas as pd  # noqa: E402
from matplotlib.ticker import FuncFormatter  # noqa: E402
from scipy import stats  # noqa: E402

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_INPUT = REPO_ROOT / "docs" / "results" / "data"
DEFAULT_OUTPUT = REPO_ROOT / "docs" / "results"

CSV_COLUMNS = [
    "generation", "best", "mean", "min", "species", "diversity",
    "frames", "wall_ms", "nodes", "connections", "solved",
]

REFERENCE = "neat"
LABELS = {
    "neat": "NEAT",
    "ga-default": "GA default (roulette, gaussian)",
    "ga-tournament": "GA tournament",
    "ga-ranking-nonuniform": "GA ranking, non-uniform mut.",
    "ga-roulette-sigma": "GA roulette, sigma scaling",
}
# Line styles are a second channel next to color, so the series stay apart in grayscale and for
# color-vision deficiencies
LINESTYLES = ["-", (0, (5, 2)), (0, (1, 1.5)), (0, (6, 2, 1, 2)), (0, (2, 2))]
MARKERS = ["o", "s", "^", "D", "v"]

# Validated categorical palette (fixed order, never cycled), with surfaces and ink per theme
THEMES = {
    "light": {
        "surface": "#fcfcfb", "text": "#0b0b0b", "text2": "#52514e", "muted": "#898781",
        "grid": "#e1e0d9", "axis": "#c3c2b7",
        "series": ["#2a78d6", "#eb6834", "#1baf7a", "#eda100", "#e87ba4"],
    },
    "dark": {
        "surface": "#1a1a19", "text": "#ffffff", "text2": "#c3c2b7", "muted": "#898781",
        "grid": "#2c2c2a", "axis": "#383835",
        "series": ["#3987e5", "#d95926", "#199e70", "#c98500", "#d55181"],
    },
}


# --------------------------------------------------------------------------------------------
# Loading


def load_manifest(input_dir: Path) -> dict:
    path = input_dir / "manifest.json"
    if not path.is_file():
        sys.exit(f"analyze.py: no manifest.json in {input_dir}")
    return json.loads(path.read_text(encoding="utf-8"))


def _read_csv(source, config: str, name: str) -> pd.DataFrame:
    frame = pd.read_csv(source)
    if list(frame.columns) != CSV_COLUMNS:
        sys.exit(f"analyze.py: unexpected columns in {config}/{name}: {list(frame.columns)}")
    frame["seed"] = int(Path(name).stem.split("-")[1])
    frame["config"] = config
    return frame


def load_runs(input_dir: Path, configs: list[str]) -> pd.DataFrame:
    """Every run of every configuration, from raw/<config>/, <config>/ or <config>.tar.gz."""
    frames = []
    for config in configs:
        directory = next((d for d in (input_dir / "raw" / config, input_dir / config) if d.is_dir()), None)
        archive = input_dir / f"{config}.tar.gz"
        if directory is not None:
            for path in sorted(directory.glob("seed-*.csv")):
                frames.append(_read_csv(path, config, path.name))
        elif archive.is_file():
            with tarfile.open(archive, "r:gz") as tar:
                for member in sorted(tar.getmembers(), key=lambda m: m.name):
                    if member.isfile() and member.name.endswith(".csv"):
                        data = tar.extractfile(member).read()
                        frames.append(_read_csv(io.BytesIO(data), config, Path(member.name).name))
        else:
            sys.exit(f"analyze.py: no runs for configuration '{config}' in {input_dir}")
    runs = pd.concat(frames, ignore_index=True)
    runs["solved"] = runs["solved"].astype(str).str.lower() == "true"
    return runs


# --------------------------------------------------------------------------------------------
# Statistics


def bootstrap_mean_ci(values: np.ndarray, rng: np.random.Generator, resamples: int):
    """Mean over axis 0 and percentile 95% bootstrap interval, column by column."""
    n = values.shape[0]
    mean = values.mean(axis=0)
    if n < 2:
        return mean, mean.copy(), mean.copy()
    idx = rng.integers(0, n, size=(resamples, n))
    boot = values[idx].mean(axis=1)
    low, high = np.percentile(boot, [2.5, 97.5], axis=0)
    return mean, low, high


def wilson(k: int, n: int) -> tuple[float, float]:
    ci = stats.binomtest(k, n).proportion_ci(confidence_level=0.95, method="wilson")
    return ci.low, ci.high


def censored_quantile(times: np.ndarray, q: float) -> float:
    """Smallest t with empirical P(T <= t) >= q, where unsolved runs are +inf (censored at the end).

    Every run lasts the same number of generations, so censoring happens at one common point and
    the Kaplan-Meier estimate reduces to this empirical distribution: a quantile that falls among
    the censored runs is only known to exceed the last generation (returned as inf).
    """
    ordered = np.sort(times)
    index = math.ceil(q * len(ordered)) - 1
    return float(ordered[max(index, 0)])


def cliffs_delta(x: np.ndarray, y: np.ndarray) -> float:
    """P(X > Y) - P(X < Y): +1 when every x beats every y."""
    diff = np.sign(x[:, None] - y[None, :])
    return float(diff.mean())


def cliffs_magnitude(delta: float) -> str:
    # Thresholds of Romano et al. (2006)
    d = abs(delta)
    if d < 0.147:
        return "negligible"
    if d < 0.33:
        return "small"
    if d < 0.474:
        return "medium"
    return "large"


def holm(pvalues: list[float]) -> list[float]:
    """Holm-Bonferroni adjusted p-values (NaN entries are left out of the family)."""
    finite = [(p, i) for i, p in enumerate(pvalues) if not math.isnan(p)]
    finite.sort()
    m = len(finite)
    adjusted = [math.nan] * len(pvalues)
    running = 0.0
    for rank, (p, i) in enumerate(finite):
        running = max(running, min(1.0, (m - rank) * p))
        adjusted[i] = running
    return adjusted


def mann_whitney(x: np.ndarray, y: np.ndarray) -> float:
    if np.all(x == x[0]) and np.all(y == x[0]):
        return 1.0
    return float(stats.mannwhitneyu(x, y, alternative="two-sided").pvalue)


def wilcoxon_paired(x: np.ndarray, y: np.ndarray) -> tuple[float, int]:
    """Two-sided Wilcoxon signed-rank p-value and number of non-zero pairs (zeros are dropped)."""
    nonzero = int(np.count_nonzero(x - y))
    if nonzero == 0:
        return 1.0, 0
    return float(stats.wilcoxon(x, y, zero_method="wilcox", alternative="two-sided").pvalue), nonzero


def mcnemar_exact(a: np.ndarray, b: np.ndarray) -> tuple[float, int, int]:
    """Exact McNemar test on paired successes; returns p and the two discordant counts."""
    only_a = int(np.sum(a & ~b))
    only_b = int(np.sum(~a & b))
    if only_a + only_b == 0:
        return 1.0, only_a, only_b
    return float(stats.binomtest(only_a, only_a + only_b, 0.5).pvalue), only_a, only_b


def sign_test(a: np.ndarray, b: np.ndarray) -> tuple[float, int, int]:
    """Paired sign test on (censored) times: who solved first; ties, including both unsolved, drop."""
    first_a = int(np.sum(a < b))
    first_b = int(np.sum(b < a))
    if first_a + first_b == 0:
        return 1.0, first_a, first_b
    return float(stats.binomtest(first_a, first_a + first_b, 0.5).pvalue), first_a, first_b


@dataclass
class ConfigSummary:
    config: str
    runs: int
    solved: int
    success_low: float
    success_high: float
    solve_median: float
    solve_q1: float
    solve_q3: float
    final_best_median: float
    final_best_mean: float
    final_best_low: float
    final_best_high: float
    final_mean_median: float
    saturated: int
    best_ever_median: float


def summarize(runs: pd.DataFrame, configs: list[str], generations: int, max_frames: int, rng, resamples):
    per_run = {}
    summaries = []
    for config in configs:
        group = runs[runs["config"] == config]
        seeds = sorted(group["seed"].unique())
        times, final_best, final_mean, best_ever = [], [], [], []
        for seed in seeds:
            run = group[group["seed"] == seed].sort_values("generation")
            solved_gens = run.loc[run["solved"], "generation"]
            times.append(float(solved_gens.iloc[0]) if len(solved_gens) else math.inf)
            final_best.append(float(run["best"].iloc[-1]))
            final_mean.append(float(run["mean"].iloc[-1]))
            best_ever.append(float(run["best"].max()))
        times = np.array(times)
        final_best = np.array(final_best)
        per_run[config] = {
            "seeds": np.array(seeds),
            "time": times,
            "solved": np.isfinite(times),
            "final_best": final_best,
            "final_mean": np.array(final_mean),
        }
        k = int(np.isfinite(times).sum())
        low, high = wilson(k, len(seeds))
        mean, ci_low, ci_high = bootstrap_mean_ci(final_best[:, None], rng, resamples)
        summaries.append(ConfigSummary(
            config=config,
            runs=len(seeds),
            solved=k,
            success_low=low,
            success_high=high,
            solve_median=censored_quantile(times, 0.5),
            solve_q1=censored_quantile(times, 0.25),
            solve_q3=censored_quantile(times, 0.75),
            final_best_median=float(np.median(final_best)),
            final_best_mean=float(mean[0]),
            final_best_low=float(ci_low[0]),
            final_best_high=float(ci_high[0]),
            final_mean_median=float(np.median(final_mean)),
            saturated=int(np.sum(final_best >= max_frames)),
            best_ever_median=float(np.median(best_ever)),
        ))
    return summaries, per_run


def compare(per_run: dict, configs: list[str], generations: int):
    reference = per_run[REFERENCE]
    rows = []
    for config in configs:
        if config == REFERENCE:
            continue
        other = per_run[config]
        if not np.array_equal(reference["seeds"], other["seeds"]):
            sys.exit(f"analyze.py: {REFERENCE} and {config} were not run on the same seeds")
        x, y = reference["final_best"], other["final_best"]
        wilcoxon_p, nonzero = wilcoxon_paired(x, y)
        mcnemar_p, only_ref, only_other = mcnemar_exact(reference["solved"], other["solved"])
        # Censored times: every unsolved run ties just after the last generation
        tx = np.where(reference["solved"], reference["time"], generations + 1)
        ty = np.where(other["solved"], other["time"], generations + 1)
        sign_p, first_ref, first_other = sign_test(tx, ty)
        rows.append({
            "config": config,
            "mwu_p": mann_whitney(x, y),
            "cliffs_delta": cliffs_delta(x, y),
            "wilcoxon_p": wilcoxon_p,
            "wilcoxon_pairs": nonzero,
            "median_diff": float(np.median(x - y)),
            "mcnemar_p": mcnemar_p,
            "only_neat_solved": only_ref,
            "only_ga_solved": only_other,
            "time_mwu_p": mann_whitney(tx.astype(float), ty.astype(float)),
            "time_cliffs_delta": cliffs_delta(tx.astype(float), ty.astype(float)),
            "sign_p": sign_p,
            "neat_first": first_ref,
            "ga_first": first_other,
        })
    for key in ("mwu_p", "wilcoxon_p", "mcnemar_p", "time_mwu_p", "sign_p"):
        for row, adjusted in zip(rows, holm([r[key] for r in rows])):
            row[key + "_holm"] = adjusted
    return rows


# --------------------------------------------------------------------------------------------
# Figures


def style_axes(ax, theme):
    ax.set_facecolor(theme["surface"])
    for side in ("top", "right"):
        ax.spines[side].set_visible(False)
    for side in ("left", "bottom"):
        ax.spines[side].set_color(theme["axis"])
    ax.tick_params(colors=theme["text2"], labelsize=9)
    ax.xaxis.label.set_color(theme["text2"])
    ax.yaxis.label.set_color(theme["text2"])
    ax.title.set_color(theme["text"])
    ax.grid(True, color=theme["grid"], linewidth=0.8)
    ax.set_axisbelow(True)


def theme_rc(theme):
    """Matplotlib defaults for a theme, so every title, label and tick wears the theme's ink."""
    return {
        "text.color": theme["text"],
        "axes.titlecolor": theme["text"],
        "axes.labelcolor": theme["text2"],
        "xtick.color": theme["text2"],
        "ytick.color": theme["text2"],
        "axes.edgecolor": theme["axis"],
        "figure.facecolor": theme["surface"],
        "axes.facecolor": theme["surface"],
    }


def new_figure(theme, ncols, width=11.0, height=4.2):
    plt.rcParams.update(theme_rc(theme))
    fig, axes = plt.subplots(1, ncols, figsize=(width, height), squeeze=False)
    fig.patch.set_facecolor(theme["surface"])
    for ax in axes[0]:
        style_axes(ax, theme)
    return fig, axes[0]


def legend(ax, theme, **kwargs):
    leg = ax.legend(frameon=True, fontsize=8.5, **kwargs)
    leg.get_frame().set_facecolor(theme["surface"])
    leg.get_frame().set_edgecolor(theme["grid"])
    for text in leg.get_texts():
        text.set_color(theme["text"])
    return leg


def suptitle(fig, theme, title, subtitle):
    height = fig.get_figheight()
    fig.suptitle(title, x=0.01, y=1 - 0.12 / height, ha="left", va="top", fontsize=13, fontweight="bold",
                 color=theme["text"])
    fig.text(0.01, 1 - 0.42 / height, subtitle, ha="left", va="top", fontsize=9, color=theme["text2"])


def top(fig) -> float:
    """Top of the plotting area, just under the title block."""
    return 1 - 0.6 / fig.get_figheight()


def thousands(value, _pos):
    if value >= 1000:
        return f"{value / 1000:g}k"
    return f"{value:g}"


def save(fig, output: Path, name: str, mode: str):
    suffix = "" if mode == "light" else "-dark"
    fig.savefig(output / f"{name}{suffix}.png", dpi=150, facecolor=fig.get_facecolor())
    plt.close(fig)


def curves(runs, config, column, generations):
    group = runs[runs["config"] == config]
    table = group.pivot(index="seed", columns="generation", values=column).sort_index()
    return table.reindex(columns=range(1, generations + 1)).to_numpy(dtype=float)


def plot_fitness(runs, configs, generations, max_frames, n_seeds, output, mode, rng_seed, resamples):
    theme = THEMES[mode]
    fig, axes = new_figure(theme, 2, height=4.6)
    x = np.arange(1, generations + 1)
    for ax, column, title in zip(axes, ("best", "mean"), ("Best fitness per generation", "Mean fitness per generation")):
        rng = np.random.default_rng(rng_seed)
        for i, config in enumerate(configs):
            mean, low, high = bootstrap_mean_ci(curves(runs, config, column, generations), rng, resamples)
            color = theme["series"][i]
            ax.fill_between(x, low, high, color=color, alpha=0.16, linewidth=0)
            ax.plot(x, mean, color=color, linestyle=LINESTYLES[i], linewidth=2.2 if config == REFERENCE else 1.8,
                    label=LABELS.get(config, config))
        ax.set_yscale("log")
        ax.yaxis.set_major_formatter(FuncFormatter(thousands))
        ax.set_xlim(1, generations)
        ax.set_xlabel("Generation")
        ax.set_ylabel("Fitness (frames survived, log scale)")
        ax.set_title(title, loc="left", fontsize=10.5)
        if column == "best":
            ax.axhline(max_frames, color=theme["muted"], linewidth=1, linestyle=(0, (3, 3)))
            ax.annotate(f"frame cap ({max_frames:,})", xy=(generations, max_frames), xytext=(-4, -12),
                        textcoords="offset points", ha="right", fontsize=8, color=theme["text2"])
    handles, labels = axes[0].get_legend_handles_labels()
    leg = fig.legend(handles, labels, loc="lower center", ncol=len(configs), frameon=False, fontsize=8.5,
                     bbox_to_anchor=(0.5, 0.0), handlelength=3)
    for text in leg.get_texts():
        text.set_color(theme["text"])
    suptitle(fig, theme, "NEAT vs GA: fitness over generations",
             f"Mean over {n_seeds} seeds; shaded bands are 95% bootstrap confidence intervals of the mean.")
    fig.tight_layout(rect=(0, 0.06, 1, top(fig)))
    save(fig, output, "fitness_curves", mode)


def plot_success(summaries, per_run, configs, generations, n_seeds, output, mode):
    theme = THEMES[mode]
    fig, axes = new_figure(theme, 2, height=4.4)
    ax = axes[0]
    x = np.arange(0, generations + 1)
    for i, config in enumerate(configs):
        times = per_run[config]["time"]
        share = [(times <= g).mean() * 100 for g in x]
        ax.step(x, share, where="post", color=theme["series"][i], linestyle=LINESTYLES[i],
                linewidth=2.2 if config == REFERENCE else 1.8, label=LABELS.get(config, config))
    ax.set_xlim(0, generations)
    ax.set_ylim(0, 100)
    ax.set_xlabel("Generation")
    ax.set_ylabel("Seeds solved so far (%)")
    ax.set_title("Cumulative share of seeds solved", loc="left", fontsize=10.5)
    legend(ax, theme, loc="upper center", bbox_to_anchor=(0.5, -0.25), ncol=2)  # below the axes: never over a curve

    ax = axes[1]
    positions = np.arange(len(configs))[::-1]
    for i, (summary, y) in enumerate(zip(summaries, positions)):
        rate = summary.solved / summary.runs * 100
        color = theme["series"][i]
        ax.plot([summary.success_low * 100, summary.success_high * 100], [y, y], color=color, linewidth=2.2,
                solid_capstyle="round")
        ax.plot(rate, y, marker=MARKERS[i], markersize=8, color=color, markeredgecolor=theme["surface"],
                markeredgewidth=1.5, linestyle="none")
        ax.annotate(f"{summary.solved}/{summary.runs}", xy=(summary.success_high * 100, y), xytext=(6, 0),
                    textcoords="offset points", va="center", fontsize=8.5, color=theme["text"])
    ax.set_yticks(positions)
    ax.set_yticklabels([LABELS.get(c, c) for c in configs])
    ax.set_xlim(0, 108)
    ax.set_xlabel("Seeds solved by the last generation (%)")
    ax.set_title("Success rate with 95% Wilson intervals", loc="left", fontsize=10.5)
    ax.grid(False, axis="y")
    suptitle(fig, theme, "NEAT vs GA: how often each configuration solves the game",
             f"{n_seeds} paired seeds, {generations} generations; solved = an agent survives the whole frame cap.")
    fig.tight_layout(rect=(0, 0, 1, top(fig)))
    save(fig, output, "success_rate", mode)


def plot_final(per_run, configs, max_frames, generations, output, mode):
    theme = THEMES[mode]
    fig, axes = new_figure(theme, 1, width=9.0, height=4.4)
    ax = axes[0]
    jitter = np.random.default_rng(7)
    for i, config in enumerate(configs):
        values = per_run[config]["final_best"]
        xs = i + jitter.uniform(-0.22, 0.22, size=len(values))
        ax.scatter(xs, values, s=34, marker=MARKERS[i], color=theme["series"][i], edgecolor=theme["surface"],
                   linewidth=1.0, zorder=3)
        median = np.median(values)
        ax.plot([i - 0.32, i + 0.32], [median, median], color=theme["text"], linewidth=2, zorder=4,
                solid_capstyle="round")
    ax.set_yscale("log")
    ax.yaxis.set_major_formatter(FuncFormatter(thousands))
    ax.axhline(max_frames, color=theme["muted"], linewidth=1, linestyle=(0, (3, 3)))
    ax.set_xticks(range(len(configs)))
    ax.set_xticklabels([LABELS.get(c, c).replace(" (", "\n(").replace(", ", ",\n", 1) if c != REFERENCE else "NEAT"
                        for c in configs], fontsize=8.5)
    ax.set_ylabel("Best fitness of the last generation (log)")
    ax.grid(False, axis="x")
    suptitle(fig, theme, f"Final fitness per seed (generation {generations})",
             "One dot per seed; the bar is the median; the dashed line is the frame cap.")
    fig.tight_layout(rect=(0, 0, 1, top(fig)))
    save(fig, output, "final_fitness", mode)


def plot_neat(runs, generations, n_seeds, output, mode, rng_seed, resamples):
    theme = THEMES[mode]
    fig, axes = new_figure(theme, 3, width=12.0, height=4.0)
    x = np.arange(1, generations + 1)
    rng = np.random.default_rng(rng_seed)
    color = theme["series"][0]

    species = curves(runs, REFERENCE, "species", generations)
    mean, low, high = bootstrap_mean_ci(species, rng, resamples)
    ax = axes[0]
    ax.fill_between(x, low, high, color=color, alpha=0.18, linewidth=0)
    ax.plot(x, mean, color=color, linewidth=2, label="Mean species")
    ax.set_ylim(bottom=0)
    ax.set_title("Species", loc="left", fontsize=10.5)
    ax.set_xlabel("Generation")
    ax.set_ylabel("Species per generation")

    ax = axes[1]
    ax.plot(x, (species == 1).mean(axis=0) * 100, color=color, linewidth=2)
    ax.set_ylim(0, 100)
    ax.set_title("Runs down to a single species", loc="left", fontsize=10.5)
    ax.set_xlabel("Generation")
    ax.set_ylabel("Seeds with 1 species (%)")

    ax = axes[2]
    for column, style, label in (("connections", "-", "Enabled connections"), ("nodes", (0, (5, 2)), "Nodes")):
        mean, low, high = bootstrap_mean_ci(curves(runs, REFERENCE, column, generations), rng, resamples)
        ax.fill_between(x, low, high, color=color, alpha=0.14, linewidth=0)
        ax.plot(x, mean, color=color, linestyle=style, linewidth=2, label=label)
    ax.set_title("Topology size (mean genome)", loc="left", fontsize=10.5)
    ax.set_xlabel("Generation")
    ax.set_ylabel("Genes per genome")
    legend(ax, theme, loc="upper left")
    for ax in axes:
        ax.set_xlim(1, generations)
    suptitle(fig, theme, "NEAT: speciation and topology growth",
             f"Mean over {n_seeds} seeds with 95% bootstrap bands. Every run starts as one species of minimal genomes "
             "(4 inputs + bias wired to 1 output).")
    fig.tight_layout(rect=(0, 0, 1, top(fig)))
    save(fig, output, "neat_dynamics", mode)


# --------------------------------------------------------------------------------------------
# Report


def fmt_gen(value: float, generations: int) -> str:
    return f">{generations}" if math.isinf(value) else f"{value:.0f}"


def fmt_p(p: float) -> str:
    if math.isnan(p):
        return "n/a"
    return "<0.001" if p < 0.001 else f"{p:.3f}"


def summary_tables(summaries, comparisons, generations):
    lines = [
        "| Configuration | Solved | Success rate (95% Wilson CI) | Generations to solve: median [IQR] "
        "| Final best: median | Final best: mean (95% CI) | At the cap |",
        "|---|---|---|---|---|---|---|",
    ]
    for s in summaries:
        lines.append(
            f"| {LABELS.get(s.config, s.config)} | {s.solved}/{s.runs} "
            f"| {s.solved / s.runs * 100:.0f}% ({s.success_low * 100:.0f}-{s.success_high * 100:.0f}%) "
            f"| {fmt_gen(s.solve_median, generations)} [{fmt_gen(s.solve_q1, generations)}, "
            f"{fmt_gen(s.solve_q3, generations)}] ({s.runs - s.solved} unsolved) "
            f"| {s.final_best_median:,.0f} | {s.final_best_mean:,.0f} ({s.final_best_low:,.0f}-{s.final_best_high:,.0f}) "
            f"| {s.saturated}/{s.runs} |"
        )
    lines += [
        "",
        "| NEAT vs | Final best: MWU p (Holm) | Cliff's δ | Wilcoxon p (Holm), non-zero pairs "
        "| Success: McNemar p (Holm), only NEAT / only GA | Time to solve: MWU p (Holm), Cliff's δ "
        "| Time to solve: sign test p (Holm), NEAT / GA first |",
        "|---|---|---|---|---|---|---|",
    ]
    for c in comparisons:
        lines.append(
            f"| {LABELS.get(c['config'], c['config'])} "
            f"| {fmt_p(c['mwu_p'])} ({fmt_p(c['mwu_p_holm'])}) "
            f"| {c['cliffs_delta']:+.2f} ({cliffs_magnitude(c['cliffs_delta'])}) "
            f"| {fmt_p(c['wilcoxon_p'])} ({fmt_p(c['wilcoxon_p_holm'])}), {c['wilcoxon_pairs']} "
            f"| {fmt_p(c['mcnemar_p'])} ({fmt_p(c['mcnemar_p_holm'])}), {c['only_neat_solved']} / {c['only_ga_solved']} "
            f"| {fmt_p(c['time_mwu_p'])} ({fmt_p(c['time_mwu_p_holm'])}), {c['time_cliffs_delta']:+.2f} "
            f"| {fmt_p(c['sign_p'])} ({fmt_p(c['sign_p_holm'])}), {c['neat_first']} / {c['ga_first']} |"
        )
    return "\n".join(lines)


def print_table(summaries, comparisons, generations):
    print(f"{'configuration':<24} {'solved':>7} {'95% Wilson':>12} {'gens to solve [IQR]':>22} "
          f"{'final best med':>15} {'at cap':>7}")
    for s in summaries:
        gens = (f"{fmt_gen(s.solve_median, generations)} [{fmt_gen(s.solve_q1, generations)}, "
                f"{fmt_gen(s.solve_q3, generations)}]")
        print(f"{s.config:<24} {s.solved:>3}/{s.runs:<3} {s.success_low * 100:>4.0f}-{s.success_high * 100:<3.0f}%  "
              f"{gens:>22} {s.final_best_median:>15,.0f} {s.saturated:>3}/{s.runs:<3}")
    print()
    print(f"{'NEAT vs':<24} {'MWU p_holm':>10} {'cliff d':>8} {'wilcoxon p_holm':>16} "
          f"{'mcnemar p_holm':>15} {'time MWU p_holm':>16} {'sign p_holm':>12}")
    for c in comparisons:
        print(f"{c['config']:<24} {fmt_p(c['mwu_p_holm']):>10} {c['cliffs_delta']:>+8.2f} "
              f"{fmt_p(c['wilcoxon_p_holm']):>16} {fmt_p(c['mcnemar_p_holm']):>15} "
              f"{fmt_p(c['time_mwu_p_holm']):>16} {fmt_p(c['sign_p_holm']):>12}")


def neat_dynamics(runs, generations):
    group = runs[runs["config"] == REFERENCE]
    species = curves(runs, REFERENCE, "species", generations)
    last = group[group["generation"] == generations]
    tail = species[:, -50:] if generations >= 50 else species
    collapsed = (tail == 1).all(axis=1)
    solved = group.groupby("seed")["solved"].any().sort_index().to_numpy()
    return {
        "peak_species_median": float(np.median(species.max(axis=1))),
        "final_species_median": float(np.median(species[:, -1])),
        "single_species_last_generation": int(np.sum(species[:, -1] == 1)),
        "single_species_whole_tail": int(collapsed.sum()),
        "tail_generations": int(tail.shape[1]),
        "unsolved_runs_collapsed": int(np.sum(collapsed & ~solved)),
        "unsolved_runs": int(np.sum(~solved)),
        "final_nodes_mean": float(last["nodes"].mean()),
        "final_connections_mean": float(last["connections"].mean()),
        "final_diversity_median": float(last["diversity"].median()),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--input", type=Path, default=DEFAULT_INPUT,
                        help="study directory or published data directory (default: docs/results/data)")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT,
                        help="where figures and summaries go (default: docs/results)")
    parser.add_argument("--resamples", type=int, default=5000, help="bootstrap resamples (default: 5000)")
    parser.add_argument("--rng-seed", type=int, default=20021, help="seed of the bootstrap (default: 20021)")
    args = parser.parse_args()

    manifest = load_manifest(args.input)
    configs = list(manifest["configs"])
    if REFERENCE not in configs:
        sys.exit(f"analyze.py: the study has no '{REFERENCE}' configuration")
    params = manifest["parameters"]
    generations, max_frames = int(params["generations"]), int(params["max_frames"])

    runs = load_runs(args.input, configs)
    counts = runs.groupby(["config", "seed"]).size()
    if (counts != generations).any():
        bad = counts[counts != generations]
        sys.exit(f"analyze.py: incomplete runs (expected {generations} generations):\n{bad}")
    n_seeds = int(runs.groupby("config")["seed"].nunique().min())

    args.output.mkdir(parents=True, exist_ok=True)
    rng = np.random.default_rng(args.rng_seed)
    summaries, per_run = summarize(runs, configs, generations, max_frames, rng, args.resamples)
    comparisons = compare(per_run, configs, generations)
    dynamics = neat_dynamics(runs, generations)

    for mode in THEMES:
        plot_fitness(runs, configs, generations, max_frames, n_seeds, args.output, mode, args.rng_seed, args.resamples)
        plot_success(summaries, per_run, configs, generations, n_seeds, args.output, mode)
        plot_final(per_run, configs, max_frames, generations, args.output, mode)
        plot_neat(runs, generations, n_seeds, args.output, mode, args.rng_seed, args.resamples)

    print(f"Study: {n_seeds} seeds x {len(configs)} configurations, {generations} generations, "
          f"population {params['population']}, frame cap {max_frames} (commit {manifest['commit'][:7]})")
    print()
    print_table(summaries, comparisons, generations)
    print()

    saturated = [s for s in summaries if s.saturated / s.runs >= 0.5]
    warnings = []
    if saturated:
        names = ", ".join(f"{s.config} ({s.saturated}/{s.runs})" for s in saturated)
        warnings.append(
            f"{names} end at the frame cap in at least half of the seeds: the final fitness is censored at "
            f"{max_frames} and loses discriminative power, so the success rate and the generations to solve "
            "are the primary metrics.")
    for warning in warnings:
        print("Note: " + warning)
    print(f"NEAT: {dynamics['single_species_whole_tail']}/{n_seeds} runs have a single species in each of the "
          f"last {dynamics['tail_generations']} generations ({dynamics['unsolved_runs_collapsed']} of the "
          f"{dynamics['unsolved_runs']} unsolved ones); final mean topology {dynamics['final_nodes_mean']:.1f} "
          f"nodes, {dynamics['final_connections_mean']:.1f} enabled connections.")

    (args.output / "summary.md").write_text(summary_tables(summaries, comparisons, generations) + "\n",
                                            encoding="utf-8")
    key_numbers = {
        "manifest": manifest,
        "seeds": n_seeds,
        "configs": {s.config: {k: (None if isinstance(v, float) and math.isinf(v) else v)
                               for k, v in vars(s).items()} for s in summaries},
        "comparisons": comparisons,
        "neat_dynamics": dynamics,
        "warnings": warnings,
    }
    (args.output / "stats.json").write_text(json.dumps(key_numbers, indent=2) + "\n", encoding="utf-8")
    print(f"\nWrote figures, summary.md and stats.json to {args.output}")


if __name__ == "__main__":
    main()
