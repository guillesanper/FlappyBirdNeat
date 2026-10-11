| Configuration | Solved | Success rate (95% Wilson CI) | Generations to solve: median [IQR] | Final best: median | Final best: mean (95% CI) | At the cap |
|---|---|---|---|---|---|---|
| NEAT | 26/30 | 87% (70-95%) | 40 [19, 60] (4 unsolved) | 5,000 | 4,077 (3,324-4,692) | 24/30 |
| GA default (roulette, gaussian) | 11/30 | 37% (22-54%) | >200 [170, >200] (19 unsolved) | 728 | 2,210 (1,459-2,987) | 11/30 |
| GA tournament | 21/30 | 70% (52-83%) | 152 [86, >200] (9 unsolved) | 5,000 | 3,592 (2,870-4,275) | 20/30 |
| GA ranking, non-uniform mut. | 11/30 | 37% (22-54%) | >200 [102, >200] (19 unsolved) | 482 | 1,883 (1,189-2,667) | 9/30 |
| GA roulette, sigma scaling | 18/30 | 60% (42-75%) | 137 [70, >200] (12 unsolved) | 5,000 | 3,101 (2,352-3,848) | 17/30 |

| NEAT vs | Final best: MWU p (Holm) | Cliff's δ | Wilcoxon p (Holm), non-zero pairs | Success: McNemar p (Holm), only NEAT / only GA | Time to solve: MWU p (Holm), Cliff's δ | Time to solve: sign test p (Holm), NEAT / GA first |
|---|---|---|---|---|---|---|
| GA default (roulette, gaussian) | 0.007 (0.022) | +0.36 (medium) | 0.030 (0.089), 21 | 0.001 (0.004), 18 / 3 | <0.001 (<0.001), -0.75 | <0.001 (<0.001), 25 / 4 |
| GA tournament | 0.377 (0.377) | +0.10 (negligible) | 0.509 (0.709), 14 | 0.180 (0.180), 7 / 2 | <0.001 (<0.001), -0.61 | <0.001 (<0.001), 24 / 4 |
| GA ranking, non-uniform mut. | <0.001 (0.004) | +0.45 (medium) | 0.001 (0.005), 22 | <0.001 (0.001), 16 / 1 | <0.001 (<0.001), -0.79 | <0.001 (<0.001), 25 / 2 |
| GA roulette, sigma scaling | 0.159 (0.319) | +0.18 (small) | 0.355 (0.709), 17 | 0.057 (0.115), 11 / 3 | <0.001 (<0.001), -0.58 | 0.008 (0.008), 22 / 7 |
