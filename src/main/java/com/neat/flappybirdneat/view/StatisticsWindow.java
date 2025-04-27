package com.neat.flappybirdneat.view;
/*
import com.neat.flappybirdneat.history.RunHistory;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.List;

public class StatisticsWindow {
    private Stage stage;
    private BorderPane root;
    private ComboBox<RunHistory> runsComboBox;
    private ChartViewer chartViewer;

    public StatisticsWindow() {
        stage = new Stage();
        stage.setTitle("Estadísticas de Ejecución");

        root = new BorderPane();

        // Panel superior con controles
        HBox controlPanel = new HBox(10);
        Label selectRunLabel = new Label("Seleccionar ejecución:");
        runsComboBox = new ComboBox<>();
        Button showButton = new Button("Mostrar");

        showButton.setOnAction(e -> updateChart());

        controlPanel.getChildren().addAll(selectRunLabel, runsComboBox, showButton);

        // Panel central con gráfico
        chartViewer = new ChartViewer();

        // Panel lateral con estadísticas
        VBox statsPanel = new VBox(10);
        statsPanel.getChildren().addAll(
                new Label("Estadísticas:"),
                new Label("Fecha: "),
                new Label("Generaciones: "),
                new Label("Mejor fitness: ")
        );

        root.setTop(controlPanel);
        root.setCenter(chartViewer);
        root.setRight(statsPanel);

        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
    }

    public void show() {
        stage.show();
    }

    public void updateRunsList(List<RunHistory> histories) {
        runsComboBox.getItems().clear();
        runsComboBox.getItems().addAll(histories);
        if (!histories.isEmpty()) {
            runsComboBox.setValue(histories.get(histories.size() - 1));
            updateChart();
        }
    }

    private void updateChart() {
        RunHistory selectedRun = runsComboBox.getValue();
        if (selectedRun == null) return;

        // Crear series de datos
        XYSeries fitnessSeries = new XYSeries("Fitness");
        XYSeries aliveSeries = new XYSeries("Agentes vivos");

        List<Double> fitnessHistory = selectedRun.getFitnessHistory();
        List<Integer> aliveHistory = selectedRun.getAliveCountsPerGeneration();

        for (int i = 0; i < fitnessHistory.size(); i++) {
            fitnessSeries.add(i + 1, fitnessHistory.get(i));
            aliveSeries.add(i + 1, aliveHistory.get(i));
        }

        // Crear dataset y gráfico
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(fitnessSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Evolución del fitness",
                "Generación",
                "Fitness",
                dataset
        );

        chartViewer.setChart(chart);

        // Actualizar estadísticas
        VBox statsPanel = (VBox) root.getRight();
        statsPanel.getChildren().clear();
        statsPanel.getChildren().addAll(
                new Label("Estadísticas:"),
                new Label("Fecha: " + selectedRun.getRunDate()),
                new Label("Generaciones: " + selectedRun.getGenerations()),
                new Label("Mejor fitness: " + String.format("%.2f", selectedRun.getBestFitness()))
        );
    }

    public void closeWindow() {
        if (stage != null && stage.isShowing()) {
            stage.close();
        }
    }
}*/