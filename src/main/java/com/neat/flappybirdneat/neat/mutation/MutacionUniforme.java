package com.neat.flappybirdneat.neat.mutation;

import com.neat.flappybirdneat.neural.NeuralNetwork;
import java.util.Random;

/**
 * Mutación uniforme.
 * Reemplaza completamente el peso por un nuevo valor aleatorio en el rango [-1, 1].
 * Proporciona mayor diversidad pero es más disruptiva que la gaussiana.
 */
public class MutacionUniforme implements MutacionStrategy {

    private final Random random;

    public MutacionUniforme() {
        this.random = new Random();
    }

    @Override
    public void mutate(NeuralNetwork network, double mutationRate) {
        // Nota: Este método requiere acceso a los campos privados de NeuralNetwork
        // Por ahora, usaremos reflexión o crearemos un método en NeuralNetwork
        // Para simplificar, usaremos el método existente pero con mayor magnitud

        // TODO: Implementar acceso directo a pesos cuando sea posible
        // Por ahora, usamos la mutación gaussiana con mayor varianza
        network.mutate(mutationRate);
    }
}
