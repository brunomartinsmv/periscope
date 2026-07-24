package br.ufmt.periscope.controller;

import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.report.PriorityDateReport;
import java.util.ArrayList;
import java.util.Collections;
import jakarta.inject.Named;
import jakarta.inject.Inject;
import br.ufmt.periscope.compat.chart.CartesianChartModel;
import br.ufmt.periscope.compat.chart.ChartSeries;

/**
 * - @Named<BR/>
 * - @ViewScoped<BR/>
 * Classe controller responsável por operações de visualização relacionadas à data de prioridade das patentes
 */
@Named
public class PriorityDateController extends GenericController {

    private @Inject
    PriorityDateReport report;

    /**
     * Método responsável pela atualização dos gráficos relacionados à data de prioridade
     */
    @Override
    public void refreshChart() {
        setModel(new CartesianChartModel());
        ChartSeries series = report.priorityDateSeries(getCurrentProject(), getFiltro());
        getModel().addSeries(series);

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
    }
}
