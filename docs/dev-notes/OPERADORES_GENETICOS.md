# Operadores Genéticos Implementados en FlappyBird NEAT

Este documento describe los nuevos operadores genéticos implementados basados en el proyecto G10P1.

## Resumen

Se han añadido múltiples operadores de **selección**, **escalado de fitness** y **mutación** para proporcionar mayor control y variabilidad al algoritmo genético NEAT.

## Operadores de Selección

### 1. Selección por Ruleta (SeleccionRuleta)
- **Descripción**: Selección proporcional al fitness
- **Uso**: `population.setSeleccionStrategy("ruleta")`
- **Características**: Cada individuo tiene probabilidad proporcional a su fitness
- **Ventajas**: Simple, mantiene diversidad
- **Desventajas**: Puede converger lentamente si hay gran diferencia entre fitness

### 2. Torneo Determinista (SeleccionTorneoDeterministico)
- **Descripción**: Elige el mejor de 3 individuos aleatorios
- **Uso**: `population.setSeleccionStrategy("torneo deterministico")`
- **Características**: Alta presión selectiva
- **Ventajas**: Rápida convergencia, simple
- **Desventajas**: Puede perder diversidad rápidamente

### 3. Torneo Probabilístico (SeleccionTorneoProbabilistico)
- **Descripción**: Torneo con probabilidad p de elegir el mejor
- **Uso**: `population.setSeleccionStrategy("torneo probabilistico")`
- **Parámetros**: p = 0.6 (por defecto)
- **Ventajas**: Balance entre exploración y explotación
- **Desventajas**: Requiere ajuste del parámetro p

### 4. Ranking (SeleccionRanking)
- **Descripción**: Asigna probabilidades según posición en ranking
- **Uso**: `population.setSeleccionStrategy("ranking")`
- **Características**: Menos sensible a diferencias extremas de fitness
- **Ventajas**: Mantiene presión selectiva constante
- **Desventajas**: Ignora magnitud de diferencias de fitness

### 5. Truncamiento (SeleccionTruncamiento)
- **Descripción**: Solo los mejores X% pueden reproducirse
- **Uso**: `population.setSeleccionStrategy("truncamiento")`
- **Parámetros**: trunc = 0.6 (60% mejores)
- **Ventajas**: Muy elitista, rápida convergencia
- **Desventajas**: Pierde diversidad muy rápido

### 6. Estocástico Universal (SeleccionEstocasticoUniversal)
- **Descripción**: Ruleta mejorada con múltiples punteros equidistantes
- **Uso**: `population.setSeleccionStrategy("estocastico universal")`
- **Características**: Menor varianza que ruleta
- **Ventajas**: Más justa que ruleta, mantiene diversidad
- **Desventajas**: Ligeramente más compleja

### 7. Restos (SeleccionRestos)
- **Descripción**: Asigna copias según fitness esperado, completa con torneo
- **Uso**: `population.setSeleccionStrategy("restos")`
- **Características**: Híbrido entre determinista y estocástico
- **Ventajas**: Garantiza representación mínima a buenos individuos
- **Desventajas**: Compleja de implementar

## Operadores de Escalado de Fitness

### 1. Escalado Lineal (EscaladoLineal)
- **Descripción**: f' = a*f + b
- **Uso**: `population.setEscaladoStrategy("lineal")`
- **Parámetros**: a=1.5, b=0.5
- **Ventajas**: Simple, evita fitness negativos
- **Cuándo usar**: Cuando hay pocas diferencias entre fitness

### 2. Escalado Sigma (EscaladoSigma)
- **Descripción**: f' = max(0, f - media + 2*sigma)
- **Uso**: `population.setEscaladoStrategy("sigma")`
- **Características**: Mantiene presión selectiva constante
- **Ventajas**: Robusto ante convergencia prematura
- **Cuándo usar**: Para mantener diversidad en fases tardías

### 3. Escalado Boltzmann (EscaladoBoltzmann)
- **Descripción**: f' = exp(f/T) / media(exp(f/T)), T decrece
- **Uso**: `population.setEscaladoStrategy("boltzmann")`
- **Parámetros**: T_inicial=100.0, factor=0.99
- **Características**: Temperatura decreciente
- **Ventajas**: Exploración inicial, explotación final
- **Cuándo usar**: Para algoritmos largos con múltiples fases

## Operadores de Mutación

### 1. Mutación Gaussiana (MutacionGaussiana)
- **Descripción**: Añade ruido gaussiano a cada peso
- **Uso**: `population.setMutacionStrategy("gaussiana")`
- **Características**: Mutaciones pequeñas y frecuentes
- **Ventajas**: Refinamiento fino, por defecto en NEAT
- **Cuándo usar**: Cuando se busca ajuste fino

### 2. Mutación Uniforme (MutacionUniforme)
- **Descripción**: Reemplaza peso por valor aleatorio
- **Uso**: `population.setMutacionStrategy("uniforme")`
- **Características**: Mutaciones grandes y disruptivas
- **Ventajas**: Mayor exploración
- **Cuándo usar**: Al inicio o cuando hay estancamiento

### 3. Mutación No Uniforme (MutacionNoUniforme)
- **Descripción**: Magnitud decrece con generaciones
- **Uso**: `population.setMutacionStrategy("no uniforme")`
- **Características**: Exploración inicial → Explotación final
- **Ventajas**: Adaptativo al progreso del algoritmo
- **Cuándo usar**: Para algoritmos con número fijo de generaciones

## Ejemplos de Uso

### Configuración Básica
```java
Population population = new Population(100);

// Usar torneo determinista
population.setSeleccionStrategy("torneo deterministico");

// Sin escalado (por defecto)
population.setEscaladoStrategy("ninguno");

// Mutación gaussiana (por defecto)
population.setMutacionStrategy("gaussiana");
```

### Configuración para Exploración
```java
// Mayor diversidad, menos presión selectiva
population.setSeleccionStrategy("estocastico universal");
population.setEscaladoStrategy("sigma");
population.setMutacionStrategy("uniforme");
```

### Configuración para Explotación
```java
// Convergencia rápida, elitista
population.setSeleccionStrategy("truncamiento");
population.setEscaladoStrategy("lineal");
population.setMutacionStrategy("gaussiana");
```

### Configuración Balanceada
```java
// Balance entre exploración y explotación
population.setSeleccionStrategy("ranking");
population.setEscaladoStrategy("boltzmann");
population.setMutacionStrategy("no uniforme");
```

## Recomendaciones

1. **Para convergencia rápida**: Truncamiento + Lineal + Gaussiana
2. **Para mantener diversidad**: Estocástico Universal + Sigma + Uniforme
3. **Para ejecuciones largas**: Ranking + Boltzmann + No Uniforme
4. **Por defecto (recomendado)**: Torneo Determinista + Sin Escalado + Gaussiana

## Integración en el Código

Los operadores se integran automáticamente en `Population.naturalSelection()`:

1. Se aplica **escalado de fitness** (si está configurado)
2. Se usa el **operador de selección** para elegir padres
3. Se aplica **cruce** entre padres
4. Se aplica **mutación** a los hijos

El elitismo se mantiene independientemente de los operadores configurados.

## Notas Técnicas

- Los operadores están basados en el proyecto G10P1 que trabaja con valores continuos
- G10P1 fue elegido por su similitud con FlappyBird NEAT (ambos usan representaciones continuas)
- G10P2 (permutaciones) y G10P3 (árboles) no eran compatibles
- El escalado de fitness se aplica temporalmente solo para la selección
- Los fitness originales se restauran después de la selección
