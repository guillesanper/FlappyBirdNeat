package com.neat.flappybirdneat.view;

import javafx.scene.Group;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.shape.Polygon;

import java.util.ArrayList;
import java.util.List;

/**
 * Dibuja una banda semitransparente (p. ej. min-max o media ± desviación estándar) superpuesta a
 * un {@link LineChart}. JavaFX no ofrece un tipo de gráfico "banda" nativo, así que se usa la
 * técnica habitual de añadir un {@link Polygon} al grupo interno ".plot-content" del gráfico y
 * recalcular sus puntos con las coordenadas de píxel de los ejes cada vez que cambian los datos,
 * el tamaño del gráfico o el rango de los ejes.
 */
public final class ChartBandUtil {

    private ChartBandUtil() {
    }

    /**
     * Recalcula (y adjunta si aún no lo estaba) el polígono de la banda a partir de dos curvas
     * alineadas por índice de generación: {@code upper} (borde superior) y {@code lower} (borde
     * inferior). No hace nada si el gráfico todavía no está en la escena (el lookup fallará y se
     * reintentará en la siguiente actualización).
     */
    public static void update(LineChart<Number, Number> chart, Polygon band, List<Double> upper, List<Double> lower) {
        Group plotContent = (Group) chart.lookup(".plot-content");
        if (plotContent == null) return;
        if (!plotContent.getChildren().contains(band)) {
            plotContent.getChildren().add(0, band);
        }

        NumberAxis xAxis = (NumberAxis) chart.getXAxis();
        NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        int n = Math.min(upper.size(), lower.size());
        if (n == 0) {
            band.getPoints().clear();
            return;
        }

        List<Double> points = new ArrayList<>(n * 4);
        for (int i = 0; i < n; i++) {
            points.add(xAxis.getDisplayPosition(i));
            points.add(yAxis.getDisplayPosition(upper.get(i)));
        }
        for (int i = n - 1; i >= 0; i--) {
            points.add(xAxis.getDisplayPosition(i));
            points.add(yAxis.getDisplayPosition(lower.get(i)));
        }
        band.getPoints().setAll(points);
    }
}
