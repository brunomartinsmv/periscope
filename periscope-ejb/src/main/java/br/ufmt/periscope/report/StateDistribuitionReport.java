package br.ufmt.periscope.report;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.StateDistribuitionRepository;
import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import br.ufmt.periscope.report.ChartSeries;

@ApplicationScoped
@Named
public class StateDistribuitionReport {

    private @Inject
    StateDistribuitionRepository repo;

    public ChartSeries StateDistribuitionSeries(Project currentProject) {
        ChartSeries series = new ChartSeries("Distribuição por Estados");
        List<Pair> i = repo.getStateDistribuitions(currentProject);
        Collections.reverse(i);

        for (Pair pair : i) {
            String state = (String) pair.getKey();
            Integer count = (Integer) pair.getValue();
            series.set(state, count);
        }

        return series;
    }

}