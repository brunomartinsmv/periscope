package br.ufmt.periscope.controller;

import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.report.StateDistribuitionReport;
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
 * Classe controller responsável por operações de visualizaçao relacionadas à distribuição estadual das patentes
 */
@Named
@ViewScoped
public class StateDistribuitionController extends GenericController {

    private @Inject
    StateDistribuitionReport report;
    private String chartStyle;

    /**
     * Método responsável por atualizar os gráficos relacionados à distribuição estadual das patentes
     */
    @Override
    public void refreshChart() {

        setModel(new CartesianChartModel());
        ChartSeries series = report.StateDistribuitionSeries(getCurrentProject());

        getModel().addSeries(series);

        setPairs(new ArrayList<Pair>());

        for (Object key : series.getData().keySet()) {
            Number value = series.getData().get(key);
            getPairs().add(new Pair(key, value));
        }

        Integer tam = series.getData().size();
        System.out.println(tam);
        setChartStyle("height :" + tam * 50 + "px");
        Collections.reverse(getPairs());
    }

    /**
     *
     * @return
     */
    public String getChartStyle() {
        return chartStyle;
    }

    /**
     *
     * @param chartStyle
     */
    public void setChartStyle(String chartStyle) {
        this.chartStyle = chartStyle;
    }

}
