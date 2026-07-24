package br.ufmt.periscope.report;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.NatureApplicantRepository;
import br.ufmt.periscope.util.Filters;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import br.ufmt.periscope.compat.chart.ChartSeries;

@ApplicationScoped
@Named
public class MainNatureApplicantReport {

    private @Inject
    NatureApplicantRepository repo;

    public ChartSeries NatureApplicantSeries(Project currentProject, Filters filtro) {
        ChartSeries series = new ChartSeries("Natureza dos Depositantes");

        List<Pair> i = repo.getNatureApplicantRepository(currentProject, filtro);

        Collections.reverse(i);
        Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
        ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);

        for (Pair pair : i) {

            String nature = bundle.getString((String) pair.getKey());
            Integer count = (Integer) pair.getValue();
            series.set(nature, count);
        }

        return series;
    }

}