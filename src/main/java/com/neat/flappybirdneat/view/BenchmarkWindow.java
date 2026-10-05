package com.neat.flappybirdneat.view;

import com.neat.flappybirdneat.benchmark.BenchmarkConfig;
import com.neat.flappybirdneat.benchmark.BenchmarkPresets;
import com.neat.flappybirdneat.benchmark.BenchmarkResult;
import com.neat.flappybirdneat.benchmark.BenchmarkRunner;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ventana de comparativa de operadores (benchmark): ejecuta el modo "Fixed MLP" con distintas
 * combinaciones predefinidas de selección/cruce/mutación/escalado ({@link BenchmarkPresets}), cada
 * una repetida con N semillas reproducibles, y superpone las curvas medias de mejor fitness por
 * generación con una banda de ± una desviación estándar.
 */
public class BenchmarkWindow {
    private static final Color[] PALETTE = {
            Color.rgb(220, 50, 47), Color.rgb(38, 139, 210), Color.rgb(42, 161, 152), Color.rgb(203, 75, 22)
    };

    private final Stage stage;
    private final int populationSize;
    private final int canvasWidth;
    private final int canvasHeight;

    private final LineChart<Number, Number> chart;
    private final NumberAxis yAxis;
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label statusLabel = new Label("Listo");
    private final Button runButton = new Button("▶ Ejecutar Benchmark");
    private final Button exportButton = new Button("Exportar CSV");

    private List<BenchmarkResult> lastResults = new ArrayList<>();

