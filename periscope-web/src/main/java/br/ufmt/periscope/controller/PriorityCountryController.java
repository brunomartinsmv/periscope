package br.ufmt.periscope.controller;

import br.ufmt.periscope.report.ChartSeries;
import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.report.PriorityCountryReport;
import java.util.ArrayList;
import java.util.Collections;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;

/**
 * Controller responsável por operações de visualização relacionadas ao país de prioridade das patentes.
 */
@Named
@ViewScoped
public class PriorityCountryController extends GenericController {

    private @Inject
    PriorityCountryReport report;

    @Override
    public void refreshChart() {
        ChartSeries series = report.mainPriorityCountrySeries(getCurrentProject(), getLimit(), getFiltro());
        applyBarChart(series, series.getLabel(), true);

        setPairs(new ArrayList<Pair>());
        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            getPairs().add(new Pair(key, value));
        }

        Collections.reverse(getPairs());
    }
}
