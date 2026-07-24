package br.ufmt.periscope.report;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import br.ufmt.periscope.compat.chart.ChartSeries;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.ApplicationDateRepository;
import br.ufmt.periscope.util.Filters;
import java.util.Collections;

@ApplicationScoped
@Named
public class ApplicationDateReport {

    private @Inject
    ApplicationDateRepository repo;

    public ChartSeries applicationDateSeries(Project currentProject, Filters filtro) {
        ChartSeries series = new ChartSeries("Depositos por ano");

        List<Pair> i = repo.getApplicationsByDate(currentProject, filtro);

        Collections.reverse(i);

        for (Pair pair : i) {
            Integer year = Integer.parseInt((String) pair.getKey());
            Integer count = (Integer) pair.getValue();
            series.set(year, count);
        }

        return series;
    }

}
