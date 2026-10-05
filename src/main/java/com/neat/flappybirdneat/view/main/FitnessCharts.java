package com.neat.flappybirdneat.view.main;

import com.neat.flappybirdneat.simulation.SimulationController;
import com.neat.flappybirdneat.view.ChartBandUtil;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

/**
 * Charts of the statistics tab: fitness per generation (best, average and best-so-far, with a
 * min-max band), genetic diversity, and number of species in NEAT mode.
 */
final class FitnessCharts {

    private final LineChart<Number, Number> fitnessChart;
    private final XYChart.Series<Number, Number> bestFitnessSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> avgFitnessSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> bestAbsoluteSeries = new XYChart.Series<>();
    private final Polygon minMaxBand = new Polygon();

    private final XYChart.Series<Number, Number> diversitySeries = new XYChart.Series<>();
    private final LineChart<Number, Number> speciesCountChart;
    private final XYChart.Series<Number, Number> speciesCountSeries = new XYChart.Series<>();

    private final ScrollPane view;

    FitnessCharts() {
        fitnessChart = createFitnessChart();
        LineChart<Number, Number> diversityChart = metricChart("Diversidad", "Diversidad Genética", diversitySeries);
        speciesCountChart = metricChart("Nº de especies", "Nº de Especies (modo NEAT)", speciesCountSeries);

        HBox evolutionMetricsBox = new HBox(10, diversityChart, speciesCountChart);
        HBox.setHgrow(diversityChart, Priority.ALWAYS);
        HBox.setHgrow(speciesCountChart, Priority.ALWAYS);

        // The charts live in a ScrollPane so they fit even when the window isn't maximized
        fitnessChart.setPrefHeight(400);
        fitnessChart.setMinHeight(300);
        VBox.setVgrow(fitnessChart, Priority.NEVER);

        VBox chartsBox = new VBox(15, fitnessChart, evolutionMetricsBox);
        chartsBox.setPadding(new Insets(5, 0, 0, 0));
        view = new ScrollPane(chartsBox);
        view.setFitToWidth(true);
        view.setPannable(true);
        VBox.setVgrow(view, Priority.ALWAYS);
    }

    Node getView() {
        return view;
    }

    private LineChart<Number, Number> createFitnessChart() {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Generación");
        xAxis.setAnimated(false);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Fitness");
        yAxis.setAnimated(false);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Evolución del Fitness a lo largo de las Generaciones");
        chart.setAnimated(false);
        chart.setCreateSymbols(false);

        bestFitnessSeries.setName("Mejor Fitness");
        avgFitnessSeries.setName("Fitness Promedio");
        bestAbsoluteSeries.setName("Mejor Absoluto");
        chart.getData().addAll(bestFitnessSeries, avgFitnessSeries, bestAbsoluteSeries);

        bestFitnessSeries.getNode().setStyle("-fx-stroke: red; -fx-stroke-width: 2px;");
        avgFitnessSeries.getNode().setStyle("-fx-stroke: blue; -fx-stroke-width: 1.5px;");
        bestAbsoluteSeries
                .getNode()
                .setStyle("-fx-stroke: green; -fx-stroke-width: 2.5px; -fx-stroke-dash-array: 5 5;");

        // Shaded band between each generation's minimum and best fitness
        minMaxBand.setFill(new Color(1, 0, 0, 0.12));
        minMaxBand.setStroke(null);
        minMaxBand.setMouseTransparent(true);
        return chart;
    }

    private static LineChart<Number, Number> metricChart(
            String yLabel, String title, XYChart.Series<Number, Number> series) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Generación");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yLabel);
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.getData().add(series);
        chart.setPrefHeight(200);
        return chart;
    }

    /**
     * Brings the charts up to date with the controller's history. The fitness series only get the
     * new points appended, unless the history shrank (the simulation was reset), in which case
     * they are rebuilt.
     */
    void update(SimulationController controller) {
        List<Double> bestHistory = controller.getBestFitnessHistory();
        List<Double> avgHistory = controller.getAvgFitnessHistory();
        List<Double> absoluteHistory = controller.getBestAbsoluteFitnessHistory();

        int currentBestSize = bestFitnessSeries.getData().size();
        int currentAvgSize = avgFitnessSeries.getData().size();
        appendNewPoints(bestFitnessSeries, bestHistory);
        appendNewPoints(avgFitnessSeries, avgHistory);
        appendNewPoints(bestAbsoluteSeries, absoluteHistory);

        if (bestHistory.size() < currentBestSize || avgHistory.size() < currentAvgSize) {
            replacePoints(bestFitnessSeries, bestHistory);
            replacePoints(avgFitnessSeries, avgHistory);
            replacePoints(bestAbsoluteSeries, absoluteHistory);
        }

        ChartBandUtil.update(fitnessChart, minMaxBand, bestHistory, controller.getMinFitnessHistory());

        replacePoints(diversitySeries, controller.getDiversityHistory());

        List<Integer> speciesHistory = controller.getSpeciesCountHistory();
        boolean neatMode = !speciesHistory.isEmpty() && speciesHistory.get(speciesHistory.size() - 1) >= 0;
        speciesCountChart.setVisible(neatMode);
        speciesCountChart.setManaged(neatMode);
        if (neatMode) {
            replacePoints(speciesCountSeries, speciesHistory);
        }
    }

    private static void appendNewPoints(XYChart.Series<Number, Number> series, List<? extends Number> history) {
        for (int i = series.getData().size(); i < history.size(); i++) {
            series.getData().add(new XYChart.Data<>(i, history.get(i)));
        }
    }

    private static void replacePoints(XYChart.Series<Number, Number> series, List<? extends Number> history) {
        series.getData().clear();
        for (int i = 0; i < history.size(); i++) {
            series.getData().add(new XYChart.Data<>(i, history.get(i)));
        }
    }
}
