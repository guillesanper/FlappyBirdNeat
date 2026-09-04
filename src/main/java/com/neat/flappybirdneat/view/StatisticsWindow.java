package com.neat.flappybirdneat.view;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import com.neat.flappybirdneat.simulation.SimulationController;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Panel de estadísticas avanzadas, en una ventana independiente (mismo patrón que
 * {@link NeuralNetworkWindow}): histograma de fitness de la generación actual y evolución de la
 * diversidad genética / nº de especies a lo largo de las generaciones.
 */
public class StatisticsWindow {
    private static final int HISTOGRAM_BUCKETS = 12;

    private final Stage stage;

    private final BarChart<String, Number> histogramChart;
    private final XYChart.Series<String, Number> histogramSeries;

    private final LineChart<Number, Number> diversityChart;
    private final XYChart.Series<Number, Number> diversitySeries;

    private final LineChart<Number, Number> speciesChart;
    private final XYChart.Series<Number, Number> speciesSeries;

    public StatisticsWindow(int width, int height) {
        stage = new Stage();
        stage.setTitle("Panel de Estadísticas Avanzado");

        // Histograma de fitness de la generación actual
        CategoryAxis histX = new CategoryAxis();
        histX.setLabel("Rango de fitness");
        NumberAxis histY = new NumberAxis();
        histY.setLabel("Nº de agentes");
        histogramChart = new BarChart<>(histX, histY);
        histogramChart.setTitle("Distribución de Fitness (Generación Actual)");
        histogramChart.setLegendVisible(false);
        histogramChart.setAnimated(false);
        histogramSeries = new XYChart.Series<>();
        histogramChart.getData().add(histogramSeries);

        // Diversidad genética a lo largo de las generaciones
        NumberAxis divX = new NumberAxis();
        divX.setLabel("Generación");
        NumberAxis divY = new NumberAxis();
        divY.setLabel("Diversidad");
        diversityChart = new LineChart<>(divX, divY);
        diversityChart.setTitle("Diversidad Genética");
        diversityChart.setCreateSymbols(false);
        diversityChart.setAnimated(false);
        diversityChart.setLegendVisible(false);
        diversitySeries = new XYChart.Series<>();
        diversityChart.getData().add(diversitySeries);

        // Nº de especies (solo tiene sentido en modo NEAT)
        NumberAxis specX = new NumberAxis();
        specX.setLabel("Generación");
        NumberAxis specY = new NumberAxis();
        specY.setLabel("Nº de especies");
        speciesChart = new LineChart<>(specX, specY);
        speciesChart.setTitle("Nº de Especies (modo NEAT)");
        speciesChart.setCreateSymbols(false);
        speciesChart.setAnimated(false);
        speciesChart.setLegendVisible(false);
        speciesSeries = new XYChart.Series<>();
        speciesChart.getData().add(speciesSeries);

        HBox evolutionCharts = new HBox(10, diversityChart, speciesChart);
        HBox.setHgrow(diversityChart, Priority.ALWAYS);
        HBox.setHgrow(speciesChart, Priority.ALWAYS);

        VBox root = new VBox(10, histogramChart, evolutionCharts);
        root.setPadding(new Insets(10));
        VBox.setVgrow(histogramChart, Priority.ALWAYS);
        VBox.setVgrow(evolutionCharts, Priority.ALWAYS);

        BorderPane borderPane = new BorderPane(root);
        stage.setScene(new Scene(borderPane, width, height));
    }

    public void show() {
        stage.show();
    }

    public void close() {
        stage.close();
    }

    public boolean isShowing() {
        return stage.isShowing();
    }

    /** Refresca los tres gráficos con el estado actual del controlador de simulación. */
    public void update(SimulationController controller) {
        updateHistogram(controller.getPopulation().getAgents());
        updateEvolutionChart(diversitySeries, controller.getDiversityHistory());

        List<Integer> speciesHistory = controller.getSpeciesCountHistory();
        boolean neatMode = !speciesHistory.isEmpty() && speciesHistory.get(speciesHistory.size() - 1) >= 0;
        speciesChart.setVisible(neatMode);
        speciesChart.setManaged(neatMode);
        if (neatMode) {
            speciesSeries.getData().clear();
            for (int i = 0; i < speciesHistory.size(); i++) {
                speciesSeries.getData().add(new XYChart.Data<>(i, speciesHistory.get(i)));
            }
        }
    }

    private void updateHistogram(FlappyBirdAgent[] agents) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (FlappyBirdAgent agent : agents) {
            min = Math.min(min, agent.getFitness());
            max = Math.max(max, agent.getFitness());
        }
        if (!Double.isFinite(min) || !Double.isFinite(max)) return;

        int[] buckets = new int[HISTOGRAM_BUCKETS];
        double range = Math.max(max - min, 1e-9);
        double bucketWidth = range / HISTOGRAM_BUCKETS;
        for (FlappyBirdAgent agent : agents) {
            int bucket = (int) ((agent.getFitness() - min) / bucketWidth);
            bucket = Math.min(Math.max(bucket, 0), HISTOGRAM_BUCKETS - 1);
            buckets[bucket]++;
        }

        histogramSeries.getData().clear();
        for (int i = 0; i < HISTOGRAM_BUCKETS; i++) {
            String label = String.format("%.0f", min + i * bucketWidth);
            histogramSeries.getData().add(new XYChart.Data<>(label, buckets[i]));
        }
    }

    private void updateEvolutionChart(XYChart.Series<Number, Number> series, List<Double> history) {
        series.getData().clear();
        for (int i = 0; i < history.size(); i++) {
            series.getData().add(new XYChart.Data<>(i, history.get(i)));
        }
    }
}
