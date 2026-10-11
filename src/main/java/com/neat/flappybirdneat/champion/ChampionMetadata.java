package com.neat.flappybirdneat.champion;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Where a champion came from.
 *
 * @param seed       global seed of the training run
 * @param generation 1-based generation in which the champion played
 * @param fitness    frames it survived in that generation
 * @param commit     git commit of the build that trained it ("unknown" if not known)
 * @param appVersion version of the build that trained it
 * @param training   settings of the run (population, frame cap, operators...): numbers, strings or booleans
 */
public record ChampionMetadata(
        long seed, int generation, double fitness, String commit, String appVersion, Map<String, Object> training) {

    public ChampionMetadata {
        Objects.requireNonNull(commit, "commit");
        Objects.requireNonNull(appVersion, "appVersion");
        Map<String, Object> normalized = new LinkedHashMap<>();
        training.forEach((key, value) -> normalized.put(key, normalize(key, value)));
        training = Collections.unmodifiableMap(normalized);
    }

    /** Integers as Long and decimals as Double, so settings compare equal after a round trip. */
    private static Object normalize(String key, Object value) {
        if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
            return ((Number) value).longValue();
        }
        if (value instanceof Double || value instanceof Float) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String || value instanceof Boolean) {
            return value;
        }
        throw new IllegalArgumentException("Training setting '" + key + "' must be a number, string or boolean");
    }
}
