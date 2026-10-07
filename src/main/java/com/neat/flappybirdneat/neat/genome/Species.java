package com.neat.flappybirdneat.neat.genome;

import com.neat.flappybirdneat.neat.FlappyBirdAgent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Especie NEAT: agrupa agentes cuyos genomas son mutuamente compatibles (distancia δ por debajo
 * del umbral configurado) alrededor de un genoma representativo. Sirve para proteger innovaciones
 * topológicas recientes: compiten primero dentro de su especie (fitness sharing) antes de competir
 * con el resto de la población por descendencia.
 */
public class Species {
    private Genome representative;
    private final List<FlappyBirdAgent> members = new ArrayList<>();
    /** Mejor fitness alcanzado por algún miembro en cualquier generación (para el estancamiento). */
    private double bestFitnessEver = Double.NEGATIVE_INFINITY;
    /** Generaciones seguidas sin superar {@link #bestFitnessEver}. */
    private int generationsWithoutImprovement = 0;

    public Species(Genome representative) {
        this.representative = representative;
    }

    /**
     * La misma especie en la generación siguiente: conserva su representante y su historial de
     * estancamiento, sin miembros (se reasignan al especiar).
     */
    public Species nextGeneration() {
        Species next = new Species(representative);
        next.bestFitnessEver = bestFitnessEver;
        next.generationsWithoutImprovement = generationsWithoutImprovement;
        return next;
    }

    /**
     * Actualiza el historial de estancamiento con el fitness de los miembros actuales: si el
     * campeón supera el mejor fitness previo, el contador vuelve a 0; si no, aumenta en 1.
     */
    public void updateStagnation() {
        FlappyBirdAgent champion = champion();
        if (champion == null) return;
        if (champion.getFitness() > bestFitnessEver) {
            bestFitnessEver = champion.getFitness();
            generationsWithoutImprovement = 0;
        } else {
            generationsWithoutImprovement++;
        }
    }

    public double getBestFitnessEver() {
        return bestFitnessEver;
    }

    public int getGenerationsWithoutImprovement() {
        return generationsWithoutImprovement;
    }

    public Genome getRepresentative() {
        return representative;
    }

    public void setRepresentative(Genome representative) {
        this.representative = representative;
    }

    public List<FlappyBirdAgent> getMembers() {
        return members;
    }

    public void addMember(FlappyBirdAgent agent) {
        members.add(agent);
    }

    public int size() {
        return members.size();
    }

    /** Suma del fitness "compartido" (fitness / tamaño de la especie) de todos sus miembros. */
    public double totalAdjustedFitness() {
        if (members.isEmpty()) return 0;
        double total = 0;
        for (FlappyBirdAgent member : members) {
            total += member.getFitness() / members.size();
        }
        return total;
    }

    /** El miembro con mayor fitness de la especie. */
    public FlappyBirdAgent champion() {
        return members.stream()
                .max(Comparator.comparingDouble(FlappyBirdAgent::getFitness))
                .orElse(null);
    }

    /**
     * Miembros ordenados de mejor a peor fitness, recortados a la fracción {@code survivalThreshold}
     * (redondeando siempre hacia arriba y dejando al menos 1), usados como candidatos a reproducirse.
     */
    public List<FlappyBirdAgent> survivors(double survivalThreshold) {
        List<FlappyBirdAgent> sorted = new ArrayList<>(members);
        sorted.sort(Comparator.comparingDouble(FlappyBirdAgent::getFitness).reversed());
        int survivorCount = Math.max(1, (int) Math.ceil(sorted.size() * survivalThreshold));
        return sorted.subList(0, Math.min(survivorCount, sorted.size()));
    }
}
