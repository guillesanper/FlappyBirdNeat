package com.neat.flappybirdneat.history;

import com.neat.flappybirdneat.neat.Population;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RunHistory implements Serializable {
    private List<GenerationData> generationDataList;

    public RunHistory() {
        generationDataList = new ArrayList<>();
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
}