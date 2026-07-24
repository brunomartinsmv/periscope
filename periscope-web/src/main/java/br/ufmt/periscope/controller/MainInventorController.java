package br.ufmt.periscope.controller;

import java.util.ArrayList;
import java.util.Collections;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import br.ufmt.periscope.report.ChartSeries;
import br.ufmt.periscope.report.MainInventorReport;
import br.ufmt.periscope.report.Pair;
import java.util.List;

/**
 * Controller responsável por operações de visualização relacionadas aos inventores.
 */
@Named
@ViewScoped
public class MainInventorController extends GenericController {

    private @Inject
    MainInventorReport report;

    @Override
    public void refreshChart() {
        ChartSeries series = report.InventorDateSeries(getCurrentProject(), getLimit(), getFiltro());
        applyBarChart(series, series.getLabel(), true);

        setPairs(new ArrayList<Pair>());
        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            getPairs().add(new Pair(key, value));
        }

        Collections.reverse(getPairs());
    }

    /**
     * Lista de inventores dado um filtro.
     */
    public List<String> getInventors(String query) {
        return report.getRepo().getInventors(getCurrentProject(), query);
    }
}
