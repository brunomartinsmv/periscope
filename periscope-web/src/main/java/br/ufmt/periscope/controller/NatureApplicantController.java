package br.ufmt.periscope.controller;

import java.util.ArrayList;
import java.util.Collections;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import br.ufmt.periscope.report.ChartSeries;
import br.ufmt.periscope.report.MainNatureApplicantReport;
import br.ufmt.periscope.report.Pair;

/**
 * Controller responsável por operações de visualização relacionadas à natureza dos depositantes.
 */
@Named
@ViewScoped
public class NatureApplicantController extends GenericController {

    private @Inject
    MainNatureApplicantReport report;

    @Override
    public void refreshChart() {
        ChartSeries series = report.NatureApplicantSeries(getCurrentProject(), getFiltro());
        applyBarChart(series, series.getLabel(), true);

        setPairs(new ArrayList<Pair>());
        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            getPairs().add(new Pair(key, value));
        }

        Collections.reverse(getPairs());
    }
}
