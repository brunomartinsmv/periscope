package br.ufmt.periscope.model;

import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import java.io.Serializable;

@Embedded
@Entity
public class ApplicantType implements Serializable, Comparable<ApplicantType> {

    private String name;

    public ApplicantType() {

    }

    public ApplicantType(String name) {
        this.name = name;
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
