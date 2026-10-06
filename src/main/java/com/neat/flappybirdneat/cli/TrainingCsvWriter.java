package com.neat.flappybirdneat.cli;

import com.neat.flappybirdneat.simulation.GenerationStats;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Locale;

/**
 * Writes the headless run's CSV: one row per generation, flushed as soon as the generation ends so
 * an interrupted run keeps every finished row. Columns:
 *
 * <pre>generation,best,mean,min,species,diversity,frames,wall_ms,nodes,connections,solved</pre>
 *
 * {@code species}, {@code nodes} and {@code connections} (mean node genes and mean enabled connection
 * genes per genome) only exist in NEAT and are left empty for the GA. {@code solved} is true when an
 * agent reached the frame cap. This format is independent of the UI's Spanish CSV export.
 */
public class TrainingCsvWriter implements AutoCloseable {

    public static final String HEADER =
            "generation,best,mean,min,species,diversity,frames,wall_ms,nodes,connections,solved";

    private final Writer writer;
    private final int maxFrames;

    public TrainingCsvWriter(Writer writer, int maxFrames) {
        this.writer = writer;
        this.maxFrames = maxFrames;
        write(HEADER);
    }

    public void writeRow(GenerationStats stats, long wallMillis) {
        boolean neat = stats.species() >= 0;
        write(String.join(
                ",",
                Integer.toString(stats.generation()),
                integer(stats.best()),
                decimal(stats.mean()),
                integer(stats.min()),
                neat ? Integer.toString(stats.species()) : "",
                decimal(stats.diversity()),
                Integer.toString(stats.frames()),
                Long.toString(wallMillis),
                neat ? decimal(stats.meanNodes()) : "",
                neat ? decimal(stats.meanConnections()) : "",
                Boolean.toString(stats.solved(maxFrames))));
    }

    private void write(String line) {
        try {
            writer.write(line);
            writer.write('\n');
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String integer(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
