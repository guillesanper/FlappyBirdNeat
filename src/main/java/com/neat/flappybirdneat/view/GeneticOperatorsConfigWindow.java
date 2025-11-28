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
    }

    private void createWindow() {
        stage = new Stage();
        stage.setTitle("Configuración de Operadores Genéticos");
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox mainLayout = new VBox(8);
        mainLayout.setPadding(new Insets(10));
        mainLayout.setStyle("-fx-background-color: #f5f5f5;");

        // Título
        Label titleLabel = new Label("Configuración de Operadores Genéticos");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.DARKBLUE);

        // Crear secciones
        VBox cruceSection = createCruceSection();
        VBox seleccionSection = createSeleccionSection();
        VBox mutacionSection = createMutacionSection();
        VBox escaladoSection = createEscaladoSection();

        // Botones de acción
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button applyButton = new Button("Aplicar");
        applyButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30;");
        applyButton.setOnAction(e -> applyConfiguration());

        Button cancelButton = new Button("Cancelar");
        cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30;");
        cancelButton.setOnAction(e -> stage.close());

        buttonBox.getChildren().addAll(applyButton, cancelButton);

        // Agregar todo al layout principal
        mainLayout.getChildren().addAll(
            titleLabel,
            cruceSection,
            seleccionSection,
            mutacionSection,
            escaladoSection,
            buttonBox
        );

        ScrollPane scrollPane = new ScrollPane(mainLayout);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f5f5f5;");

        Scene scene = new Scene(scrollPane, 600, 450);
        stage.setScene(scene);
    }

    private VBox createCruceSection() {
        VBox section = new VBox(5);
        section.setPadding(new Insets(8));
        section.setStyle("-fx-background-color: #fafafa; -fx-background-radius: 3; -fx-border-color: #cccccc; -fx-border-radius: 3; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");

        Label titleLabel = new Label("🧬 Operador de Cruce");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        cruceComboBox = new ComboBox<>();
        cruceComboBox.getItems().addAll("Uniforme", "Punto Único", "Aritmético");
        cruceComboBox.setValue("Uniforme");
        cruceComboBox.setOnAction(e -> updateCruceParameters());

        cruceParam1Label = new Label("Alpha (0-1):");
        cruceParam1Field = new TextField("0.5");
        cruceParam1Field.setPrefWidth(80);
        cruceParam1Label.setVisible(false);
        cruceParam1Field.setVisible(false);

        HBox paramBox = new HBox(10);
        paramBox.setAlignment(Pos.CENTER_LEFT);
        paramBox.getChildren().addAll(cruceParam1Label, cruceParam1Field);

        section.getChildren().addAll(titleLabel, cruceComboBox, paramBox);
        return section;
    }

    private VBox createSeleccionSection() {
        VBox section = new VBox(5);
        section.setPadding(new Insets(8));
        section.setStyle("-fx-background-color: #fafafa; -fx-background-radius: 3; -fx-border-color: #cccccc; -fx-border-radius: 3; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");

        Label titleLabel = new Label("🎯 Operador de Selección");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        seleccionComboBox = new ComboBox<>();
        seleccionComboBox.getItems().addAll(
            "Ruleta", "Torneo Determinístico", "Torneo Probabilístico",
            "Ranking", "Truncamiento", "Estocástico Universal", "Restos"
        );
        seleccionComboBox.setValue("Ruleta");
        seleccionComboBox.setOnAction(e -> updateSeleccionParameters());

        seleccionParam1Label = new Label("Parámetro:");
        seleccionParam1Field = new TextField("0.75");
        seleccionParam1Field.setPrefWidth(80);
        seleccionParam1Label.setVisible(false);
        seleccionParam1Field.setVisible(false);

        HBox paramBox = new HBox(10);
        paramBox.setAlignment(Pos.CENTER_LEFT);
        paramBox.getChildren().addAll(seleccionParam1Label, seleccionParam1Field);

        section.getChildren().addAll(titleLabel, seleccionComboBox, paramBox);
        return section;
    }

    private VBox createMutacionSection() {
        VBox section = new VBox(5);
        section.setPadding(new Insets(8));
        section.setStyle("-fx-background-color: #fafafa; -fx-background-radius: 3; -fx-border-color: #cccccc; -fx-border-radius: 3; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");

        Label titleLabel = new Label("🔀 Operador de Mutación");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        mutacionComboBox = new ComboBox<>();
        mutacionComboBox.getItems().addAll("Gaussiana", "Uniforme", "No Uniforme");
        mutacionComboBox.setValue("Gaussiana");
        mutacionComboBox.setOnAction(e -> updateMutacionParameters());

        mutacionParam1Label = new Label("Sigma:");
        mutacionParam1Field = new TextField("0.1");
        mutacionParam1Field.setPrefWidth(80);
        mutacionParam2Label = new Label("Max Gen:");
        mutacionParam2Field = new TextField("1000");
        mutacionParam2Field.setPrefWidth(80);
        mutacionParam3Label = new Label("b:");
        mutacionParam3Field = new TextField("2.0");
        mutacionParam3Field.setPrefWidth(80);

        mutacionParam1Label.setVisible(false);
        mutacionParam1Field.setVisible(false);
        mutacionParam2Label.setVisible(false);
        mutacionParam2Field.setVisible(false);
        mutacionParam3Label.setVisible(false);
        mutacionParam3Field.setVisible(false);

        GridPane paramGrid = new GridPane();
        paramGrid.setHgap(10);
        paramGrid.setVgap(3);
        paramGrid.add(mutacionParam1Label, 0, 0);
        paramGrid.add(mutacionParam1Field, 1, 0);
        paramGrid.add(mutacionParam2Label, 0, 1);
        paramGrid.add(mutacionParam2Field, 1, 1);
        paramGrid.add(mutacionParam3Label, 0, 2);
        paramGrid.add(mutacionParam3Field, 1, 2);

        section.getChildren().addAll(titleLabel, mutacionComboBox, paramGrid);
        return section;
    }

    private VBox createEscaladoSection() {
        VBox section = new VBox(5);
        section.setPadding(new Insets(8));
        section.setStyle("-fx-background-color: #fafafa; -fx-background-radius: 3; -fx-border-color: #cccccc; -fx-border-radius: 3; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 3, 0, 0, 1);");

        Label titleLabel = new Label("📊 Operador de Escalado");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        escaladoComboBox = new ComboBox<>();
        escaladoComboBox.getItems().addAll("Ninguno", "Lineal", "Sigma", "Boltzmann");
        escaladoComboBox.setValue("Ninguno");
        escaladoComboBox.setOnAction(e -> updateEscaladoParameters());

        escaladoParam1Label = new Label("Param 1:");
        escaladoParam1Field = new TextField("2.0");
        escaladoParam1Field.setPrefWidth(80);
        escaladoParam2Label = new Label("Param 2:");
        escaladoParam2Field = new TextField("1.0");
        escaladoParam2Field.setPrefWidth(80);

        escaladoParam1Label.setVisible(false);
        escaladoParam1Field.setVisible(false);
        escaladoParam2Label.setVisible(false);
        escaladoParam2Field.setVisible(false);

        GridPane paramGrid = new GridPane();
        paramGrid.setHgap(10);
        paramGrid.setVgap(3);
        paramGrid.add(escaladoParam1Label, 0, 0);
        paramGrid.add(escaladoParam1Field, 1, 0);
        paramGrid.add(escaladoParam2Label, 0, 1);
        paramGrid.add(escaladoParam2Field, 1, 1);

        section.getChildren().addAll(titleLabel, escaladoComboBox, paramGrid);
        return section;
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

    public void show() {
        stage.show();
    }
}
