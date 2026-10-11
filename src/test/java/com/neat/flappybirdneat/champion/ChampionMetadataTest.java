package com.neat.flappybirdneat.champion;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChampionMetadataTest {

    @Test
    void trainingSettingsAreNormalizedToLongAndDouble() {
        Map<String, Object> training = new LinkedHashMap<>();
        training.put("population", 50);
        training.put("rate", 0.5f);
        training.put("name", "x");
        training.put("flag", true);

        ChampionMetadata metadata = new ChampionMetadata(1, 2, 3, "c", "v", training);

        assertEquals(Map.of("population", 50L, "rate", 0.5, "name", "x", "flag", true), metadata.training());
        assertThrows(
                UnsupportedOperationException.class, () -> metadata.training().put("other", 1L));
    }

    @Test
    void otherSettingTypesAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ChampionMetadata(1, 2, 3, "c", "v", Map.of("list", List.of(1))));
    }

    @Test
    void aMissingBundledChampionIsReported() {
        assertThrows(IllegalStateException.class, () -> ChampionFile.loadBundled("no-such-champion.json"));
    }
}
