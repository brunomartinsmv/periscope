package br.ufmt.periscope.controller;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.qualifier.CurrentProject;
import br.ufmt.periscope.report.ChartSeries;
import br.ufmt.periscope.report.Pair;
import br.ufmt.periscope.repository.PatentRepository;
import br.ufmt.periscope.util.Filters;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.faces.event.ValueChangeEvent;
import jakarta.inject.Inject;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.line.LineChartModel;

/**
 * Modelo abstrato generico dos controllers.<BR/>
 * Quase todos os controllers de gráficos do sistema extendem essa classe.
 */
public abstract class GenericController implements Serializable {

    private static final long serialVersionUID = 1L;

    private @Inject
    @CurrentProject
    Project currentProject;
    private @Inject
    PatentRepository patentRepository;
    private BarChartModel barModel = new BarChartModel();
    private LineChartModel lineModel = new LineChartModel();
    private boolean chartEmpty = true;
    private Date minDate, maxDate;
    private List<Pair> pairs;
    private @Inject
    Filters filtro;
    private int limit = 5;

    /**
     * Método pós construtor da classe abstrata generica dos controllers.<BR/>
     * Atualiza os parametros do filtro, o intervalo de datas existentes no projeto e o gráfico sendo mostrado atualmente.
     */
    @PostConstruct
    public void init() {
        setMinDate(getPatentRepository().getMinDate(getCurrentProject(), 1));
        setMaxDate(getPatentRepository().getMaxDate(getCurrentProject(), 1));
        getFiltro().setComplete(false);
        getFiltro().setSelecionaData(1);
        getFiltro().setInicio(getMinDate());
        getFiltro().setFim(getMaxDate());

        refreshChart();
    }

    /**
     * Método abstrato que atualiza os gráficos do sistema.
     */
    public abstract void refreshChart();

    /**
     * Builds a horizontal or vertical bar chart from a report series.
     */
    protected void applyBarChart(ChartSeries series, String title, boolean horizontal) {
        this.barModel = ChartModels.bar(series, title, horizontal);
        this.chartEmpty = ChartModels.isEmpty(series);
    }

    /**
     * Builds a line chart from a report series.
     */
    protected void applyLineChart(ChartSeries series, String title) {
        this.lineModel = ChartModels.line(series, title);
        this.chartEmpty = ChartModels.isEmpty(series);
    }

    public PatentRepository getPatentRepository() {
        return patentRepository;
    }

    public void setPatentRepository(PatentRepository patentRepository) {
        this.patentRepository = patentRepository;
    }

    public Date getMinDate() {
        return minDate;
    }

    public void setMinDate(Date minDate) {
        this.minDate = minDate;
    }

    public Date getMaxDate() {
        return maxDate;
    }

    public void setMaxDate(Date maxDate) {
        this.maxDate = maxDate;
    }

    public Project getCurrentProject() {
        return currentProject;
    }

    public void setCurrentProject(Project currentProject) {
        this.currentProject = currentProject;
    }

    public BarChartModel getBarModel() {
        return barModel;
    }

    public void setBarModel(BarChartModel barModel) {
        this.barModel = barModel != null ? barModel : new BarChartModel();
    }

    public LineChartModel getLineModel() {
        return lineModel;
    }

    public void setLineModel(LineChartModel lineModel) {
        this.lineModel = lineModel != null ? lineModel : new LineChartModel();
    }

    public boolean isChartEmpty() {
        return chartEmpty;
    }

    public void setChartEmpty(boolean empty) {
        this.chartEmpty = empty;
    }

    public boolean isHasData() {
        return !chartEmpty;
    }

    public List<Pair> getPairs() {
        return pairs;
    }

    public void setPairs(List<Pair> pairs) {
        this.pairs = pairs;
    }

    public Filters getFiltro() {
        return filtro;
    }

    public void setFiltro(Filters filtro) {
        this.filtro = filtro;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * Filter's change listener.
     */
    public void selectListener(ValueChangeEvent event) {
        int newSel = (Integer) event.getNewValue();
        getFiltro().setInicio(getPatentRepository().getMinDate(getCurrentProject(), newSel));
        getFiltro().setFim(getPatentRepository().getMaxDate(getCurrentProject(), newSel));
        setMinDate(getFiltro().getInicio());
        setMaxDate(getFiltro().getFim());
    }
}
