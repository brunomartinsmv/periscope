package br.ufmt.periscope.controller;

import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;

import br.ufmt.periscope.report.ChartSeries;
import br.ufmt.periscope.report.MainApplicantReport;
import br.ufmt.periscope.report.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Controller responsável por operações de visualização relacionadas aos depositantes.
 */
@Named
@ViewScoped
public class MainApplicantController extends GenericController {

    private @Inject
    MainApplicantReport report;

    @Override
    public void refreshChart() {
        ChartSeries series = report.mainApplicantSeries(getCurrentProject(), getLimit(), getFiltro());
        applyBarChart(series, series.getLabel(), true);

        setPairs(new ArrayList<Pair>());
        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            getPairs().add(new Pair(key, value));
        }

        Collections.reverse(getPairs());
    }

    /**
     * Lista de depositantes dado um filtro.
     */
    public List<String> getApplicants(String query) {
        return report.getRepo().getApplicants(getCurrentProject(), query);
    }
}
