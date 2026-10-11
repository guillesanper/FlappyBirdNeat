package com.neat.flappybirdneat.champion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import com.neat.flappybirdneat.neat.genome.ConnectionGene;
import com.neat.flappybirdneat.neat.genome.Genome;
import com.neat.flappybirdneat.neat.genome.NodeGene;
import com.neat.flappybirdneat.neat.genome.NodeType;
import com.neat.flappybirdneat.neural.NeuralNetwork;
import com.neat.flappybirdneat.simulation.EngineType;
import com.neat.flappybirdneat.simulation.TrainingEngine;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads and writes champion files: versioned JSON that, unlike the Java-serialized {@code *.neat}
 * history snapshots, survives refactorings and can be read by other tools.
 *
 * <pre>{@code
 * {
 *   "format": "flappy-neat-brain",
 *   "version": 1,
 *   "engine": "neat",                    // or "mlp" (the GA's fixed 4-8-1 network)
 *   "metadata": {"seed": 42, "generation": 17, "fitness": 20000.0, "commit": "abc1234",
 *                "appVersion": "2.0.0", "training": {"population": 50, ...}},
 *   "network": {...}
 * }
 * }</pre>
 *
 * A NEAT network lists its {@code nodes} ({@code id}, {@code type}: input, bias, hidden or output;
 * inputs and outputs in order) and {@code connections} ({@code innovation}, {@code in}, {@code out},
 * {@code weight}, {@code enabled}), plus {@code inputs}, {@code outputs} and {@code biasNode}. An MLP
 * lists {@code weightsInputHidden} ([input][hidden]), {@code weightsHiddenOutput} ([hidden][output]),
 * {@code biasHidden} and {@code biasOutput}. Weights are written with every digit, so a champion
 * read back computes exactly the same outputs.
 */
public final class ChampionFile {

    public static final String FORMAT = "flappy-neat-brain";
    public static final int VERSION = 1;

    /** Bundled NEAT champion (champions/neat-champion.json in the repository). */
    public static final String BUNDLED_NEAT = "neat-champion.json";

    /** Bundled GA champion (champions/mlp-champion.json in the repository). */
    public static final String BUNDLED_MLP = "mlp-champion.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ChampionFile() {}

    // ---------------------------------------------------------------- writing

