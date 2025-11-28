package com.neat.flappybirdneat.view;

import com.neat.flappybirdneat.neat.Population;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Panel de controles para configurar los operadores genéticos del algoritmo NEAT.
 * Diseño profesional y moderno con JavaFX.
 */
public class GeneticOperatorsPanel extends VBox {

    private Population population;

    // Controles de Selección
    private ComboBox<String> selectionComboBox;
    private Slider selectionParamSlider;
    private Label selectionParamLabel;
    private Label selectionDescLabel;

    // Controles de Escalado
    private ComboBox<String> scalingComboBox;
    private Slider scalingParam1Slider;
    private Label scalingParam1Label;
    private Label scalingDescLabel;

    // Controles de Mutación
    private ComboBox<String> mutationComboBox;
    private Slider mutationRateSlider;
    private Label mutationRateLabel;
    private Label mutationDescLabel;

    // Control de Elitismo
    private Slider elitismSlider;
    private Label elitismLabel;

    // Indicadores de estado
    private Label currentSelectionLabel;
    private Label currentScalingLabel;
    private Label currentMutationLabel;

    public GeneticOperatorsPanel(Population population) {
        this.population = population;

        setSpacing(15);
        setPadding(new Insets(15));
        setStyle("-fx-background-color: #f5f5f5;");
        setMaxWidth(400);

        // Título principal
        Label titleLabel = new Label("⚙ Operadores Genéticos");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.web("#2c3e50"));

        // Indicadores de configuración actual
        VBox statusBox = createStatusBox();

        // Secciones principales
        TitledPane selectionPane = createSelectionPane();
        TitledPane scalingPane = createScalingPane();
        TitledPane mutationPane = createMutationPane();
        TitledPane elitismPane = createElitismPane();

        // Botón de aplicar cambios
        Button applyButton = new Button("✓ Aplicar Cambios");
        applyButton.setStyle(
            "-fx-background-color: #27ae60; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 10px 20px; " +
            "-fx-cursor: hand;"
        );
        applyButton.setMaxWidth(Double.MAX_VALUE);
        applyButton.setOnAction(e -> applyChanges());

        // Botón de resetear a valores por defecto
        Button resetButton = new Button("↻ Valores Por Defecto");
        resetButton.setStyle(
            "-fx-background-color: #95a5a6; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 12px; " +
            "-fx-padding: 8px 15px; " +
            "-fx-cursor: hand;"
        );
        resetButton.setMaxWidth(Double.MAX_VALUE);
        resetButton.setOnAction(e -> resetToDefaults());

        // Añadir todos los componentes
        getChildren().addAll(
            titleLabel,
            createSeparator(),
            statusBox,
            createSeparator(),
            selectionPane,
            scalingPane,
            mutationPane,
            elitismPane,
            applyButton,
            resetButton
        );

