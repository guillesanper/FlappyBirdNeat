package com.neat.flappybirdneat.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

/**
 * The headless path must not load JavaFX. The test JVM has JavaFX loaded already (other tests use
 * it), so the CLI runs inside an isolated class loader that sees only the application classes and
 * SLF4J, on top of the bootstrap loader, and records every class it is asked for.
 */
class CliLoadsNoJavaFxTest {

    @TempDir
    Path tempDir;

    /**
     * Loader over the given URLs whose parent is the bootstrap loader (java.base and friends). Not the
     * platform loader: that one delegates packages of named application modules, JavaFX included,
     * to the application loader when the tests run on the module path.
     */
    private static final class RecordingClassLoader extends URLClassLoader {
        final List<String> requested = Collections.synchronizedList(new ArrayList<>());

        RecordingClassLoader(URL[] urls) {
            super(urls, null);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            requested.add(name);
            return super.loadClass(name, resolve);
        }
    }

    @Test
    void headlessRunNeverRequestsAJavaFxClass() throws Exception {
        List<URL> urls = new ArrayList<>();
        urls.add(location(Cli.class));
        urls.add(location(LoggerFactory.class));
        urls.add(location(com.google.gson.Gson.class));
        try {
            urls.add(location(Class.forName("org.slf4j.simple.SimpleServiceProvider")));
        } catch (ClassNotFoundException e) {
            // Without a provider SLF4J falls back to a no-op logger, which is fine here
        }

        try (RecordingClassLoader loader = new RecordingClassLoader(urls.toArray(URL[]::new))) {
            // Load and initialise Main the way the launcher does, then run the CLI through it
            Class<?> main = Class.forName("com.neat.flappybirdneat.Main", true, loader);
            assertSame(loader, main.getClassLoader(), "Main must come from the isolated loader");

            Class<?> cli = Class.forName(Cli.class.getName(), true, loader);
            Method run = cli.getMethod("run", String[].class, PrintStream.class, PrintStream.class);
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            String[] args = {
                "--headless",
                "--engine",
                "neat",
                "--seed",
                "5",
                "--generations",
                "2",
                "--population",
                "10",
                "--out",
                tempDir.resolve("isolated.csv").toString(),
                "--save-champion",
                tempDir.resolve("isolated.json").toString()
            };
            Object exit = run.invoke(
                    null,
                    args,
                    new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                    new PrintStream(err, true, StandardCharsets.UTF_8));

            assertEquals(Cli.EXIT_OK, exit, err.toString(StandardCharsets.UTF_8));
            assertTrue(
                    loader.requested.contains("com.neat.flappybirdneat.champion.ChampionFile"),
                    "the champion should have been saved through the isolated loader");
            assertTrue(
                    loader.requested.contains("com.neat.flappybirdneat.simulation.TrainingEngine"),
                    "the training core should have been loaded through the isolated loader");
            List<String> javafx = loader.requested.stream()
                    .filter(name -> name.startsWith("javafx.") || name.startsWith("com.sun.javafx."))
                    .toList();
            assertEquals(List.of(), javafx);
        }
    }

    private static URL location(Class<?> type) {
        return type.getProtectionDomain().getCodeSource().getLocation();
    }
}
