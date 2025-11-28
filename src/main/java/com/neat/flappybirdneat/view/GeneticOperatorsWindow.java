package com.neat.flappybirdneat.view;

import com.neat.flappybirdneat.neat.Population;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

/**
 * Ventana independiente para configurar los operadores genéticos.
 */
public class GeneticOperatorsWindow {

    private Stage stage;
    private GeneticOperatorsPanel panel;
    private Population population;

    public GeneticOperatorsWindow(Population population) {
        this.population = population;
        initialize();
    }

    private void initialize() {
        stage = new Stage();
        stage.setTitle("⚙ Configuración de Operadores Genéticos");

        // Crear panel de operadores
        panel = new GeneticOperatorsPanel(population);

        // Envolver en ScrollPane para permitir scroll si es necesario
        ScrollPane scrollPane = new ScrollPane(panel);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #f5f5f5;");

        Scene scene = new Scene(scrollPane, 450, 700);

        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(400);
        stage.setMinHeight(600);
    }

    /**
     * Muestra la ventana
     */
    public void show() {
        if (!stage.isShowing()) {
            stage.show();
        } else {
            stage.toFront();
        }
    }

    /**
     * Cierra la ventana
     */
    public void close() {
        stage.close();
    }

    /**
     * Verifica si la ventana está visible
     */
    public boolean isShowing() {
        return stage.isShowing();
    }

    /**
     * Actualiza la referencia a la población (útil al reiniciar)
     */
    public void updatePopulation(Population newPopulation) {
        this.population = newPopulation;
        // Recrear el panel con la nueva población
        panel = new GeneticOperatorsPanel(newPopulation);
        ScrollPane scrollPane = new ScrollPane(panel);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #f5f5f5;");
        stage.getScene().setRoot(scrollPane);
    }
}
