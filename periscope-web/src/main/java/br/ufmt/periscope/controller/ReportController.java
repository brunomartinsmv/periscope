package br.ufmt.periscope.controller;

import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;

import br.ufmt.periscope.compat.chart.CartesianChartModel;
import br.ufmt.periscope.compat.chart.ChartSeries;

import br.ufmt.periscope.report.MainApplicantReport;
import br.ufmt.periscope.report.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
@Named("report")
@ViewScoped
public class ReportController extends GenericController {

    private @Inject
    MainApplicantReport report;

    public void refreshChart() {

        setModel(new CartesianChartModel());
        ChartSeries series = report.mainApplicantSeries(getCurrentProject(), getLimit(), getFiltro());
        getModel().addSeries(series);

        setPairs(new ArrayList<Pair>());
        for (Object key : series.getData().keySet()) {
            Integer value = (Integer) series.getData().get(key);
            getPairs().add(new Pair(key, value));
        }

        Collections.reverse(getPairs());
    }

    /**
     *
     * @param query
     * @return
     */
    public List<String> getApplicants(String query) {
        List<String> teste = report.getRepo().getApplicants(getCurrentProject(), query);
        return teste;

    }
}