        // Cargar configuración actual
        loadCurrentConfiguration();
    }

    /**
     * Crea la caja de estado que muestra la configuración actual
     */
    private VBox createStatusBox() {
        VBox box = new VBox(8);
        box.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 12px; " +
            "-fx-border-color: #bdc3c7; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 8px;"
        );

        Label statusTitle = new Label("📊 Configuración Actual");
        statusTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        statusTitle.setTextFill(Color.web("#34495e"));

        currentSelectionLabel = new Label("Selección: Ruleta");
        currentScalingLabel = new Label("Escalado: Ninguno");
        currentMutationLabel = new Label("Mutación: Gaussiana");

        currentSelectionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        currentScalingLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        currentMutationLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        box.getChildren().addAll(
            statusTitle,
            currentSelectionLabel,
            currentScalingLabel,
            currentMutationLabel
        );

        return box;
    }

    /**
     * Crea el panel de configuración de selección
     */
    private TitledPane createSelectionPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: white;");

        // ComboBox de método de selección
        Label methodLabel = new Label("Método de Selección:");
        methodLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        selectionComboBox = new ComboBox<>();
        selectionComboBox.getItems().addAll(
            "Ruleta",
            "Torneo Determinista",
            "Torneo Probabilístico",
            "Ranking",
            "Truncamiento",
            "Estocástico Universal",
            "Restos"
        );
        selectionComboBox.setValue("Ruleta");
        selectionComboBox.setMaxWidth(Double.MAX_VALUE);
        selectionComboBox.setStyle("-fx-font-size: 12px;");

        // Descripción del método
        selectionDescLabel = new Label();
        selectionDescLabel.setWrapText(true);
        selectionDescLabel.setStyle(
            "-fx-font-size: 11px; " +
            "-fx-text-fill: #7f8c8d; " +
            "-fx-padding: 5px; " +
            "-fx-background-color: #ecf0f1; " +
            "-fx-background-radius: 5px;"
        );
        updateSelectionDescription("Ruleta");

        // Parámetro configurable (para métodos que lo necesiten)
        selectionParamLabel = new Label("Parámetro:");
        selectionParamLabel.setFont(Font.font("System", 11));
        selectionParamLabel.setVisible(false);

        selectionParamSlider = new Slider(0, 1, 0.6);
        selectionParamSlider.setShowTickLabels(true);
        selectionParamSlider.setShowTickMarks(true);
        selectionParamSlider.setMajorTickUnit(0.2);
        selectionParamSlider.setVisible(false);

        // Listener para cambios en el ComboBox
        selectionComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateSelectionDescription(newVal);
            updateSelectionParamVisibility(newVal);
        });

        content.getChildren().addAll(
            methodLabel,
            selectionComboBox,
            selectionDescLabel,
            selectionParamLabel,
            selectionParamSlider
        );

        TitledPane pane = new TitledPane("🎯 Selección", content);
        pane.setExpanded(true);
        stylePane(pane);

        return pane;
    }

    /**
     * Crea el panel de configuración de escalado
     */
    private TitledPane createScalingPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: white;");

        Label methodLabel = new Label("Método de Escalado:");
        methodLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        scalingComboBox = new ComboBox<>();
        scalingComboBox.getItems().addAll(
            "Ninguno",
            "Lineal",
            "Sigma",
            "Boltzmann"
        );
        scalingComboBox.setValue("Ninguno");
        scalingComboBox.setMaxWidth(Double.MAX_VALUE);
        scalingComboBox.setStyle("-fx-font-size: 12px;");

        scalingDescLabel = new Label();
        scalingDescLabel.setWrapText(true);
        scalingDescLabel.setStyle(
            "-fx-font-size: 11px; " +
            "-fx-text-fill: #7f8c8d; " +
            "-fx-padding: 5px; " +
            "-fx-background-color: #ecf0f1; " +
            "-fx-background-radius: 5px;"
        );
        updateScalingDescription("Ninguno");

        scalingParam1Label = new Label("Temperatura inicial:");
        scalingParam1Label.setFont(Font.font("System", 11));
        scalingParam1Label.setVisible(false);

        scalingParam1Slider = new Slider(50, 200, 100);
        scalingParam1Slider.setShowTickLabels(true);
        scalingParam1Slider.setShowTickMarks(true);
        scalingParam1Slider.setMajorTickUnit(50);
        scalingParam1Slider.setVisible(false);

        scalingComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateScalingDescription(newVal);
            updateScalingParamVisibility(newVal);
        });

        content.getChildren().addAll(
            methodLabel,
            scalingComboBox,
            scalingDescLabel,
            scalingParam1Label,
            scalingParam1Slider
        );

        TitledPane pane = new TitledPane("📈 Escalado de Fitness", content);
        pane.setExpanded(false);
        stylePane(pane);

        return pane;
    }

    /**
     * Crea el panel de configuración de mutación
     */
    private TitledPane createMutationPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: white;");

        Label methodLabel = new Label("Método de Mutación:");
        methodLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        mutationComboBox = new ComboBox<>();
        mutationComboBox.getItems().addAll(
            "Gaussiana",
            "Uniforme",
            "No Uniforme"
        );
        mutationComboBox.setValue("Gaussiana");
        mutationComboBox.setMaxWidth(Double.MAX_VALUE);
        mutationComboBox.setStyle("-fx-font-size: 12px;");

        mutationDescLabel = new Label();
        mutationDescLabel.setWrapText(true);
        mutationDescLabel.setStyle(
            "-fx-font-size: 11px; " +
            "-fx-text-fill: #7f8c8d; " +
            "-fx-padding: 5px; " +
            "-fx-background-color: #ecf0f1; " +
            "-fx-background-radius: 5px;"
        );
        updateMutationDescription("Gaussiana");

        mutationRateLabel = new Label("Tasa de Mutación: 0.10");
        mutationRateLabel.setFont(Font.font("System", FontWeight.BOLD, 11));

        mutationRateSlider = new Slider(0.01, 0.5, 0.1);
        mutationRateSlider.setShowTickLabels(true);
        mutationRateSlider.setShowTickMarks(true);
        mutationRateSlider.setMajorTickUnit(0.1);
        mutationRateSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            mutationRateLabel.setText(String.format("Tasa de Mutación: %.2f", newVal.doubleValue()));
        });

        mutationComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateMutationDescription(newVal);
        });

        content.getChildren().addAll(
            methodLabel,
            mutationComboBox,
            mutationDescLabel,
            mutationRateLabel,
            mutationRateSlider
        );

        TitledPane pane = new TitledPane("🧬 Mutación", content);
        pane.setExpanded(false);
        stylePane(pane);

        return pane;
    }

    /**
     * Crea el panel de configuración de elitismo
     */
    private TitledPane createElitismPane() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setStyle("-fx-background-color: white;");

        elitismLabel = new Label("Tasa de Elitismo: 10%");
        elitismLabel.setFont(Font.font("System", FontWeight.BOLD, 11));

        elitismSlider = new Slider(0, 0.5, 0.1);
        elitismSlider.setShowTickLabels(true);
        elitismSlider.setShowTickMarks(true);
        elitismSlider.setMajorTickUnit(0.1);
        elitismSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            elitismLabel.setText(String.format("Tasa de Elitismo: %.0f%%", newVal.doubleValue() * 100));
        });

        Label descLabel = new Label(
            "El elitismo conserva los mejores individuos sin modificación.\n" +
            "Mayor elitismo = Mayor presión selectiva."
        );
        descLabel.setWrapText(true);
        descLabel.setStyle(
            "-fx-font-size: 11px; " +
            "-fx-text-fill: #7f8c8d; " +
            "-fx-padding: 5px; " +
            "-fx-background-color: #ecf0f1; " +
            "-fx-background-radius: 5px;"
        );

        content.getChildren().addAll(
            elitismLabel,
            elitismSlider,
            descLabel
        );

        TitledPane pane = new TitledPane("👑 Elitismo", content);
        pane.setExpanded(false);
        stylePane(pane);

        return pane;
    }

    /**
     * Aplica estilos uniformes a los TitledPane
     */
    private void stylePane(TitledPane pane) {
        pane.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #bdc3c7; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 5px; " +
            "-fx-background-radius: 5px;"
        );
    }

    /**
     * Crea un separador visual
     */
    private Region createSeparator() {
        Region separator = new Region();
        separator.setPrefHeight(1);
        separator.setStyle("-fx-background-color: #bdc3c7;");
        return separator;
    }

    /**
     * Actualiza la descripción del método de selección
     */
    private void updateSelectionDescription(String method) {
        String desc = switch (method) {
            case "Ruleta" -> "Selección proporcional al fitness. Mayor fitness → Mayor probabilidad.";
            case "Torneo Determinista" -> "Elige el mejor de 3 individuos aleatorios. Alta presión selectiva.";
            case "Torneo Probabilístico" -> "Similar al determinista pero con probabilidad de elegir el peor.";
            case "Ranking" -> "Probabilidad basada en posición, no en fitness absoluto. Reduce convergencia prematura.";
            case "Truncamiento" -> "Solo los mejores X% se reproducen. Muy elitista.";
            case "Estocástico Universal" -> "Ruleta mejorada con menor varianza. Más justa.";
            case "Restos" -> "Híbrido entre determinista y estocástico. Garantiza representación mínima.";
            default -> "";
        };
        selectionDescLabel.setText(desc);
    }

    /**
     * Actualiza la descripción del método de escalado
     */
    private void updateScalingDescription(String method) {
        String desc = switch (method) {
            case "Ninguno" -> "Sin escalado. Se usa el fitness original.";
            case "Lineal" -> "Transformación lineal: f' = a*f + b. Evita fitness negativos.";
            case "Sigma" -> "Basado en desviación estándar. Mantiene presión constante.";
            case "Boltzmann" -> "Con temperatura decreciente. Exploración inicial → Explotación final.";
            default -> "";
        };
        scalingDescLabel.setText(desc);
    }

    /**
     * Actualiza la descripción del método de mutación
     */
    private void updateMutationDescription(String method) {
        String desc = switch (method) {
            case "Gaussiana" -> "Añade ruido gaussiano pequeño. Ideal para ajuste fino.";
            case "Uniforme" -> "Reemplaza peso por valor aleatorio. Mayor exploración.";
            case "No Uniforme" -> "Magnitud decrece con generaciones. Adaptativo.";
            default -> "";
        };
        mutationDescLabel.setText(desc);
    }

    /**
     * Actualiza la visibilidad de parámetros de selección
     */
    private void updateSelectionParamVisibility(String method) {
        boolean needsParam = method.equals("Torneo Probabilístico") ||
                           method.equals("Ranking") ||
                           method.equals("Truncamiento");

        selectionParamLabel.setVisible(needsParam);
        selectionParamSlider.setVisible(needsParam);

        if (needsParam) {
            if (method.equals("Torneo Probabilístico")) {
                selectionParamLabel.setText("Probabilidad (p):");
                selectionParamSlider.setMin(0.5);
                selectionParamSlider.setMax(1.0);
                selectionParamSlider.setValue(0.6);
            } else if (method.equals("Truncamiento")) {
                selectionParamLabel.setText("Proporción seleccionada:");
                selectionParamSlider.setMin(0.1);
                selectionParamSlider.setMax(0.9);
                selectionParamSlider.setValue(0.6);
            } else if (method.equals("Ranking")) {
                selectionParamLabel.setText("Beta:");
                selectionParamSlider.setMin(1.0);
                selectionParamSlider.setMax(2.0);
                selectionParamSlider.setValue(1.5);
            }
        }
    }

    /**
     * Actualiza la visibilidad de parámetros de escalado
     */
    private void updateScalingParamVisibility(String method) {
        boolean needsParam = method.equals("Boltzmann");
        scalingParam1Label.setVisible(needsParam);
        scalingParam1Slider.setVisible(needsParam);
    }

    /**
     * Carga la configuración actual de la población
     */
    private void loadCurrentConfiguration() {
        // La configuración por defecto ya está cargada
        // Si queremos leer de la población actual, lo haríamos aquí
        updateStatusLabels();
    }

    /**
     * Aplica los cambios a la población
     */
    private void applyChanges() {
        // Aplicar método de selección
        String selectionMethod = convertSelectionName(selectionComboBox.getValue());
        if (selectionComboBox.getValue().equals("Torneo Probabilístico") ||
            selectionComboBox.getValue().equals("Ranking") ||
            selectionComboBox.getValue().equals("Truncamiento")) {
            // Usar el parámetro del slider
            // population.setSeleccionStrategy(método, parámetro) - esto requeriría modificar la API
        }
        population.setSeleccionStrategy(selectionMethod);

        // Aplicar método de escalado
        String scalingMethod = scalingComboBox.getValue().toLowerCase();
        population.setEscaladoStrategy(scalingMethod);

        // Aplicar método de mutación
        String mutationMethod = mutationComboBox.getValue().toLowerCase();
        population.setMutacionStrategy(mutationMethod);

        // Aplicar elitismo
        population.setElitismRate(elitismSlider.getValue());

        // Actualizar labels de estado
        updateStatusLabels();

        // Mostrar confirmación
        showConfirmation();
    }

    /**
     * Resetea a los valores por defecto
     */
    private void resetToDefaults() {
        selectionComboBox.setValue("Ruleta");
        scalingComboBox.setValue("Ninguno");
        mutationComboBox.setValue("Gaussiana");
        mutationRateSlider.setValue(0.1);
        elitismSlider.setValue(0.1);

        applyChanges();
    }

    /**
     * Actualiza los labels de estado
     */
    private void updateStatusLabels() {
        currentSelectionLabel.setText("Selección: " + selectionComboBox.getValue());
        currentScalingLabel.setText("Escalado: " + scalingComboBox.getValue());
        currentMutationLabel.setText("Mutación: " + mutationComboBox.getValue());
    }

    /**
     * Muestra una confirmación visual
     */
    private void showConfirmation() {
        Label confirmLabel = new Label("✓ Cambios aplicados correctamente");
        confirmLabel.setStyle(
            "-fx-background-color: #27ae60; " +
            "-fx-text-fill: white; " +
            "-fx-padding: 8px 12px; " +
            "-fx-background-radius: 5px; " +
            "-fx-font-size: 11px;"
        );

        getChildren().add(confirmLabel);

        // Remover después de 2 segundos
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                javafx.application.Platform.runLater(() -> getChildren().remove(confirmLabel));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Convierte el nombre del método de selección al formato esperado
     */
    private String convertSelectionName(String displayName) {
        return switch (displayName) {
            case "Ruleta" -> "ruleta";
            case "Torneo Determinista" -> "torneo deterministico";
            case "Torneo Probabilístico" -> "torneo probabilistico";
            case "Ranking" -> "ranking";
            case "Truncamiento" -> "truncamiento";
            case "Estocástico Universal" -> "estocastico universal";
            case "Restos" -> "restos";
            default -> "ruleta";
        };
    }
}
