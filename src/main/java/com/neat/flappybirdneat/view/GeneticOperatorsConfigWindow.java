package com.neat.flappybirdneat.view;

import com.neat.flappybirdneat.neat.Population;
import com.neat.flappybirdneat.neat.crossover.*;
import com.neat.flappybirdneat.neat.mutation.*;
import com.neat.flappybirdneat.neat.selection.*;
import com.neat.flappybirdneat.neat.scaling.*;
import com.neat.flappybirdneat.simulation.SimulationController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Ventana de configuración de operadores genéticos.
 * Permite seleccionar y configurar cruces, selecciones, mutaciones y escalado.
 */
public class GeneticOperatorsConfigWindow {
    private Stage stage;
    private Population population;
    private SimulationController controller;

    // Controles de Cruce
    private ComboBox<String> cruceComboBox;
    private TextField cruceParam1Field;
    private Label cruceParam1Label;

    // Controles de Selección
    private ComboBox<String> seleccionComboBox;
    private TextField seleccionParam1Field;
    private Label seleccionParam1Label;

    // Controles de Mutación
    private ComboBox<String> mutacionComboBox;
    private TextField mutacionParam1Field;
    private TextField mutacionParam2Field;
    private TextField mutacionParam3Field;
    private Label mutacionParam1Label;
    private Label mutacionParam2Label;
    private Label mutacionParam3Label;

    // Controles de Escalado
    private ComboBox<String> escaladoComboBox;
    private TextField escaladoParam1Field;
    private TextField escaladoParam2Field;
    private Label escaladoParam1Label;
    private Label escaladoParam2Label;

    public GeneticOperatorsConfigWindow(Population population, SimulationController controller) {
        this.population = population;
        this.controller = controller;
        createWindow();
        loadCurrentConfiguration(); // Cargar configuración actual
    }

    private void createWindow() {
        stage = new Stage();
        stage.setTitle("Configuración de Operadores Genéticos");
        stage.initModality(Modality.APPLICATION_MODAL);

        // Panel principal con GridPane para organizar labels y campos
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(15));
        gridPane.setHgap(10);
        gridPane.setVgap(8);
        gridPane.setStyle("-fx-background-color: #f5f5f5;");

        int row = 0;

