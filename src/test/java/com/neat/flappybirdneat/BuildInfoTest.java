package com.neat.flappybirdneat;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;
import org.junit.jupiter.api.Test;

class BuildInfoTest {

    private static BuildInfo of(String version, String commit, String dirty) {
        Properties properties = new Properties();
        if (version != null) properties.setProperty("version", version);
        if (commit != null) properties.setProperty("commit", commit);
        if (dirty != null) properties.setProperty("dirty", dirty);
        return new BuildInfo(properties);
    }

    @Test
    void theBuildRecordsItsVersionAndCommit() {
        BuildInfo current = BuildInfo.current();
        assertFalse(current.version().startsWith("${"), current.version());
        assertNotNull(current.commit());
    }

    @Test
    void uncommittedChangesAreFlagged() {
        assertEquals("abc1234", of("2.0.0", "abc1234", "false").commit());
        assertEquals("abc1234-dirty", of("2.0.0", "abc1234", "true").commit());
    }

    @Test
    void missingOrUnfilteredValuesAreUnknown() {
        BuildInfo info = of("${project.version}", "${git.commit.id.abbrev}", "${git.dirty}");
        assertEquals(BuildInfo.UNKNOWN, info.version());
        assertEquals(BuildInfo.UNKNOWN, info.commit());
        assertEquals(BuildInfo.UNKNOWN, of(null, null, "true").commit());
    }
}
