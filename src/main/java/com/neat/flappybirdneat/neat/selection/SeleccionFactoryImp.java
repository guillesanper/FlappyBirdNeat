package com.neat.flappybirdneat.neat.selection;

/**
 * Implementación concreta de {@link SeleccionFactory}. Solo accesible a través de {@link SeleccionFactory#getInstance()}.
 */
class SeleccionFactoryImp extends SeleccionFactory {

    @Override
    public Seleccion getSeleccionStrategy(String tipo, double... params) {
        String t = tipo.toLowerCase();
        if (t.equals("ruleta")) {
            return new SeleccionRuleta();
        } else if (t.equals("torneo deterministico") || t.equals("torneo_deterministico")) {
            return new SeleccionTorneoDeterministico();
        } else if (t.equals("torneo probabilistico") || t.equals("torneo_probabilistico")) {
            return params.length >= 1 ? new SeleccionTorneoProbabilistico(params[0]) : new SeleccionTorneoProbabilistico();
        } else if (t.equals("ranking")) {
            return params.length >= 1 ? new SeleccionRanking(params[0]) : new SeleccionRanking();
        } else if (t.equals("truncamiento")) {
            return params.length >= 1 ? new SeleccionTruncamiento(params[0]) : new SeleccionTruncamiento();
        } else if (t.equals("estocastico universal") || t.equals("estocastico_universal") || t.equals("sus")) {
            return new SeleccionEstocasticoUniversal();
        } else if (t.equals("restos")) {
            return new SeleccionRestos();
        } else {
            throw new IllegalArgumentException("Tipo de selección desconocido: " + tipo);
        }
    }
}
