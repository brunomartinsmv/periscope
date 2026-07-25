package br.ufmt.periscope.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.bson.types.ObjectId;

@Entity
public class Country implements Serializable, Comparable<Country> {

    private static final long serialVersionUID = 1L;

    @Id
    private ObjectId id;
    private String acronym;
    private String name;
    private List<State> states = new ArrayList<State>();

    public Country() {
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
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

    public String getName() {
        if (name != null) {
            name = name.toUpperCase();
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<State> getStates() {
        return states;
    }

    public void setStates(List<State> states) {
        this.states = states;
    }

    @Override
    public int compareTo(Country o) {
        int ret;
        ret = this.name.compareTo(o.name);
        if (ret == 0) {
            if (this.acronym != null) {
                return this.acronym.compareTo(o.acronym);
            }
        }
        return ret;
    }

}
