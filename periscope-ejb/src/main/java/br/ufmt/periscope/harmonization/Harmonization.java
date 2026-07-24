package br.ufmt.periscope.harmonization;

import br.ufmt.periscope.indexer.PatentIndexer;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.query.updates.UpdateOperators;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.util.ArrayList;

import static dev.morphia.query.filters.Filters.eq;

@ApplicationScoped
@Named
/**
 * Realiza a harmonização na base de dados, tanto para inventores quanto
 * para aplicante.
 */
public class Harmonization {

    private @Inject
    Datastore ds;
    private @Inject
    PatentIndexer indexer;

    /**
     * Aplica a regra de harmonização
     * @param rule
     */
    public void applyRule(Rule rule) {
        if (rule == null) {
            return;
        }
        switch (rule.getType()) {
            case APPLICANT:
                applyApplicantRule(rule);
                break;
            case INVENTOR:
                applyInventorRule(rule);
                break;
            default:
                break;

        }
    }

    /**
     * Aplica a regra para aplicante
     * @param rule
     */
    private void applyApplicantRule(Rule rule) {
        Project project = rule.getProject();
        if (rule.getCountry() != null) {
            rule.getCountry().setStates(null);
        }
        String[] applicants = rule.getSubstitutions().toArray(new String[0]);
        for (String applicant : applicants) {
            ds.find(Patent.class)
                    .filter(eq("project", project), eq("applicants.name", applicant))
                    .update(new UpdateOptions().multi(true),
                            UpdateOperators.set("applicants.$.name", rule.getName()),
                            UpdateOperators.set("applicants.$.harmonized", true),
                            UpdateOperators.set("applicants.$.acronym", rule.getAcronym()),
                            // Applicant uses "type" (ApplicantType); Rule still stores it as "nature"
                            UpdateOperators.set("applicants.$.type", rule.getNature()),
                            UpdateOperators.set("applicants.$.country", rule.getCountry()),
                            UpdateOperators.set("applicants.$.state", rule.getState()));
        }
        indexer.indexRule(new ArrayList<String>(rule.getSubstitutions()), rule.getName(),
                null, null, rule.getProject());
    }

    /**
     * Aplica a regra para inventores
     * @param rule
     */
    private void applyInventorRule(Rule rule) {
        Project project = rule.getProject();
        if (rule.getCountry() != null) {
            rule.getCountry().setStates(null);
        }
        String[] inventors = rule.getSubstitutions().toArray(new String[0]);
        for (String inventor : inventors) {
            ds.find(Patent.class)
                    .filter(eq("project", project), eq("inventors.name", inventor))
                    .update(new UpdateOptions().multi(true),
                            UpdateOperators.set("inventors.$.name", rule.getName()),
                            UpdateOperators.set("inventors.$.harmonized", true),
                            UpdateOperators.set("inventors.$.country", rule.getCountry()),
                            UpdateOperators.set("inventors.$.state", rule.getState()),
                            // Inventor has no nature/type field — only name/acronym/geo/harmonized
                            UpdateOperators.set("inventors.$.acronym", rule.getAcronym()));
        }

        indexer.indexRule(null, null, new ArrayList<String>(rule.getSubstitutions()),
                rule.getName(), rule.getProject());
    }
}
