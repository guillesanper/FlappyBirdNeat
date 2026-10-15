package com.neat.flappybirdneat.history;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RunHistory implements Serializable {
    // Value computed before the seed was added, so histories saved by older versions still load
    private static final long serialVersionUID = 1167312263577267276L;

    private List<GenerationData> generationDataList;
    /** Global seed of the run (its pipes and evolution), or null if it was not recorded. */
    private Long seed;

    public RunHistory() {
        this(null);
    }

    /** @param seed global seed of the run, or null if unknown */
    public RunHistory(Long seed) {
        generationDataList = new ArrayList<>();
        this.seed = seed;
    }

    public void addGenerationData(GenerationData data) {
        generationDataList.add(data);
    }

    public List<GenerationData> getGenerationDataList() {
        return generationDataList;
    }

    public int getGenerations() {
        return generationDataList.size();
    }

    /** @return the global seed of the run, or null for histories recorded without it */
    public Long getSeed() {
        return seed;
    }
}
