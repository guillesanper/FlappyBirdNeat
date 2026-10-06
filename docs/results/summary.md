| Configuration | Solved | Success rate (95% Wilson CI) | Generations to solve: median [IQR] | Final best: median | Final best: mean (95% CI) | At the cap |
|---|---|---|---|---|---|---|
| NEAT | 19/30 | 63% (46-78%) | 64 [29, >200] (11 unsolved) | 20,000 | 12,154 (8,879-15,423) | 18/30 |
| GA default (roulette, gaussian) | 10/30 | 33% (19-51%) | >200 [172, >200] (20 unsolved) | 728 | 7,228 (4,025-10,485) | 10/30 |
| GA tournament | 21/30 | 70% (52-83%) | 154 [98, >200] (9 unsolved) | 20,000 | 14,095 (10,896-17,028) | 20/30 |
| GA ranking, non-uniform mut. | 11/30 | 37% (22-54%) | >200 [126, >200] (19 unsolved) | 666 | 7,381 (4,168-10,618) | 10/30 |
| GA roulette, sigma scaling | 17/30 | 57% (39-73%) | 178 [83, >200] (13 unsolved) | 20,000 | 11,821 (8,565-15,072) | 17/30 |

| NEAT vs | Final best: MWU p (Holm) | Cliff's δ | Wilcoxon p (Holm), non-zero pairs | Success: McNemar p (Holm), only NEAT / only GA | Time to solve: MWU p (Holm), Cliff's δ | Time to solve: sign test p (Holm), NEAT / GA first |
|---|---|---|---|---|---|---|
| GA default (roulette, gaussian) | 0.352 (1.000) | +0.13 (negligible) | 0.270 (0.811), 24 | 0.064 (0.254), 14 / 5 | 0.001 (0.005), -0.45 | 0.023 (0.068), 18 / 6 |
| GA tournament | 0.221 (0.883) | -0.16 (small) | 0.140 (0.562), 19 | 0.791 (1.000), 6 / 8 | 0.121 (0.189), -0.23 | 0.122 (0.244), 18 / 9 |
| GA ranking, non-uniform mut. | 0.378 (1.000) | +0.13 (negligible) | 0.341 (0.811), 26 | 0.115 (0.346), 14 / 6 | 0.005 (0.015), -0.40 | 0.015 (0.059), 19 / 6 |
| GA roulette, sigma scaling | 0.557 (1.000) | -0.08 (negligible) | 0.497 (0.811), 21 | 0.804 (1.000), 9 / 7 | 0.094 (0.189), -0.24 | 0.327 (0.327), 16 / 10 |
