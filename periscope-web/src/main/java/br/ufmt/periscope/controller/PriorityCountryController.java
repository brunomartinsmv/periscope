package br.ufmt.periscope.controller;

import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.report.PriorityCountryReport;
import java.util.ArrayList;
import java.util.Collections;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import br.ufmt.periscope.compat.chart.CartesianChartModel;
import br.ufmt.periscope.compat.chart.ChartSeries;

/**
 * - @Named<BR/>
 * - @ViewScoped<BR/>
 * Classe controller responsável por operações de visualização relacionadas ao país de prioridade das patentes
 */
@Named
@ViewScoped
public class PriorityCountryController extends GenericController {

    private @Inject
    PriorityCountryReport report;

    /**
     * Método responsável pela atualização dos gráficos relacionados ao país de prioridade
     */
    @Override
    public void refreshChart() {
        setModel(new CartesianChartModel());
        ChartSeries series = report.mainPriorityCountrySeries(getCurrentProject(), getLimit(), getFiltro());
        getModel().addSeries(series);

        setPairs(new ArrayList<Pair>());
        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            getPairs().add(new Pair(key, value));
        }

        Collections.reverse(getPairs());
    }
}
