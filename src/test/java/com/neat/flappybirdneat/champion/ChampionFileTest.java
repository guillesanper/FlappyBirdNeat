package com.neat.flappybirdneat.champion;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.config.GeneticOperatorsConfig;
import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.neat.genome.Genome;
import com.neat.flappybirdneat.neat.genome.InnovationTracker;
import com.neat.flappybirdneat.neat.genome.NeatConfig;
import com.neat.flappybirdneat.neural.Brain;
import com.neat.flappybirdneat.neural.NeuralNetwork;
import com.neat.flappybirdneat.simulation.EngineType;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChampionFileTest {

    @TempDir
    Path tempDir;

    private static ChampionMetadata metadata(EngineType engine) {
        return new ChampionMetadata(
                -8_123_456_789_012L,
                17,
                20_000,
                "abc1234",
                "2.0.0",
                TrainingSettings.of(engine, 50, 20_000, new GeneticOperatorsConfig(), new NeatConfig()));
    }

    private static Genome evolvedGenome(long seed) {
        Random random = new Random(seed);
        InnovationTracker tracker = new InnovationTracker();
        Genome genome = new Genome(4, 1, random, tracker);
        for (int i = 0; i < 10; i++) {
            genome.mutateAddNode(random, tracker);
            genome.mutateAddConnection(random, tracker);
            genome.mutateWeights(random, 0.8);
        }
        return genome;
    }

    private static String toJson(Champion champion) throws IOException {
        StringWriter writer = new StringWriter();
        ChampionFile.write(champion, writer);
        return writer.toString();
    }

    private static Champion fromJson(String json) throws IOException {
        return ChampionFile.read(new StringReader(json));
    }

    private static void assertSameOutputs(Brain expected, Brain actual) {
        Random inputs = new Random(99);
        for (int i = 0; i < 500; i++) {
            double[] input = {
                inputs.nextDouble() * 2 - 1,
                inputs.nextGaussian(),
                inputs.nextDouble() * 2 - 1,
                inputs.nextDouble() * 2 - 1
            };
            assertArrayEquals(expected.feedForward(input), actual.feedForward(input), "outputs must be identical");
        }
    }

    @Test
    void neatRoundTripGivesIdenticalOutputsAndMetadata() throws IOException {
        Genome genome = evolvedGenome(21);
        Champion champion = new Champion(EngineType.NEAT, genome, metadata(EngineType.NEAT));

        Path file = tempDir.resolve("champ.json");
        ChampionFile.save(champion, file);
        Champion loaded = ChampionFile.load(file);

        assertEquals(EngineType.NEAT, loaded.engine());
        assertSameOutputs(genome, loaded.brain());
        assertEquals(champion.metadata(), loaded.metadata());
        Genome loadedGenome = (Genome) loaded.brain();
        assertEquals(genome.getNodes().size(), loadedGenome.getNodes().size());
        assertEquals(
                genome.getConnections().size(), loadedGenome.getConnections().size());
        for (int i = 0; i < genome.getConnections().size(); i++) {
            var expected = genome.getConnections().get(i);
            var actual = loadedGenome.getConnections().get(i);
            assertEquals(expected.getInnovationNumber(), actual.getInnovationNumber());
            assertEquals(expected.isEnabled(), actual.isEnabled());
            assertEquals(expected.getWeight(), actual.getWeight());
        }
    }

    @Test
    void mlpRoundTripGivesIdenticalOutputs() throws IOException {
        NeuralNetwork network = new NeuralNetwork(4, 8, 1, new Random(8));
        Champion champion = new Champion(EngineType.GA, network, metadata(EngineType.GA));

        Champion loaded = fromJson(toJson(champion));

        assertEquals(EngineType.GA, loaded.engine());
        assertSameOutputs(network, loaded.brain());
        assertEquals("RouletteSelection", loaded.metadata().training().get("selection"));
        assertEquals("none", loaded.metadata().training().get("scaling"));
    }

    @Test
    void headerAndTrainingSettingsAreWrittenAsDocumented() throws IOException {
        String json = toJson(new Champion(EngineType.NEAT, evolvedGenome(1), metadata(EngineType.NEAT)));

        assertTrue(json.contains("\"format\": \"flappy-neat-brain\""), json);
        assertTrue(json.contains("\"version\": 1"), json);
        assertTrue(json.contains("\"engine\": \"neat\""), json);
        assertTrue(json.contains("\"innovation\""), json);
        Map<String, Object> training = fromJson(json).metadata().training();
        assertEquals(50L, training.get("population"));
        assertEquals(3.0, training.get("compatibilityThreshold"));
    }

    @Test
    void championOfAnAgentKeepsACopyOfItsBrain() {
        Genome genome = evolvedGenome(4);
        FlappyBirdAgent agent = new FlappyBirdAgent(genome);
        Champion champion = Champion.of(agent, metadata(EngineType.NEAT));

        double[] input = {0.1, 0.2, 0.3, 0.4};
        double before = champion.brain().feedForward(input)[0];
        genome.mutateWeights(new Random(1), 1.0);

        assertEquals(EngineType.NEAT, champion.engine());
        assertEquals(before, champion.brain().feedForward(input)[0]);
        assertEquals(
                EngineType.GA,
                Champion.of(new FlappyBirdAgent(4, 8, 1, new Random(2)), metadata(EngineType.GA))
                        .engine());
    }

    @Test
    void engineAndBrainMustMatch() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Champion(EngineType.GA, evolvedGenome(1), metadata(EngineType.GA)));
    }

    // ------------------------------------------------------------ invalid files

    private String validNeatJson() throws IOException {
        return toJson(new Champion(EngineType.NEAT, evolvedGenome(3), metadata(EngineType.NEAT)));
    }

    private static String rejection(String json) {
        return assertThrows(ChampionFormatException.class, () -> fromJson(json)).getMessage();
    }

    @Test
    void unknownVersionGivesAClearError() throws IOException {
        String message = rejection(validNeatJson().replace("\"version\": 1", "\"version\": 2"));
        assertEquals("Unsupported champion file version 2: this build of FlappyBirdNEAT reads version 1", message);
    }

    @Test
    void unknownEngineGivesAClearError() throws IOException {
        String message = rejection(validNeatJson().replace("\"engine\": \"neat\"", "\"engine\": \"lstm\""));
        assertEquals("Unknown engine 'lstm' in champion file (expected neat or mlp)", message);
    }

    @Test
    void otherFormatsAndBrokenJsonAreRejected() throws IOException {
        assertTrue(rejection(validNeatJson().replace("flappy-neat-brain", "something-else"))
                .contains("format is 'something-else'"));
        assertTrue(rejection("{\"format\": ").contains("invalid JSON"));
        assertTrue(rejection("[1, 2]").contains("expected a JSON object"));
        assertTrue(rejection("{\"format\": 'flappy-neat-brain'}").contains("invalid JSON"));
        assertTrue(rejection(validNeatJson() + "{}").contains("after the JSON object"));
        assertTrue(rejection("{}").contains("Missing field 'format'"));
        assertTrue(rejection("{\"format\": 3}").contains("'format' must be a string"));
    }

    @Test
    void inconsistentNetworksAreRejected() throws IOException {
        String json = validNeatJson();
        assertTrue(rejection(json.replace("\"inputs\": 4", "\"inputs\": 5")).contains("5 inputs"));
        assertTrue(rejection(json.replace("\"outputs\": 1", "\"outputs\": 2")).contains("2 outputs"));
        assertTrue(rejection(json.replace("\"type\": \"bias\"", "\"type\": \"recurrent\""))
                .contains("Unknown node type 'recurrent'"));
        assertTrue(rejection(json.replace("\"type\": \"bias\"", "\"type\": \"hidden\""))
                .startsWith("Invalid neat network"));
        assertTrue(rejection(json.replace("\"generation\": 17", "\"generation\": 1.5"))
                .contains("'generation' must be an integer"));
        assertTrue(rejection(json.replace("\"enabled\": true", "\"enabled\": \"yes\""))
                .contains("must be a boolean"));

        String mlp =
                toJson(new Champion(EngineType.GA, new NeuralNetwork(4, 8, 1, new Random(1)), metadata(EngineType.GA)));
        assertTrue(rejection(mlp.replace("\"hidden\": 8", "\"hidden\": 7")).contains("do not match"));
        assertTrue(rejection(mlp.replaceFirst("\"biasOutput\": \\[", "\"biasOutput\": [\"x\", "))
                .contains("must be a number"));
    }
}
