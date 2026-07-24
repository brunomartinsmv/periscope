package br.ufmt.periscope.compat.chart;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compat shim for PrimeFaces 3.x {@code ChartSeries} (removed in PF Charts.js era).
 * Full chart redesign deferred to Fase 5.
 */
public class ChartSeries implements Serializable {

    private static final long serialVersionUID = 1L;

    private String label;
    private final Map<Object, Number> data = new LinkedHashMap<Object, Number>();

    public ChartSeries() {
    }

    public ChartSeries(String label) {
        this.label = label;
    }

    public void set(Object key, Number value) {
        data.put(key, value);
    }

    public Map<Object, Number> getData() {
        return data;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
