package br.ufmt.periscope.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.Transient;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bson.types.ObjectId;

@Entity
public class Rule implements Serializable {

    @Id
    private ObjectId id;
    private String name;
    private String acronym;

    private Set<String> substitutions;

    private Country country;

    private State state;

    private ApplicantType nature;
    private RuleType type;

    @Transient
    private List<Applicant> appSugestions;
    @Transient
    private List<Inventor> invSugestions;

    @Reference
    private Project project;

    public Rule() {
        country = new Country();
        state = new State();
        nature = new ApplicantType();
        substitutions = new HashSet<String>();
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        if (name != null) {
            name = name.toUpperCase();
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAcronym() {
        if (acronym != null) {
            acronym = acronym.toUpperCase();
        }
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public Set<String> getSubstitutions() {
        return substitutions;
    }

    public void setSubstitutions(Set<String> substitutions) {
        this.substitutions = substitutions;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public ApplicantType getNature() {
        if (nature == null) {
            nature = new ApplicantType();
        }
        return nature;
    }

    public void setNature(ApplicantType nature) {
        this.nature = nature;
    }

    public RuleType getType() {
        return type;
    }

    public void setType(RuleType type) {
        this.type = type;
    }

    public Project getProject() {
        return project;
    }

    public State getState() {
        if (state == null) {
            state = new State();
        }
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public List<Applicant> getAppSugestions() {
        return appSugestions;
    }

    public void setAppSugestions(List<Applicant> appSugestions) {
        this.appSugestions = appSugestions;
    }

    public List<Inventor> getInvSugestions() {
        return invSugestions;
    }

    public void setInvSugestions(List<Inventor> invSugestions) {
        this.invSugestions = invSugestions;
    }

}
