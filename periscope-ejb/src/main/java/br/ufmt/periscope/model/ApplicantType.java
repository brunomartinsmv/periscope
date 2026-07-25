package br.ufmt.periscope.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import java.io.Serializable;
import org.bson.types.ObjectId;

@Entity
public class ApplicantType implements Serializable, Comparable<ApplicantType> {

    private static final long serialVersionUID = 1L;

    @Id
    private ObjectId id;
    private String name;

    public ApplicantType() {

    }

    public ApplicantType(String name) {
        this.name = name;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(ApplicantType o) {
        return this.name.compareTo(o.name);
    }
}
