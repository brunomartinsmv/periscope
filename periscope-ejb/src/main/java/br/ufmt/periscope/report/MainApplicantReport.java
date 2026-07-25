package br.ufmt.periscope.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import br.ufmt.periscope.compat.chart.ChartSeries;

import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.repository.ApplicantRepository;
import br.ufmt.periscope.util.Filters;

import dev.morphia.Datastore;
import org.bson.Document;

@ApplicationScoped
@Named
public class MainApplicantReport {

    private @Inject
    ApplicantRepository repo;
    private @Inject
    Datastore ds;

    public ChartSeries mainApplicantSeries(Project currentProject, int limit, Filters filtro) {
        ChartSeries series = new ChartSeries("Número de Depositos");
        repo.updateMainApplicants(currentProject, filtro);
        List<Document> it = ds.getDatabase()
                .getCollection("mainApplicant")
                .find()
                .sort(new Document("value", -1))
                .limit(limit)
                .into(new ArrayList<Document>());

        Collections.reverse(it);

        for (Document obj : it) {
            String name = (String) obj.get("_id");
            Number count = (Number) obj.get("value");
            series.set(name, count.intValue());
        }

        return series;
    }

    public ApplicantRepository getRepo() {
        return repo;
    }

}
