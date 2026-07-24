package br.ufmt.periscope.controller;

import br.ufmt.periscope.report.ChartSeries;
import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.report.RegionDistribuitionReport;
import java.util.ArrayList;
import java.util.Collections;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;

/**
 * Controller responsável por operações de visualização relacionadas à distribuição regional das patentes.
 */
@Named
@ViewScoped
public class RegionDistribuitionController extends GenericController {

    private @Inject
    RegionDistribuitionReport report;

    @Override
    public void refreshChart() {
        ChartSeries series = report.RegionDistribuitionSeries(getCurrentProject());
        applyBarChart(series, series.getLabel(), true);

        setPairs(new ArrayList<Pair>());
        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            getPairs().add(new Pair(key, value));
        }

        Collections.reverse(getPairs());
    }
}