    public static void save(Champion champion, Path file) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            write(champion, writer);
        }
    }

    public static void write(Champion champion, Writer writer) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        root.addProperty("version", VERSION);
        root.addProperty("engine", engineName(champion.engine()));
        root.add("metadata", metadataJson(champion.metadata()));
        root.add(
                "network",
                champion.brain() instanceof Genome genome
                        ? genomeJson(genome)
                        : networkJson((NeuralNetwork) champion.brain()));
        try {
            GSON.toJson(root, writer);
            writer.write('\n');
        } catch (com.google.gson.JsonIOException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private static String engineName(EngineType engine) {
        return engine == EngineType.NEAT ? "neat" : "mlp";
    }

    private static JsonObject metadataJson(ChampionMetadata metadata) {
        JsonObject json = new JsonObject();
        json.addProperty("seed", metadata.seed());
        json.addProperty("generation", metadata.generation());
        json.addProperty("fitness", metadata.fitness());
        json.addProperty("commit", metadata.commit());
        json.addProperty("appVersion", metadata.appVersion());
        JsonObject training = new JsonObject();
        metadata.training().forEach((key, value) -> training.add(key, primitive(value)));
        json.add("training", training);
        return json;
    }

    /** The metadata holds Long, Double, String or Boolean values only (see {@link ChampionMetadata}). */
    private static JsonPrimitive primitive(Object value) {
        if (value instanceof Number number) return new JsonPrimitive(number);
        if (value instanceof Boolean bool) return new JsonPrimitive(bool);
        return new JsonPrimitive((String) value);
    }

    private static JsonObject genomeJson(Genome genome) {
        JsonObject json = new JsonObject();
        json.addProperty("inputs", genome.getNumInputs());
        json.addProperty("outputs", genome.getNumOutputs());
        json.addProperty("biasNode", genome.getBiasNodeId());
        JsonArray nodes = new JsonArray();
        for (NodeGene node : genome.getNodes()) {
            JsonObject nodeJson = new JsonObject();
            nodeJson.addProperty("id", node.getId());
            nodeJson.addProperty("type", node.getType().name().toLowerCase(Locale.ROOT));
            nodes.add(nodeJson);
        }
        json.add("nodes", nodes);
        JsonArray connections = new JsonArray();
        for (ConnectionGene connection : genome.getConnections()) {
            JsonObject connectionJson = new JsonObject();
            connectionJson.addProperty("innovation", connection.getInnovationNumber());
            connectionJson.addProperty("in", connection.getInNode());
            connectionJson.addProperty("out", connection.getOutNode());
            connectionJson.addProperty("weight", connection.getWeight());
            connectionJson.addProperty("enabled", connection.isEnabled());
            connections.add(connectionJson);
        }
        json.add("connections", connections);
        return json;
    }

    private static JsonObject networkJson(NeuralNetwork network) {
        JsonObject json = new JsonObject();
        json.addProperty("inputs", network.getInputSize());
        json.addProperty("hidden", network.getHiddenSize());
        json.addProperty("outputs", network.getOutputSize());
        json.add("weightsInputHidden", matrix(network.getWeightsInputHidden()));
        json.add("weightsHiddenOutput", matrix(network.getWeightsHiddenOutput()));
        json.add("biasHidden", vector(network.getBiasHidden()));
        json.add("biasOutput", vector(network.getBiasOutput()));
        return json;
    }

    private static JsonArray matrix(double[][] values) {
        JsonArray rows = new JsonArray();
        for (double[] row : values) rows.add(vector(row));
        return rows;
    }

    private static JsonArray vector(double[] values) {
        JsonArray array = new JsonArray();
        for (double value : values) array.add(value);
        return array;
    }

    // ---------------------------------------------------------------- reading

    public static Champion load(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return read(reader);
        }
    }

    /**
     * Loads a champion bundled with the application ({@link #BUNDLED_NEAT} or {@link #BUNDLED_MLP}).
     *
     * @throws IllegalStateException if this build does not include it
     */
    public static Champion loadBundled(String name) {
        try (InputStream in = ChampionFile.class.getResourceAsStream(name)) {
            if (in == null) throw new IllegalStateException("This build has no bundled champion " + name);
            return read(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("The bundled champion " + name + " is invalid", e);
        }
    }

    /**
     * @throws ChampionFormatException if the content is not a champion this build can read; the
     *     message says why and is meant for the user
     */
    public static Champion read(Reader reader) throws IOException {
        JsonObject root;
        try {
            JsonReader json = new JsonReader(reader);
            json.setStrictness(Strictness.STRICT);
            JsonElement element = JsonParser.parseReader(json);
            if (!atEnd(json)) {
                throw new ChampionFormatException("Not a champion file: unexpected content after the JSON object");
            }
            if (!element.isJsonObject()) {
                throw new ChampionFormatException("Not a champion file: expected a JSON object");
            }
            root = element.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new ChampionFormatException("Not a champion file: invalid JSON (" + e.getMessage() + ")", e);
        }

        String format = string(root, "format");
        if (!FORMAT.equals(format)) {
            throw new ChampionFormatException(
                    "Not a champion file: format is '" + format + "', expected '" + FORMAT + "'");
        }
        int version = integer(root, "version");
        if (version != VERSION) {
            throw new ChampionFormatException("Unsupported champion file version " + version
                    + ": this build of FlappyBirdNEAT reads version " + VERSION);
        }
        String engineName = string(root, "engine");
        EngineType engine = switch (engineName) {
            case "neat" -> EngineType.NEAT;
            case "mlp" -> EngineType.GA;
            default ->
                throw new ChampionFormatException(
                        "Unknown engine '" + engineName + "' in champion file (expected neat or mlp)");
        };

        ChampionMetadata metadata = readMetadata(object(root, "metadata"));
        JsonObject network = object(root, "network");
        try {
            if (engine == EngineType.NEAT) {
                return new Champion(engine, readGenome(network), metadata);
            }
            return new Champion(engine, readNetwork(network), metadata);
        } catch (IllegalArgumentException e) {
            throw new ChampionFormatException("Invalid " + engineName + " network: " + e.getMessage(), e);
        }
    }

    private static boolean atEnd(JsonReader json) throws IOException {
        try {
            return json.peek() == JsonToken.END_DOCUMENT;
        } catch (MalformedJsonException e) {
            return false;
        }
    }

    private static ChampionMetadata readMetadata(JsonObject json) throws ChampionFormatException {
        Map<String, Object> training = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object(json, "training").entrySet()) {
            training.put(entry.getKey(), value("metadata.training." + entry.getKey(), entry.getValue()));
        }
        return new ChampionMetadata(
                longValue(json, "seed"),
                integer(json, "generation"),
                number(json, "fitness"),
                string(json, "commit"),
                string(json, "appVersion"),
                training);
    }

    private static Object value(String name, JsonElement element) throws ChampionFormatException {
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) return primitive.getAsBoolean();
            if (primitive.isString()) return primitive.getAsString();
            // Integers stay integers ("50"), decimals stay decimals ("3.0"), as they were written
            String literal = primitive.getAsString();
            boolean decimal = literal.contains(".") || literal.contains("e") || literal.contains("E");
            try {
                return decimal
                        ? (Object) primitive.getAsDouble()
                        : (Object) primitive.getAsBigDecimal().longValueExact();
            } catch (ArithmeticException e) {
                throw new ChampionFormatException("Field '" + name + "' is out of range");
            }
        }
        throw new ChampionFormatException("Field '" + name + "' must be a number, string or boolean");
    }

    private static Genome readGenome(JsonObject json) throws ChampionFormatException {
        int inputs = gameInputs(integer(json, "inputs"));
        int outputs = gameOutputs(integer(json, "outputs"));
        List<NodeGene> nodes = new ArrayList<>();
        for (JsonObject node : objects(json, "nodes")) {
            String type = string(node, "type");
            NodeType nodeType;
            try {
                nodeType = NodeType.valueOf(type.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new ChampionFormatException("Unknown node type '" + type + "'");
            }
            nodes.add(new NodeGene(integer(node, "id"), nodeType));
        }
        List<ConnectionGene> connections = new ArrayList<>();
        for (JsonObject connection : objects(json, "connections")) {
            connections.add(new ConnectionGene(
                    integer(connection, "in"),
                    integer(connection, "out"),
                    number(connection, "weight"),
                    bool(connection, "enabled"),
                    integer(connection, "innovation")));
        }
        return Genome.restore(inputs, outputs, integer(json, "biasNode"), nodes, connections);
    }

    private static NeuralNetwork readNetwork(JsonObject json) throws ChampionFormatException {
        gameInputs(integer(json, "inputs"));
        gameOutputs(integer(json, "outputs"));
        NeuralNetwork network = NeuralNetwork.fromWeights(
                matrix(json, "weightsInputHidden"),
                matrix(json, "weightsHiddenOutput"),
                vector(json, "biasHidden"),
                vector(json, "biasOutput"));
        if (network.getInputSize() != integer(json, "inputs")
                || network.getHiddenSize() != integer(json, "hidden")
                || network.getOutputSize() != integer(json, "outputs")) {
            throw new ChampionFormatException("Invalid mlp network: the weights do not match inputs, hidden, outputs");
        }
        return network;
    }

    /** The game feeds every brain the same inputs and reads one output: other sizes cannot play it. */
    private static int gameInputs(int inputs) throws ChampionFormatException {
        if (inputs != TrainingEngine.AGENT_INPUTS) {
            throw new ChampionFormatException(
                    "The network has " + inputs + " inputs; the game provides " + TrainingEngine.AGENT_INPUTS);
        }
        return inputs;
    }

    private static int gameOutputs(int outputs) throws ChampionFormatException {
        if (outputs != TrainingEngine.AGENT_OUTPUTS) {
            throw new ChampionFormatException(
                    "The network has " + outputs + " outputs; the game reads " + TrainingEngine.AGENT_OUTPUTS);
        }
        return outputs;
    }

    // ---------------------------------------------------------------- typed field access

    private static JsonElement field(JsonObject json, String name) throws ChampionFormatException {
        JsonElement element = json.get(name);
        if (element == null || element.isJsonNull()) {
            throw new ChampionFormatException("Missing field '" + name + "' in champion file");
        }
        return element;
    }

    private static JsonPrimitive primitive(JsonObject json, String name, String kind) throws ChampionFormatException {
        JsonElement element = field(json, name);
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            boolean matches = switch (kind) {
                case "a number" -> primitive.isNumber();
                case "a string" -> primitive.isString();
                default -> primitive.isBoolean();
            };
            if (matches) return primitive;
        }
        throw new ChampionFormatException("Field '" + name + "' must be " + kind);
    }

    private static String string(JsonObject json, String name) throws ChampionFormatException {
        return primitive(json, name, "a string").getAsString();
    }

    private static boolean bool(JsonObject json, String name) throws ChampionFormatException {
        return primitive(json, name, "a boolean").getAsBoolean();
    }

    private static double number(JsonObject json, String name) throws ChampionFormatException {
        double value = primitive(json, name, "a number").getAsDouble();
        if (!Double.isFinite(value)) throw new ChampionFormatException("Field '" + name + "' must be finite");
        return value;
    }

    private static long longValue(JsonObject json, String name) throws ChampionFormatException {
        try {
            return primitive(json, name, "a number").getAsBigDecimal().longValueExact();
        } catch (ArithmeticException e) {
            throw new ChampionFormatException("Field '" + name + "' must be an integer");
        }
    }

    private static int integer(JsonObject json, String name) throws ChampionFormatException {
        try {
            return primitive(json, name, "a number").getAsBigDecimal().intValueExact();
        } catch (ArithmeticException e) {
            throw new ChampionFormatException("Field '" + name + "' must be an integer");
        }
    }

    private static JsonObject object(JsonObject json, String name) throws ChampionFormatException {
        JsonElement element = field(json, name);
        if (!element.isJsonObject()) throw new ChampionFormatException("Field '" + name + "' must be an object");
        return element.getAsJsonObject();
    }

    private static JsonArray array(JsonObject json, String name) throws ChampionFormatException {
        JsonElement element = field(json, name);
        if (!element.isJsonArray()) throw new ChampionFormatException("Field '" + name + "' must be an array");
        return element.getAsJsonArray();
    }

    private static List<JsonObject> objects(JsonObject json, String name) throws ChampionFormatException {
        List<JsonObject> objects = new ArrayList<>();
        for (JsonElement element : array(json, name)) {
            if (!element.isJsonObject()) {
                throw new ChampionFormatException("Every entry of '" + name + "' must be an object");
            }
            objects.add(element.getAsJsonObject());
        }
        return objects;
    }

    private static double[] vector(JsonObject json, String name) throws ChampionFormatException {
        return vector(name, array(json, name));
    }

    private static double[] vector(String name, JsonArray array) throws ChampionFormatException {
        double[] values = new double[array.size()];
        for (int i = 0; i < values.length; i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
                throw new ChampionFormatException("Every entry of '" + name + "' must be a number");
            }
            values[i] = element.getAsDouble();
        }
        return values;
    }

    private static double[][] matrix(JsonObject json, String name) throws ChampionFormatException {
        JsonArray rows = array(json, name);
        double[][] values = new double[rows.size()][];
        for (int i = 0; i < values.length; i++) {
            if (!rows.get(i).isJsonArray()) {
                throw new ChampionFormatException("Every row of '" + name + "' must be an array");
            }
            values[i] = vector(name, rows.get(i).getAsJsonArray());
        }
        return values;
    }
}
