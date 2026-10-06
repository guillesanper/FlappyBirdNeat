package com.neat.flappybirdneat.neat;

import static org.junit.jupiter.api.Assertions.*;

import com.neat.flappybirdneat.neat.crossover.*;
import com.neat.flappybirdneat.neat.mutation.*;
import com.neat.flappybirdneat.neat.scaling.*;
import com.neat.flappybirdneat.neat.selection.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Every operator factory accepts both its Spanish and its English keys. */
class OperatorFactoriesTest {

    @ParameterizedTest
    @CsvSource({
        "ruleta, RouletteSelection",
        "roulette, RouletteSelection",
        "torneo_deterministico, DeterministicTournamentSelection",
        "deterministic_tournament, DeterministicTournamentSelection",
        "torneo_probabilistico, ProbabilisticTournamentSelection",
        "probabilistic_tournament, ProbabilisticTournamentSelection",
        "ranking, RankingSelection",
        "truncamiento, TruncationSelection",
        "truncation, TruncationSelection",
        "estocastico_universal, StochasticUniversalSelection",
        "stochastic_universal, StochasticUniversalSelection",
        "restos, RemainderSelection",
        "remainder, RemainderSelection"
    })
    void selectionFactoryAcceptsSpanishAndEnglishKeys(String key, String expectedClass) {
        assertEquals(
                expectedClass,
                SelectionFactory.getInstance()
                        .getSelectionStrategy(key)
                        .getClass()
                        .getSimpleName());
    }

    @ParameterizedTest
    @CsvSource({
        "uniforme, UniformCrossover",
        "uniform, UniformCrossover",
        "punto_unico, SinglePointCrossover",
        "single_point, SinglePointCrossover",
        "aritmetico, ArithmeticCrossover",
        "arithmetic, ArithmeticCrossover"
    })
    void crossoverFactoryAcceptsSpanishAndEnglishKeys(String key, String expectedClass) {
        assertEquals(
                expectedClass,
                CrossoverFactory.getInstance()
                        .getCrossoverStrategy(key)
                        .getClass()
                        .getSimpleName());
    }

    @ParameterizedTest
    @CsvSource({
        "gaussiana, GaussianMutation",
        "gaussian, GaussianMutation",
        "uniforme, UniformMutation",
        "uniform, UniformMutation",
        "no_uniforme, NonUniformMutation",
        "nonuniform, NonUniformMutation"
    })
    void mutationFactoryAcceptsSpanishAndEnglishKeys(String key, String expectedClass) {
        assertEquals(
                expectedClass,
                MutationFactory.getInstance()
                        .getMutationStrategy(key)
                        .getClass()
                        .getSimpleName());
    }

    @ParameterizedTest
    @CsvSource({"lineal, LinearScaling", "linear, LinearScaling", "sigma, SigmaScaling", "boltzmann, BoltzmannScaling"})
    void scalingFactoryAcceptsSpanishAndEnglishKeys(String key, String expectedClass) {
        assertEquals(
                expectedClass,
                ScalingFactory.getInstance().getScalingStrategy(key).getClass().getSimpleName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ninguno", "none"})
    void scalingFactoryMapsNoneToNull(String key) {
        assertNull(ScalingFactory.getInstance().getScalingStrategy(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "rulette", "tournament"})
    void unknownSelectionKeysAreRejected(String key) {
        assertThrows(
                IllegalArgumentException.class,
                () -> SelectionFactory.getInstance().getSelectionStrategy(key));
    }
}
