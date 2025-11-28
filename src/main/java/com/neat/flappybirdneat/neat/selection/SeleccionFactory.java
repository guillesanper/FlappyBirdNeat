package com.neat.flappybirdneat.neat.selection;

public class SeleccionFactory {

    public static Seleccion getMetodoSeleccion(String tipo) {
        String t = tipo.toLowerCase();
        if (t.equals("ruleta")) {
            return new SeleccionRuleta();
        } else if (t.equals("torneo deterministico") || t.equals("torneo_deterministico")) {
            return new SeleccionTorneoDeterministico();
        } else if (t.equals("torneo probabilistico") || t.equals("torneo_probabilistico")) {
            return new SeleccionTorneoProbabilistico();
        } else if (t.equals("ranking")) {
            return new SeleccionRanking();
        } else if (t.equals("truncamiento")) {
            return new SeleccionTruncamiento();
        } else if (t.equals("estocastico universal") || t.equals("estocastico_universal") || t.equals("sus")) {
            return new SeleccionEstocasticoUniversal();
        } else if (t.equals("restos")) {
            return new SeleccionRestos();
        } else {
            throw new IllegalArgumentException("Tipo de selección desconocido: " + tipo);
        }
    }

    public static Seleccion getMetodoSeleccion(String tipo, double param) {
        String t = tipo.toLowerCase();
        if (t.equals("torneo probabilistico") || t.equals("torneo_probabilistico")) {
            return new SeleccionTorneoProbabilistico(param);
        } else if (t.equals("ranking")) {
            return new SeleccionRanking(param);
        } else if (t.equals("truncamiento")) {
            return new SeleccionTruncamiento(param);
        } else {
            return getMetodoSeleccion(tipo);
        }
    }
}
