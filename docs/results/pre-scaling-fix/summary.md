| Configuration | Solved | Success rate (95% Wilson CI) | Generations to solve: median [IQR] | Final best: median | Final best: mean (95% CI) | At the cap |
|---|---|---|---|---|---|---|
| NEAT | 26/30 | 87% (70-95%) | 44 [24, 68] (4 unsolved) | 20,000 | 17,383 (14,765-19,350) | 26/30 |
| GA default (roulette, gaussian) | 10/30 | 33% (19-51%) | >200 [172, >200] (20 unsolved) | 728 | 7,228 (4,025-10,485) | 10/30 |
| GA tournament | 20/30 | 67% (49-81%) | 160 [99, >200] (10 unsolved) | 20,000 | 13,592 (10,364-16,768) | 20/30 |
| GA ranking, non-uniform mut. | 9/30 | 30% (17-48%) | >200 [137, >200] (21 unsolved) | 482 | 6,256 (3,179-9,564) | 8/30 |
| GA roulette, sigma scaling | 17/30 | 57% (39-73%) | 178 [83, >200] (13 unsolved) | 20,000 | 11,821 (8,565-15,072) | 17/30 |

| NEAT vs | Final best: MWU p (Holm) | Cliff's δ | Wilcoxon p (Holm), non-zero pairs | Success: McNemar p (Holm), only NEAT / only GA | Time to solve: MWU p (Holm), Cliff's δ | Time to solve: sign test p (Holm), NEAT / GA first |
|---|---|---|---|---|---|---|
| GA default (roulette, gaussian) | <0.001 (0.001) | +0.48 (large) | 0.012 (0.036), 23 | <0.001 (0.003), 19 / 3 | <0.001 (<0.001), -0.77 | <0.001 (<0.001), 26 / 3 |
| GA tournament | 0.117 (0.117) | +0.18 (small) | 0.158 (0.266), 12 | 0.109 (0.109), 8 / 2 | <0.001 (<0.001), -0.63 | <0.001 (<0.001), 24 / 4 |
| GA ranking, non-uniform mut. | <0.001 (<0.001) | +0.56 (large) | <0.001 (0.002), 23 | <0.001 (<0.001), 18 / 1 | <0.001 (<0.001), -0.81 | <0.001 (<0.001), 26 / 1 |
| GA roulette, sigma scaling | 0.032 (0.063) | +0.26 (small) | 0.133 (0.266), 16 | 0.035 (0.070), 12 / 3 | <0.001 (<0.001), -0.59 | 0.008 (0.008), 22 / 7 |
