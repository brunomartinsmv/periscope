package br.ufmt.periscope.controller;

import java.util.ArrayList;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;

import br.ufmt.periscope.compat.chart.CartesianChartModel;
import br.ufmt.periscope.compat.chart.ChartSeries;

import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.report.PublicationDateReport;
import br.ufmt.periscope.util.PDFTextParser;
import java.util.Collections;

/**
 * - @Named<BR/>
 * - @ViewScoped<BR/>
 * Classe controller responsável por operações de visualização relacionadas à data de publicação das patentes
 */
@Named("publicantDateReport")
@ViewScoped
public class PublicationDateController extends GenericController {

    private @Inject
    PublicationDateReport report;

    /**
     *  Método responsável pela atualização dos gráficos relacionados à data de publicação das patentes
     */
    @Override
    public void refreshChart() {
        setModel(new CartesianChartModel());
        ChartSeries series = report.publicationDateSeries(getCurrentProject(), getFiltro());
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
