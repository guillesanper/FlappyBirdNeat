package com.neat.flappybirdneat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/** Version and git commit this build was made from (see build-info.properties, filtered by Maven). */
public final class BuildInfo {

    static final String UNKNOWN = "unknown";

    private static final BuildInfo CURRENT = load();

    private final String version;
    private final String commit;

    BuildInfo(Properties properties) {
        this.version = valueOrUnknown(properties.getProperty("version"));
        String id = valueOrUnknown(properties.getProperty("commit"));
        boolean dirty = Boolean.parseBoolean(properties.getProperty("dirty"));
        this.commit = dirty && !id.equals(UNKNOWN) ? id + "-dirty" : id;
    }

    public static BuildInfo current() {
        return CURRENT;
    }

    /** @return the project version, e.g. "2.0.0" */
    public String version() {
        return version;
    }

    /** @return the abbreviated commit id, suffixed with "-dirty" for uncommitted changes, or "unknown" */
    public String commit() {
        return commit;
    }

    private static BuildInfo load() {
        Properties properties = new Properties();
        try (InputStream in = BuildInfo.class.getResourceAsStream("build-info.properties")) {
            if (in != null) properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new BuildInfo(properties);
    }

    /** Unfiltered placeholders (e.g. a build outside git) count as unknown. */
    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() || value.startsWith("${") ? UNKNOWN : value;
    }
}