        // Título
        Label titleLabel = new Label("Configuración de Operadores Genéticos");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.DARKBLUE);
        GridPane.setColumnSpan(titleLabel, 2);
        gridPane.add(titleLabel, 0, row++, 2, 1);

        // Separador
        Region separator1 = new Region();
        separator1.setPrefHeight(10);
        GridPane.setColumnSpan(separator1, 2);
        gridPane.add(separator1, 0, row++, 2, 1);

        // === CRUCE ===
        Label cruceHeader = new Label("🧬 CRUCE");
        cruceHeader.setFont(Font.font("System", FontWeight.BOLD, 13));
        GridPane.setColumnSpan(cruceHeader, 2);
        gridPane.add(cruceHeader, 0, row++, 2, 1);

        gridPane.add(new Label("Método de Cruce:"), 0, row);
        cruceComboBox = new ComboBox<>();
        cruceComboBox.getItems().addAll("Uniforme", "Punto Único", "Aritmético");
        cruceComboBox.setValue("Uniforme");
        cruceComboBox.setPrefWidth(200);
        cruceComboBox.setOnAction(e -> updateCruceParameters());
        gridPane.add(cruceComboBox, 1, row++);

        cruceParam1Label = new Label("Alpha (0-1):");
        cruceParam1Field = new TextField("0.5");
        cruceParam1Field.setPrefWidth(200);
        cruceParam1Label.setVisible(false);
        cruceParam1Field.setVisible(false);
        gridPane.add(cruceParam1Label, 0, row);
        gridPane.add(cruceParam1Field, 1, row++);

        // Separador
        Region separator2 = new Region();
        separator2.setPrefHeight(10);
        GridPane.setColumnSpan(separator2, 2);
        gridPane.add(separator2, 0, row++, 2, 1);

        // === SELECCIÓN ===
        Label seleccionHeader = new Label("🎯 SELECCIÓN");
        seleccionHeader.setFont(Font.font("System", FontWeight.BOLD, 13));
        GridPane.setColumnSpan(seleccionHeader, 2);
        gridPane.add(seleccionHeader, 0, row++, 2, 1);

        gridPane.add(new Label("Método de Selección:"), 0, row);
        seleccionComboBox = new ComboBox<>();
        seleccionComboBox.getItems().addAll(
            "Ruleta", "Torneo Determinístico", "Torneo Probabilístico",
            "Ranking", "Truncamiento", "Estocástico Universal", "Restos"
        );
        seleccionComboBox.setValue("Ruleta");
        seleccionComboBox.setPrefWidth(200);
        seleccionComboBox.setOnAction(e -> updateSeleccionParameters());
        gridPane.add(seleccionComboBox, 1, row++);

        seleccionParam1Label = new Label("Parámetro:");
        seleccionParam1Field = new TextField("0.75");
        seleccionParam1Field.setPrefWidth(200);
        seleccionParam1Label.setVisible(false);
        seleccionParam1Field.setVisible(false);
        gridPane.add(seleccionParam1Label, 0, row);
        gridPane.add(seleccionParam1Field, 1, row++);

        // Separador
        Region separator3 = new Region();
        separator3.setPrefHeight(10);
        GridPane.setColumnSpan(separator3, 2);
        gridPane.add(separator3, 0, row++, 2, 1);

        // === MUTACIÓN ===
        Label mutacionHeader = new Label("🔀 MUTACIÓN");
        mutacionHeader.setFont(Font.font("System", FontWeight.BOLD, 13));
        GridPane.setColumnSpan(mutacionHeader, 2);
        gridPane.add(mutacionHeader, 0, row++, 2, 1);

        gridPane.add(new Label("Método de Mutación:"), 0, row);
        mutacionComboBox = new ComboBox<>();
        mutacionComboBox.getItems().addAll("Gaussiana", "Uniforme", "No Uniforme");
        mutacionComboBox.setValue("Gaussiana");
        mutacionComboBox.setPrefWidth(200);
        mutacionComboBox.setOnAction(e -> updateMutacionParameters());
        gridPane.add(mutacionComboBox, 1, row++);

        mutacionParam1Label = new Label("Sigma:");
        mutacionParam1Field = new TextField("0.1");
        mutacionParam1Field.setPrefWidth(200);
        mutacionParam1Label.setVisible(false);
        mutacionParam1Field.setVisible(false);
        gridPane.add(mutacionParam1Label, 0, row);
        gridPane.add(mutacionParam1Field, 1, row++);

        mutacionParam2Label = new Label("Max Generaciones:");
        mutacionParam2Field = new TextField("1000");
        mutacionParam2Field.setPrefWidth(200);
        mutacionParam2Label.setVisible(false);
        mutacionParam2Field.setVisible(false);
        gridPane.add(mutacionParam2Label, 0, row);
        gridPane.add(mutacionParam2Field, 1, row++);

        mutacionParam3Label = new Label("b:");
        mutacionParam3Field = new TextField("2.0");
        mutacionParam3Field.setPrefWidth(200);
        mutacionParam3Label.setVisible(false);
        mutacionParam3Field.setVisible(false);
        gridPane.add(mutacionParam3Label, 0, row);
        gridPane.add(mutacionParam3Field, 1, row++);

        // Separador
        Region separator4 = new Region();
        separator4.setPrefHeight(10);
        GridPane.setColumnSpan(separator4, 2);
        gridPane.add(separator4, 0, row++, 2, 1);

        // === ESCALADO ===
        Label escaladoHeader = new Label("📊 ESCALADO");
        escaladoHeader.setFont(Font.font("System", FontWeight.BOLD, 13));
        GridPane.setColumnSpan(escaladoHeader, 2);
        gridPane.add(escaladoHeader, 0, row++, 2, 1);

        gridPane.add(new Label("Método de Escalado:"), 0, row);
        escaladoComboBox = new ComboBox<>();
        escaladoComboBox.getItems().addAll("Ninguno", "Lineal", "Sigma", "Boltzmann");
        escaladoComboBox.setValue("Ninguno");
        escaladoComboBox.setPrefWidth(200);
        escaladoComboBox.setOnAction(e -> updateEscaladoParameters());
        gridPane.add(escaladoComboBox, 1, row++);

        escaladoParam1Label = new Label("Param 1:");
        escaladoParam1Field = new TextField("2.0");
        escaladoParam1Field.setPrefWidth(200);
        escaladoParam1Label.setVisible(false);
        escaladoParam1Field.setVisible(false);
        gridPane.add(escaladoParam1Label, 0, row);
        gridPane.add(escaladoParam1Field, 1, row++);

        escaladoParam2Label = new Label("Param 2:");
        escaladoParam2Field = new TextField("1.0");
        escaladoParam2Field.setPrefWidth(200);
        escaladoParam2Label.setVisible(false);
        escaladoParam2Field.setVisible(false);
        gridPane.add(escaladoParam2Label, 0, row);
        gridPane.add(escaladoParam2Field, 1, row++);

        // Separador
        Region separator5 = new Region();
        separator5.setPrefHeight(15);
        GridPane.setColumnSpan(separator5, 2);
        gridPane.add(separator5, 0, row++, 2, 1);

        // Botones de acción
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button applyButton = new Button("Aplicar");
        applyButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 30;");
        applyButton.setOnAction(e -> applyConfiguration());

        Button cancelButton = new Button("Cancelar");
        cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 30;");
        cancelButton.setOnAction(e -> stage.close());

        buttonBox.getChildren().addAll(applyButton, cancelButton);
        GridPane.setColumnSpan(buttonBox, 2);
        gridPane.add(buttonBox, 0, row++, 2, 1);

        Scene scene = new Scene(gridPane, 450, 600);
        stage.setScene(scene);
    }

    private void updateCruceParameters() {
        String selected = cruceComboBox.getValue();
        cruceParam1Label.setVisible(false);
        cruceParam1Field.setVisible(false);

        if ("Aritmético".equals(selected)) {
            cruceParam1Label.setText("Alpha (0-1):");
            cruceParam1Label.setVisible(true);
            cruceParam1Field.setVisible(true);
        }
    }

    private void updateSeleccionParameters() {
        String selected = seleccionComboBox.getValue();
        seleccionParam1Label.setVisible(false);
        seleccionParam1Field.setVisible(false);

        if ("Torneo Probabilístico".equals(selected)) {
            seleccionParam1Label.setText("Probabilidad (0-1):");
            seleccionParam1Field.setText("0.75");
            seleccionParam1Label.setVisible(true);
            seleccionParam1Field.setVisible(true);
        } else if ("Ranking".equals(selected)) {
            seleccionParam1Label.setText("Beta:");
            seleccionParam1Field.setText("2.0");
            seleccionParam1Label.setVisible(true);
            seleccionParam1Field.setVisible(true);
        } else if ("Truncamiento".equals(selected)) {
            seleccionParam1Label.setText("Truncamiento (0-1):");
            seleccionParam1Field.setText("0.5");
            seleccionParam1Label.setVisible(true);
            seleccionParam1Field.setVisible(true);
        }
    }

    private void updateMutacionParameters() {
        String selected = mutacionComboBox.getValue();
        mutacionParam1Label.setVisible(false);
        mutacionParam1Field.setVisible(false);
        mutacionParam2Label.setVisible(false);
        mutacionParam2Field.setVisible(false);
        mutacionParam3Label.setVisible(false);
        mutacionParam3Field.setVisible(false);

        if ("Gaussiana".equals(selected)) {
            mutacionParam1Label.setText("Sigma:");
            mutacionParam1Field.setText("0.1");
            mutacionParam1Label.setVisible(true);
            mutacionParam1Field.setVisible(true);
        } else if ("No Uniforme".equals(selected)) {
            mutacionParam2Label.setText("Max Generaciones:");
            mutacionParam2Field.setText("1000");
            mutacionParam2Label.setVisible(true);
            mutacionParam2Field.setVisible(true);
            mutacionParam3Label.setText("b:");
            mutacionParam3Field.setText("2.0");
            mutacionParam3Label.setVisible(true);
            mutacionParam3Field.setVisible(true);
        }
    }

    private void updateEscaladoParameters() {
        String selected = escaladoComboBox.getValue();
        escaladoParam1Label.setVisible(false);
        escaladoParam1Field.setVisible(false);
        escaladoParam2Label.setVisible(false);
        escaladoParam2Field.setVisible(false);

        if ("Lineal".equals(selected)) {
            escaladoParam1Label.setText("a:");
            escaladoParam1Field.setText("2.0");
            escaladoParam2Label.setText("b:");
            escaladoParam2Field.setText("1.0");
            escaladoParam1Label.setVisible(true);
            escaladoParam1Field.setVisible(true);
            escaladoParam2Label.setVisible(true);
            escaladoParam2Field.setVisible(true);
        } else if ("Boltzmann".equals(selected)) {
            escaladoParam1Label.setText("T0:");
            escaladoParam1Field.setText("100.0");
            escaladoParam2Label.setText("T_min:");
            escaladoParam2Field.setText("1.0");
            escaladoParam1Label.setVisible(true);
            escaladoParam1Field.setVisible(true);
            escaladoParam2Label.setVisible(true);
            escaladoParam2Field.setVisible(true);
        }
    }

    private void applyConfiguration() {
        try {
            // Aplicar Cruce
            String cruceType = cruceComboBox.getValue();
            if ("Uniforme".equals(cruceType)) {
                population.setCruceStrategy(new CruceUniforme());
            } else if ("Punto Único".equals(cruceType)) {
                population.setCruceStrategy(new CrucePuntoUnico());
            } else if ("Aritmético".equals(cruceType)) {
                double alpha = Double.parseDouble(cruceParam1Field.getText());
                population.setCruceStrategy(new CruceAritmetico(alpha));
            }

            // Aplicar Selección
            String seleccionType = seleccionComboBox.getValue();
            if ("Ruleta".equals(seleccionType)) {
                population.setSeleccionStrategy(new SeleccionRuleta());
            } else if ("Torneo Determinístico".equals(seleccionType)) {
                population.setSeleccionStrategy(new SeleccionTorneoDeterministico());
            } else if ("Torneo Probabilístico".equals(seleccionType)) {
                double prob = Double.parseDouble(seleccionParam1Field.getText());
                population.setSeleccionStrategy(new SeleccionTorneoProbabilistico(prob));
            } else if ("Ranking".equals(seleccionType)) {
                double beta = Double.parseDouble(seleccionParam1Field.getText());
                population.setSeleccionStrategy(new SeleccionRanking(beta));
            } else if ("Truncamiento".equals(seleccionType)) {
                double trunc = Double.parseDouble(seleccionParam1Field.getText());
                population.setSeleccionStrategy(new SeleccionTruncamiento(trunc));
            } else if ("Estocástico Universal".equals(seleccionType)) {
                population.setSeleccionStrategy(new SeleccionEstocasticoUniversal());
            } else if ("Restos".equals(seleccionType)) {
                population.setSeleccionStrategy(new SeleccionRestos());
            }

            // Aplicar Mutación
            String mutacionType = mutacionComboBox.getValue();
            if ("Gaussiana".equals(mutacionType)) {
                if (mutacionParam1Field.isVisible()) {
                    double sigma = Double.parseDouble(mutacionParam1Field.getText());
                    population.setMutacionStrategy(new MutacionGaussiana(sigma));
                } else {
                    population.setMutacionStrategy(new MutacionGaussiana());
                }
            } else if ("Uniforme".equals(mutacionType)) {
                population.setMutacionStrategy(new MutacionUniforme());
            } else if ("No Uniforme".equals(mutacionType)) {
                int maxGen = Integer.parseInt(mutacionParam2Field.getText());
                double b = Double.parseDouble(mutacionParam3Field.getText());
                population.setMutacionStrategy(new MutacionNoUniforme(b, maxGen, 0.0));
            }

            // Aplicar Escalado
            String escaladoType = escaladoComboBox.getValue();
            if ("Ninguno".equals(escaladoType)) {
                population.setEscaladoStrategy((Escalado) null);
            } else if ("Lineal".equals(escaladoType)) {
                if (escaladoParam1Field.isVisible()) {
                    double a = Double.parseDouble(escaladoParam1Field.getText());
                    double b = Double.parseDouble(escaladoParam2Field.getText());
                    population.setEscaladoStrategy(new EscaladoLineal(a, b));
                } else {
                    population.setEscaladoStrategy(new EscaladoLineal());
                }
            } else if ("Sigma".equals(escaladoType)) {
                population.setEscaladoStrategy(new EscaladoSigma());
            } else if ("Boltzmann".equals(escaladoType)) {
                if (escaladoParam1Field.isVisible()) {
                    double t0 = Double.parseDouble(escaladoParam1Field.getText());
                    double tmin = Double.parseDouble(escaladoParam2Field.getText());
                    population.setEscaladoStrategy(new EscaladoBoltzmann(t0, tmin));
                } else {
                    population.setEscaladoStrategy(new EscaladoBoltzmann(100.0));
                }
            }

            // Guardar configuración en el controlador para persistencia
            if (controller != null) {
                controller.updateOperatorsConfig();
            }

            // Mostrar mensaje de éxito
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Configuración Aplicada");
            alert.setHeaderText(null);
            alert.setContentText("Los operadores genéticos han sido configurados correctamente.");
            alert.showAndWait();

            stage.close();

        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error en los parámetros");
            alert.setContentText("Por favor, verifica que todos los parámetros sean valores numéricos válidos.");
            alert.showAndWait();
        }
    }

    /**
     * Carga la configuración actual de la población y la muestra en la interfaz
     */
    private void loadCurrentConfiguration() {
        // Cargar Cruce
        CruceStrategy cruce = population.getCruceStrategy();
        if (cruce != null) {
            String cruceClassName = cruce.getClass().getSimpleName();
            if (cruceClassName.equals("CruceUniforme")) {
                cruceComboBox.setValue("Uniforme");
            } else if (cruceClassName.equals("CrucePuntoUnico")) {
                cruceComboBox.setValue("Punto Único");
            } else if (cruceClassName.equals("CruceAritmetico")) {
                cruceComboBox.setValue("Aritmético");
                try {
                    // Usar reflexión para obtener alpha
                    java.lang.reflect.Field alphaField = cruce.getClass().getDeclaredField("alpha");
                    alphaField.setAccessible(true);
                    double alpha = alphaField.getDouble(cruce);
                    cruceParam1Field.setText(String.valueOf(alpha));
                } catch (Exception e) {
                    cruceParam1Field.setText("0.5");
                }
            }
        }
        updateCruceParameters();

        // Cargar Selección
        Seleccion seleccion = population.getSeleccionStrategy();
        if (seleccion != null) {
            String seleccionClassName = seleccion.getClass().getSimpleName();
            if (seleccionClassName.equals("SeleccionRuleta")) {
                seleccionComboBox.setValue("Ruleta");
            } else if (seleccionClassName.equals("SeleccionTorneoDeterministico")) {
                seleccionComboBox.setValue("Torneo Determinístico");
            } else if (seleccionClassName.equals("SeleccionTorneoProbabilistico")) {
                seleccionComboBox.setValue("Torneo Probabilístico");
                try {
                    java.lang.reflect.Field probField = seleccion.getClass().getDeclaredField("probability");
                    probField.setAccessible(true);
                    double prob = probField.getDouble(seleccion);
                    seleccionParam1Field.setText(String.valueOf(prob));
                } catch (Exception e) {
                    seleccionParam1Field.setText("0.75");
                }
            } else if (seleccionClassName.equals("SeleccionRanking")) {
                seleccionComboBox.setValue("Ranking");
                try {
                    java.lang.reflect.Field betaField = seleccion.getClass().getDeclaredField("beta");
                    betaField.setAccessible(true);
                    double beta = betaField.getDouble(seleccion);
                    seleccionParam1Field.setText(String.valueOf(beta));
                } catch (Exception e) {
                    seleccionParam1Field.setText("2.0");
                }
            } else if (seleccionClassName.equals("SeleccionTruncamiento")) {
                seleccionComboBox.setValue("Truncamiento");
                try {
                    java.lang.reflect.Field truncField = seleccion.getClass().getDeclaredField("truncamiento");
                    truncField.setAccessible(true);
                    double trunc = truncField.getDouble(seleccion);
                    seleccionParam1Field.setText(String.valueOf(trunc));
                } catch (Exception e) {
                    seleccionParam1Field.setText("0.5");
                }
            } else if (seleccionClassName.equals("SeleccionEstocasticoUniversal")) {
                seleccionComboBox.setValue("Estocástico Universal");
            } else if (seleccionClassName.equals("SeleccionRestos")) {
                seleccionComboBox.setValue("Restos");
            }
        }
        updateSeleccionParameters();

        // Cargar Mutación
        MutacionStrategy mutacion = population.getMutacionStrategy();
        if (mutacion != null) {
            String mutacionClassName = mutacion.getClass().getSimpleName();
            if (mutacionClassName.equals("MutacionGaussiana")) {
                mutacionComboBox.setValue("Gaussiana");
                try {
                    java.lang.reflect.Field magField = mutacion.getClass().getDeclaredField("magnitude");
                    magField.setAccessible(true);
                    double sigma = magField.getDouble(mutacion);
                    mutacionParam1Field.setText(String.valueOf(sigma));
                } catch (Exception e) {
                    mutacionParam1Field.setText("0.1");
                }
            } else if (mutacionClassName.equals("MutacionUniforme")) {
                mutacionComboBox.setValue("Uniforme");
            } else if (mutacionClassName.equals("MutacionNoUniforme")) {
                mutacionComboBox.setValue("No Uniforme");
                try {
                    java.lang.reflect.Field bField = mutacion.getClass().getDeclaredField("b");
                    bField.setAccessible(true);
                    double b = bField.getDouble(mutacion);
                    mutacionParam3Field.setText(String.valueOf(b));

                    java.lang.reflect.Field maxGenField = mutacion.getClass().getDeclaredField("maxGenerations");
                    maxGenField.setAccessible(true);
                    int maxGen = maxGenField.getInt(mutacion);
                    mutacionParam2Field.setText(String.valueOf(maxGen));
                } catch (Exception e) {
                    mutacionParam2Field.setText("1000");
                    mutacionParam3Field.setText("2.0");
                }
            }
        }
        updateMutacionParameters();

        // Cargar Escalado
        Escalado escalado = population.getEscaladoStrategy();
        if (escalado == null) {
            escaladoComboBox.setValue("Ninguno");
        } else {
            String escaladoClassName = escalado.getClass().getSimpleName();
            if (escaladoClassName.equals("EscaladoLineal")) {
                escaladoComboBox.setValue("Lineal");
                try {
                    java.lang.reflect.Field aField = escalado.getClass().getDeclaredField("a");
                    aField.setAccessible(true);
                    double a = aField.getDouble(escalado);
                    escaladoParam1Field.setText(String.valueOf(a));

                    java.lang.reflect.Field bField = escalado.getClass().getDeclaredField("b");
                    bField.setAccessible(true);
                    double b = bField.getDouble(escalado);
                    escaladoParam2Field.setText(String.valueOf(b));
                } catch (Exception e) {
                    escaladoParam1Field.setText("2.0");
                    escaladoParam2Field.setText("1.0");
                }
            } else if (escaladoClassName.equals("EscaladoSigma")) {
                escaladoComboBox.setValue("Sigma");
            } else if (escaladoClassName.equals("EscaladoBoltzmann")) {
                escaladoComboBox.setValue("Boltzmann");
                try {
                    java.lang.reflect.Field t0Field = escalado.getClass().getDeclaredField("T0");
                    t0Field.setAccessible(true);
                    double t0 = t0Field.getDouble(escalado);
                    escaladoParam1Field.setText(String.valueOf(t0));

                    java.lang.reflect.Field tMinField = escalado.getClass().getDeclaredField("T_min");
                    tMinField.setAccessible(true);
                    double tMin = tMinField.getDouble(escalado);
                    escaladoParam2Field.setText(String.valueOf(tMin));
                } catch (Exception e) {
                    escaladoParam1Field.setText("100.0");
                    escaladoParam2Field.setText("1.0");
                }
            }
        }
        updateEscaladoParameters();
    }

    public void show() {
        stage.show();
    }
}
