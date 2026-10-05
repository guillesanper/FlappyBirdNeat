# Guía de Integración de la Interfaz de Operadores Genéticos

## Archivos Creados

✅ **GeneticOperatorsPanel.java** - Panel de controles con diseño profesional
✅ **GeneticOperatorsWindow.java** - Ventana independiente para los operadores

## Integración en FlappyBirdGameUI.java

Para integrar la ventana de operadores genéticos en la interfaz principal, sigue estos pasos:

### 1. Añadir Variables de Instancia (línea ~70)

```java
// Ventana de configuración de operadores genéticos  
private GeneticOperatorsWindow geneticOperatorsWindow;
```

### 2. Inicializar en el Constructor (línea ~75)

```java
public FlappyBirdGameUI() {
    networkWindow = null;
    geneticOperatorsWindow = null;  // AÑADIR ESTA LÍNEA
}
```

### 3. Inicializar la Ventana en initialize() (línea ~95, después de networkWindow)

```java
// Inicializar ventana de red neuronal
networkWindow = new NeuralNetworkWindow(600, 400);

// Inicializar ventana de operadores genéticos
geneticOperatorsWindow = new GeneticOperatorsWindow(population);  // AÑADIR
```

### 4. Crear Botón de Operadores Genéticos (línea ~235, antes de showNetworkButton)

```java
// Botón para configurar operadores genéticos
Button geneticOperatorsButton = new Button("⚙ Operadores Genéticos");
geneticOperatorsButton.setStyle(
    "-fx-background-color: #3498db; " +
    "-fx-text-fill: white; " +
    "-fx-font-weight: bold; " +
    "-fx-padding: 8px 15px; " +
    "-fx-cursor: hand;"
);
geneticOperatorsButton.setOnAction(e -> {
    if (geneticOperatorsWindow.isShowing()) {
        geneticOperatorsWindow.close();
        geneticOperatorsButton.setText("⚙ Operadores Genéticos");
    } else {
        geneticOperatorsWindow.show();
        geneticOperatorsButton.setText("⚙ Ocultar Operadores");
    }
});
```

### 5. Añadir Botón al Panel (línea ~270)

```java
infoPanel.getChildren().addAll(
    generationLabel,
    aliveLabel,
    scoreLabel,
    bestFitnessLabel,
    speedLabel,
    pauseButton,
    speedSliderLabel,
    speedSlider,
    nextGenButton,
    resetButton,
    fastSimulationButton,
    showBestButton,
    loopCheckbox,
    maxGenLabel,
    maxGenSlider,
    saveHistoryButton,
    loadHistoryButton,
    showAllAgentsCheckbox,
    autoRestartCheckbox,
    geneticOperatorsButton,  // AÑADIR ESTA LÍNEA
    showNetworkButton
);
```

### 6. Cerrar Ventana al Cerrar Aplicación (línea ~285)

```java
primaryStage.setOnCloseRequest(e -> {
    if (networkWindow.isShowing()) {
        networkWindow.close();
    }
    // AÑADIR ESTAS LÍNEAS:
    if (geneticOperatorsWindow != null && geneticOperatorsWindow.isShowing()) {
        geneticOperatorsWindow.close();
    }
    stopGameLoop();
});
```

### 7. Actualizar Población en resetSimulation() (línea ~695)

```java
private void resetSimulation() {
    population = new Population(POPULATION_SIZE);
    game.reset();
    currentGeneration = 1;
    gamePaused = false;
    
    // AÑADIR ESTAS LÍNEAS:
    // Actualizar ventana de operadores genéticos si está abierta
    if (geneticOperatorsWindow != null && geneticOperatorsWindow.isShowing()) {
        geneticOperatorsWindow.updatePopulation(population);
    }
}
```

## Uso de la Interfaz

1. **Ejecuta la aplicación** de FlappyBird NEAT

2. **Haz clic en "⚙ Operadores Genéticos"** en el panel derecho

3. **Se abrirá una ventana independiente** con controles profesionales para:
   - 🎯 **Selección**: 7 métodos disponibles con descripciones
   - 📈 **Escalado**: 4 opciones (Ninguno, Lineal, Sigma, Boltzmann)
   - 🧬 **Mutación**: 3 estrategias (Gaussiana, Uniforme, No Uniforme)
   - 👑 **Elitismo**: Slider configurable

4. **Configura los operadores** según tus preferencias

5. **Haz clic en "✓ Aplicar Cambios"** para aplicar la configuración

6. **Los cambios se aplicarán** a partir de la siguiente generación

## Características de la Interfaz

- ✨ **Diseño Moderno**: Estilo profesional con colores agradables
- 📋 **TitledPane Expandibles**: Organización clara con acordeón
- 💡 **Descripciones Contextuales**: Cada operador tiene una explicación
- 🎛️ **Sliders para Parámetros**: Control preciso de valores
- ✓ **Confirmación Visual**: Feedback al aplicar cambios
- 🔄 **Botón de Reset**: Volver a valores por defecto rápidamente

## Preview de la Interfaz

```
┌─────────────────────────────────┐
│  ⚙ Operadores Genéticos         │
├─────────────────────────────────┤
│  📊 Configuración Actual        │
│  Selección: Ruleta              │
│  Escalado: Ninguno              │
│  Mutación: Gaussiana            │
├─────────────────────────────────┤
│  🎯 Selección                   │▼│
│  └─ [ComboBox con 7 opciones]   │
│     [Descripción del método]    │
├─────────────────────────────────┤
│  📈 Escalado de Fitness         │▶│
├─────────────────────────────────┤
│  🧬 Mutación                    │▶│
├─────────────────────────────────┤
│  👑 Elitismo                    │▶│
├─────────────────────────────────┤
│  [✓ Aplicar Cambios]            │
│  [↻ Valores Por Defecto]        │
└─────────────────────────────────┘
```

## Compilación

```bash
mvn clean compile
mvn javafx:run
```

Si hay errores de compilación después de integrar, verifica que todas las importaciones estén correctas.

## Notas Importantes

- Los cambios se aplican a la población actual
- Al reiniciar la simulación, se puede actualizar la ventana con la nueva población
- La ventana es independiente y puede moverse libremente
- Los valores se pueden modificar en cualquier momento, incluso durante la simulación
