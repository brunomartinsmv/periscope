package br.ufmt.periscope.report;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ordered label→value series produced by report aggregations (EJB),
 * consumed by the JSF chart builders and the REST report API.
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

    public boolean isEmpty() {
        return data.isEmpty();
    }
}
