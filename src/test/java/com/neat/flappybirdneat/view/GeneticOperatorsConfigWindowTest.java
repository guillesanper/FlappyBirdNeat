package com.neat.flappybirdneat.view;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.neat.mutation.NonUniformMutation;
import org.junit.jupiter.api.Test;

class GeneticOperatorsConfigWindowTest {

    /**
     * Regresión: la ventana creaba la mutación no uniforme con b como magnitud inicial y beta = 0,
     * así que la magnitud no decrecía nunca. Con b como exponente decrece con las generaciones.
     */
    @Test
    void nonUniformMutationFromTheWindowDecaysWithItsExponent() {
        NonUniformMutation mutation = GeneticOperatorsConfigWindow.nonUniformMutation(100, 2.0);

        assertEquals(2.0, mutation.getBeta());
        assertEquals(100, mutation.getMaxGenerations());
        double initial = mutation.getCurrentMagnitude();
        assertEquals(NonUniformMutation.DEFAULT_INITIAL_MAGNITUDE, initial, 1e-12);

        mutation.update(50);
        assertEquals(initial * 0.25, mutation.getCurrentMagnitude(), 1e-12);
    }
}
