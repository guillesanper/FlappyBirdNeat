package com.neat.flappybirdneat.neat.selection;

/**
 * Implementación concreta de {@link SelectionFactory}. Solo accesible a través de {@link SelectionFactory#getInstance()}.
 */
class SelectionFactoryImpl extends SelectionFactory {

    @Override
    public SelectionStrategy getSelectionStrategy(String type, double... params) {
        String t = type.toLowerCase();
        if (t.equals("roulette") || t.equals("roulette")) {
            return new RouletteSelection();
        } else if (t.equals("torneo deterministico")
                || t.equals("torneo_deterministico")
                || t.equals("deterministic_tournament")) {
            return new DeterministicTournamentSelection();
        } else if (t.equals("torneo probabilistico")
                || t.equals("torneo_probabilistico")
                || t.equals("probabilistic_tournament")) {
            return params.length >= 1
                    ? new ProbabilisticTournamentSelection(params[0])
                    : new ProbabilisticTournamentSelection();
        } else if (t.equals("ranking")) {
            return params.length >= 1 ? new RankingSelection(params[0]) : new RankingSelection();
        } else if (t.equals("truncamiento") || t.equals("truncation")) {
            return params.length >= 1 ? new TruncationSelection(params[0]) : new TruncationSelection();
        } else if (t.equals("estocastico universal")
                || t.equals("estocastico_universal")
                || t.equals("stochastic_universal")
                || t.equals("sus")) {
            return new StochasticUniversalSelection();
        } else if (t.equals("restos") || t.equals("remainder")) {
            return new RemainderSelection();
        } else {
            throw new IllegalArgumentException("Tipo de selección desconocido: " + type);
        }
    }
}
