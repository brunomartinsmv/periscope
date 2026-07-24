package br.ufmt.periscope.report;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.PriorityCountryRepository;
import br.ufmt.periscope.util.Filters;
import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import br.ufmt.periscope.compat.chart.ChartSeries;

@Named
public class PriorityCountryReport {

    private @Inject
    PriorityCountryRepository repo;

    public ChartSeries mainPriorityCountrySeries(Project currentProject, int limit, Filters filtro) {
        ChartSeries series = new ChartSeries("Países de Prioridade");

        List<Pair> i = repo.getPriorities(currentProject, limit, filtro);

        Collections.reverse(i);

        for (Pair pair : i) {
            String country = (String) pair.getKey();
            Integer count = (Integer) pair.getValue();
            series.set(country, count);
        }

        return series;
    }
}
