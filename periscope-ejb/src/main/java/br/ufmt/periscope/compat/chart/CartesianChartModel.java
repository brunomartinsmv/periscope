package br.ufmt.periscope.compat.chart;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Compat shim for PrimeFaces 3.x {@code CartesianChartModel}.
 */
public class CartesianChartModel implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<ChartSeries> series = new ArrayList<ChartSeries>();

    public void addSeries(ChartSeries chartSeries) {
        series.add(chartSeries);
    }

    public List<ChartSeries> getSeries() {
        return series;
    }

    public void clear() {
        series.clear();
    }
}
