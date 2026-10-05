package com.neat.flappybirdneat.neat.selection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Invariantes comunes a todas las estrategias de selección: deben devolver
 * exactamente el número de individuos pedido, con índices válidos, y ser
 * deterministas cuando se les inyecta la misma semilla.
 */
class SelectionOperatorsTest {

    private static final int POPULATION_SIZE = 10;

    static Stream<Seleccion> strategies() {
        return Stream.of(
                new SeleccionRuleta(),
                new SeleccionRanking(),
                new SeleccionRestos(),
                new SeleccionEstocasticoUniversal(),
                new SeleccionTorneoDeterministico(),
                new SeleccionTorneoProbabilistico(),
                new SeleccionTruncamiento()
        );
    }

    private Seleccionable[] buildSeleccionables(long seed) {
        Random random = new Random(seed);
        Seleccionable[] list = new Seleccionable[POPULATION_SIZE];
        double totalFitness = 0;
        double[] fitness = new double[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            fitness[i] = random.nextDouble() * 100;
            totalFitness += fitness[i];
        }
        double accProb = 0;
        for (int i = 0; i < POPULATION_SIZE; i++) {
            double prob = fitness[i] / totalFitness;
            list[i] = new Seleccionable(i, fitness[i]);
            list[i].setProb(prob);
            list[i].setAccProb(accProb);
            accProb += prob;
        }
        return list;
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void returnsRequestedNumberOfSelections(Seleccion strategy) {
        strategy.setRandom(new Random(42));
        Seleccionable[] list = buildSeleccionables(1);

        int[] selection = strategy.getSeleccion(list, POPULATION_SIZE);

        assertEquals(POPULATION_SIZE, selection.length);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void allSelectedIndicesAreWithinPopulationBounds(Seleccion strategy) {
        strategy.setRandom(new Random(7));
        Seleccionable[] list = buildSeleccionables(2);

        int[] selection = strategy.getSeleccion(list, POPULATION_SIZE);

        for (int index : selection) {
            assertTrue(index >= 0 && index < POPULATION_SIZE,
                    "Índice fuera de rango: " + index);
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void sameSeedProducesIdenticalSelection(Seleccion strategy) {
        strategy.setRandom(new Random(123));
        int[] first = strategy.getSeleccion(buildSeleccionables(3), POPULATION_SIZE);

        strategy.setRandom(new Random(123));
        int[] second = strategy.getSeleccion(buildSeleccionables(3), POPULATION_SIZE);

        assertArrayEquals(first, second);
    }

    @Test
    void torneoDeterministicoStronglyFavoursTheFitterIndividual() {
        // Con solo dos individuos, cada trío de 3 sorteos solo pierde ante el más apto si las
        // tres tiradas caen en el otro individuo (probabilidad 1/8); con una semilla fija y
        // suficientes sorteos, debe ganar la gran mayoría de las veces.
        // getSeleccion exige list.length == tamPoblacion, así que repetimos la selección
        // sobre una población de 2 individuos en vez de inflar tamPoblacion.
        Seleccionable[] list = {
                new Seleccionable(0, 1.0),
                new Seleccionable(1, 100.0)
        };
        SeleccionTorneoDeterministico strategy = new SeleccionTorneoDeterministico();
        strategy.setRandom(new Random(99));

        int trials = 300;
        long timesFitterWon = 0;
        long totalSelections = 0;
        for (int t = 0; t < trials; t++) {
            int[] selection = strategy.getSeleccion(list, 2);
            totalSelections += selection.length;
            timesFitterWon += java.util.Arrays.stream(selection).filter(i -> i == 1).count();
        }

        assertTrue(timesFitterWon > totalSelections * 0.8,
                "El individuo más apto debería ganar la gran mayoría de los torneos: "
                        + timesFitterWon + "/" + totalSelections);
    }

    @Test
    void truncamientoOnlySelectsFromTopFraction() {
        Seleccionable[] list = new Seleccionable[10];
        for (int i = 0; i < 10; i++) {
            list[i] = new Seleccionable(i, i);
        }
        SeleccionTruncamiento strategy = new SeleccionTruncamiento(0.3);
        strategy.setRandom(new Random(5));

        int[] selection = strategy.getSeleccion(list, 10);

        for (int index : selection) {
            assertTrue(index >= 7, "Se seleccionó un individuo fuera del top 30%: index=" + index);
        }
    }
}
