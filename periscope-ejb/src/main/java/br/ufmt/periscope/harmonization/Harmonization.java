package br.ufmt.periscope.harmonization;

import br.ufmt.periscope.indexer.PatentIndexer;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Rule;
import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.query.updates.UpdateOperators;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.util.ArrayList;
import org.bson.types.ObjectId;

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
        ObjectId projectId = rule.getProject().getId();
        if (rule.getCountry() != null) {
            rule.getCountry().setStates(null);
        }
        String[] applicants = rule.getSubstitutions().toArray(new String[0]);
        for (String applicant : applicants) {
            ds.find(Patent.class)
                    .filter(eq("project.$id", projectId), eq("applicants.name", applicant))
                    .update(new UpdateOptions().multi(true),
                            UpdateOperators.set("applicants.$.name", rule.getName()),
                            UpdateOperators.set("applicants.$.harmonized", true),
                            UpdateOperators.set("applicants.$.acronym", rule.getAcronym()),
                            UpdateOperators.set("applicants.$.nature", rule.getNature()),
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
        ObjectId projectId = rule.getProject().getId();
        if (rule.getCountry() != null) {
            rule.getCountry().setStates(null);
        }
        String[] inventors = rule.getSubstitutions().toArray(new String[0]);
        for (String inventor : inventors) {
            ds.find(Patent.class)
                    .filter(eq("project.$id", projectId), eq("inventors.name", inventor))
                    .update(new UpdateOptions().multi(true),
                            UpdateOperators.set("inventors.$.name", rule.getName()),
                            UpdateOperators.set("inventors.$.harmonized", true),
                            UpdateOperators.set("inventors.$.country", rule.getCountry()),
                            UpdateOperators.set("inventors.$.state", rule.getState()),
                            UpdateOperators.set("inventors.$.nature", rule.getNature()),
                            UpdateOperators.set("inventors.$.acronym", rule.getAcronym()));
        }

        indexer.indexRule(null, null, new ArrayList<String>(rule.getSubstitutions()),
                rule.getName(), rule.getProject());
    }
}
