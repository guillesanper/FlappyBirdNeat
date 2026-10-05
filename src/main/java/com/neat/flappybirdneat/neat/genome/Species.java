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

    public Species(Genome representative) {
        this.representative = representative;
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
        return members.stream().max(Comparator.comparingDouble(FlappyBirdAgent::getFitness)).orElse(null);
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