    public BenchmarkWindow(int populationSize, int canvasWidth, int canvasHeight) {
        this.populationSize = populationSize;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;

        stage = new Stage();
        stage.setTitle("Comparativa de Operadores (Benchmark)");

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Generación");
        yAxis = new NumberAxis();
        yAxis.setLabel("Mejor Fitness (media ± desv. estándar)");
        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Comparativa de Operadores Genéticos");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        // Tamaño mínimo generoso para que las curvas y las bandas media±desv.est. no queden
        // aplastadas; si hay muchas generaciones el ancho crece y el ScrollPane de más abajo
        // permite desplazarse en lugar de comprimir el eje X.
        chart.setMinHeight(550);
        chart.setMinWidth(900);
        VBox.setVgrow(chart, Priority.ALWAYS);

        Label seedsLabel = new Label("Semillas:");
        Spinner<Integer> seedsSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 5));
        seedsSpinner.setEditable(true);
        seedsSpinner.setPrefWidth(80);

        Label generationsLabel = new Label("Generaciones:");
        Spinner<Integer> generationsSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 1000, 50));
        generationsSpinner.setEditable(true);
        generationsSpinner.setPrefWidth(80);

        runButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        exportButton.setDisable(true);
        progressBar.setPrefWidth(300);

        HBox controls = new HBox(10, seedsLabel, seedsSpinner, generationsLabel, generationsSpinner, runButton, exportButton);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(10));

        HBox statusBox = new HBox(10, progressBar, statusLabel);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setPadding(new Insets(0, 10, 10, 10));

        runButton.setOnAction(e -> runBenchmark(seedsSpinner.getValue(), generationsSpinner.getValue()));
        exportButton.setOnAction(e -> exportCsv());

        ScrollPane chartScroll = new ScrollPane(chart);
        chartScroll.setFitToWidth(false);
        chartScroll.setFitToHeight(false);
        chartScroll.setPannable(true);
        VBox.setVgrow(chartScroll, Priority.ALWAYS);

        VBox root = new VBox(controls, statusBox, chartScroll);
        BorderPane borderPane = new BorderPane(root);
        stage.setScene(new Scene(borderPane, 950, 700));
        stage.setMinWidth(700);
        stage.setMinHeight(500);
    }

    public void show() {
        stage.show();
    }

    private void runBenchmark(int seeds, int generations) {
        runButton.setDisable(true);
        exportButton.setDisable(true);
        chart.getData().clear();
        progressBar.setProgress(0);
        // Ensanchar el gráfico si hay muchas generaciones, para que cada punto tenga sitio
        // y el ScrollPane permita desplazarse horizontalmente en lugar de aplastar el eje X.
        chart.setPrefWidth(Math.max(900, generations * 20));

        List<BenchmarkConfig> configs = BenchmarkPresets.defaultConfigs(generations);

        Task<List<BenchmarkResult>> task = new Task<>() {
            @Override
            protected List<BenchmarkResult> call() {
                return BenchmarkRunner.run(configs, seeds, generations, populationSize, canvasWidth, canvasHeight,
                        (completed, total, message) -> Platform.runLater(() -> {
                            progressBar.setProgress((double) completed / total);
                            statusLabel.setText(message + " (" + completed + "/" + total + ")");
                        }));
            }
        };

        task.setOnSucceeded(e -> {
            lastResults = task.getValue();
            plotResults(lastResults);
            statusLabel.setText("Benchmark completo (" + seeds + " semillas x " + generations + " generaciones)");
            runButton.setDisable(false);
            exportButton.setDisable(false);
        });

        task.setOnFailed(e -> {
            runButton.setDisable(false);
            Throwable ex = task.getException();
            statusLabel.setText("Error: " + (ex != null ? ex.getMessage() : "desconocido"));
            Alert alert = new Alert(Alert.AlertType.ERROR, "El benchmark falló: " +
                    (ex != null ? ex.getMessage() : "error desconocido"));
            alert.showAndWait();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void plotResults(List<BenchmarkResult> results) {
        chart.getData().clear();

        // El rango del eje Y se calcula SOLO a partir de las curvas de media: son los valores
        // que realmente importa comparar. Si se incluyera la banda ± desv. estándar sin más,
        // una única generación con una desviación anómala (p. ej. un agente que explota un bug
        // del juego en una semilla) dispararía el rango a millones y aplastaría todas las demás
        // curvas contra el eje X. Las bandas se dibujan igualmente, pero se recortan (clamp) al
        // rango visible en vez de deformar la escala.
        double meanMin = Double.POSITIVE_INFINITY;
        double meanMax = Double.NEGATIVE_INFINITY;
        for (BenchmarkResult result : results) {
            for (double v : result.getMeanCurve()) {
                meanMin = Math.min(meanMin, v);
                meanMax = Math.max(meanMax, v);
            }
        }
        double lowerBound = 0;
        double upperBound = 1;
        if (Double.isFinite(meanMin) && Double.isFinite(meanMax)) {
            double range = Math.max(1.0, meanMax - meanMin);
            double margin = range * 0.2;
            lowerBound = Math.max(0, meanMin - margin);
            upperBound = meanMax + margin;
            yAxis.setAutoRanging(false);
            yAxis.setLowerBound(lowerBound);
            yAxis.setUpperBound(upperBound);
            yAxis.setTickUnit(Math.max(1.0, (upperBound - lowerBound) / 10.0));
        }
        final double clampLower = lowerBound;
        final double clampUpper = upperBound;

        for (int i = 0; i < results.size(); i++) {
            BenchmarkResult result = results.get(i);
            Color color = PALETTE[i % PALETTE.length];

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(result.getLabel());
            List<Double> mean = result.getMeanCurve();
            for (int g = 0; g < mean.size(); g++) {
                series.getData().add(new XYChart.Data<>(g, mean.get(g)));
            }
            chart.getData().add(series);
            if (series.getNode() != null) {
                String hex = String.format("#%02x%02x%02x", (int) (color.getRed() * 255),
                        (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
                series.getNode().setStyle("-fx-stroke: " + hex + "; -fx-stroke-width: 2px;");
            }

            List<Double> upper = new ArrayList<>(mean.size());
            List<Double> lower = new ArrayList<>(mean.size());
            List<Double> stdDev = result.getStdDevCurve();
            for (int g = 0; g < mean.size(); g++) {
                double u = mean.get(g) + stdDev.get(g);
                double l = mean.get(g) - stdDev.get(g);
                // Clamp de ambos extremos al rango visible completo (no solo el borde que
                // "sobresale"): si una banda entera queda fuera del rango, limitar solo un
                // lado invertía upper por debajo de lower, produciendo un polígono
                // auto-intersecante que Prism rellena como bloques rectangulares.
                upper.add(Math.max(clampLower, Math.min(clampUpper, u)));
                lower.add(Math.max(clampLower, Math.min(clampUpper, l)));
            }

            Polygon band = new Polygon();
            band.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.15));
            band.setStroke(null);
            band.setMouseTransparent(true);
            Platform.runLater(() -> ChartBandUtil.update(chart, band, upper, lower));
        }
    }

    private void exportCsv() {
        if (lastResults.isEmpty()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar comparativa de benchmark");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(file)) {
            Map<String, BenchmarkResult> byLabel = new LinkedHashMap<>();
            for (BenchmarkResult result : lastResults) byLabel.put(result.getLabel(), result);

            StringBuilder header = new StringBuilder("Generacion");
            for (String label : byLabel.keySet()) {
                header.append(",\"").append(label).append(" (media)\"");
                header.append(",\"").append(label).append(" (desv.est.)\"");
            }
            writer.println(header);

            int generations = lastResults.get(0).getMeanCurve().size();
            for (int g = 0; g < generations; g++) {
                StringBuilder row = new StringBuilder(String.valueOf(g + 1));
                for (BenchmarkResult result : byLabel.values()) {
                    row.append(',').append(result.getMeanCurve().get(g));
                    row.append(',').append(result.getStdDevCurve().get(g));
                }
                writer.println(row);
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Comparativa exportada correctamente.");
            alert.showAndWait();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error al exportar: " + ex.getMessage());
            alert.showAndWait();
        }
    }
}
