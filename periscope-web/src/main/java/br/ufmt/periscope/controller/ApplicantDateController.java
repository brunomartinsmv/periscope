package br.ufmt.periscope.controller;

import java.util.ArrayList;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;

import br.ufmt.periscope.report.ApplicationDateReport;
import br.ufmt.periscope.report.ChartSeries;
import br.ufmt.periscope.report.Pair;
import java.util.Collections;

/**
 * Controller responsável por operações de visualização relacionadas à data de deposito das patentes.
 */
@Named("applicantDateReport")
@ViewScoped
public class ApplicantDateController extends GenericController {

    private @Inject
    ApplicationDateReport report;

    @Override
    public void refreshChart() {
        ChartSeries series = report.applicationDateSeries(getCurrentProject(), getFiltro());

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
