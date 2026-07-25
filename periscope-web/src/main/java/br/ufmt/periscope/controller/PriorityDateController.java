package br.ufmt.periscope.controller;

import br.ufmt.periscope.report.ChartSeries;
import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.report.PriorityDateReport;
import java.util.ArrayList;
import java.util.Collections;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;

/**
 * Controller responsável por operações de visualização relacionadas à data de prioridade das patentes.
 */
@Named
@ViewScoped
public class PriorityDateController extends GenericController {

    private @Inject
    PriorityDateReport report;

    @Override
    public void refreshChart() {
        ChartSeries series = report.priorityDateSeries(getCurrentProject(), getFiltro());

        setPairs(new ArrayList<Pair>());

        Object keyInvalid = null;
        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            if (value.intValue() == -1) {
                keyInvalid = key;
            }
            getPairs().add(new Pair(key, value));
        }
        if (keyInvalid != null) {
            series.getData().remove(keyInvalid);
        }
        Collections.reverse(getPairs());

        applyLineChart(series, series.getLabel());
    }
}
