package br.ufmt.periscope.controller;

import br.ufmt.periscope.report.ChartSeries;
import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.report.StateDistribuitionReport;
import java.util.ArrayList;
import java.util.Collections;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;

/**
 * Controller responsável por operações de visualizaçao relacionadas à distribuição estadual das patentes.
 */
@Named
@ViewScoped
public class StateDistribuitionController extends GenericController {

    private @Inject
    StateDistribuitionReport report;
    private String chartStyle = "height:300px";

    @Override
    public void refreshChart() {
        ChartSeries series = report.StateDistribuitionSeries(getCurrentProject());
        applyBarChart(series, series.getLabel(), true);

        setPairs(new ArrayList<Pair>());
        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            getPairs().add(new Pair(key, value));
        }

        int tam = series.getData().size();
        setChartStyle("height:" + Math.max(300, tam * 50) + "px");
        Collections.reverse(getPairs());
    }

    public String getChartStyle() {
        return chartStyle;
    }

    public void setChartStyle(String chartStyle) {
        this.chartStyle = chartStyle;
    }
}
