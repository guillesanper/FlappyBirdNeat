package com.neat.flappybirdneat.neat.selection;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Invariantes comunes a todas las estrategias de selección: deben devolver
 * exactamente el número de individuos pedido, con índices válidos, y ser
 * deterministas cuando se les inyecta la misma semilla.
 */
class SelectionOperatorsTest {

    /** Generador que se pasa a las estrategias en cada llamada (JUnit crea una instancia por test). */
    private Random random = new Random(0);

    private static final int POPULATION_SIZE = 10;

    static Stream<SelectionStrategy> strategies() {
        return Stream.of(
                new RouletteSelection(),
                new RankingSelection(),
                new RemainderSelection(),
                new StochasticUniversalSelection(),
                new DeterministicTournamentSelection(),
                new ProbabilisticTournamentSelection(),
                new TruncationSelection());
    }

    private Selectable[] buildSelectables(long seed) {
        Random random = new Random(seed);
        Selectable[] list = new Selectable[POPULATION_SIZE];
        double totalFitness = 0;
        double[] fitness = new double[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            fitness[i] = random.nextDouble() * 100;
            totalFitness += fitness[i];
        }
        double accProb = 0;
        for (int i = 0; i < POPULATION_SIZE; i++) {
            double prob = fitness[i] / totalFitness;
            list[i] = new Selectable(i, fitness[i]);
            list[i].setProb(prob);
            list[i].setAccProb(accProb);
            accProb += prob;
        }
        return list;
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void returnsRequestedNumberOfSelections(SelectionStrategy strategy) {
        random = new Random(42);
        Selectable[] list = buildSelectables(1);

        int[] selection = strategy.select(list, POPULATION_SIZE, random);

        assertEquals(POPULATION_SIZE, selection.length);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void allSelectedIndicesAreWithinPopulationBounds(SelectionStrategy strategy) {
        random = new Random(7);
        Selectable[] list = buildSelectables(2);

        int[] selection = strategy.select(list, POPULATION_SIZE, random);

        for (int index : selection) {
            assertTrue(index >= 0 && index < POPULATION_SIZE, "Índice fuera de rango: " + index);
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void sameSeedProducesIdenticalSelection(SelectionStrategy strategy) {
        random = new Random(123);
        int[] first = strategy.select(buildSelectables(3), POPULATION_SIZE, random);

        random = new Random(123);
        int[] second = strategy.select(buildSelectables(3), POPULATION_SIZE, random);

        assertArrayEquals(first, second);
    }

    @Test
    void torneoDeterministicoStronglyFavoursTheFitterIndividual() {
        // Con solo dos individuos, cada trío de 3 sorteos solo pierde ante el más apto si las
        // tres tiradas caen en el otro individuo (probabilidad 1/8); con una semilla fija y
        // suficientes sorteos, debe ganar la gran mayoría de las veces.
        // select exige list.length == tamPoblacion, así que repetimos la selección
        // sobre una población de 2 individuos en vez de inflar tamPoblacion.
        Selectable[] list = {new Selectable(0, 1.0), new Selectable(1, 100.0)};
        DeterministicTournamentSelection strategy = new DeterministicTournamentSelection();
        random = new Random(99);

        int trials = 300;
        long timesFitterWon = 0;
        long totalSelections = 0;
        for (int t = 0; t < trials; t++) {
            int[] selection = strategy.select(list, 2, random);
            totalSelections += selection.length;
            timesFitterWon +=
                    java.util.Arrays.stream(selection).filter(i -> i == 1).count();
        }

        assertTrue(
                timesFitterWon > totalSelections * 0.8,
                "El individuo más apto debería ganar la gran mayoría de los torneos: " + timesFitterWon + "/"
                        + totalSelections);
    }

    static Stream<SelectionStrategy> wholePopulationStrategies() {
        return Stream.of(
                new DeterministicTournamentSelection(),
                new ProbabilisticTournamentSelection(),
                new RankingSelection(),
                new RouletteSelection(),
                new StochasticUniversalSelection(),
                new RemainderSelection());
    }

    /**
     * Regresión: con elitismo, Population pide menos padres (count) que individuos tiene la
     * lista, ordenada de mejor a peor. Los torneos sorteaban en [0, count) y el ranking solo
     * repartía probabilidad entre los count primeros, así que los peores nunca podían ser padres.
     */
    @ParameterizedTest
    @MethodSource("wholePopulationStrategies")
    void selectsFromTheWholePopulationWhenFewerParentsThanIndividualsAreRequested(SelectionStrategy strategy) {
        int size = 50;
        int count = 45; // población 50 con un 10 % de élite
        random = new Random(11);
        int fromTail = 0;
        for (int t = 0; t < 200; t++) {
            Selectable[] list = new Selectable[size];
            double total = size * (size + 1) / 2.0;
            double accProb = 0;
            for (int i = 0; i < size; i++) {
                double fitness = size - i;
                list[i] = new Selectable(i, fitness);
                list[i].setProb(fitness / total);
                list[i].setAccProb(accProb);
                accProb += fitness / total;
            }
            for (int index : strategy.select(list, count, random)) {
                if (index >= count) fromTail++;
            }
        }

        assertTrue(fromTail > 0, "Los " + (size - count) + " peores individuos nunca fueron elegidos como padres");
    }

    private Selectable[] withProbabilities(double... probs) {
        Selectable[] list = new Selectable[probs.length];
        double accProb = 0;
        for (int i = 0; i < probs.length; i++) {
            list[i] = new Selectable(i, probs[i] * 100);
            list[i].setProb(probs[i]);
            list[i].setAccProb(accProb);
            accProb += probs[i];
        }
        return list;
    }

    /**
     * Regresión: SUS comparaba cada puntero con el inicio del intervalo (accProb) en vez de con su
     * final, así que elegía al individuo siguiente al que contiene el puntero. Con dos individuos
     * al 50 % y dos punteros separados 0.5, cada uno debe salir exactamente una vez.
     */
    @Test
    void stochasticUniversalPicksTheIndividualWhoseIntervalContainsEachPointer() {
        StochasticUniversalSelection strategy = new StochasticUniversalSelection();
        for (long seed = 0; seed < 50; seed++) {
            int[] selection = strategy.select(withProbabilities(0.5, 0.5), 2, new Random(seed));
            java.util.Arrays.sort(selection);
            assertArrayEquals(new int[] {0, 1}, selection, "seed " + seed);
        }
    }

    /**
     * Regresión: la selección por restos redondeaba (en vez de truncar) el número esperado de
     * copias y descartaba por completo a un individuo si sus copias no cabían, rellenando con un
     * torneo. Con probabilidades 0.7/0.3 y 10 plazas, las copias esperadas son exactamente 7 y 3.
     */
    @Test
    void remainderGivesEachIndividualTheIntegerPartOfItsExpectedCopies() {
        int[] selection = new RemainderSelection().select(withProbabilities(0.7, 0.3), 10, new Random(3));

        long zeros = java.util.Arrays.stream(selection).filter(i -> i == 0).count();
        assertEquals(7, zeros);
        assertEquals(3, selection.length - zeros);
    }

    @Test
    void truncamientoOnlySelectsFromTopFraction() {
        Selectable[] list = new Selectable[10];
        for (int i = 0; i < 10; i++) {
            list[i] = new Selectable(i, i);
        }
        TruncationSelection strategy = new TruncationSelection(0.3);
        random = new Random(5);

        int[] selection = strategy.select(list, 10, random);

        for (int index : selection) {
            assertTrue(index >= 7, "Se seleccionó un individuo fuera del top 30%: index=" + index);
        }
    }
}
