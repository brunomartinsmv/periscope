package br.ufmt.periscope.util;

import br.ufmt.periscope.model.Applicant;
import br.ufmt.periscope.model.ApplicantType;
import br.ufmt.periscope.model.Classification;
import br.ufmt.periscope.model.Country;
import br.ufmt.periscope.model.Files;
import br.ufmt.periscope.model.History;
import br.ufmt.periscope.model.Inventor;
import br.ufmt.periscope.model.Patent;
import br.ufmt.periscope.model.Priority;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.Rule;
import br.ufmt.periscope.model.State;
import br.ufmt.periscope.model.User;
import com.mongodb.client.gridfs.GridFSBucket;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Factory CDI para instâncias do modelo e GridFSBucket.
 */
@Named
public class ResourcesLazy {

    private @Inject Instance<Applicant> applicantProvider;
    private @Inject Instance<ApplicantType> applicantTypeProvider;
    private @Inject Instance<Classification> classificationProvider;
    private @Inject Instance<Country> countryProvider;
    private @Inject Instance<Files> filesProvider;
    private @Inject Instance<History> historyProvider;
    private @Inject Instance<Inventor> inventorProvider;
    private @Inject Instance<Patent> patentProvider;
    private @Inject Instance<Priority> priorityProvider;
    private @Inject Instance<Project> projectProvider;
    private @Inject Instance<Rule> ruleProvider;
    private @Inject Instance<State> stateProvider;
    private @Inject Instance<User> userProvider;
    private @Inject Instance<GridFSBucket> fsProvider;

    public Applicant getApplicant() {
        return applicantProvider.get();
    }

    public ApplicantType getApplicantType() {
        return applicantTypeProvider.get();
    }

    public Classification getClassification() {
        return classificationProvider.get();
    }

    public Country getCountry() {
        return countryProvider.get();
    }

    public Files getFiles() {
        return filesProvider.get();
    }

    public History getHistory() {
        return historyProvider.get();
    }

    public Inventor getInventor() {
        return inventorProvider.get();
    }

    public Patent getPatent() {
        return patentProvider.get();
    }

    public Priority getPriority() {
        return priorityProvider.get();
    }

    public Project getProject() {
        return projectProvider.get();
    }

    public Rule getRule() {
        return ruleProvider.get();
    }

    public State getState() {
        return stateProvider.get();
    }

    public User getUser() {
        return userProvider.get();
    }

    public GridFSBucket getFS() {
        return fsProvider.get();
    }
}
