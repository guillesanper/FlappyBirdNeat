package com.neat.flappybirdneat.config;

import com.neat.flappybirdneat.neat.Population;
import com.neat.flappybirdneat.neat.crossover.*;
import com.neat.flappybirdneat.neat.mutation.*;
import com.neat.flappybirdneat.neat.selection.*;
import com.neat.flappybirdneat.neat.scaling.*;

/**
 * Almacena la configuración de operadores genéticos
 * para persistir entre reinicios de simulación
 */
public class GeneticOperatorsConfig {
    private Seleccion seleccionStrategy;
    private Escalado escaladoStrategy;
    private MutacionStrategy mutacionStrategy;
    private CruceStrategy cruceStrategy;

    public GeneticOperatorsConfig() {
        // Valores por defecto
        seleccionStrategy = new SeleccionRuleta();
        escaladoStrategy = null;
        mutacionStrategy = new MutacionGaussiana();
        cruceStrategy = new CruceUniforme();
    }

    /**
     * Aplica la configuración guardada a una población
     */
    public void applyTo(Population population) {
        population.setSeleccionStrategy(seleccionStrategy);
        population.setEscaladoStrategy(escaladoStrategy);
        population.setMutacionStrategy(mutacionStrategy);
        population.setCruceStrategy(cruceStrategy);
    }

    /**
     * Actualiza la configuración desde una población
     */
    public void updateFrom(Population population) {
        this.seleccionStrategy = population.getSeleccionStrategy();
        this.escaladoStrategy = population.getEscaladoStrategy();
        this.mutacionStrategy = population.getMutacionStrategy();
        this.cruceStrategy = population.getCruceStrategy();
    }

    // Getters
    public Seleccion getSeleccionStrategy() { return seleccionStrategy; }
    public Escalado getEscaladoStrategy() { return escaladoStrategy; }
    public MutacionStrategy getMutacionStrategy() { return mutacionStrategy; }
    public CruceStrategy getCruceStrategy() { return cruceStrategy; }

    // Setters
    public void setSeleccionStrategy(Seleccion s) { this.seleccionStrategy = s; }
    public void setEscaladoStrategy(Escalado e) { this.escaladoStrategy = e; }
    public void setMutacionStrategy(MutacionStrategy m) { this.mutacionStrategy = m; }
    public void setCruceStrategy(CruceStrategy c) { this.cruceStrategy = c; }
}
